package net.ib.mn.model

import android.content.Context
import net.ib.mn.utils.LocaleUtil.getAppLocale
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class HallAggHistoryModel {
    var idol: IdolModel? = null
    var heart: Long = 0
    var createdAt: Date = Date()
    var imageUrl: String? = null
    var rank: Int = 0
    var type: String = ""
    var status: String? = null
    var difference: Int = 0
    var resource_uri: String? = null
    var league: String? = null
    var refdate: String = ""

    fun getRefdate(context: Context): String {
        //서버에서 보내준 refDate 포맷팅
        val fixedDateFormat = SimpleDateFormat("yyyy-MM-dd", getAppLocale(context))
        fixedDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        //원하는 날짜 pattern으로 포맷팅
        val returnDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, getAppLocale(context))
        returnDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        try {
            return returnDateFormat.format(fixedDateFormat.parse(refdate))
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return ""
    }


    fun getResourceId(): String {
        val splitUri =
            resource_uri!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return splitUri[splitUri.size - 1]
    }
}
