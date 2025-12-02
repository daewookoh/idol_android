package net.ib.mn.data.remote.interceptor

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.ib.mn.domain.model.GCode
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * gcode 처리 Interceptor
 *
 * old 프로젝트의 ApiModule에서 처리하던 gcode 88888 (점검 상태) 감지 로직
 * 응답 Body를 파싱하여 gcode를 확인하고, 점검 상태면 이벤트를 발행
 *
 * 사용법:
 * - MainActivity나 StartupActivity에서 maintenanceEvent를 구독
 * - 점검 상태 감지 시 점검 화면으로 이동
 */
@Singleton
class GcodeInterceptor @Inject constructor() : Interceptor {

    // 점검 상태 이벤트 발행용 (SharedFlow)
    private val _maintenanceEvent = MutableSharedFlow<MaintenanceInfo>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val maintenanceEvent: SharedFlow<MaintenanceInfo> = _maintenanceEvent.asSharedFlow()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // 2XX 이외의 응답은 passthrough (old 프로젝트와 동일)
        if (response.code !in 200..299) {
            return response
        }

        // 응답 Body 읽기 (gcode 확인용)
        val responseBody = response.body?.string()

        responseBody?.let { body ->
            try {
                val jsonResponse = JSONObject(body)
                val gcode = jsonResponse.optInt("gcode", 0)

                // gcode == 88888: 서버 점검 상태 (old 프로젝트의 SharedBridgeManager.setData 대체)
                if (gcode == GCode.MAINTENANCE) {
                    Log.w("GcodeInterceptor", "========================================")
                    Log.w("GcodeInterceptor", "🔧 서버 점검 상태 감지 (gcode: $gcode)")
                    Log.w("GcodeInterceptor", "  - URL: ${request.url}")
                    Log.w("GcodeInterceptor", "  - Response: $body")
                    Log.w("GcodeInterceptor", "========================================")

                    // 점검 정보 추출
                    val mcode = jsonResponse.optInt("mcode", 0)
                    val msg = jsonResponse.optString("msg", null)
                    val description = jsonResponse.optString("description", null)

                    // 점검 이벤트 발행 (구독자에게 알림)
                    _maintenanceEvent.tryEmit(
                        MaintenanceInfo(
                            gcode = gcode,
                            mcode = mcode,
                            message = msg ?: description,
                            rawJson = jsonResponse
                        )
                    )
                }

                // 일반적인 gcode 로깅 (디버그)
                if (gcode != 0 && gcode != GCode.MAINTENANCE) {
                    Log.d("GcodeInterceptor", "API returned gcode: $gcode for ${request.url}")
                }

            } catch (e: Exception) {
                // JSON 파싱 실패 시 무시 (비-JSON 응답 가능)
                Log.v("GcodeInterceptor", "Failed to parse response as JSON: ${e.message}")
            }
        }

        // Body를 다시 생성하여 반환 (body는 한 번만 읽을 수 있으므로)
        return response.newBuilder()
            .body(responseBody?.toResponseBody(response.body?.contentType()))
            .build()
    }
}

/**
 * 점검 상태 정보
 */
data class MaintenanceInfo(
    val gcode: Int,
    val mcode: Int,
    val message: String?,
    val rawJson: JSONObject
)
