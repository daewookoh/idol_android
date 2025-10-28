package net.ib.mn.domain.model

data class IdolFiledData(
    val id: Int,
    val heart: Long,
    val top3: String?,
    val top3Type: String?,
    val top3ImageVer: String?,
    val imageUrl: String?,
    val imageUrl2: String?,
    val imageUrl3: String?,
)