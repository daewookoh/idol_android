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

/**
 * IdolRankingHistory 화면으로 이동하기 위한 CompositionLocal
 *
 * CUMULATIVE 타입 랭킹 아이템 클릭 시 사용됩니다.
 * MainScreen에서 제공하고, cumulativeRankingItems에서 소비합니다.
 */
val LocalIdolRankingHistoryClick = compositionLocalOf<(RankingItem) -> Unit> {
    // 기본값: 아무 동작도 하지 않음
    { _ -> }
}

/**
 * DailyRankingHistory 화면으로 이동하기 위한 CompositionLocal
 *
 * 명예의 전당 일일 랭킹 아이템 클릭 시 사용됩니다.
 * MainScreen에서 제공하고, HofDailyRankingItem에서 소비합니다.
 *
 * @param dailyRankModel 일일 랭킹 모델
 * @param chartCode 차트 코드 (app 플레이버에서 필요)
 */
val LocalHofDailyItemClick = compositionLocalOf<(net.ib.mn.data.remote.dto.DailyRankModel, String) -> Unit> {
    // 기본값: 아무 동작도 하지 않음
    { _, _ -> }
}

/**
 * HeartPickDetail 화면으로 이동하기 위한 CompositionLocal
 *
 * HeartPick 카드 클릭 시 사용됩니다.
 * MainScreen에서 제공하고, HeartPickRankingSubPage에서 소비합니다.
 *
 * @param heartPickId 하트픽 ID
 */
val LocalHeartPickDetailClick = compositionLocalOf<(Int) -> Unit> {
    // 기본값: 아무 동작도 하지 않음
    { _ -> }
}

/**
 * ThemePickDetail 화면으로 이동하기 위한 CompositionLocal
 *
 * ThemePick 카드 클릭 시 사용됩니다.
 * MainScreen에서 제공하고, RankingPage에서 소비합니다.
 *
 * @param themePickId 테마픽 ID
 */
val LocalThemePickDetailClick = compositionLocalOf<(Int) -> Unit> {
    // 기본값: 아무 동작도 하지 않음
    { _ -> }
}

/**
 * ThemePickLive 화면으로 이동하기 위한 CompositionLocal
 *
 * ThemePick 실시간 순위 화면으로 이동 시 사용됩니다.
 * MainScreen에서 제공하고, ThemePickDetailScreen에서 소비합니다.
 *
 * @param themePickId 테마픽 ID
 */
val LocalThemePickLiveClick = compositionLocalOf<(Int) -> Unit> {
    // 기본값: 아무 동작도 하지 않음
    { _ -> }
}

/**
 * ImagePickDetail 화면으로 이동하기 위한 CompositionLocal
 *
 * ImagePick 카드 클릭 시 사용됩니다.
 * MainScreen에서 제공하고, OnePickRankingSubPage에서 소비합니다.
 *
 * @param imagePickId 이미지픽 ID
 */
val LocalImagePickDetailClick = compositionLocalOf<(Int) -> Unit> {
    // 기본값: 아무 동작도 하지 않음
    { _ -> }
}

/**
 * ImagePickLive 화면으로 이동하기 위한 CompositionLocal
 *
 * ImagePick 실시간 순위 화면으로 이동 시 사용됩니다.
 * MainScreen에서 제공하고, ImagePickDetailScreen에서 소비합니다.
 *
 * @param imagePickId 이미지픽 ID
 */
val LocalImagePickLiveClick = compositionLocalOf<(Int) -> Unit> {
    // 기본값: 아무 동작도 하지 않음
    { _ -> }
}
