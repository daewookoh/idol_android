package net.ib.mn.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IdolLiteModel(
    val groupId: Long = 0L,
    val id: Long = 0L,
    val imageUrl: String = "",
    val name: String = "",
    val nameEN: String = "",
    val nameJP: String = "",
    val nameZH: String = "",
    val nameZHTW: String = "",
) : Parcelable