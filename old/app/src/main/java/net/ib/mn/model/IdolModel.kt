package net.ib.mn.model

import android.content.Context
import android.os.Parcelable
import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import net.ib.mn.domain.model.Idol
import net.ib.mn.utils.Util
import java.io.Serializable
import java.util.Locale

@Parcelize
data class IdolModel(
    @SerializedName("id") private var id: Int = 0,
    @SerializedName("miracle_count") var miracleCount: Int = 0,
    @SerializedName("angel_count") var angelCount: Int = 0,
    @SerializedName("rookie_count") var rookieCount: Int = 0,
    @SerializedName("anniversary") var anniversary: String? = "N",
    @SerializedName("anniversary_days") var anniversaryDays: Int? = null,
    @SerializedName("birthday") var birthDay: String? = null,
    @SerializedName("burning_day") var burningDay: String? = null,
    @SerializedName("category") val category: String = "",
    @SerializedName("comeback_day") var comebackDay: String? = null,
    @SerializedName("debut_day") var debutDay: String? = null,
    @SerializedName("description") var description: String = "",
    @SerializedName("fairy_count") var fairyCount: Int = 0,
    @SerializedName("group_id") var groupId: Int = 0,
    @SerializedName("heart") var heart: Long = 0,
    @SerializedName("image_url") var imageUrl: String? = null,
    @SerializedName("image_url2") var imageUrl2: String? = null,
    @SerializedName("image_url3") var imageUrl3: String? = null,
    @SerializedName("is_viewable") var isViewable: String = "Y",
    @SerializedName("name") private var name: String = "",
    @SerializedName("name_en") var nameEn: String = "",
    @SerializedName("name_jp") var nameJp: String = "",
    @SerializedName("name_zh") var nameZh: String = "",
    @SerializedName("name_zh_tw") var nameZhTw: String = "",
    @SerializedName("resource_uri") var resourceUri: String = "",
    @SerializedName("top3") var top3: String? = null,
    @SerializedName("top3_type") var top3Type: String? = null,
    @SerializedName("top3_seq") var top3Seq: Int = -1, // top3_ver
    @SerializedName("top3_image_ver") var top3ImageVer: String = "", // 2025.8.8 추가
    @SerializedName("type") var type: String = "",
    @SerializedName("info_seq") var infoSeq: Int = -1, //서버에 이미 info_ver 필드가 존재(저장된 값의 형태가 다름)해서 변경해서 보내주기로 함
    @SerializedName("is_lunar_birthday") var isLunarBirthday: String? = null,
    @SerializedName("most_count") var mostCount: Int = 0,
    @SerializedName("most_count_desc") var mostCountDesc: String? = null,
    @SerializedName("update_ts") var updateTs: Int = 0,
    @SerializedName("source_app") var sourceApp: String? = null,
    @SerializedName("fd_name") var fdName: String? = null,
    @SerializedName("fd_name_en") var fdNameEn: String? = null,
) : Serializable, Parcelable {
    //    var id: Int = 0
//        get() = resourceUri.split("/").last { it.isNotEmpty() }.toInt()
    var isMost: Boolean = false
    var isFavorite: Boolean = false
    var rank: Int = 0

    // 스케쥴에서 쓰길래 만들어줌... 추후에 이거 없이 쓸 수 있도록 수정해야 함
    constructor(id: Int, groupId: Int) : this() {
        this.id = id
        this.groupId = groupId
        this.resourceUri = "/${id}"
    }

    fun getId(): Int {
        return if (id == 0) {
            try {
                resourceUri.split("/").last { it.isNotEmpty() }.toInt()
            } catch (e: Exception) {//혹시나 exception 이 생기면  id값을 보내줌.
                return id
            }
        } else {
            id
        }
    }

    //그냥 id 값이 필요할때용
    fun getOriginalId(): Int {
        return id
    }

    // 셀럽용
    fun getGroup_id(): Int {
        return id
    }

    fun getName(context: Context?): String {
        try {
            var lang = Util.getSystemLanguage(context)
            if (lang != null) {
                lang = lang.lowercase()
                if (lang.startsWith("en") && !TextUtils.isEmpty(nameEn)) {
                    return nameEn
                } else if (lang.startsWith("ko")) {
                    return name
                } else if (lang.startsWith("zh_tw") && !TextUtils.isEmpty(nameZhTw)) {
                    return nameZhTw
                } else if (lang.startsWith("zh") && !TextUtils.isEmpty(nameZh)) {
                    return nameZh
                } else if (lang.startsWith("ja") && !TextUtils.isEmpty(nameJp)) {
                    return nameJp
                } else if (!TextUtils.isEmpty(nameEn)) {
                    return nameEn
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return nameEn
    }

    fun getName(): String {
        return name
    }

    fun setName(n: String) {
        name = n
    }

    fun getFdName(context: Context) : String? {
        var lang = Util.getSystemLanguage(context)
        if (lang != null) {
            lang = lang.lowercase(Locale.getDefault())

            return if (lang.startsWith("ko")) {
                fdName
            }  else {
                fdNameEn
            }
        }
        return ""
    }

//    fun setLocalizedName(context: Context?) {
//        try {
//            var lang = Util.getSystemLanguage(context)
//            if (lang != null) {
//                lang = lang.toLowerCase()
//                nameKo=name
//                if (lang.startsWith("en") && !TextUtils.isEmpty(nameEn)) {
//                    name = nameEn
//                } else if(lang.startsWith("ko") && !TextUtils.isEmpty(nameKo)){
//                    name = nameKo
//                }
//                else if (lang.startsWith("zh_tw") && !TextUtils.isEmpty(nameZhTw)) {
//                    name = nameZhTw
//                } else if (lang.startsWith("zh") && !TextUtils.isEmpty(nameZh)) {
//                    name = nameZh
//                } else if (lang.startsWith("ja") &&  !TextUtils.isEmpty(nameJp)) {
//                    name = nameJp
//                } else if (!TextUtils.isEmpty(nameEn)) {
//                    name = nameEn
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

    // 셀럽용
    //객체 주소값이 변하지 않고 값만 바뀌어야 될경우 사용함.
    fun copy(idol: IdolModel) {
        heart = idol.heart
        top3 = idol.top3
        top3Type = idol.top3Type
        anniversary = idol.anniversary
        anniversaryDays = idol.anniversaryDays
        angelCount = idol.angelCount
        rookieCount = idol.rookieCount
        birthDay = idol.birthDay
        burningDay = idol.burningDay
        comebackDay = idol.comebackDay
        debutDay = idol.debutDay
        description = idol.description
        fairyCount = idol.fairyCount
        groupId = idol.groupId
        imageUrl = idol.imageUrl
        imageUrl2 = idol.imageUrl2
        imageUrl3 = idol.imageUrl3
        infoSeq = idol.infoSeq
        isViewable = idol.isViewable
        nameEn = idol.nameEn
        nameJp = idol.nameJp
        nameZh = idol.nameZh
        nameZhTw = idol.nameZhTw
        name = idol.name
        miracleCount = idol.miracleCount
        sourceApp = idol.sourceApp
        fdName = idol.fdName
        fdNameEn = idol.fdNameEn
        top3Seq = idol.top3Seq
        top3ImageVer = idol.top3ImageVer
    }
}

fun IdolModel.toDomain(): Idol {
    return Idol(
        getOriginalId(),
        miracleCount,
        angelCount,
        rookieCount,
        anniversary ?: "N",
        anniversaryDays,
        birthDay,
        burningDay,
        category,
        comebackDay,
        debutDay,
        description,
        fairyCount,
        groupId,
        heart,
        imageUrl,
        imageUrl2,
        imageUrl3,
        isViewable,
        getName(),
        nameEn,
        nameJp,
        nameZh,
        nameZhTw,
        resourceUri,
        top3,
        top3Type,
        top3Seq,
        top3ImageVer,
        type,
        infoSeq,
        isLunarBirthday,
        mostCount,
        mostCountDesc,
        updateTs,
        sourceApp,
        fdName,
        fdNameEn
    )
}

fun Idol.toPresentation(): IdolModel =
    IdolModel(
        id,
        miracleCount,
        angelCount,
        rookieCount,
        anniversary ?: "N",
        anniversaryDays,
        birthDay,
        burningDay,
        category,
        comebackDay,
        debutDay,
        description,
        fairyCount,
        groupId,
        heart,
        imageUrl,
        imageUrl2,
        imageUrl3,
        isViewable,
        name,
        nameEn,
        nameJp,
        nameZh,
        nameZhTw,
        resourceUri,
        top3,
        top3Type,
        top3Seq,
        top3ImageVer,
        type,
        infoSeq,
        isLunarBirthday,
        mostCount,
        mostCountDesc,
        updateTs,
        sourceApp,
        fdName,
        fdNameEn
    )