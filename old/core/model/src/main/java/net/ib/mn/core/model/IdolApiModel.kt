package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import java.io.Serializable
import java.lang.Exception

/**
 * model 통합전 core 모듈에서 사용할 모델
 */
@kotlinx.serialization.Serializable
data class IdolApiModel(
    @SerialName("id") @Transient private var id: Int = 0,
    @SerialName("miracle_count") var miracleCount: Int = 0,
    @SerialName("angel_count") var angelCount: Int = 0,
    @SerialName("rookie_count") var rookieCount: Int = 0,
    @SerialName("anniversary") var anniversary: String? = "N",
    @SerialName("anniversary_days") var anniversaryDays: Int? = null,
    @SerialName("birthday") var birthDay: String? = null,
    @SerialName("burning_day") var burningDay: String? = null,
    @SerialName("category") val category: String = "",
    @SerialName("comeback_day") var comebackDay: String? = null,
    @SerialName("deathday") var deathDay: String? = null,
    @SerialName("debut_day") var debutDay: String? = null,
    @SerialName("description") var description: String = "",
    @SerialName("fairy_count") var fairyCount: Int = 0,
    @SerialName("group_id") var groupId: Int = 0,
    @SerialName("heart") var heart: Long = 0,
    @SerialName("image_url") var imageUrl: String? = null,
    @SerialName("image_url2") var imageUrl2: String? = null,
    @SerialName("image_url3") var imageUrl3: String? = null,
    @SerialName("is_gaonaward") val isGaonAward: String? = null,
    @SerialName("is_viewable") var isViewable: String = "Y",
    @SerialName("lgcode") val lgCode: String? = null,
    @SerialName("name") private var name: String = "",
    @SerialName("name_en") var nameEn: String = "",
    @SerialName("name_jp") var nameJp: String = "",
    @SerialName("name_zh") var nameZh: String = "",
    @SerialName("name_zh_tw") var nameZhTw: String = "",
    @SerialName("resource_uri") var resourceUri: String = "",
    @SerialName("top3") var top3: String? = null,
    @SerialName("top3_type") var top3Type: String? = null,
    @SerialName("top3_seq") var top3Seq: Int = -1,
    @SerialName("type") var type: String = "",
    @SerialName("info_seq") var infoSeq: Int = -1, //서버에 이미 info_ver 필드가 존재(저장된 값의 형태가 다름)해서 변경해서 보내주기로 함
    @SerialName("is_lunar_birthday") var isLunarBirthday : String? = null,
    @SerialName("most_count") var mostCount : Int = 0,
    @SerialName("most_count_desc") var mostCountDesc : String? = null,
    @SerialName("update_ts") var updateTs : Int = 0,
    @SerialName("league") var league: String? = null,
    @SerialName("source_app") var sourceApp: String? = null,
) {
    init {
        id = resourceUri.split("/").last { it.isNotEmpty() }.toInt()
    }
}