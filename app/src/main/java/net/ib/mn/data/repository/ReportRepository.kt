package net.ib.mn.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ib.mn.data.remote.api.ReportApi
import net.ib.mn.data.remote.api.ReportUserRequest
import net.ib.mn.domain.repository.ConfigRepository
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val reportApi: ReportApi,
    private val configRepository: ConfigRepository
) {
    /**
     * 유저 신고 가능 여부 확인
     */
    suspend fun getReportPossible(userId: Int): ReportPossibleResult = withContext(Dispatchers.IO) {
        try {
            val response = reportApi.getReportPossible(userId)
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: "{}"
                val json = JSONObject(body)
                val success = json.optBoolean("success", false)
                if (success) {
                    // ConfigRepository 캐시에서 reportHeart 가져오기 (Old: configModel.reportHeart)
                    val reportHeart = configRepository.getReportHeart()
                    ReportPossibleResult.Success(reportHeart = reportHeart)
                } else {
                    val gcode = json.optInt("gcode", 0)
                    ReportPossibleResult.AlreadyReported(gcode = gcode)
                }
            } else {
                ReportPossibleResult.Error("서버 오류가 발생했습니다.")
            }
        } catch (e: Exception) {
            ReportPossibleResult.Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
        }
    }

    /**
     * 유저 신고하기
     */
    suspend fun reportUser(userId: Int, reason: String): ReportResult = withContext(Dispatchers.IO) {
        try {
            val response = reportApi.reportUser(ReportUserRequest(userId, reason))
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: "{}"
                val json = JSONObject(body)
                val success = json.optBoolean("success", false)
                if (success) {
                    ReportResult.Success
                } else {
                    val gcode = json.optInt("gcode", 0)
                    ReportResult.Error(gcode = gcode)
                }
            } else {
                ReportResult.Error(gcode = 0)
            }
        } catch (e: Exception) {
            ReportResult.Error(gcode = 0)
        }
    }
}

sealed class ReportPossibleResult {
    /** 신고 가능 - reportHeart: 신고 시 차감될 하트 수 */
    data class Success(val reportHeart: Int = 0) : ReportPossibleResult()
    data class AlreadyReported(val gcode: Int) : ReportPossibleResult()
    data class Error(val message: String) : ReportPossibleResult()
}

sealed class ReportResult {
    data object Success : ReportResult()
    data class Error(val gcode: Int) : ReportResult()
}
