package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import net.ib.mn.R

/**
 * 랭킹 아이템 데이터 클래스
 * @Immutable: 불변 데이터로 표시하여 불필요한 리컴포지션 방지
 *
 * old 프로젝트의 ranking_item.xml 및 NewRankingAdapter.kt 기반
 */
@Immutable
data class RankingItemData(
    val rank: Int,
    val name: String,
    val voteCount: String,
    val photoUrl: String? = null,
    val id: String = "",  // 고유 ID로 사용 (변경 추적에 중요)

    // 추가 필드 (old 프로젝트 기반)
    val groupName: String? = null,  // 그룹명 (예: "TWICE")
    val anniversary: String? = null,  // 기념일 타입 ("BIRTH", "DEBUT", "COMEBACK", "MEMORIAL_DAY", "ALL_IN_DAY")
    val anniversaryDays: Int = 0,  // 몰빵일 일수
    val miracleCount: Int = 0,  // 미라클 배지 수
    val fairyCount: Int = 0,  // 요정 배지 수
    val angelCount: Int = 0,  // 천사 배지 수
    val rookieCount: Int = 0,  // 루키 배지 수
    val superRookieCount: Int = 0,  // 슈퍼 루키 배지 수
    val isFavorite: Boolean = false,  // 최애 여부 (배경색 하이라이트)
    val heartCount: Long = 0,  // 실제 하트 수 (프로그레스 바 계산용)
    val maxHeartCount: Long = 0,  // 1등 하트 수 (프로그레스 바 계산용)
    val minHeartCount: Long = 0,  // 꼴등 하트 수 (프로그레스 바 계산용)
    val top3ImageUrls: List<String?> = listOf(null, null, null),  // 펼치기 이미지 3개
    val top3VideoUrls: List<String?> = listOf(null, null, null),  // 펼치기 동영상 3개
) {
    // equals/hashCode는 data class가 자동 생성하지만,
    // LazyColumn의 key로 사용할 고유 식별자
    fun itemKey(): String = id.ifEmpty { "$rank-$name" }
}

/**
 * ExoTop3 데이터 클래스
 * @Immutable: 불변 데이터로 표시하여 불필요한 리컴포지션 방지
 */
@Immutable
data class ExoTop3Data(
    val id: String,                      // 고유 ID (예: "ranking_SOLO_M")
    val imageUrls: List<String?>,        // 3개의 이미지 URL
    val videoUrls: List<String?> = listOf(null, null, null), // 3개의 동영상 URL
    val isVisible: Boolean = true        // HorizontalPager의 currentPage로 제어
)

/**
 * MainRankingList - 스크롤 가능한 랭킹 리스트
 *
 * 구조:
 * - 상단: ExoTop3 (exoTop3Data가 있으면 자동 표시)
 * - 중간~하단: MainRankingItem 리스트
 *
 * @param items 랭킹 아이템 리스트
 * @param exoTop3Data ExoTop3 배너 데이터 (nullable)
 * @param listState LazyColumn의 스크롤 상태 (탭 전환 시에도 유지됨)
 * @param onItemClick 아이템 클릭 이벤트 (index, item)
 */
@Composable
fun MainRankingList(
    items: List<RankingItemData>,
    exoTop3Data: ExoTop3Data? = null,
    listState: LazyListState = rememberLazyListState(),
    onItemClick: (Int, RankingItemData) -> Unit = { _, _ -> }
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100)),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // ExoTop3 배너 (첫 번째 아이템)
        if (exoTop3Data != null) {
            item(key = "exo_top3_${exoTop3Data.id}") {
                ExoTop3(
                    id = exoTop3Data.id,
                    imageUrls = exoTop3Data.imageUrls,
                    videoUrls = exoTop3Data.videoUrls,
                    isVisible = exoTop3Data.isVisible
                )
            }
        }

        // 랭킹 아이템 리스트
        // key를 사용하여 아이템이 변경될 때 올바른 리컴포지션 수행
        // 랭킹이 바뀌면 item.id는 유지되므로 순서만 바뀌고 불필요한 리렌더링 방지
        itemsIndexed(
            items = items,
            key = { _, item -> item.itemKey() }  // 고유 ID로 아이템 추적
        ) { index, item ->
            MainRankingItem(
                rank = item.rank,
                name = item.name,
                voteCount = item.voteCount,
                photoUrl = item.photoUrl,
                showDivider = false,
                onClick = { onItemClick(index, item) }
            )
        }
    }
}
