package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AwardChartsModel(
    @SerialName("code") val code: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("subname") val subname: String? = null,
    @SerialName("desc") val desc: String? = null, // 실시간 설명
    @SerialName("example_title") val exampleTitle: String? = null, // 예시 제목
    @SerialName("example_desc") val exampleDesc: String? = null, // 예시 설명
) : java.io.Serializable