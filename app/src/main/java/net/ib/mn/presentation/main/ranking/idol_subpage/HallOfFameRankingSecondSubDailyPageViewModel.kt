package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 명예전당 - 일일 순위 ViewModel
 *
 * hofs/ API를 호출하여 일일 순위 데이터를 가져옵니다.
 *
 * SavedStateHandle을 사용하여 기간 선택 상태를 저장:
 * - 앱을 내렸다 올려도 유지 (바텀 네비게이션 이동 시에도 유지)
 * - 앱을 재시작하면 리셋 (프로세스 종료 후)
 *
 * OLD 프로젝트와의 주요 차이점:
 * - 탭 변경 시 currentPosition과 historyParam을 유지
 * - loadData 호출 시 newChartCode와 함께 현재 선택된 기간(historyParam)을 전달
 */
@HiltViewModel(assistedFactory = HallOfFameRankingSecondSubDailyPageViewModel.Factory::class)
class HallOfFameRankingSecondSubDailyPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @Assisted private val exoTabSwitchType: Int,
    private val rankingRepository: RankingRepository,
    private val preferencesManager: net.ib.mn.data.local.PreferencesManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String, exoTabSwitchType: Int): HallOfFameRankingSecondSubDailyPageViewModel
    }

    companion object {
        private const val KEY_CURRENT_POSITION = "currentPosition"
    }

    private val _rankingData = MutableStateFlow<List<net.ib.mn.data.remote.dto.DailyRankModel>>(emptyList())
    val rankingData: StateFlow<List<net.ib.mn.data.remote.dto.DailyRankModel>> = _rankingData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // CDN URL (PreferencesManager에서 가져옴, 기본값: https://hswnpegrwdch3017979.gcdn.ntruss.com)
    val cdnUrl: StateFlow<String> = preferencesManager.cdnUrl
        .map { it ?: "https://hswnpegrwdch3017979.gcdn.ntruss.com" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "https://hswnpegrwdch3017979.gcdn.ntruss.com"
        )

    // History 관련 state
    private val _historyYear = MutableStateFlow<String?>(null)
    val historyYear: StateFlow<String?> = _historyYear.asStateFlow()

    private val _historyMonth = MutableStateFlow<String?>(null)
    val historyMonth: StateFlow<String?> = _historyMonth.asStateFlow()

    private val _showPrevButton = MutableStateFlow(true)
    val showPrevButton: StateFlow<Boolean> = _showPrevButton.asStateFlow()

    private val _showNextButton = MutableStateFlow(false)
    val showNextButton: StateFlow<Boolean> = _showNextButton.asStateFlow()

    private var historyList = mutableListOf<HistoryItem>()

    // 현재 기간 선택 위치 (0 = 최신, 1 이상 = 과거 달)
    // SavedStateHandle을 사용하여 바텀 네비게이션 이동 시에도 유지
    private var currentPosition: Int
        get() = savedStateHandle.get<Int>(KEY_CURRENT_POSITION) ?: 0
        set(value) {
            savedStateHandle[KEY_CURRENT_POSITION] = value
        }

    // 현재 실행 중인 로드 작업 (중복 호출 방지)
    private var currentLoadJob: Job? = null

    init {

        // 초기 로드는 Page의 LaunchedEffect에서 처리
    }

    /**
     * 데이터 로드
     *
     * OLD 프로젝트의 getHofDayData와 동일한 로직:
     * - newChartCode: 탭 변경 시 새로운 차트 코드 (null이면 초기 chartCode 사용)
     * - explicitHistoryParam: 기간 버튼 클릭 시 명시적으로 전달된 historyParam
     *
     * @param newChartCode 새로운 차트 코드 (탭 변경 시)
     * @param explicitHistoryParam 명시적으로 전달된 historyParam (기간 버튼 클릭 시)
     */
    fun loadData(newChartCode: String? = null, explicitHistoryParam: String? = null) {
        val codeToUse = newChartCode ?: chartCode

        // 이전 로드 작업 취소
        currentLoadJob?.cancel()

        currentLoadJob = viewModelScope.launch {

            rankingRepository.getHofs(codeToUse, explicitHistoryParam).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _isLoading.value = true
                        _error.value = null
                    }
                    is ApiResult.Success -> {
                        _isLoading.value = false
                        _error.value = null

                        // Parse ranking data from JSON
                        val rankingList = parseRankingData(result.data)

                        // Calculate rank for top3 (old 프로젝트 HallOfFameViewModel 로직과 동일)
                        val processedList = calculateRank(rankingList)

                        _rankingData.value = processedList

                        // Parse history only when explicitHistoryParam is null (initial load)
                        // OLD 프로젝트: if (historyParam != null) return@get
                        if (explicitHistoryParam == null) {
                            parseHistory(result.data)
                        }

                        updatePrevNextVisibility()
                    }
                    is ApiResult.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Unknown error"
                    }
                }
            }
        }
    }

    private fun parseHistory(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val historyArray = jsonObject.optJSONArray("history") ?: return

            historyList.clear()
            for (i in 0 until historyArray.length()) {
                val historyObj = historyArray.getJSONObject(i)
                val historyYear = historyObj.optString("history_year", "")
                val historyMonthRaw = historyObj.optString("history_month", "")

                // API에서 "11" 같은 숫자로 오면 "11월" (한국어) 또는 "Nov" (영어)로 변환
                val historyMonth = formatHistoryMonth(historyMonthRaw)


                historyList.add(
                    HistoryItem(
                        historyYear = historyYear,
                        historyMonth = historyMonth,
                        historyParam = historyObj.optString("history_param", ""),
                        nextHistoryParam = historyObj.optString("next_history_param", "")
                    )
                )
            }

            historyList.reverse()
        } catch (e: Exception) {
        }
    }

    /**
     * history_month 값을 포맷팅 (Old 프로젝트의 HallHistoryModel.historyMonth와 동일)
     * "11" -> "11월" (한국어) 또는 "Nov" (영어)
     */
    private fun formatHistoryMonth(monthString: String): String {
        if (monthString.isEmpty()) return monthString

        return try {
            val stringToDate = SimpleDateFormat("MM", Locale.getDefault())
            val dateToString = SimpleDateFormat("MMM", Locale.getDefault())
            val date = stringToDate.parse(monthString)
            date?.let { dateToString.format(it) } ?: monthString
        } catch (e: Exception) {
            monthString
        }
    }

    private fun updatePrevNextVisibility() {
        if (currentPosition == 0) {
            _historyYear.value = null
            _historyMonth.value = null
            _showNextButton.value = false
        } else {
            val item = historyList.getOrNull(currentPosition - 1)
            _historyYear.value = item?.historyYear ?: ""
            _historyMonth.value = item?.historyMonth ?: ""
            _showNextButton.value = true

        }

        _showPrevButton.value = currentPosition < historyList.size
    }

    /**
     * 이전 기간 버튼 클릭
     *
     * OLD 프로젝트 HallOfFameDayFragment.onClick(ivPrev) 로직과 동일:
     * - currentPosition += 1
     * - historyParam = tagArrayList[currentPosition]
     * - getHofDayData(chartCode, historyParam)
     */
    fun onPrevClicked(currentChartCode: String) {
        if (currentPosition < historyList.size) {
            currentPosition += 1
            val historyParam = buildHistoryParam()
            loadData(currentChartCode, historyParam)
        }
    }

    /**
     * 다음 기간 버튼 클릭
     *
     * OLD 프로젝트 HallOfFameDayFragment.onClick(ivNext) 로직과 동일:
     * - currentPosition -= 1
     * - historyParam = tagArrayList[currentPosition]
     * - getHofDayData(chartCode, historyParam)
     */
    fun onNextClicked(currentChartCode: String) {
        if (currentPosition != 0) {
            currentPosition -= 1
            val historyParam = buildHistoryParam()
            loadData(currentChartCode, historyParam)
        }
    }

    /**
     * currentPosition을 기반으로 historyParam 생성
     *
     * OLD 프로젝트:
     * - currentPosition == 0 → historyParam = null (최신)
     * - currentPosition > 0 → historyParam = "&{history_param}&{next_history_param}"
     */
    private fun buildHistoryParam(): String? {
        return if (currentPosition > 0 && currentPosition <= historyList.size) {
            val item = historyList[currentPosition - 1]
            "&${item.historyParam}&${item.nextHistoryParam}"
        } else {
            null
        }
    }

    /**
     * 탭 변경 시 호출
     *
     * OLD 프로젝트의 segmentedButton.setOnClickListener 로직과 동일:
     * - historyParam = tagArrayList[currentPosition] (현재 기간 유지)
     * - getHofDayData(newChartCode, historyParam)
     */
    fun onTabChanged(newChartCode: String) {
        val historyParam = buildHistoryParam()
        loadData(newChartCode, historyParam)
    }

    /**
     * JSON 문자열을 DailyRankModel 리스트로 파싱
     *
     * old 프로젝트와 동일하게 "objects" 키를 사용하고 reversed() 처리
     *
     * @param jsonString hofs/ API 응답 JSON
     * @return DailyRankModel 리스트 (역순)
     */
    private fun parseRankingData(jsonString: String): List<net.ib.mn.data.remote.dto.DailyRankModel> {
        return try {

            val jsonObject = JSONObject(jsonString)

            // old 프로젝트: response.getJSONArray("objects")
            val objectsArray = jsonObject.optJSONArray("objects")

            if (objectsArray == null) {
                return emptyList()
            }


            val gson = Gson()
            val listType = object : TypeToken<List<net.ib.mn.data.remote.dto.DailyRankModel>>() {}.type

            val result: List<net.ib.mn.data.remote.dto.DailyRankModel>? =
                gson.fromJson(objectsArray.toString(), listType)


            // old 프로젝트: _dayHofList.postValue(presentDayHofList.reversed())
            result?.reversed() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 랭킹 계산 (old 프로젝트 HallOfFameViewModel과 동일)
     *
     * heart 기준 top3에만 rank를 0, 1, 2로 설정
     */
    private fun calculateRank(list: List<net.ib.mn.data.remote.dto.DailyRankModel>): List<net.ib.mn.data.remote.dto.DailyRankModel> {
        if (list.isEmpty()) return list

        // heart로 내림차순 정렬하여 top3 추출
        val top3 = list.sortedByDescending { it.heart }.take(3)

        // top3의 각 아이템에 index(0, 1, 2) 매핑
        val rankMap = top3.mapIndexed { index, item -> item.id to index }.toMap()


        // 원본 리스트를 순회하면서 rankMap에 있는 아이템만 rank 설정
        return list.map { item ->
            rankMap[item.id]?.let { rank ->
                // idol이 null이면 기본값으로 IdolInfo 생성
                val updatedIdol = item.idol?.copy(rank = rank) ?: net.ib.mn.data.remote.dto.IdolInfo(rank = rank)
                item.copy(idol = updatedIdol)
            } ?: item
        }
    }

    data class HistoryItem(
        val historyYear: String,
        val historyMonth: String,
        val historyParam: String,
        val nextHistoryParam: String
    )
}
