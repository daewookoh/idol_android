package net.ib.mn.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BaseDataModel<T>(
    var data: T? = null,
    var success: Boolean = false,
    var gcode: Int = 0,
    var msg: String? = null,
)
