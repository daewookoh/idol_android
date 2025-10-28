package net.ib.mn.model

import android.content.Context
import android.text.TextUtils
import net.ib.mn.utils.Util
import java.util.Date
import java.util.Locale

// TODO 셀럽 차트코드 적용되면 stable 처리
class HallModel {
    val createdAt: Date? = null
    var idol: IdolModel? = null
    var id: Int = 0
    val heart: Long = 0
    val imageUrl: String? = null
    val imageUrl2: String? = null
    val imageUrl3: String? = null
    val event: String? = null
        get() {
            if (field == null) {
                return ""
            }
            return field
        }
    val history: String? = null
    private val name: String? = null
    private val name_en: String? = null
    private val name_jp: String? = null
    private val name_zh: String? = null
    private val name_zh_tw: String? = null
    val resource_uri: String? = null
    val type: String? = null
    private var first_day: String? = null
    private var last_day: String? = null
    private var banner_url: String? = null
    var trendId: String? = null
    val count: Int = 0
    val score: Int = 0
    var rank: Int = -1
    val idol_id = 0
    var difference: Int = 0

    //급상승 1위 id를 받아옴.
    var topOneDifferenceId: Int = -1 //급상승  1위  id

    var status: String? = null

    private val start_date: String? = null

    private val end_date: String? = null

    private var league: String? = null

    var chartName: String? = null

    fun getResourceId(): String {
        val splitUri =
            resource_uri!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return splitUri[splitUri.size - 1]
    }

    fun getName(context: Context?): String? {
        try {
            var lang = Util.getSystemLanguage(context)
            if (lang != null) {
                lang = lang.lowercase(Locale.getDefault())

                if (lang.startsWith("en") && !TextUtils.isEmpty(name_en)) {
                    return name_en
                } else if (lang.startsWith("zh_tw") && !TextUtils.isEmpty(name_zh_tw)) {
                    return name_zh_tw
                } else if (lang.startsWith("zh") && !TextUtils.isEmpty(name_zh)) {
                    return name_zh
                } else if (lang.startsWith("ja") && !TextUtils.isEmpty(name_jp)) {
                    return name_jp
                } else if (lang.startsWith("ko")) {
                    return name
                }
            }
        } catch (e: Exception) {
        }

        return name_en
    }

}
