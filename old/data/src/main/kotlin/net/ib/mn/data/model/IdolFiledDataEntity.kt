package net.ib.mn.data.model

import net.ib.mn.domain.model.IdolFiledData

enum class IdolContentTypeDataEntity(val type: String) {
    PHOTO("P"),
    VIDEO("V");
}

data class IdolFiledDataEntity(
    val id: Int,
    var heart: Long,
    val top3: String?,
    val top3Type: String?,
    val top3ImageVer: String?,
    val imageUrl: String?,
    val imageUrl2: String?,
    val imageUrl3: String?,
)

fun IdolFiledData.toIdolFiledDataEntity(): IdolFiledDataEntity {
    return IdolFiledDataEntity(
        id = id,
        heart = heart,
        top3 = top3,
        top3Type = top3Type,
        top3ImageVer = top3ImageVer,
        imageUrl = imageUrl,
        imageUrl2 = imageUrl2,
        imageUrl3 = imageUrl3,
    )
}