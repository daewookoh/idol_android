package net.ib.mn.data.remote.dto

/**
 * 투표 Top100 유저 모델
 *
 * old 프로젝트의 UserModel을 기반으로 생성
 * users/ranked_user/ API 응답 데이터
 * 특정 아이돌에게 투표한 유저들의 랭킹
 *
 * Note: Gson의 LOWER_CASE_WITH_UNDERSCORES 정책 사용
 * - JSON snake_case → Kotlin camelCase 자동 변환
 * - @SerializedName 사용하지 않음 (Old 프로젝트와 동일)
 */
data class VoterTop100Model(
    val id: Int = 0,
    val nickname: String = "",
    val imageUrl: String? = null,
    val levelHeart: Long = 0,
    val level: Int = 0,
    val email: String? = null,
    val emoticon: EmoticonModel? = null,
    // 순위 (클라이언트에서 설정, 0부터 시작)
    var rank: Int = 0
)

/**
 * 이모티콘 모델
 */
data class EmoticonModel(
    val id: Int = 0,
    val emojiUrl: String? = null
)
