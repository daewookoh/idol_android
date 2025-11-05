package net.ib.mn.presentation.main.ranking.idol_subpage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import net.ib.mn.ui.components.RankingItemData
import net.ib.mn.util.IdolImageUtil
import java.text.Collator
import java.text.NumberFormat
import java.util.Locale

/**
 * Group (그룹) 랭킹 ViewModel
 *
 * charts/idol_ids/ API 사용
 * 남녀 변경에 영향을 받음
 */
@HiltViewModel(assistedFactory = GroupRankingSubPageViewModel.Factory::class)
class GroupRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @ApplicationContext private val context: Context,
    private val rankingRepository: RankingRepository,
    private val idolDao: IdolDao
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankingItemData>,
            val topIdol: IdolEntity? = null
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 현재 사용 중인 차트 코드 (남녀 변경 시 업데이트됨)
    private var currentChartCode: String = chartCode

    // 코드별 캐시 (남녀 변경 시에도 이전 데이터 유지)
    private val codeToIdListMap = mutableMapOf<String, ArrayList<Int>>()

    init {
        android.util.Log.d("GroupRankingSubPageVM", "🆕 ViewModel created for chartCode: $chartCode")
        loadRankingData()
    }

    /**
     * 남녀 변경 시 호출 - 새로운 차트 코드로 데이터 로드
     */
    fun reloadWithNewCode(newCode: String) {
        android.util.Log.d("GroupRankingSubPageVM", "🔄 Reloading with new code: $newCode (previous: $currentChartCode)")

        // 같은 코드면 캐시된 데이터 사용
        if (newCode == currentChartCode) {
            val cachedIds = codeToIdListMap[newCode]
            if (cachedIds != null && cachedIds.isNotEmpty()) {
                android.util.Log.d("GroupRankingSubPageVM", "✓ Using cached data for $newCode")
                viewModelScope.launch(Dispatchers.IO) {
                    queryIdolsByIdsFromDb(cachedIds)
                }
                return
            }
        }

        // 새로운 코드로 업데이트하고 데이터 로드
        currentChartCode = newCode

        val cachedIds = codeToIdListMap[newCode]
        if (cachedIds != null && cachedIds.isNotEmpty()) {
            android.util.Log.d("GroupRankingSubPageVM", "✓ Using cached data for $newCode")
            viewModelScope.launch(Dispatchers.IO) {
                queryIdolsByIdsFromDb(cachedIds)
            }
        } else {
            android.util.Log.d("GroupRankingSubPageVM", "📡 Fetching new data for $newCode")
            loadRankingData()
        }
    }

    private fun loadRankingData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            android.util.Log.d("GroupRankingSubPageVM", "========================================")
            android.util.Log.d("GroupRankingSubPageVM", "[Group] Loading ranking data")
            android.util.Log.d("GroupRankingSubPageVM", "  - currentChartCode: $currentChartCode")
            android.util.Log.d("GroupRankingSubPageVM", "  - API: charts/idol_ids/")

            // charts/idol_ids/ API 호출
            rankingRepository.getChartIdolIds(currentChartCode).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d("GroupRankingSubPageVM", "⏳ Loading...")
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d("GroupRankingSubPageVM", "✅ SUCCESS - IDs count: ${result.data.size}")
                        val ids = ArrayList(result.data)
                        codeToIdListMap[currentChartCode] = ids
                        queryIdolsByIdsFromDb(ids)
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e("GroupRankingSubPageVM", "❌ ERROR: ${result.message}")
                        _uiState.value = UiState.Error(result.message ?: "Error loading data")
                    }
                }
            }
        }
    }

    private suspend fun queryIdolsByIdsFromDb(ids: List<Int>) {
        if (ids.isEmpty()) {
            _uiState.value = UiState.Success(emptyList())
            return
        }

        try {
            val idols = idolDao.getIdolsByIds(ids)

            if (idols.isEmpty()) {
                _uiState.value = UiState.Error("DB에 아이돌 데이터가 없습니다.")
                return
            }

            // Heart 기준 정렬 및 순위 계산
            val sortedIdols = sortAndRankIdols(idols)

            // 프로그레스 바 계산을 위한 최대/최소 점수
            val maxScore = sortedIdols.maxOfOrNull { it.idol.heart } ?: 0L
            val minScore = sortedIdols.minOfOrNull { it.idol.heart } ?: 0L

            // RankingItemData로 변환
            val rankItems = sortedIdols.map { idolWithRank ->
                // name에서 이름과 그룹명 분리 (예: "디오_EXO" -> name="디오", groupName="EXO")
                val nameParts = idolWithRank.idol.name.split("_")
                val actualName = nameParts.getOrNull(0) ?: idolWithRank.idol.name
                val actualGroupName = nameParts.getOrNull(1)

                RankingItemData(
                    rank = idolWithRank.rank,
                    name = actualName,
                    voteCount = formatHeartCount(idolWithRank.idol.heart.toInt()),
                    photoUrl = idolWithRank.idol.imageUrl,
                    id = idolWithRank.idol.id.toString(),
                    groupName = actualGroupName,
                    miracleCount = idolWithRank.idol.miracleCount,
                    fairyCount = idolWithRank.idol.fairyCount,
                    angelCount = idolWithRank.idol.angelCount,
                    rookieCount = idolWithRank.idol.rookieCount,
                    heartCount = idolWithRank.idol.heart,
                    maxHeartCount = maxScore,
                    minHeartCount = minScore,
                    top3ImageUrls = IdolImageUtil.getTop3ImageUrls(idolWithRank.idol),
                    top3VideoUrls = IdolImageUtil.getTop3VideoUrls(idolWithRank.idol)
                )
            }

            android.util.Log.d("GroupRankingSubPageVM", "✅ Processed ${rankItems.size} items")

            _uiState.value = UiState.Success(
                items = rankItems,
                topIdol = sortedIdols.firstOrNull()?.idol
            )
        } catch (e: Exception) {
            android.util.Log.e("GroupRankingSubPageVM", "❌ Exception: ${e.message}", e)
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    private fun sortAndRankIdols(idols: List<IdolEntity>): List<IdolWithRank> {
        val collator = Collator.getInstance(Locale.ROOT).apply {
            strength = Collator.PRIMARY
        }

        val sorted = idols.sortedWith(
            compareByDescending<IdolEntity> { it.heart }
                .thenComparator { a, b -> collator.compare(a.name, b.name) }
        )

        val result = mutableListOf<IdolWithRank>()
        sorted.forEachIndexed { index, idol ->
            val rank = if (index > 0 && sorted[index - 1].heart == idol.heart) {
                result[index - 1].rank
            } else {
                index + 1
            }
            result.add(IdolWithRank(idol, rank))
        }

        return result
    }

    private fun formatHeartCount(count: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(count)
    }

    data class IdolWithRank(val idol: IdolEntity, val rank: Int)

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String): GroupRankingSubPageViewModel
    }
}
