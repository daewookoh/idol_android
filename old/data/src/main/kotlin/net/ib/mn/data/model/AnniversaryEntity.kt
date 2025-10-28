package net.ib.mn.data.model

import net.ib.mn.domain.model.Anniversary

data class AnniversaryEntity(
    var idolId: Int = 0,
    var anniversary: String? = "N",
    val anniversaryDays: Int? = null,
    val burningDay: String? = null,
    val heart: Long,
    val top3: String?,
    val top3Type: String?
)

fun Anniversary.toEntity(): AnniversaryEntity =
    AnniversaryEntity(
        idolId,
        anniversary,
        anniversaryDays,
        burningDay,
        heart,
        top3,
        top3Type
    )
