package net.ib.mn.utils

import android.content.Context
import net.ib.mn.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

fun getCurrentDateTime(): Long {
    val timeZone = TimeZone.getTimeZone("UTC")
    return Calendar.getInstance(timeZone).time.time
}

fun isWithin48Hours(currentDateTime: Long, serverDateString: String): Boolean {

    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        this.timeZone = timeZone
    }

    val serverDate = formatter.parse(serverDateString)
    val hoursDifference = (currentDateTime - serverDate?.time as Long) / (1000 * 60 * 60)
    return hoursDifference < 48
}

fun isWithin48Hours(serverDate: Date): Boolean {
    val utcTimeZone = TimeZone.getTimeZone("UTC")

    val currentDate = Date()
    val currentUtcOffset = utcTimeZone.getOffset(currentDate.time)
    val currentUtcDate = Date(currentDate.time + currentUtcOffset)

    val timeDifferenceInMillis = currentUtcDate.time - serverDate.time
    val hoursDifference = timeDifferenceInMillis / (1000 * 60 * 60)
    return hoursDifference < 48
}

fun getKSTMidnightEpochTime(): Long {
    val kstZone = ZoneId.of("Asia/Seoul")
    val nowKST = ZonedDateTime.now(kstZone)
    val midnightKST = nowKST.toLocalDate().atStartOfDay(kstZone)
    return midnightKST.toInstant().toEpochMilli()
}

fun parseDateStringToMillis(dateString: String): Long {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.parse(dateString)?.time ?: 0L
}

fun formatDateForDisplay(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    formatter.timeZone = TimeZone.getDefault()
    return formatter.format(Date(millis))
}

fun getUserTimeZone(): ZoneId {
    return TimeZone.getDefault().toZoneId()
}

fun getDdayString(context: Context, currentTime: Long, targetTime: Long): String {
    val userZoneId = getUserTimeZone()
    val currentDate = Instant.ofEpochMilli(currentTime)
        .atZone(userZoneId)
        .toLocalDate()

    val targetDate = Instant.ofEpochMilli(targetTime)
        .atZone(userZoneId)
        .toLocalDate()

    val diffInDays = ChronoUnit.DAYS.between(currentDate, targetDate).toString()
    return context.getString(R.string.vote_dday, diffInDays)
}

fun calculateDDayFromUserTimeZone(beginAt: Date): Int {
    val userZoneId = getUserTimeZone()
    val nowUserZone = ZonedDateTime.now(userZoneId).toLocalDate().atStartOfDay(userZoneId)
    val beginUserZone = Instant.ofEpochMilli(beginAt.time).atZone(userZoneId).toLocalDate().atStartOfDay(userZoneId)
    val diffInDays = ChronoUnit.DAYS.between(nowUserZone.toLocalDate(), beginUserZone.toLocalDate())
    return diffInDays.toInt()
}