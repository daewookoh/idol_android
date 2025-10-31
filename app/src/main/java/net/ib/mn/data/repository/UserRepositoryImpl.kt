package net.ib.mn.data.repository

import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.api.UserApi
import net.ib.mn.data.remote.dto.*
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * User Repository 구현체
 */
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val preferencesManager: PreferencesManager
) : UserRepository {

    override fun getUserSelf(etag: String?): Flow<ApiResult<UserSelfResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // Get ts (timestamp) from DataStore's UserInfo
            // NOTE: old 프로젝트와 동일하게 ts parameter 전달
            val userInfo = preferencesManager.userInfo.first()
            val ts = userInfo?.ts ?: 0

            android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ========================================")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl] Calling getUserSelf API")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - UserInfo exists: ${userInfo != null}")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - TS from DataStore: ${userInfo?.ts}")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - TS to send: $ts")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl]   - ETag: $etag")
            android.util.Log.d("USER_INFO", "[UserRepositoryImpl] ========================================")

            // AuthInterceptor가 자동으로 Authorization 헤더를 추가하므로 여기서는 제거
            val response = userApi.getUserSelf(ts, etag)

            // HTTP 304 Not Modified (캐시 유효)
            if (response.code() == 304) {
                // 캐시된 데이터 사용 - DataStore의 userInfo를 그대로 사용
                // 304는 데이터가 변경되지 않았음을 의미하므로 로컬 데이터가 최신 상태
                emit(ApiResult.Error(
                    exception = Exception("Cache valid - use local data"),
                    code = 304
                ))
                return@flow
            }

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // NOTE: UserSelfResponse 구조가 {objects: [...], ...} 형식이므로
                // success 필드가 없음. objects 배열이 있으면 성공으로 판단
                if (body.objects.isNotEmpty()) {
                    // 새로운 ETag 저장
                    val newETag = response.headers()["ETag"]
                    newETag?.let {
                        preferencesManager.setUserSelfETag(it)
                        android.util.Log.d("UserRepositoryImpl", "✓ ETag saved: $it")
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
            val params = mapOf(
                "type" to type,
                "value" to value,
                "app_id" to appId
            )

            val response = userApi.validate(params)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
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

        try {
            // TODO: 실제 API 구현 필요
            // old 프로젝트의 UsersApi.signUp() 호출
            // SIGNATURE 헤더 생성 필요

            // 임시 구현: 항상 성공 반환
            emit(ApiResult.Success(CommonResponse(
                success = true,
                message = "Sign up successful (stub implementation)"
            )))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Sign up error: ${e.message}"
            ))
        }
    }
}
