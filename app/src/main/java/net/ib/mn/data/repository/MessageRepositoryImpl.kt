package net.ib.mn.data.repository

import net.ib.mn.data.remote.api.MessageApi
import net.ib.mn.data.remote.dto.MessageCouponResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageApi: MessageApi
) : MessageRepository {

    override fun getMessageCoupon(): Flow<ApiResult<MessageCouponResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // AuthInterceptor가 자동으로 Authorization 헤더를 추가
            val response = messageApi.getMessageCoupon()

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
}
