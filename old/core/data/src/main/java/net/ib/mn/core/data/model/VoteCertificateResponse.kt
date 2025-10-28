package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoteCertificateResponse(
    @SerialName("grade") val grade: String = "",
    @SerialName("idol") val idol: IdolLiteResponse = IdolLiteResponse(),
    @SerialName("refdate") val refDate: String = "",
    @SerialName("vote") val vote: Long = 0L
)