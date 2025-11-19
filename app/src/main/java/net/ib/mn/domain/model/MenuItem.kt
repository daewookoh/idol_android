package net.ib.mn.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * 메뉴 아이템 타입
 */
sealed class MenuItem(
    open val id: String,
    @StringRes open val labelResId: Int,
    @DrawableRes open val iconResId: Int
)

/**
 * 아이콘 메뉴 아이템 (4열 그리드로 표시)
 */
data class IconMenuItem(
    override val id: String,
    override val labelResId: Int,
    override val iconResId: Int,
    val type: IconMenuType,
    val hasBadge: Boolean = false,
    @DrawableRes val badgeIconResId: Int? = null
) : MenuItem(id, labelResId, iconResId)

/**
 * 텍스트 메뉴 아이템 (리스트로 표시)
 */
data class TextMenuItem(
    override val id: String,
    override val labelResId: Int,
    override val iconResId: Int,
    val type: TextMenuType,
    val hasBadge: Boolean = false
) : MenuItem(id, labelResId, iconResId)

/**
 * 아이콘 메뉴 타입
 */
enum class IconMenuType {
    SUPPORT,        // 고객센터
    FREE_CHARGE,    // 무료충전소
    ATTENDANCE,     // 출석체크
    EVENT,          // 이벤트
    STORE,          // 상점
    NOTICE,         // 공지사항
    FREE_BOARD      // 자유게시판
}

/**
 * 텍스트 메뉴 타입
 */
enum class TextMenuType {
    VOTE_CERTIFICATE,   // 투표 인증서
    FREE_BOARD,         // 자유게시판
    STORE,              // 상점
    NOTICE,             // 공지사항
    INVITE_FRIEND,      // 친구 초대
    HISTORY,            // 기록실
    GAME,               // 미니게임
    QUIZ,               // 퀴즈
    FACE                // 닮은꼴
}
