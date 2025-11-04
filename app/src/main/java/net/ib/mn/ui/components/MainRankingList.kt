package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import net.ib.mn.R

/**
 * 랭킹 아이템 데이터 클래스
 */
data class RankingItemData(
    val rank: Int,
    val name: String,
    val voteCount: String,
    val photoUrl: String? = null,
    val id: String = ""
)

/**
 * 배너 데이터 클래스
 */
data class BannerData(
    val title: String? = null,
    val imageUrl: String? = null
)

/**
 * MainRankingList - 스크롤 가능한 랭킹 리스트
 *
 * 구조:
 * - 상단: ExoTop3 (첫번째 데이터가 있으면 자동 표시)
 * - 중간~하단: MainRankingItem 리스트
 *
 * @param items 랭킹 아이템 리스트
 * @param onBannerClick 배너 클릭 이벤트
 * @param onItemClick 아이템 클릭 이벤트 (index, item)
 */
@Composable
fun MainRankingList(
    items: List<RankingItemData>,
    onBannerClick: () -> Unit = {},
    onItemClick: (Int, RankingItemData) -> Unit = { _, _ -> }
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100)),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // 첫번째 데이터가 있으면 ExoTop3 표시
        if (items.isNotEmpty()) {
            item(key = "banner_top") {
                val firstItem = items.first()
                ExoTop3(
                    title = "${firstItem.rank}위 - ${firstItem.name}",
                    imageUrl = firstItem.photoUrl,
                    onClick = onBannerClick
                )
            }
        }

        // 랭킹 아이템 리스트
        itemsIndexed(
            items = items,
            key = { index, item -> item.id.ifEmpty { "item_$index" } }
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
