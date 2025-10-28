package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

class ScheduleModel : Serializable {
    var allday: Int = 0
    var article_id: Int = 0
    var id: Int = 0
    var dtstart: Date = Date()
    var category: String = ""
    var created_at: Date = Date()
    var extra: String? = null
    var idol: IdolModel? = null
    var idol_ids: List<Int> = emptyList()
    var is_viewable: String? = null
    var is_readonly: String? = null
    var location: String? = null
    var num_comments: Int = 0
    var num_dup: Int = 0
    var num_no: Int = 0
    var num_yes: Int = 0

    @SerializedName("resource_uri")
    var resource_uri: String? = null
    var title: String? = null
    var updated_at: Date? = null
    var url: String? = null
    var user: UserModel? = null
    var lat: String? = null
    var lng: String? = null
    var vote: String? = ""

    companion object {
        private const val serialVersionUID = 1L
    }
}
