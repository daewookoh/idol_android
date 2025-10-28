package net.ib.mn.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import net.ib.mn.data.model.IdolEntity
import net.ib.mn.local.LocalMapper
import net.ib.mn.local.room.IdolRoomConstant

@Entity(
    tableName = IdolRoomConstant.Table.IDOL,
    indices = [Index(name = "idx_type_category", value = ["type", "category"]), Index("id")]
)
data class IdolLocal(
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
    val infoSeq: Int = -1, //서버에 이미 info_ver 필드가 존재(저장된 값의 형태가 다름)해서 변경해서 보내주기로 함
    val isLunarBirthday: String? = null,
    val mostCount: Int = 0,
    val mostCountDesc: String? = null,
    val updateTs: Int = 0,
    val sourceApp: String? = null,
    val fdName: String? = null,
    val fdNameEn: String? = null
) : LocalMapper<IdolEntity> {

    override fun toData(): IdolEntity =
        IdolEntity(
            id,
            miracleCount,
            angelCount,
            rookieCount,
            anniversary,
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
}

fun IdolEntity.toLocal(): IdolLocal =
    IdolLocal(
        id,
        miracleCount,
        angelCount,
        rookieCount,
        anniversary,
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
        top3ImageVer ?: "",
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