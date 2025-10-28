package com.example.idol_android.data.repository

import com.example.idol_android.data.remote.api.UserApi
import com.example.idol_android.data.remote.dto.*
import com.example.idol_android.domain.model.ApiResult
import com.example.idol_android.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * User Repository 구현체
 */
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi
) : UserRepository {

    override fun getUserSelf(etag: String?): Flow<ApiResult<UserSelfResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // TODO: Get token from DataStore
            val token = "Bearer YOUR_TOKEN_HERE"

            val response = userApi.getUserSelf(token, etag)

            // HTTP 304 Not Modified (캐시 유효)
            if (response.code() == 304) {
                // 캐시된 데이터 사용 (별도 처리 필요)
                // TODO: 로컬 캐시에서 불러오기
                emit(ApiResult.Error(
                    exception = Exception("Cache valid - use local data"),
                    code = 304
                ))
                return@flow
            }

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    // 새로운 ETag 저장
                    val newETag = response.headers()["ETag"]
                    // TODO: DataStore에 ETag 저장

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

    override fun getUserStatus(): Flow<ApiResult<UserStatusResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val token = "Bearer YOUR_TOKEN_HERE"
            val response = userApi.getUserStatus(token)

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
            val token = "Bearer YOUR_TOKEN_HERE"
            val response = userApi.getIabKey(token)

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
            val token = "Bearer YOUR_TOKEN_HERE"
            val response = userApi.getBlocks(token)

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
}
