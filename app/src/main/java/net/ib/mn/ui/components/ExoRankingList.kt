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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.unit.dp
import net.ib.mn.R
import java.text.NumberFormat
import java.util.Locale

/**
 * 랭킹 아이템 데이터 클래스
 * @Immutable: 불변 데이터로 표시하여 불필요한 리컴포지션 방지
 *
 * old 프로젝트의 ranking_item.xml 및 NewRankingAdapter.kt 기반
 *
 * equals 최적화:
 * - data class의 자동 equals는 모든 필드를 비교하므로 비효율적
 * - 실제 UI에 영향을 미치는 주요 필드만 비교하여 불필요한 리컴포지션 방지
 * - 순위, 이름, 투표수, 배지 수 등 핵심 필드만 비교
 */
@Immutable
data class RankingItemData(
    val rank: Int,
    val name: String,  // "이름_그룹명" 형식 (예: "디오_EXO")
    val voteCount: String,
    val photoUrl: String? = null,
    val id: String = "",  // 고유 ID로 사용 (변경 추적에 중요)

    // 추가 필드 (old 프로젝트 기반)
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
    // LazyColumn의 key로 사용할 고유 식별자
    fun itemKey(): String = id.ifEmpty { "$rank-$name" }

    // equals 최적화: UI에 영향을 미치는 필드만 비교
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RankingItemData) return false

        // 1. ID가 다르면 다른 아이템
        if (id != other.id) return false

        // 2. 핵심 UI 필드만 비교 (순서는 변경 빈도가 높은 순)
        if (rank != other.rank) return false
        if (heartCount != other.heartCount) return false
        if (voteCount != other.voteCount) return false

        // 3. 배지 수 비교
        if (miracleCount != other.miracleCount) return false
        if (fairyCount != other.fairyCount) return false
        if (angelCount != other.angelCount) return false
        if (rookieCount != other.rookieCount) return false
        if (superRookieCount != other.superRookieCount) return false

        // 4. 기념일 비교
        if (anniversary != other.anniversary) return false
        if (anniversaryDays != other.anniversaryDays) return false

        // 5. 최애 여부
        if (isFavorite != other.isFavorite) return false

        // 6. 이름, 사진 URL (거의 변경되지 않음)
        if (name != other.name) return false
        if (photoUrl != other.photoUrl) return false

        // 7. 프로그레스 바 계산용 (전체 리스트에서 공통)
        if (maxHeartCount != other.maxHeartCount) return false
        if (minHeartCount != other.minHeartCount) return false

        // 8. Top3 URL (확장 시에만 사용, 거의 변경 안 됨)
        if (top3ImageUrls != other.top3ImageUrls) return false
        if (top3VideoUrls != other.top3VideoUrls) return false

        return true
    }

    // equals를 override하면 hashCode도 override 필요
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + rank
        result = 31 * result + heartCount.hashCode()
        result = 31 * result + voteCount.hashCode()
        result = 31 * result + miracleCount
        result = 31 * result + fairyCount
        result = 31 * result + angelCount
        result = 31 * result + isFavorite.hashCode()
        return result
    }
}


/**
 * ExoRankingList - 스크롤 가능한 랭킹 리스트
 *
 * 구조:
 * - 상단: ExoTop3 (topIdol이 있으면 자동 표시)
 * - 중간~하단: ExoRanking (아이템 리스트)
 *
 * 순수 UI 컴포넌트:
 * - ViewModel에서 완전히 가공된 데이터를 받아 표시만 함
 * - 정렬, 순위 계산, max/min 계산 등의 비즈니스 로직은 ViewModel에서 처리
 *
 * @param items 완전히 정렬되고 가공된 랭킹 아이템 리스트
 * @param topIdol 1위 아이돌 RankingItemData (ExoTop3 표시용, nullable)
 * @param isVisible 화면 가시성 (ExoTop3 동영상 재생 제어)
 * @param listState LazyColumn의 스크롤 상태 (탭 전환 시에도 유지됨)
 * @param onItemClick 아이템 클릭 이벤트 (index, item)
 * @param onVoteSuccess 투표 성공 콜백 (ViewModel로 전달)
 */
@Composable
fun ExoRankingList(
    items: List<RankingItemData>,
    topIdol: RankingItemData? = null,
    isVisible: Boolean = true,
    itemType: String = "MAIN",
    listState: LazyListState = rememberLazyListState(),
    onItemClick: (Int, RankingItemData) -> Unit = { _, _ -> },
    onVoteSuccess: (Int, Long) -> Unit = { _, _ -> }
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // ExoTop3 배너 (첫 번째 아이템)
        if (topIdol != null) {
            item(key = "exo_top3_${topIdol.id}") {
                ExoTop3(
                    rankingItemData = topIdol,
                    isVisible = isVisible
                )
            }
        }

        exoRankingItems(
            items = items,
            type = itemType,
            onItemClick = onItemClick,
            onVoteSuccess = onVoteSuccess
        )
    }
}
