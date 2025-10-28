package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRoomCreateModel(
    @SerialName("idol_id")val idolId:Int?= 0,
    @SerialName("locale") val locale: String? = "",
    @SerialName("title") val title: String? = "",
    @SerialName("desc") val desc: String? = "",
    @SerialName("is_most_only") val isMostOnly: String? = "",
    @SerialName("is_anonymity") val isAnonymity: String? = "",
    @SerialName("level_limit") val levelLimit: Int? = 0,
    @SerialName("max_people") val maxPeople: Int? = 0
)
