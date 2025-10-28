package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BadWordModel(
    @SerialName("exc") val exc: List<String> = emptyList(),
    @SerialName("type") val type: String,
    @SerialName("word") val word: String
)