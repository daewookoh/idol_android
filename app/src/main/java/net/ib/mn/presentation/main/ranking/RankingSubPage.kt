package net.ib.mn.presentation.main.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.data.model.TypeListModel
import net.ib.mn.ui.components.MainRankingList
import net.ib.mn.ui.components.RankingItemData
import net.ib.mn.util.IdolImageUtil

/**
 * 공통 Ranking SubPage
 *
 * old 프로젝트와 동일한 데이터 가공 방식:
 * 1. charts/idol_ids/ API로 아이돌 ID 리스트 획득
 * 2. ID 리스트로 로컬 DB 조회
 * 3. Heart 기준 정렬 및 순위 계산
 * 4. MainRankingList로 표시
 */
@Composable
fun RankingSubPage(
    type: TypeListModel,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    // ViewModel 주입 (AssistedInject)
    val viewModel: RankingSubPageViewModel = hiltViewModel<RankingSubPageViewModel, RankingSubPageViewModel.Factory> { factory ->
        factory.create(type)
    }

    val uiState by viewModel.uiState.collectAsState()

    // type.code가 변경되면 데이터 새로고침
    // HorizontalPager의 key가 변경되어도 ViewModel이 재사용될 수 있으므로
    // type.code 변경을 명시적으로 감지하여 리로드
    LaunchedEffect(type.code) {
        android.util.Log.d("RankingSubPage", "========================================")
        android.util.Log.d("RankingSubPage", "[RankingSubPage] type.code changed")
        android.util.Log.d("RankingSubPage", "  - New code: ${type.code}")
        android.util.Log.d("RankingSubPage", "  - Calling reloadWithNewCode")
        android.util.Log.d("RankingSubPage", "========================================")
        viewModel.reloadWithNewCode(type.code)
    }

    when (uiState) {
        is RankingSubPageViewModel.UiState.Loading -> {
            // 로딩 상태
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.main))
            }
        }

        is RankingSubPageViewModel.UiState.Error -> {
            // 에러 상태
            val error = uiState as RankingSubPageViewModel.UiState.Error
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "오류: ${error.message}",
                    fontSize = 16.sp,
                    color = colorResource(R.color.main)
                )
            }
        }

        is RankingSubPageViewModel.UiState.Success -> {
            // 성공 상태: MainRankingList에 ExoTop3 + 랭킹 아이템 표시
            val success = uiState as RankingSubPageViewModel.UiState.Success

            if (success.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "랭킹 데이터가 없습니다.",
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_dimmed)
                    )
                }
            } else {
                // ExoTop3 데이터 생성 (1위 아이돌의 top3 이미지)
                val exoTop3Data = success.topIdol?.let { topIdol ->
                    val imageUrls = IdolImageUtil.getTop3ImageUrls(topIdol)
                    val videoUrls = IdolImageUtil.getTop3VideoUrls(topIdol)

                    net.ib.mn.ui.components.ExoTop3Data(
                        id = "ranking_${type.code}",  // 각 탭마다 고유 ID
                        imageUrls = imageUrls,
                        videoUrls = videoUrls,
                        isVisible = isVisible  // HorizontalPager의 currentPage로 제어
                    )
                }

                // RankItem을 RankingItemData로 변환
                val rankingItems = success.items.map { item ->
                    RankingItemData(
                        rank = item.rank,
                        name = item.name,
                        voteCount = item.voteCount,
                        photoUrl = item.photoUrl,
                        id = item.idolId.toString() // 아이돌 ID를 고유 식별자로 사용
                    )
                }

                // MainRankingList 표시 (ExoTop3가 첫 번째 아이템으로 포함됨)
                MainRankingList(
                    items = rankingItems,
                    exoTop3Data = exoTop3Data,  // ExoTop3 데이터 전달
                    onItemClick = { rank, item ->
                        // 아이템 클릭 핸들러 (필요시 구현)
                        android.util.Log.d("RankingSubPage", "Clicked: Rank $rank - ${item.name}")
                    }
                )
            }
        }
    }
}
