package net.ib.mn.core.model

import kotlinx.serialization.Serializable

/**
 * api 오류 선처리 후 후처리하지 않게 빈 모델 보내는 용도
 */
@Serializable
data class EmptyModel (
    var msg: String? = null,
    var success : Boolean = false,
)
