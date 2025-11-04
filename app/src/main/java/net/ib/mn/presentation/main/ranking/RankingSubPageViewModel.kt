package net.ib.mn.presentation.main.ranking

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
import net.ib.mn.data.model.TypeListModel
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import java.text.Collator
import java.text.NumberFormat
import java.util.Locale

/**
 * RankingSubPage ViewModel
 *
 * old 프로젝트와 동일한 데이터 가공 방식:
 * 1. API로 차트 코드별 아이돌 ID 리스트 획득
 * 2. ID 리스트로 로컬 DB 조회
 * 3. Heart 기준 정렬 및 순위 계산
 * 4. MainRankingList용 데이터로 변환
 */
@HiltViewModel(assistedFactory = RankingSubPageViewModel.Factory::class)
class RankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val type: TypeListModel,
    @ApplicationContext private val context: Context,
    private val rankingRepository: RankingRepository,
    private val idolDao: IdolDao
) : ViewModel() {

    /**
     * UI 상태 정의
     */
    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankItem>,
            val topIdol: IdolEntity? = null  // 1위 아이돌 (ExoTop3용)
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // old 프로젝트와 동일: 캐싱을 위한 변수
    private var currentChartCode = ""
    private var idList = arrayListOf<Int>()

    init {
        loadRankingData()
    }

    /**
     * 차트 코드가 변경되었을 때 데이터 새로고침
     * (성별 카테고리 변경 시 호출)
     */
    fun reloadWithNewCode(newCode: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            android.util.Log.d("RankingSubPageVM", "🔄 Reloading with new code: $newCode (previous: ${type.code})")

            // 코드가 변경되었으면 캐시 초기화
            if (newCode != currentChartCode) {
                android.util.Log.d("RankingSubPageVM", "  ✓ Code changed, clearing cache")
                currentChartCode = ""
                idList.clear()
            }

            loadRankingData(newCode)
        }
    }

    /**
     * 랭킹 데이터 로드
     *
     * old 프로젝트와 동일한 방식:
     * 1. PR_ 또는 GLOBAL로 시작하는 코드는 getChartIdolIds API 사용
     * 2. 캐시된 idList가 있으면 API 호출 없이 DB만 조회
     * 3. ID 리스트로 DB 조회 후 정렬 및 순위 계산
     *
     * @param overrideCode 코드를 덮어쓸 경우 사용 (성별 변경 시)
     */
    private fun loadRankingData(overrideCode: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            val code = overrideCode ?: type.code
            android.util.Log.d("RankingSubPageVM", "========================================")
            android.util.Log.d("RankingSubPageVM", "[RankingSubPageViewModel] Loading ranking data")
            android.util.Log.d("RankingSubPageVM", "  - type: ${type.type}")
            android.util.Log.d("RankingSubPageVM", "  - code: $code")
            android.util.Log.d("RankingSubPageVM", "  - isFemale: ${type.isFemale}")

            // PR_ 또는 GLOBAL로 시작하는 경우만 처리
            if (code != null && (code.startsWith("PR_") || code.startsWith("GLOBAL"))) {
                android.util.Log.d("RankingSubPageVM", "✓ Using charts/idol_ids/ API for code: $code")
                loadIdolsByChartCode(code)
            } else {
                android.util.Log.d("RankingSubPageVM", "❌ Code does not start with PR_ or GLOBAL, skipping")
                _uiState.value = UiState.Error("지원하지 않는 차트 타입입니다.")
            }
        }
    }

    /**
     * 차트 코드로 아이돌 데이터 로드 (old 프로젝트 방식)
     *
     * 1. 캐시 체크: 동일 chartCode이고 idList가 있으면 DB만 조회
     * 2. 새로운 차트: API로 ID 리스트 획득 후 DB 조회
     * 3. 정렬 및 순위 계산
     */
    private suspend fun loadIdolsByChartCode(chartCode: String) {
        // 캐시 체크
        if (idList.isNotEmpty() && currentChartCode == chartCode) {
            android.util.Log.d("RankingSubPageVM", "📦 Using cached idList, querying DB only")
            queryIdolsByIdsFromDb(idList)
            return
        }

        // 새로운 차트: API 호출
        currentChartCode = chartCode

        rankingRepository.getChartIdolIds(chartCode).collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    android.util.Log.d("RankingSubPageVM", "⏳ Loading idol IDs from API...")
                }
                is ApiResult.Success -> {
                    android.util.Log.d("RankingSubPageVM", "✅ Idol IDs loaded from API")
                    android.util.Log.d("RankingSubPageVM", "  - IDs count: ${result.data.size}")
                    android.util.Log.d("RankingSubPageVM", "  - First 10 IDs: ${result.data.take(10)}")

                    idList = ArrayList(result.data)
                    queryIdolsByIdsFromDb(idList)
                }
                is ApiResult.Error -> {
                    android.util.Log.e("RankingSubPageVM", "❌ Error loading idol IDs: ${result.message}")
                    _uiState.value = UiState.Error(
                        result.message ?: "아이돌 ID 목록을 불러오는데 실패했습니다."
                    )
                }
            }
        }
    }

    /**
     * ID 리스트로 DB 조회 후 정렬 및 순위 계산 (old 프로젝트와 동일)
     */
    private suspend fun queryIdolsByIdsFromDb(ids: List<Int>) {
        if (ids.isEmpty()) {
            android.util.Log.w("RankingSubPageVM", "⚠️ ID list is empty")
            _uiState.value = UiState.Success(emptyList())
            return
        }

        try {
            android.util.Log.d("RankingSubPageVM", "🔍 Querying idols from DB...")
            android.util.Log.d("RankingSubPageVM", "  - Query IDs: ${ids.take(10)}...")

            // DB에서 아이돌 조회
            val idols = idolDao.getIdolsByIds(ids)

            android.util.Log.d("RankingSubPageVM", "📊 Idols retrieved from DB:")
            android.util.Log.d("RankingSubPageVM", "  - Total count: ${idols.size}")

            if (idols.isEmpty()) {
                android.util.Log.w("RankingSubPageVM", "⚠️ No idols found in DB for given IDs")
                _uiState.value = UiState.Error("DB에 아이돌 데이터가 없습니다. 앱을 다시 시작해주세요.")
                return
            }

            // 정렬 및 순위 계산
            val sortedIdols = sortAndRankIdols(idols)

            android.util.Log.d("RankingSubPageVM", "✅ Sorted and ranked ${sortedIdols.size} idols")

            // RankItem으로 변환
            val rankItems = sortedIdols.map { idolWithRank ->
                RankItem(
                    rank = idolWithRank.rank,
                    name = idolWithRank.idol.name,
                    voteCount = formatHeartCount(idolWithRank.idol.heartCount),
                    photoUrl = idolWithRank.idol.imageUrl,
                    idolId = idolWithRank.idol.id  // 아이돌 고유 ID 추가
                )
            }

            android.util.Log.d("RankingSubPageVM", "📋 Top 5 items:")
            rankItems.take(5).forEach { item ->
                android.util.Log.d("RankingSubPageVM", "  - Rank ${item.rank}: ${item.name} (${item.voteCount})")
            }

            // 1위 아이돌 (ExoTop3용)
            val topIdol = sortedIdols.firstOrNull()?.idol

            _uiState.value = UiState.Success(
                items = rankItems,
                topIdol = topIdol
            )
        } catch (e: Exception) {
            android.util.Log.e("RankingSubPageVM", "❌ Exception querying DB: ${e.message}", e)
            _uiState.value = UiState.Error(
                e.message ?: "DB 조회 중 오류가 발생했습니다."
            )
        }
    }

    /**
     * 아이돌 정렬 및 순위 계산 (old 프로젝트와 동일)
     *
     * 1차 정렬: Heart 내림차순
     * 2차 정렬: 이름 오름차순 (동점일 경우)
     * 순위: 동점자는 같은 순위 부여
     */
    private fun sortAndRankIdols(idols: List<IdolEntity>): List<IdolWithRank> {
        // Collator for locale-aware string comparison
        val collator = Collator.getInstance(Locale.ROOT).apply {
            strength = Collator.PRIMARY
        }

        // 정렬
        val sorted = idols.sortedWith(
            compareByDescending<IdolEntity> { it.heartCount }
                .thenComparator { a, b -> collator.compare(a.name, b.name) }
        )

        // 순위 계산
        val result = mutableListOf<IdolWithRank>()
        sorted.forEachIndexed { index, idol ->
            val rank = if (index > 0 && sorted[index - 1].heartCount == idol.heartCount) {
                result[index - 1].rank  // 동점일 경우 같은 순위
            } else {
                index + 1  // 순위는 1부터 시작
            }
            result.add(IdolWithRank(idol, rank))
        }

        return result
    }

    /**
     * Heart count 포맷팅 (천 단위 콤마)
     */
    private fun formatHeartCount(count: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(count)
    }

    /**
     * 랭킹 데이터 새로고침 (old 프로젝트와 동일)
     *
     * API 호출 없이 캐시된 idList로 DB만 조회
     */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true

            android.util.Log.d("RankingSubPageVM", "🔄 Refreshing data (DB only)...")

            if (idList.isEmpty()) {
                android.util.Log.w("RankingSubPageVM", "⚠️ No cached idList, reloading from API")
                loadRankingData()
            } else {
                queryIdolsByIdsFromDb(idList)
            }

            _isRefreshing.value = false
        }
    }

    /**
     * AssistedFactory for Hilt
     *
     * 런타임에 type 파라미터를 주입받기 위해 필요
     */
    @AssistedFactory
    interface Factory {
        fun create(type: TypeListModel): RankingSubPageViewModel
    }
}

/**
 * MainRankingList에 전달할 랭킹 아이템 데이터 클래스
 */
data class RankItem(
    val rank: Int,
    val name: String,
    val voteCount: String,
    val photoUrl: String?,
    val idolId: Int  // 아이돌 고유 ID (LazyColumn key로 사용)
)

/**
 * 순위 정보를 포함한 아이돌 데이터
 */
data class IdolWithRank(
    val idol: IdolEntity,
    val rank: Int
)
