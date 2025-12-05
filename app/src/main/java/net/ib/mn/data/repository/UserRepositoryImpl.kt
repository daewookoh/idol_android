package net.ib.mn.data.repository

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.remote.api.UserApi
import net.ib.mn.data.remote.dto.*
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import net.ib.mn.util.Constants
import net.ib.mn.util.DeviceUtil
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import net.ib.mn.util.logW
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * User Repository 구현체
 */
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context,
    private val deviceUtil: DeviceUtil,
    private val idolDao: IdolDao,
    private val userCacheRepository: UserCacheRepository
) : UserRepository {

    override fun getUserSelf(etag: String?, cacheControl: String?, timestamp: Int?): Flow<ApiResult<UserSelfResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // timestamp가 있으면 캐시 무효화를 위해 사용, 없으면 DataStore의 ts 사용
            val userInfo = preferencesManager.userInfo.first()
            val ts = timestamp ?: (userInfo?.ts ?: 0)

            logD("USER_INFO", "[UserRepositoryImpl] ========================================")
            logD("USER_INFO", "[UserRepositoryImpl] Calling getUserSelf API")
            logD("USER_INFO", "[UserRepositoryImpl]   - UserInfo exists: ${userInfo != null}")
            logD("USER_INFO", "[UserRepositoryImpl]   - TS from DataStore: ${userInfo?.ts}")
            logD("USER_INFO", "[UserRepositoryImpl]   - Timestamp parameter: $timestamp")
            logD("USER_INFO", "[UserRepositoryImpl]   - TS to send: $ts")
            logD("USER_INFO", "[UserRepositoryImpl]   - ETag: $etag")
            logD("USER_INFO", "[UserRepositoryImpl]   - Cache-Control: $cacheControl")
            logD("USER_INFO", "[UserRepositoryImpl] ========================================")

            // AuthInterceptor가 자동으로 Authorization 헤더를 추가하므로 여기서는 제거
            val response = userApi.getUserSelf(ts, etag, cacheControl)

            // HTTP 304 Not Modified (캐시 유효)
            if (response.code() == 304) {
                // 캐시된 데이터 사용 - DataStore의 userInfo를 그대로 사용
                // 304는 데이터가 변경되지 않았음을 의미하므로 로컬 데이터가 최신 상태
                logD("UserRepositoryImpl", "📦 HTTP 304: Using cached data")

                emit(ApiResult.Error(ApiError.Http(
                    httpCode = 304,
                    message = "Cache valid - use local data",
                    exception = Exception("Cache valid - use local data")
                )))
                return@flow
            }

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // NOTE: UserSelfResponse 구조가 {objects: [...], ...} 형식이므로
                // success 필드가 없음. objects 배열이 있으면 성공으로 판단
                if (body.objects.isNotEmpty()) {
                    // 응답 데이터 로그 출력
                    val firstObject = body.objects.firstOrNull()
                    logD("USER_INFO", "[UserRepositoryImpl] ========================================")
                    logD("USER_INFO", "[UserRepositoryImpl] getUserSelf API Response:")
                    logD("USER_INFO", "[UserRepositoryImpl]   - User ID: ${firstObject?.id}")

                    if (firstObject?.most == null) {
                        logW("USER_INFO", "[UserRepositoryImpl]   ⚠️ Most is NULL - User has no favorite idol set")
                    } else {
                        logD("USER_INFO", "[UserRepositoryImpl]   - Most: ${firstObject.most}")
                        logD("USER_INFO", "[UserRepositoryImpl]   - Most ID: ${firstObject.most.id}")
                        logD("USER_INFO", "[UserRepositoryImpl]   - Most Name: ${firstObject.most.name}")
                        logD("USER_INFO", "[UserRepositoryImpl]   - Most Type: ${firstObject.most.type}")
                        logD("USER_INFO", "[UserRepositoryImpl]   - Most ChartCode: ${firstObject.most.chartCodes?.firstOrNull()}")
                        logD("USER_INFO", "[UserRepositoryImpl]   - Most Category: ${firstObject.most.category}")
                        logD("USER_INFO", "[UserRepositoryImpl]   - Most GroupId: ${firstObject.most.groupId}")
                    }
                    logD("USER_INFO", "[UserRepositoryImpl] ========================================")

                    // 새로운 ETag 저장 (cacheControl이 설정되지 않은 경우만, 즉 캐시를 사용하는 경우만)
                    if (cacheControl == null && timestamp == null) {
                        val newETag = response.headers()["ETag"]
                        newETag?.let {
                            preferencesManager.setUserSelfETag(it)
                            logD("UserRepositoryImpl", "✓ ETag saved: $it")
                        }
                    } else {
                        logD("UserRepositoryImpl", "⚠️ ETag not saved (cache disabled: cacheControl=$cacheControl, timestamp=$timestamp)")
                    }

                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(ApiError.Business(
                        gcode = 0,
                        message = "User data not found in response",
                        exception = Exception("User data not found in response")
                    )))
                }
            } else {
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    override fun getUserStatus(): Flow<ApiResult<UserStatusResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // AuthInterceptor가 자동으로 Authorization 헤더를 추가
            val response = userApi.getUserStatus()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(ApiError.Business(
                        gcode = 0,
                        message = "API returned success=false",
                        exception = Exception("API returned success=false")
                    )))
                }
            } else {
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    override fun getIabKey(): Flow<ApiResult<IabKeyResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // AuthInterceptor가 자동으로 Authorization 헤더를 추가
            val response = userApi.getIabKey()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(ApiError.Business(
                        gcode = 0,
                        message = "API returned success=false",
                        exception = Exception("API returned success=false")
                    )))
                }
            } else {
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    override fun getBlocks(): Flow<ApiResult<BlockListResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // AuthInterceptor가 자동으로 Authorization 헤더를 추가
            val response = userApi.getBlocks()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(ApiError.Business(
                        gcode = 0,
                        message = "API returned success=false",
                        exception = Exception("API returned success=false")
                    )))
                }
            } else {
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    override fun validateUser(
        type: String,
        value: String,
        appId: String
    ): Flow<ApiResult<ValidateResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // Old 프로젝트: mutableMapOf(type to value) 형태로 전송
            // 예: type="nickname", value="test" -> {"nickname": "test", "app_id": "appId"}
            // NOT: {"type": "nickname", "value": "test", "app_id": "appId"}
            val params = mutableMapOf<String, String?>(type to value)
            params["app_id"] = appId

            logD("ValidateUserAPI", "========================================")
            logD("ValidateUserAPI", "API Request Parameters:")
            logD("ValidateUserAPI", "  - type: $type")
            logD("ValidateUserAPI", "  - value: $value")
            logD("ValidateUserAPI", "  - appId: $appId")
            logD("ValidateUserAPI", "  - params: $params")
            logD("ValidateUserAPI", "========================================")

            val response = userApi.validate(params)

            logD("ValidateUserAPI", "========================================")
            logD("ValidateUserAPI", "API Response:")
            logD("ValidateUserAPI", "  - isSuccessful: ${response.isSuccessful}")
            logD("ValidateUserAPI", "  - code: ${response.code()}")
            logD("ValidateUserAPI", "  - errorBody: ${response.errorBody()?.string()}")
            logD("ValidateUserAPI", "  - body: ${response.body()}")
            logD("ValidateUserAPI", "========================================")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                logD("ValidateUserAPI", "========================================")
                logD("ValidateUserAPI", "Parsed Response Body:")
                logD("ValidateUserAPI", "  - success: ${body.success}")
                logD("ValidateUserAPI", "  - message: ${body.message}")
                logD("ValidateUserAPI", "  - domain: ${body.domain}")
                logD("ValidateUserAPI", "  - gcode: ${body.gcode}")
                logD("ValidateUserAPI", "  - mcode: ${body.mcode}")
                logD("ValidateUserAPI", "========================================")
                
                emit(ApiResult.Success(body))
            } else {
                val errorBody = response.errorBody()?.string()
                logE("ValidateUserAPI", "API Error: HTTP ${response.code()}")
                logE("ValidateUserAPI", "Error Body: $errorBody")
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    override fun signIn(
        domain: String,
        email: String,
        password: String,
        deviceKey: String,
        gmail: String,
        deviceId: String,
        appId: String
    ): Flow<ApiResult<SignInResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val request = SignInRequest(
                domain = domain,
                email = email,
                passwd = password,
                deviceKey = deviceKey,
                gmail = gmail,
                deviceId = deviceId,
                appId = appId
            )

            val response = userApi.signIn(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // 서버가 정상 응답한 경우 body.success 여부와 관계없이 Success로 emit
                // body.success=false는 비즈니스 로직상의 실패 (비밀번호 틀림, 점검 등)
                // gcode, mcode를 포함한 응답을 ViewModel에서 처리하도록 함
                emit(ApiResult.Success(body))
            } else {
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    override fun signUp(
        email: String,
        password: String,
        nickname: String,
        domain: String,
        recommenderCode: String,
        appId: String
    ): Flow<ApiResult<CommonResponse>> = flow {
        emit(ApiResult.Loading)

        // Domain에 따른 로그 태그 설정
        val signUpTag = when (domain) {
            Constants.DOMAIN_KAKAO -> "KAKAO_SIGNUP"
            Constants.DOMAIN_GOOGLE -> "GOOGLE_SIGNUP"
            Constants.DOMAIN_FACEBOOK -> "FACEBOOK_SIGNUP"
            else -> "SignUpAPI"
        }

        try {
            // Old 프로젝트: domain이 null이거나 email이면 비밀번호를 MD5 해싱
            var processedPassword = password
            var processedDomain = domain
            if (domain == Constants.DOMAIN_EMAIL || domain.isEmpty()) {
                processedPassword = md5salt(password) ?: password
                processedDomain = Constants.DOMAIN_EMAIL
            }

            // Device info
            val deviceId = deviceUtil.getDeviceUUID()
            val gmail = deviceUtil.getGmail()
            val deviceKey = preferencesManager.fcmToken.first() ?: ""
            // Old 프로젝트: getString(R.string.app_version) 사용
            val version = context.getString(net.ib.mn.R.string.app_version)
            val time = System.currentTimeMillis()

            // Signature 생성
            val signature = getSignature(time)

            // SignUpRequest 생성 (Old 프로젝트와 동일한 필드 순서)
            val request = SignUpRequest(
                domain = processedDomain,
                email = email,
                passwd = processedPassword,
                nickname = nickname,
                referralCode = recommenderCode.trim(),
                pushKey = deviceKey,
                gmail = gmail,
                version = version,
                appId = appId,
                deviceId = deviceId,
                googleAccount = "N", // Old 프로젝트: 모든 경우에 "N"
                time = time,
                facebookId = null  // 더 이상 사용하지 않음
            )

            logD(signUpTag, "========================================")
            logD(signUpTag, "SignUp API Request:")
            logD(signUpTag, "  domain: ${request.domain}")
            logD(signUpTag, "  email: ${request.email}")
            logD(signUpTag, "  passwd: ${request.passwd.take(20)}...")
            logD(signUpTag, "  nickname: ${request.nickname}")
            logD(signUpTag, "  referralCode: ${request.referralCode}")
            logD(signUpTag, "  pushKey: ${request.pushKey}")
            logD(signUpTag, "  gmail: ${request.gmail}")
            logD(signUpTag, "  version: ${request.version}")
            logD(signUpTag, "  googleAccount: ${request.googleAccount}")
            logD(signUpTag, "  time: ${request.time}")
            logD(signUpTag, "  appId: ${request.appId}")
            logD(signUpTag, "  deviceId: ${request.deviceId}")
            logD(signUpTag, "========================================")

            val response = userApi.signUp(signature, request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                logD(signUpTag, "========================================")
                logD(signUpTag, "SignUp API Response:")
                logD(signUpTag, "  success: ${body.success}")
                logD(signUpTag, "  message: ${body.message}")
                logD(signUpTag, "  gcode: ${body.gcode}")
                logD(signUpTag, "  mcode: ${body.mcode}")
                logD(signUpTag, "========================================")
                emit(ApiResult.Success(body))
            } else {
                val errorBody = response.errorBody()?.string()
                logE(signUpTag, "========================================")
                logE(signUpTag, "SignUp API Error: HTTP ${response.code()}")
                logE(signUpTag, "Error Body: $errorBody")
                logE(signUpTag, "========================================")
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            logE(signUpTag, "========================================")
            logE(signUpTag, "SignUp API HttpException", e)
            logE(signUpTag, "  code: ${e.code()}")
            logE(signUpTag, "  message: ${e.message}")
            logE(signUpTag, "========================================")
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            logE(signUpTag, "========================================")
            logE(signUpTag, "SignUp API IOException", e)
            logE(signUpTag, "  message: ${e.message}")
            logE(signUpTag, "========================================")
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            logE(signUpTag, "========================================")
            logE(signUpTag, "SignUp API Exception", e)
            logE(signUpTag, "  message: ${e.message}")
            logE(signUpTag, "========================================")
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    override fun findId(deviceId: String?): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)

        try {
            val response = userApi.findId(deviceId)

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val responseString = responseBody.string()

                try {
                    val jsonObject = org.json.JSONObject(responseString)
                    val idsArray = jsonObject.optJSONArray("ids") ?: org.json.JSONArray()
                    
                    if (idsArray.length() > 0) {
                        var ids = ""
                        for (i in 0 until idsArray.length()) {
                            val idInfo = idsArray.optJSONObject(i)
                            if (idInfo != null) {
                                val domain = idInfo.optString("domain", "")
                                ids += if (domain.equals("E", ignoreCase = true)) {
                                    // 이메일 도메인인 경우 email 반환
                                    idInfo.optString("email", "")
                                } else {
                                    // SNS 도메인인 경우 domain_desc 반환
                                    idInfo.optString("domain_desc", "")
                                }
                                ids += "\n"
                            }
                        }
                        ids = ids.trim()
                        emit(ApiResult.Success(ids))
                    } else {
                        // 아이디를 찾을 수 없음
                        emit(ApiResult.Success(""))
                    }
                } catch (e: org.json.JSONException) {
                    logE("FindIdAPI", "JSON parsing error", e)
                    emit(ApiResult.Error(ApiError.Unknown(
                        message = "Failed to parse response",
                        exception = e
                    )))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                logE("FindIdAPI", "HTTP ${response.code()}: $errorBody")
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            logE("FindIdAPI", "HttpException", e)
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: Exception) {
            logE("FindIdAPI", "Exception", e)
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    override fun findPassword(email: String): Flow<ApiResult<CommonResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val request = FindPasswordRequest(email = email)
            val response = userApi.findPassword(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                logD("FindPasswordAPI", "Success: ${body.success}, message: ${body.message}")
                emit(ApiResult.Success(body))
            } else {
                val errorBody = response.errorBody()?.string()
                logE("FindPasswordAPI", "HTTP ${response.code()}: $errorBody")
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            logE("FindPasswordAPI", "HttpException", e)
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: Exception) {
            logE("FindPasswordAPI", "Exception", e)
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    // ============================================================
    // Signature & Password Hashing (Old 프로젝트와 동일)
    // ============================================================

    private val EXOKEY = "dus-"

    /**
     * MD5 해싱 (Old 프로젝트의 md5salt 함수)
     */
    private fun md5salt(s: String): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val key = EXOKEY + s
            md.update(key.toByteArray(StandardCharsets.UTF_8), 0, key.length)
            val messageDigest = md.digest()
            val number = BigInteger(1, messageDigest)
            var md5 = number.toString(16)
            while (md5.length < 32) md5 = "0$md5"
            md5
        } catch (e: NoSuchAlgorithmException) {
            logE("SignUpAPI", "MD5 error", e)
            null
        }
    }

    /**
     * AES 암호화 (Old 프로젝트의 AES_Encode 함수)
     */
    private fun aesEncode(str: String, iv: ByteArray?): ByteArray {
        return try {
            val key = "FF7BF8C3B7C844C956B0344B71D166A49E5A19D7DBA9408E2D1E77A8340010CC"
            val tsBytes = str.toByteArray(StandardCharsets.UTF_8)
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(key.toByteArray())
            val newKey = SecretKeySpec(digest, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING")
            cipher.init(Cipher.ENCRYPT_MODE, newKey, IvParameterSpec(iv))
            cipher.doFinal(tsBytes)
        } catch (e: Exception) {
            logE("SignUpAPI", "AES encode error", e)
            ByteArray(0)
        }
    }

    /**
     * Signature 생성 (Old 프로젝트의 getSignature 함수)
     */
    private fun getSignature(time: Long): String {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val output = ByteArrayOutputStream()
        val trTime = time / 1000
        val sig = aesEncode(trTime.toString(), iv)
        try {
            output.write(iv)
            output.write(sig)
        } catch (e: IOException) {
            logE("SignUpAPI", "Signature write error", e)
        }
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * UserSelf 데이터를 로드하고 DataStore와 Local DB에 저장
     * StartUpViewModel의 loadUserSelf 로직을 Repository로 이동
     *
     * @param isInitialLoad 최초 로드 여부 (true일 때만 최애 성별로 defaultCategory 덮어쓰기)
     */
    override suspend fun loadAndSaveUserSelf(cacheControl: String?, isInitialLoad: Boolean): Result<Boolean> {
        return try {
            var shouldNavigateToLogin = false

            getUserSelf(
                etag = null,
                cacheControl = cacheControl,
                timestamp = (System.currentTimeMillis() / 1000).toInt()
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {}
                    is ApiResult.Success -> {
                        val data = result.data.objects.firstOrNull()

                        data?.let { userData ->
                            val userDomain = userData.domain ?: preferencesManager.loginDomain.first()
                            val savedLoginEmail = preferencesManager.loginEmail.first()
                            val emailToSave = savedLoginEmail ?: userData.email

                            preferencesManager.setUserInfo(
                                id = userData.id,
                                email = emailToSave,
                                username = userData.username,
                                nickname = userData.nickname,
                                profileImage = userData.profileImage,
                                heart = userData.heart,
                                diamond = userData.diamond,
                                strongHeart = userData.strongHeart,
                                weakHeart = userData.weakHeart,
                                level = userData.level,
                                levelHeart = userData.levelHeart,
                                power = userData.power,
                                resourceUri = userData.resourceUri,
                                pushKey = userData.pushKey,
                                createdAt = userData.createdAt,
                                pushFilter = userData.pushFilter,
                                statusMessage = userData.statusMessage,
                                ts = userData.ts,
                                itemNo = userData.itemNo,
                                domain = userDomain,
                                giveHeart = userData.giveHeart
                            )

                            // Cache user data in memory for quick access
                            userCacheRepository.setUserData(userData)

                            val chartCode = userData.most?.chartCodes
                                ?.firstOrNull { !it.startsWith("AW_") && !it.startsWith("DF_") }
                                ?: userData.most?.chartCodes?.firstOrNull()

                            val category = userData.most?.category

                            userData.most?.let { most ->
                                // 기존 idol 데이터가 있으면 필요한 필드만 업데이트
                                // fairyCount, anniversary, burningDay 등 badge 관련 필드는 기존 값 유지
                                val existingIdol = idolDao.getIdolById(most.id)
                                if (existingIdol != null) {
                                    val updatedIdol = existingIdol.copy(
                                        heart = most.heart ?: existingIdol.heart,
                                        imageUrl = most.imageUrl ?: existingIdol.imageUrl,
                                        imageUrl2 = most.imageUrl2 ?: existingIdol.imageUrl2,
                                        imageUrl3 = most.imageUrl3 ?: existingIdol.imageUrl3,
                                        name = most.name ?: existingIdol.name,
                                        nameEn = most.nameEn ?: existingIdol.nameEn,
                                        nameJp = most.nameJp ?: existingIdol.nameJp,
                                        nameZh = most.nameZh ?: existingIdol.nameZh,
                                        nameZhTw = most.nameZhTw ?: existingIdol.nameZhTw,
                                        type = most.type ?: existingIdol.type,
                                        category = most.category ?: existingIdol.category,
                                        groupId = most.groupId ?: existingIdol.groupId,
                                        resourceUri = most.resourceUri ?: existingIdol.resourceUri
                                        // fairyCount, angelCount, miracleCount, rookieCount,
                                        // anniversary, anniversaryDays, burningDay 등은 기존 값 유지
                                    )
                                    idolDao.upsert(updatedIdol)
                                } else {
                                    // 기존 데이터가 없으면 새로 생성
                                    idolDao.upsert(most.toEntity())
                                }
                            }

                            // 최초 로드 시에만 최애의 성별로 defaultCategory 설정
                            // 이후 로드에서는 사용자가 수동으로 선택한 성별을 존중
                            if (isInitialLoad && category != null) {
                                userCacheRepository.setDefaultCategory(category)
                                logD("USER_INFO", "[UserRepositoryImpl] ✓ Initial load: Setting defaultCategory to $category (favorite idol gender)")
                            } else if (!isInitialLoad && category != null) {
                                logD("USER_INFO", "[UserRepositoryImpl] ⚠️ Not initial load: Skipping defaultCategory update (user preference preserved)")
                            }

                            if (chartCode != null) {
                                preferencesManager.setDefaultChartCode(chartCode)
                                userCacheRepository.setDefaultChartCode(chartCode)
                            }

                            if (isInitialLoad) {
                                // 실서버일 경우 최애(MY_FAVORITE)를, 테스트서버일 경우 HOT(0)을 기본 탭으로 설정
                                val defaultTagId = if (net.ib.mn.util.ServerUrl.isTestServer()) {
                                    net.ib.mn.presentation.main.freeboard.FreeBoardContract.State.TAG_ID_HOT
                                } else {
                                    net.ib.mn.util.Constants.MY_FAVORITE_TAG_ID
                                }
                                preferencesManager.setFreeBoardSelectedTagId(defaultTagId)
                                logD("USER_INFO", "[UserRepositoryImpl] ✓ Initial load: Setting FreeBoardSelectedTagId to $defaultTagId (${if (net.ib.mn.util.ServerUrl.isTestServer()) "HOT - TestServer" else "MY_FAVORITE - Production"})")
                            }

                            kotlinx.coroutines.delay(100)
                        }
                    }
                    is ApiResult.Error -> {
                        if (result.code == 401) {
                            preferencesManager.clearAll()
                            shouldNavigateToLogin = true
                        }
                    }
                }
            }

            if (shouldNavigateToLogin) {
                Result.failure(Exception("Unauthorized"))
            } else {
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun checkEvent(
        version: String,
        gmail: String,
        isVM: Boolean,
        isRooted: Boolean,
        deviceId: String
    ): Flow<ApiResult<CheckEventResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // old 프로젝트와 동일한 is_vm 파라미터 변환
            // T: VM이면서 루팅됨, R: 루팅됨, F: 정상
            val paramVM = when {
                isVM && isRooted -> "T"
                isRooted -> "R"
                else -> "F"
            }

            logD("CheckEventAPI", "========================================")
            logD("CheckEventAPI", "API Request:")
            logD("CheckEventAPI", "  - version: $version")
            logD("CheckEventAPI", "  - gmail: $gmail")
            logD("CheckEventAPI", "  - isVM: $paramVM")
            logD("CheckEventAPI", "  - deviceId: $deviceId")
            logD("CheckEventAPI", "========================================")

            val response = userApi.checkEvent(version, gmail, paramVM, deviceId)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                logD("CheckEventAPI", "========================================")
                logD("CheckEventAPI", "API Response:")
                logD("CheckEventAPI", "  - success: ${body.success}")
                logD("CheckEventAPI", "  - showWelcomeMission: ${body.showWelcomeMission}")
                logD("CheckEventAPI", "  - mostPicks: ${body.mostPicks}")
                logD("CheckEventAPI", "  - guideUrl: ${body.guideUrl}")
                logD("CheckEventAPI", "  - banners count: ${body.banners?.size ?: 0}")
                logD("CheckEventAPI", "========================================")

                if (body.success) {
                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(ApiError.Business(
                        gcode = 0,
                        message = "API returned success=false",
                        exception = Exception("API returned success=false")
                    )))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                logE("CheckEventAPI", "HTTP ${response.code()}: $errorBody")
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            logE("CheckEventAPI", "HttpException", e)
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            logE("CheckEventAPI", "IOException", e)
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            logE("CheckEventAPI", "Exception", e)
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }
}
