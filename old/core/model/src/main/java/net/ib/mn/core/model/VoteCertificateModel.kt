package net.ib.mn.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VoteCertificateModel(
    val grade: String = "",
    val idol: IdolLiteModel = IdolLiteModel(),
    val refDate: String = "",
    val vote: Long = 0L
) : Parcelable
