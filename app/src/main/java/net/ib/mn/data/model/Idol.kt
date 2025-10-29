package net.ib.mn.data.model

/**
 * Idol 도메인 모델.
 * 전체 앱에서 사용하는 기본 모델.
 */
data class Idol(
    val id: Int,
    val name: String,
    val group: String?,
    val imageUrl: String?,
    val heartCount: Int = 0,
    val isTop3: Boolean = false
)
