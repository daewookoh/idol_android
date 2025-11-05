package net.ib.mn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import net.ib.mn.data.model.Idol

/**
 * Room Entity for Idol data - 완전히 old 프로젝트(IdolLocal)와 동일한 구조
 *
 * old 프로젝트: local/src/main/kotlin/net/ib/mn/local/model/IdolLocal.kt
 */
@Entity(
    tableName = "idols",
    indices = [Index(name = "idx_type_category", value = ["type", "category"]), Index("id")]
)
data class IdolEntity(
    @PrimaryKey val id: Int,
    val miracleCount: Int = 0,
    val angelCount: Int = 0,
    val rookieCount: Int = 0,
    val anniversary: String = "N",
    val anniversaryDays: Int? = null,
    val birthDay: String? = null,
    val burningDay: String? = null,
    val category: String = "",
    val comebackDay: String? = null,
    val debutDay: String? = null,
    val description: String = "",
    val fairyCount: Int = 0,
    val groupId: Int = 0,
    val heart: Long = 0,
    val imageUrl: String? = null,
    val imageUrl2: String? = null,
    val imageUrl3: String? = null,
    val isViewable: String = "Y",
    val name: String = "",
    val nameEn: String = "",
    val nameJp: String = "",
    val nameZh: String = "",
    val nameZhTw: String = "",
    val resourceUri: String = "",
    val top3: String? = null,
    val top3Type: String? = null,
    val top3Seq: Int = -1,
    val top3ImageVer: String = "",
    val type: String = "",
    val infoSeq: Int = -1,
    val isLunarBirthday: String? = null,
    val mostCount: Int = 0,
    val mostCountDesc: String? = null,
    val updateTs: Int = 0,
    val sourceApp: String? = null,
    val fdName: String? = null,
    val fdNameEn: String? = null
)

/**
 * Extension function to convert Entity to Domain model.
 */
fun IdolEntity.toDomain(): Idol {
    return Idol(
        id = id,
        name = name,
        group = null,  // TODO: group 정보는 groupId로부터 가져와야 함
        imageUrl = imageUrl,
        heartCount = heart.toInt(),
        isTop3 = top3 != null
    )
}

/**
 * Extension function to convert Domain model to Entity.
 */
fun Idol.toEntity(): IdolEntity {
    return IdolEntity(
        id = id,
        name = name,
        imageUrl = imageUrl,
        heart = heartCount.toLong(),
        top3 = if (isTop3) "1,2,3" else null  // placeholder
    )
}
