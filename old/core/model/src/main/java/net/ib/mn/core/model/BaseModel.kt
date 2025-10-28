package net.ib.mn.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.Throwable

@Serializable
data class BaseModel<T>(
    var data: T? = null,
    var message: String? = null,
    var success : Boolean = false,
    var description: String? = null,
    var code: Int? = null, // http status code
    var gcode: Int? = null, // 점검 처리용
    @Transient
    var error: Throwable? = null, // 에러 처리용
)
