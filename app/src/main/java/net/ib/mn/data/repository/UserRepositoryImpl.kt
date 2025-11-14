package net.ib.mn.data.repository

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.remote.api.UserApi
import net.ib.mn.data.remote.dto.*
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import net.ib.mn.util.Constants
import net.ib.mn.util.DeviceUtil
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
    private val userCacheRepository: UserCacheRepository,
    private val idolRepository: net.ib.mn.domain.repository.IdolRepository,
    private val idolApi: net.ib.mn.data.remote.api.IdolApi
) : UserRepository {

    override fun getUserSelf(etag: String?, cacheControl: String?, timestamp: Int?): Flow<ApiResult<UserSelfResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // timestamp가 있으면 캐시 무효화를 위해 사용, 없으면 DataStore의 ts 사용
            val userInfo = preferencesManager.userInfo.first()
            val ts = timestamp ?: (userInfo?.ts ?: 0)

            android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ========================================")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl] Calling getUserSelf API")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - UserInfo exists: ${userInfo != null}")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - TS from DataStore: ${userInfo?.ts}")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Timestamp parameter: $timestamp")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - TS to send: $ts")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - ETag: $etag")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Cache-Control: $cacheControl")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ========================================")

            // AuthInterceptor가 자동으로 Authorization 헤더를 추가하므로 여기서는 제거
            val response = userApi.getUserSelf(ts, etag, cacheControl)

            // HTTP 304 Not Modified (캐시 유효)
            if (response.code() == 304) {
                // 캐시된 데이터 사용 - DataStore의 userInfo를 그대로 사용
                // 304는 데이터가 변경되지 않았음을 의미하므로 로컬 데이터가 최신 상태
                android.util.Log.d("UserRepositoryImpl", "📦 HTTP 304: Using cached data")

                emit(ApiResult.Error(
                    exception = Exception("Cache valid - use local data"),
                    code = 304,
                    data = userInfo  // 캐시된 데이터를 함께 전달
                ))
                return@flow
            }

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // NOTE: UserSelfResponse 구조가 {objects: [...], ...} 형식이므로
                // success 필드가 없음. objects 배열이 있으면 성공으로 판단
                if (body.objects.isNotEmpty()) {
                    // 응답 데이터 로그 출력
                    val firstObject = body.objects.firstOrNull()
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ========================================")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl] getUserSelf API Full Response:")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Raw Response Body: $body")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - First Object: $firstObject")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ========================================")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl] getUserSelf API Response (Parsed):")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - User ID: ${firstObject?.id}")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Email: ${firstObject?.email}")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Username: ${firstObject?.username}")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Nickname: ${firstObject?.nickname}")
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Hearts: ${firstObject?.hearts}")

                    if (firstObject?.most == null) {
                        android.util.Log.w("USER_INFO", "[UserRepositoryImpl]   ⚠️ Most is NULL - User has no favorite idol set")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   🔍 Fetching SECRET_ROOM_IDOL (id=${Constants.SECRET_ROOM_IDOL_ID}) from API...")

                        // most가 없으면 SECRET_ROOM_IDOL_ID로 조회 (API 직접 호출)
                        try {
                            val idsString = listOf(Constants.SECRET_ROOM_IDOL_ID).joinToString(",")
                            val idolResponse = idolApi.getIdolsByIds(idsString, null)

                            if (idolResponse.isSuccessful && idolResponse.body() != null) {
                                val idolListResponse = idolResponse.body()!!
                                android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   📦 Response data: ${idolListResponse.data?.size} idols")

                                val secretIdol = idolListResponse.data?.firstOrNull()
                                if (secretIdol != null) {
                                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   ✓ SECRET_ROOM_IDOL fetched: ${secretIdol.name} (id=${secretIdol.id})")
                                    try {
                                        val idolEntity = secretIdol.toEntity()
                                        idolDao.upsert(idolEntity)
                                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   ✓ SECRET_ROOM_IDOL upserted to DB: ${secretIdol.name}")

                                        // UserCacheRepository에도 mostIdolId 설정
                                        val chartCode = secretIdol.resourceUri?.substringAfterLast("/")?.replace("/", "")
                                        userCacheRepository.setMostIdolId(
                                            idolId = secretIdol.id,
                                            category = secretIdol.category,
                                            chartCode = chartCode
                                        )
                                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   ✓ SECRET_ROOM_IDOL cached: id=${secretIdol.id}, category=${secretIdol.category}, chartCode=$chartCode")
                                    } catch (e: Exception) {
                                        android.util.Log.e("USER_INFO", "[UserRepositoryImpl]   ❌ Failed to upsert SECRET_ROOM_IDOL", e)
                                    }
                                } else {
                                    android.util.Log.w("USER_INFO", "[UserRepositoryImpl]   ⚠️ SECRET_ROOM_IDOL not found in API response")
                                }
                            } else {
                                android.util.Log.e("USER_INFO", "[UserRepositoryImpl]   ❌ Failed to fetch SECRET_ROOM_IDOL: HTTP ${idolResponse.code()}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("USER_INFO", "[UserRepositoryImpl]   ❌ Exception while fetching SECRET_ROOM_IDOL", e)
                        }
                    } else {
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ========================================")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl] Most Idol Data (Before DB Upsert):")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Raw Most Object: ${firstObject.most}")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Most ID: ${firstObject.most.id}")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Most Name: ${firstObject.most.name}")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Most Name EN: ${firstObject.most.nameEn}")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Most Type: ${firstObject.most.type}")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Most Category: ${firstObject.most.category}")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Most GroupId: ${firstObject.most.groupId}")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Most ChartCodes: ${firstObject.most.chartCodes}")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Most ImageUrl: ${firstObject.most.imageUrl}")
                        android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Most Heart: ${firstObject.most.heart}")

                        // most 데이터를 idols DB에 upsert
                        try {
                            val idolEntity = firstObject.most.toEntity()
                            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - Converted IdolEntity: $idolEntity")
                            idolDao.upsert(idolEntity)
                            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   ✓ Most idol upserted to DB: ${firstObject.most.name} (id=${firstObject.most.id})")

                            // UserCacheRepository에도 mostIdolId 설정
                            val chartCode = firstObject.most.resourceUri?.substringAfterLast("/")?.replace("/", "")
                            userCacheRepository.setMostIdolId(
                                idolId = firstObject.most.id,
                                category = firstObject.most.category,
                                chartCode = chartCode
                            )
                            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   ✓ Most idol cached: id=${firstObject.most.id}, category=${firstObject.most.category}, chartCode=$chartCode")
                        } catch (e: Exception) {
                            android.util.Log.e("USER_INFO", "[UserRepositoryImpl]   ❌ Failed to upsert most idol to DB", e)
                        }
                    }
                    android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ========================================")

                    // 새로운 ETag 저장 (cacheControl이 설정되지 않은 경우만, 즉 캐시를 사용하는 경우만)
                    if (cacheControl == null && timestamp == null) {
                        val newETag = response.headers()["ETag"]
                        newETag?.let {
                            preferencesManager.setUserSelfETag(it)
                            android.util.Log.d("UserRepositoryImpl", "✓ ETag saved: $it")
                        }
                    } else {
                        android.util.Log.d("UserRepositoryImpl", "⚠️ ETag not saved (cache disabled: cacheControl=$cacheControl, timestamp=$timestamp)")
                    }

                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(
                        exception = Exception("User data not found in response"),
                        code = response.code()
                    ))
                }
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
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
                    emit(ApiResult.Error(
                        exception = Exception("API returned success=false"),
                        code = response.code()
                    ))
                }
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
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
                    emit(ApiResult.Error(
                        exception = Exception("API returned success=false"),
                        code = response.code()
                    ))
                }
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
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
                    emit(ApiResult.Error(
                        exception = Exception("API returned success=false"),
                        code = response.code()
                    ))
                }
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
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

            android.util.Log.d("ValidateUserAPI", "========================================")
            android.util.Log.d("ValidateUserAPI", "API Request Parameters:")
            android.util.Log.d("ValidateUserAPI", "  - type: $type")
            android.util.Log.d("ValidateUserAPI", "  - value: $value")
            android.util.Log.d("ValidateUserAPI", "  - appId: $appId")
            android.util.Log.d("ValidateUserAPI", "  - params: $params")
            android.util.Log.d("ValidateUserAPI", "========================================")

            val response = userApi.validate(params)

            android.util.Log.d("ValidateUserAPI", "========================================")
            android.util.Log.d("ValidateUserAPI", "API Response:")
            android.util.Log.d("ValidateUserAPI", "  - isSuccessful: ${response.isSuccessful}")
            android.util.Log.d("ValidateUserAPI", "  - code: ${response.code()}")
            android.util.Log.d("ValidateUserAPI", "  - errorBody: ${response.errorBody()?.string()}")
            android.util.Log.d("ValidateUserAPI", "  - body: ${response.body()}")
            android.util.Log.d("ValidateUserAPI", "========================================")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                android.util.Log.d("ValidateUserAPI", "========================================")
                android.util.Log.d("ValidateUserAPI", "Parsed Response Body:")
                android.util.Log.d("ValidateUserAPI", "  - success: ${body.success}")
                android.util.Log.d("ValidateUserAPI", "  - message: ${body.message}")
                android.util.Log.d("ValidateUserAPI", "  - domain: ${body.domain}")
                android.util.Log.d("ValidateUserAPI", "  - gcode: ${body.gcode}")
                android.util.Log.d("ValidateUserAPI", "  - mcode: ${body.mcode}")
                android.util.Log.d("ValidateUserAPI", "========================================")
                
                emit(ApiResult.Success(body))
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("ValidateUserAPI", "API Error: HTTP ${response.code()}")
                android.util.Log.e("ValidateUserAPI", "Error Body: $errorBody")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
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
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
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

            android.util.Log.d(signUpTag, "========================================")
            android.util.Log.d(signUpTag, "SignUp API Request:")
            android.util.Log.d(signUpTag, "  domain: ${request.domain}")
            android.util.Log.d(signUpTag, "  email: ${request.email}")
            android.util.Log.d(signUpTag, "  passwd: ${request.passwd.take(20)}...")
            android.util.Log.d(signUpTag, "  nickname: ${request.nickname}")
            android.util.Log.d(signUpTag, "  referralCode: ${request.referralCode}")
            android.util.Log.d(signUpTag, "  pushKey: ${request.pushKey}")
            android.util.Log.d(signUpTag, "  gmail: ${request.gmail}")
            android.util.Log.d(signUpTag, "  version: ${request.version}")
            android.util.Log.d(signUpTag, "  googleAccount: ${request.googleAccount}")
            android.util.Log.d(signUpTag, "  time: ${request.time}")
            android.util.Log.d(signUpTag, "  appId: ${request.appId}")
            android.util.Log.d(signUpTag, "  deviceId: ${request.deviceId}")
            android.util.Log.d(signUpTag, "========================================")

            val response = userApi.signUp(signature, request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                android.util.Log.d(signUpTag, "========================================")
                android.util.Log.d(signUpTag, "SignUp API Response:")
                android.util.Log.d(signUpTag, "  success: ${body.success}")
                android.util.Log.d(signUpTag, "  message: ${body.message}")
                android.util.Log.d(signUpTag, "  gcode: ${body.gcode}")
                android.util.Log.d(signUpTag, "  mcode: ${body.mcode}")
                android.util.Log.d(signUpTag, "========================================")
                emit(ApiResult.Success(body))
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e(signUpTag, "========================================")
                android.util.Log.e(signUpTag, "SignUp API Error: HTTP ${response.code()}")
                android.util.Log.e(signUpTag, "Error Body: $errorBody")
                android.util.Log.e(signUpTag, "========================================")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e(signUpTag, "========================================")
            android.util.Log.e(signUpTag, "SignUp API HttpException", e)
            android.util.Log.e(signUpTag, "  code: ${e.code()}")
            android.util.Log.e(signUpTag, "  message: ${e.message}")
            android.util.Log.e(signUpTag, "========================================")
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message}"
            ))
        } catch (e: IOException) {
            android.util.Log.e(signUpTag, "========================================")
            android.util.Log.e(signUpTag, "SignUp API IOException", e)
            android.util.Log.e(signUpTag, "  message: ${e.message}")
            android.util.Log.e(signUpTag, "========================================")
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e(signUpTag, "========================================")
            android.util.Log.e(signUpTag, "SignUp API Exception", e)
            android.util.Log.e(signUpTag, "  message: ${e.message}")
            android.util.Log.e(signUpTag, "========================================")
            emit(ApiResult.Error(
                exception = e,
                message = "Sign up error: ${e.message}"
            ))
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
                    android.util.Log.e("FindIdAPI", "JSON parsing error", e)
                    emit(ApiResult.Error(
                        exception = e,
                        message = "Failed to parse response"
                    ))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("FindIdAPI", "HTTP ${response.code()}: $errorBody")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("FindIdAPI", "HttpException", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code()
            ))
        } catch (e: Exception) {
            android.util.Log.e("FindIdAPI", "Exception", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Find ID error: ${e.message}"
            ))
        }
    }

    override fun findPassword(email: String): Flow<ApiResult<CommonResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val request = FindPasswordRequest(email = email)
            val response = userApi.findPassword(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                android.util.Log.d("FindPasswordAPI", "Success: ${body.success}, message: ${body.message}")
                emit(ApiResult.Success(body))
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("FindPasswordAPI", "HTTP ${response.code()}: $errorBody")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("FindPasswordAPI", "HttpException", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code()
            ))
        } catch (e: Exception) {
            android.util.Log.e("FindPasswordAPI", "Exception", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Find password error: ${e.message}"
            ))
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
            android.util.Log.e("SignUpAPI", "MD5 error", e)
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
            android.util.Log.e("SignUpAPI", "AES encode error", e)
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
            android.util.Log.e("SignUpAPI", "Signature write error", e)
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
                                hearts = userData.hearts,
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
                                val idolEntity = most.toEntity()
                                idolDao.upsert(idolEntity)
                            }

                            // 최초 로드 시에만 최애의 성별로 defaultCategory 설정
                            // 이후 로드에서는 사용자가 수동으로 선택한 성별을 존중
                            if (isInitialLoad && category != null) {
                                userCacheRepository.setDefaultCategory(category)
                                android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ✓ Initial load: Setting defaultCategory to $category (favorite idol gender)")
                            } else if (!isInitialLoad && category != null) {
                                android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ⚠️ Not initial load: Skipping defaultCategory update (user preference preserved)")
                            }

                            if (chartCode != null) {
                                preferencesManager.setDefaultChartCode(chartCode)
                                userCacheRepository.setDefaultChartCode(chartCode)
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
}
