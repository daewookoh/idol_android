package net.ib.mn.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

object DateUtil {
    fun getHallOfFameDateString(): String {
        val today = Date()
        var cal: Calendar = GregorianCalendar()
        cal.time = today
        cal.add(Calendar.DATE, -30)
        cal[Calendar.HOUR_OF_DAY] = 11
        val fromDate = cal.time

        cal = GregorianCalendar()
        cal.setTime(today)
        cal[Calendar.HOUR_OF_DAY] = 23
        var toDate = cal.getTime()
        val f = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        val fromText = f.format(fromDate)
        if (today.time < toDate.time) {
            // time less than agg time
            cal = GregorianCalendar()
            cal.setTime(today)
            cal.add(Calendar.DATE, -1)
            cal[Calendar.HOUR_OF_DAY] = 23
            toDate = cal.getTime()
        }
        val toText = f.format(toDate)

        return "$fromText ~ $toText"
    }

    fun Long.getCurrentDateFromTimeMills(): String {
        val currentTimeMillis = this
        val date = Date(currentTimeMillis)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return dateFormat.format(date)
    }

    fun Long.isSameDate() : Boolean {
        val currentTimeMillis = this
        val date = Date(currentTimeMillis)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val currentDate = dateFormat.format(Date())
        val compareDate = dateFormat.format(date)

        return currentDate == compareDate
    }

    fun formatCreatedAtRelativeToNow(
        now: Calendar,
        created: Calendar,
        articleCreatedAt: Date,
        locale: Locale
    ): String {
        created.time = articleCreatedAt

        return if (
            now.get(Calendar.YEAR) == created.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == created.get(Calendar.DAY_OF_YEAR)
        ) {
            val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
            timeFormat.format(articleCreatedAt)
        } else {
            val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            dateFormat.format(articleCreatedAt)
        }
    }
}