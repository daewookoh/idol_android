package net.ib.mn.ui.components

import androidx.compose.runtime.compositionLocalOf

/**
 * 랭킹 아이템 클릭 콜백을 위한 CompositionLocal
 *
 * MainScreen에서 제공하고, ExoRankingItem에서 소비합니다.
 * 이를 통해 콜백 체인 없이 직접 CommunityScreen을 열 수 있습니다.
 *
 * 사용 예시:
 * ```
 * // MainScreen에서 제공
 * CompositionLocalProvider(
 *     LocalRankingItemClick provides { item -> viewModel.openCommunity(item) }
 * ) {
 *     RankingPage()
 * }
 *
 * // ExoRankingItem에서 소비
 * val onItemClick = LocalRankingItemClick.current
 * Row(modifier = Modifier.clickable { onItemClick(item) }) { ... }
 * ```
 */
val LocalRankingItemClick = compositionLocalOf<(RankingItem) -> Unit> {
    // 기본값: 아무 동작도 하지 않음
    { _ -> }
}
