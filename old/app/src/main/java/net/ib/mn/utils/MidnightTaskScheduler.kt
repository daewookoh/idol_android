package net.ib.mn.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

import java.util.Calendar
import java.util.TimeZone

/**
 * KST 0시에 미션 초기화하는 스케줄러
 */
class MidnightTaskScheduler(private val context: Context) {
    fun setDailyAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MidnightReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // alarmManager로 실행시 약간의 딜레이가 있으므로 집계시간 끝날 무렵에 실행되도록 설정
        val nextMidnight = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1) // 이미 시간이 지났다면 다음날 0시로 설정
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnight.timeInMillis,
            pendingIntent
        )
        Log.d("MidnightTask", "실행 예약됨: ${nextMidnight.time}")
    }
}

class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            Log.d("MidnightTask", "MidnightReceiver 실행됨!")

            // 미션 초기화
            Util.setPreference(context, Const.PREF_MISSION_COMPLETED, false)
            EventBus.sendEvent(true, Const.BROADCAST_MANAGER_MESSAGE) // 순위 화면 갱신

            // 알람 재등록
            MidnightTaskScheduler(context).setDailyAlarm()
        }
    }
}