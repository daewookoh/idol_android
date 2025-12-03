package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import net.ib.mn.util.logW
import kotlinx.coroutines.flow.map
import net.ib.mn.data.remote.api.MessageApi
import net.ib.mn.data.remote.dto.MessageCouponResponse
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.MessageRepository
import org.json.JSONObject
import javax.inject.Inject

private const val TAG = "MessageRepository"

/**
 * MessageRepository 구현체
 *
 * BaseRepository의 safeApiCall 패턴을 사용하여 중복 코드 제거
 */
class MessageRepositoryImpl @Inject constructor(
    private val messageApi: MessageApi
) : BaseRepository(), MessageRepository {

    override fun getMessageCoupon(): Flow<ApiResult<MessageCouponResponse>> =
        safeApiCall { messageApi.getMessageCoupon() }
            .map { result ->
                when (result) {
                    is ApiResult.Success -> {
                        if (result.data.success) {
                            result
                        } else {
                            ApiResult.Error(ApiError.Business(gcode = 0, message = "API returned success=false"))
                        }
                    }
                    else -> result
                }
            }

    override suspend fun checkNewNotification(afterDate: String?): Boolean {
        return try {
            val response = messageApi.getNotifications(type = "P", after = afterDate)

            if (response.isSuccessful && response.body() != null) {
                val bodyString = response.body()!!.string()
                val jsonObject = JSONObject(bodyString)

                if (jsonObject.optBoolean("success", false)) {
                    val meta = jsonObject.optJSONObject("meta")
                    val totalCount = meta?.optInt("total_count", 0) ?: 0
                    logD(TAG, "checkNewNotification: totalCount=$totalCount")
                    totalCount > 0
                } else {
                    logW(TAG, "checkNewNotification: API returned success=false")
                    false
                }
            } else {
                logW(TAG, "checkNewNotification: API call failed with code=${response.code()}")
                false
            }
        } catch (e: Exception) {
            logE(TAG, "checkNewNotification: Error", e)
            false
        }
    }
}
