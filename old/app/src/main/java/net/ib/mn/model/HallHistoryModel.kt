package net.ib.mn.model

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Objects
import java.util.TimeZone

class HallHistoryModel {
    var historyTxt: String? = null
    var historyParam: String? = null
    var nextHistoryParam: String? = null
    var history_month: String? = null
    var historyYear: String? = null
    var year_month: String? = null

    val historyMonth: String?
        get() {
            val stringToDate = SimpleDateFormat(
                "MM",
                Locale.getDefault()
            ) //서버에서 보내주는 스트링을 Date로 변경할 때 사용
            val dateToString = SimpleDateFormat(
                "MMM",
                Locale.getDefault()
            ) //Date로 변경된 값을 format에 맞춰 String으로 변경
            try {
                val date = stringToDate.parse(history_month)
                history_month =
                    dateToString.format(Objects.requireNonNull(date))
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            return history_month
        }

    val yearMonth: String?
        get() {
            val yearMonth = historyYear + "." + history_month
            val simpleDateFormat =
                SimpleDateFormat("yyyy.MM", Locale.getDefault())
            simpleDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

            try {
                val date = simpleDateFormat.parse(yearMonth)
                year_month =
                    simpleDateFormat.format(Objects.requireNonNull(date))
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            return year_month
        }
}
