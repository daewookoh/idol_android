package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ObjectBaseDataModel<T>(
    @SerialName("object") var data: T? = null,
    var success: Boolean = false,
    var gcode: Int = 0,
    var msg: String? = null,
    var description: String? = null,
)
