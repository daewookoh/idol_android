package net.ib.mn.utils

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.StartupActivity
import org.json.JSONObject

object ErrorControl {
    /* 글쓰기 금지된 사용자 */
    const val ERROR_1111: Int = 1111

    /* 최애설정 API */
    const val ERROR_1143: Int = 1143
    const val ERROR_1144: Int = 1144
    const val ERROR_1145: Int = 1145

    /*코플계정 연동 API */
    const val ERROR_1990: Int = 1990

    /* 하트상점 영수증 검증 API */
    const val ERROR_3000: Int = 3000
    const val ERROR_3010: Int = 3010
    const val ERROR_3011: Int = 3011

    /* 게시물,댓글 작성 API */
    const val ERROR_2000: Int = 2000 // 반복해서 작성 오류
    const val ERROR_2090: Int = 2090
    const val ERROR_2091: Int = 2091

    /* 가입제한여부 확인 API and 회원가입 API */
    const val ERROR_1001: Int = 1001
    const val ERROR_1010: Int = 1010

    /* 닉네임 변경 API and 가입제한여부 확인 API and 회원가입 API*/
    const val ERROR_1011: Int = 1011
    const val ERROR_1012: Int = 1012
    const val ERROR_1013: Int = 1013

    /* 비밀번호 찾기 API */
    const val ERROR_1002: Int = 1002
    const val ERROR_1030: Int = 1030

    /* 로그인 API */
    const val ERROR_1031: Int = 1031

    /* 	즐겨찾기 해제 API */
    const val ERROR_1164: Int = 1164

    /* 하트지급 API */
    const val ERROR_1200: Int = 1200
    const val ERROR_1201: Int = 1201

    /* 코플 적립금으로 하트 구매*/
    const val ERROR_1912: Int = 1912
    const val ERROR_1991: Int = 1991

    /* 게시물 신고 API */
    const val ERROR_2200: Int = 2200
    const val ERROR_2201: Int = 2201
    const val ERROR_2202: Int = 2202
    const val ERROR_2203: Int = 2203
    const val ERROR_2204: Int = 2204

    /*유저 신고 API*/
    const val ERROR_2300: Int = 2300
    const val ERROR_2301: Int = 2301
    const val ERROR_2302: Int = 2302
    const val ERROR_2303: Int = 2303
    const val ERROR_2304: Int = 2304

    // 채팅방 신고
    const val ERROR_2400: Int = 2400
    const val ERROR_2401: Int = 2401
    const val ERROR_2402: Int = 2402
    const val ERROR_2403: Int = 2403
    const val ERROR_2404: Int = 2404

    //댓글 신고 api
    const val ERROR_2500: Int = 2500
    const val ERROR_2501: Int = 2501
    const val ERROR_2502: Int = 2502
    const val ERROR_2503: Int = 2503
    const val ERROR_2504: Int = 2504

    //이미 있는 이미지 업로드 되었을 때
    const val ERROR_3900: Int = 3900

    //게시글 준비가 끝나지 않고 작업중인 상태
    const val ERROR_3901: Int = 3901

    //게시글 업로드 중 문제가 발생해서 이미지 처리에 문제가 생긴 상태
    const val ERROR_3902: Int = 3902

    /* 구독 하트 수령 */
    const val ERROR_4000: Int = 4000

    /* iOS에서 구독중인데 안드에서도 구독하는 경우 1회 발생 */
    const val ERROR_6000: Int = 6000

    /* 개인/그룹 투표 API */
    const val ERROR_4010: Int = 4010
    const val ERROR_4011: Int = 4011
    const val ERROR_4020: Int = 4020

    /* 구맵복원 실패 */
    const val ERROR_5000: Int = 5000

    /*이미 구독중인 경우*/
    const val ERROR_5010: Int = 5010

    /*동일 Google 계정을 사용하는 다른 최애돌 계정에서 데일리팩 구독중*/
    const val ERROR_5011: Int = 5011
    const val ERROR_9000: Int = 9000

    /* 친구 예외 */
    const val ERROR_8000: Int = 8000
    const val ERROR_8001: Int = 8001
    const val ERROR_8003: Int = 8003

    const val ERROR_9997: Int = 9997
    const val ERROR_9998: Int = 9998
    const val ERROR_9999: Int = 9999

    // 서버 오류 메시지
    const val ERROR_88888: Int = 88888

    const val ERROR_9996: Int = 9996

    // 비디오광고 미적립
    const val ERROR_3333: Int = 3333

    // 원픽 벤
    const val ERROR_1117: Int = 1117

    // 앱내 예외 처리
    const val ERROR_START_ACTIVITY: Int = 101


    /**
     * gcode에 따른 에러 메시지를 반환한다.
     * msg가 있으면 msg를 반환하고 없으면 gcode에 따른 메시지를 반환한다.
     */
    fun parseError(context: Context?, gcode: Int, msg: String?): String? {
        if (msg != null && !msg.isEmpty()) {
            return msg
        }

        try {
            val jsonObject = JSONObject()
            jsonObject.put("gcode", gcode)
            return parseError(context, jsonObject)
        } catch (e: Exception) {
            return null
        }
    }

    @Suppress("NAME_SHADOWING")
    fun parseError(context: Context?, json: JSONObject?): String? {
        val json = json ?: return null
        if (json.optInt("gcode") == ERROR_88888 && json.optInt("mcode") == 1) {
            val i = Intent(context, StartupActivity::class.java)
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // 앱 처음 시작시 이걸 안하면 StartupActivity가 계속 실행됨
            context!!.startActivity(i)
            return null
        } else {
            var responseMsg: String? = ""
            var response_gcode = ""
            val gcode = json.optInt("gcode")
            // custom server error message
            val msg = json.optString("msg")

            if (context == null) return "Unknown error."

            when (gcode) {
                ERROR_1001 -> responseMsg = context.getString(R.string.error_1001)
                ERROR_1002 -> responseMsg = context.getString(R.string.error_1002)
                ERROR_1010 -> responseMsg = context.getString(R.string.error_1010)
                ERROR_1011 -> responseMsg = context.getString(R.string.error_1011)
                ERROR_1012 -> responseMsg = context.getString(R.string.error_1012)
                ERROR_1013 -> responseMsg = context.getString(R.string.error_1013)
                ERROR_1030 -> responseMsg = context.getString(R.string.error_1030)
                ERROR_1031 -> responseMsg = context.getString(R.string.error_1031)
                ERROR_1145 -> responseMsg =
                    context.getString(if (BuildConfig.CELEB) R.string.actor_error_1145 else R.string.error_1145)

                ERROR_1164 -> responseMsg = context.getString(R.string.error_1164)
                ERROR_1200 -> responseMsg = context.getString(R.string.error_1200)
                ERROR_1201 -> responseMsg = context.getString(R.string.error_1201)
                ERROR_1912 -> responseMsg = context.getString(R.string.error_1912)
                ERROR_1991 -> responseMsg = context.getString(R.string.error_1991)
                ERROR_2000 -> responseMsg = if (TextUtils.isEmpty(msg)) {
                    context.getString(R.string.error_abnormal_default)
                } else {
                    msg
                }

                ERROR_2090 -> responseMsg = context.getString(R.string.error_2090)
                ERROR_2091 -> responseMsg = context.getString(R.string.error_2091)
                ERROR_2200, ERROR_2300 -> responseMsg = context.getString(R.string.error_2200)
                ERROR_2201 -> responseMsg =
                    context.getString(R.string.failed_to_report__already_reported)

                ERROR_2301 -> responseMsg =
                    context.getString(R.string.failed_to_report_user__already_reported)

                ERROR_2202, ERROR_2302 -> responseMsg =
                    context.getString(R.string.failed_to_report_2202)

                ERROR_2203, ERROR_2303 -> responseMsg =
                    context.getString(R.string.failed_to_report_2203)

                ERROR_2204, ERROR_2304 -> responseMsg = context.getString(R.string.not_enough_heart)
                ERROR_2400 -> responseMsg = context.getString(R.string.chat_room_limit_level)
                ERROR_2401 -> responseMsg = context.getString(R.string.chat_report_error_2401)
                ERROR_2402 -> responseMsg = context.getString(R.string.failed_to_report_2202)
                ERROR_2403 -> responseMsg = context.getString(R.string.failed_to_report_2203)
                ERROR_2404 -> responseMsg = context.getString(R.string.not_enough_heart)
                ERROR_2500 -> {
                    responseMsg = context.getString(R.string.error_2200)
                }

                ERROR_2501 -> responseMsg = context.getString(R.string.comment_report_error_2501)
                ERROR_2502 -> responseMsg = context.getString(R.string.failed_to_report_2202)
                ERROR_2503 -> responseMsg = context.getString(R.string.failed_to_report_2203)
                ERROR_2504 -> responseMsg = context.getString(R.string.not_enough_heart)
                ERROR_4010 -> responseMsg = context.getString(R.string.error_4010)
                ERROR_4011 -> responseMsg = context.getString(R.string.error_4011)
                ERROR_4020 -> responseMsg = context.getString(R.string.error_4020)
                ERROR_8000 -> responseMsg = context.getString(R.string.error_8000)
                ERROR_8001 -> responseMsg = context.getString(R.string.error_8001)
                ERROR_8003 -> responseMsg = context.getString(R.string.error_8003)
                ERROR_9000, ERROR_1143, ERROR_1144, ERROR_1990, ERROR_3000, ERROR_3010, ERROR_3011 -> {
                    response_gcode = context.getString(R.string.error_abnormal)
                    responseMsg = String.format(response_gcode, gcode.toString())
                }

                ERROR_3333 -> responseMsg = context.getString(R.string.video_ad_cancelled)
                ERROR_9996 -> responseMsg = context.getString(R.string.error_9996)
                ERROR_9997 -> responseMsg = context.getString(R.string.error_9997)
                ERROR_9998 -> responseMsg = context.getString(R.string.error_9998)
                ERROR_9999 -> responseMsg = context.getString(R.string.error_1000_heart_give)
                9900 -> responseMsg = context.getString(R.string.error_9900)
                9901 -> responseMsg = context.getString(R.string.error_9901)
                9902 -> responseMsg = context.getString(R.string.error_9902)
                9903 -> responseMsg = context.getString(R.string.error_9903)
                9904 -> responseMsg = context.getString(R.string.error_9904)
                ERROR_88888 -> responseMsg = null
                else -> responseMsg = context.getString(R.string.error_abnormal_default)
            }
            return responseMsg
        }
    }
}
