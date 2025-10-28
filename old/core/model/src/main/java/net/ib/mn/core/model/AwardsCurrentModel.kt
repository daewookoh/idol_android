package net.ib.mn.core.model

import kotlinx.serialization.Serializable


@Serializable
data class AwardsCurrentModel(
    var award: AwardModel? = null,
    var gcode: Int? = null,
    var msg: String? = null,
    var success : Boolean = false,
)
