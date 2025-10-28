package net.ib.mn.domain.model

data class Anniversary(
    var idolId: Int = 0,
    var anniversary: String? = "N",
    val anniversaryDays: Int? = null,
    val burningDay: String? = null,
    val heart: Long,
    val top3: String?,
    val top3Type: String?
)