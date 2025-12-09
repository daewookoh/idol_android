package net.ib.mn.presentation.search

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.base.BaseViewModel
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * Search 화면 ViewModel
 *
 * old 프로젝트의 SearchHistoryViewModel을 MVI 패턴으로 재구현
 * - 핫 트렌드 (인기 검색어) 로드
 * - 최근 검색어 관리 (DataStore)
 * - 자동완성 추천
 * - 인앱 배너
 *
 * Navigation 3 활용:
 * - 네비게이션은 Screen에서 LocalAppNavigator로 직접 처리
 * - ViewModel은 비즈니스 로직(상태 관리, API 호출, 데이터 저장)에만 집중
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val preferencesManager: PreferencesManager
) : BaseViewModel<SearchContract.State, SearchContract.Intent, SearchContract.Effect>() {

    companion object {
        private const val MAX_RECENT_SEARCHES = 10
        private const val SUGGEST_DEBOUNCE_MS = 300L
    }

    private var suggestJob: Job? = null

    override fun createInitialState(): SearchContract.State = SearchContract.State()

    init {
        loadInitialData()
    }

    override fun handleIntent(intent: SearchContract.Intent) {
        when (intent) {
            is SearchContract.Intent.ResetSearchQuery -> resetSearchQuery()
            is SearchContract.Intent.SetSearchFocused -> setSearchFocused(intent.focused)
            is SearchContract.Intent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is SearchContract.Intent.SaveSearchAndNavigate -> saveSearchAndNavigate(intent.keyword)
            is SearchContract.Intent.DeleteRecentSearch -> deleteRecentSearch(intent.keyword)
            is SearchContract.Intent.ClearAllRecentSearches -> clearAllRecentSearches()
        }
    }

    /**
     * 검색어 리셋 (화면 진입 시)
     */
    private fun resetSearchQuery() {
        suggestJob?.cancel()
        setState {
            copy(
                searchQuery = "",
                displayMode = SearchContract.DisplayMode.TRENDS,
                suggestions = emptyList()
            )
        }
        // 핫 트렌드 다시 로드 (Old 프로젝트의 onResume 로직과 동일)
        loadHotTrends()
    }

    /**
     * 검색창 포커스 상태 변경 (Old 프로젝트의 KeyboardVisibilityUtil 로직과 동일)
     * - 포커스 O + 입력 X: 최근 검색어 표시 (HISTORY)
     * - 포커스 O + 입력 O: 자동완성 표시 (SUGGESTIONS)
     * - 포커스 X: 핫 트렌드 표시 (TRENDS)
     */
    private fun setSearchFocused(focused: Boolean) {
        if (focused) {
            // 포커스가 있을 때
            if (currentState.searchQuery.isBlank()) {
                // 입력이 없으면 최근 검색어 표시
                setState { copy(displayMode = SearchContract.DisplayMode.HISTORY) }
            } else {
                // 입력이 있으면 자동완성 표시
                setState { copy(displayMode = SearchContract.DisplayMode.SUGGESTIONS) }
            }
        } else {
            // 포커스가 없을 때 핫 트렌드 표시
            if (currentState.searchQuery.isBlank()) {
                setState { copy(displayMode = SearchContract.DisplayMode.TRENDS) }
                loadHotTrends()
            }
        }
    }

    /**
     * 초기 데이터 로드
     * - 핫 트렌드
     * - 최근 검색어
     * - 인앱 배너
     */
    private fun loadInitialData() {
        loadHotTrends()
        loadRecentSearches()
        loadBanners()
    }

    /**
     * 핫 트렌드 로드
     */
    private fun loadHotTrends() {
        searchRepository.getTrends()
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is ApiResult.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                hotTrends = result.data,
                                error = null
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = result.error.message
                            )
                        }
                    }
                }
            }
            .catch { e ->
                setState { copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 최근 검색어 로드 (DataStore)
     */
    private fun loadRecentSearches() {
        viewModelScope.launch {
            preferencesManager.recentSearches.collect { searches ->
                setState { copy(recentSearches = searches) }
            }
        }
    }

    /**
     * 인앱 배너 로드
     * 검색 화면 전용 배너 사용 (PreferencesManager에서 로드)
     */
    private fun loadBanners() {
        viewModelScope.launch {
            preferencesManager.inAppBannerSearch.collect { bannerJson ->
                val banners = bannerJson?.let { json ->
                    runCatching {
                        val type = object : com.google.gson.reflect.TypeToken<List<net.ib.mn.domain.model.InAppBanner>>() {}.type
                        com.google.gson.Gson().fromJson<List<net.ib.mn.domain.model.InAppBanner>>(json, type)
                    }.getOrDefault(emptyList())
                } ?: emptyList()
                setState { copy(banners = banners) }
            }
        }
    }

    /**
     * 검색어 입력 변경 처리 (Old 프로젝트의 TextWatcher 로직과 동일)
     * - 입력이 있으면 자동완성 표시 (SUGGESTIONS)
     * - 입력이 없으면 최근 검색어 표시 (HISTORY) - 포커스가 있는 상태이므로
     */
    private fun updateSearchQuery(query: String) {
        setState { copy(searchQuery = query) }

        if (query.isBlank()) {
            // 검색어가 비어있으면 최근 검색어 모드로 전환 (포커스가 있는 상태)
            setState {
                copy(
                    displayMode = SearchContract.DisplayMode.HISTORY,
                    suggestions = emptyList()
                )
            }
            suggestJob?.cancel()
        } else {
            // 검색어가 있으면 자동완성 모드로 전환 + 디바운스 적용
            setState { copy(displayMode = SearchContract.DisplayMode.SUGGESTIONS) }
            loadSuggestions(query)
        }
    }

    /**
     * 자동완성 추천 로드 (디바운스 적용)
     */
    private fun loadSuggestions(query: String) {
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(SUGGEST_DEBOUNCE_MS)

            searchRepository.getSuggestions(query)
                .onEach { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            setState { copy(suggestions = result.data) }
                        }
                        is ApiResult.Error -> {
                            // 자동완성 에러는 무시 (UX 방해 최소화)
                        }
                        is ApiResult.Loading -> { /* ignore */ }
                    }
                }
                .catch { /* ignore */ }
                .launchIn(this)
        }
    }

    /**
     * 검색어 저장 후 네비게이션 가능 신호
     * - 최근 검색어에 추가
     * - 키보드 숨기기
     * - 저장 완료 Effect 발행
     */
    private fun saveSearchAndNavigate(keyword: String) {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.isBlank()) return

        // 키보드 숨기기
        setEffect { SearchContract.Effect.HideKeyboard }

        // 최근 검색어에 추가
        viewModelScope.launch {
            addToRecentSearches(trimmedKeyword)
            // 저장 완료 - Screen에서 네비게이션 진행
            setEffect { SearchContract.Effect.SearchSaved(trimmedKeyword) }
        }
    }

    /**
     * 최근 검색어에 추가
     * - 중복 제거 (기존 항목 삭제 후 맨 앞에 추가)
     * - 최대 10개 유지
     */
    private suspend fun addToRecentSearches(keyword: String) {
        val currentSearches = currentState.recentSearches.toMutableList()

        // 중복 제거
        currentSearches.remove(keyword)

        // 맨 앞에 추가
        currentSearches.add(0, keyword)

        // 최대 개수 유지
        val limitedSearches = currentSearches.take(MAX_RECENT_SEARCHES)

        // DataStore에 저장
        preferencesManager.saveRecentSearches(limitedSearches)
    }

    /**
     * 최근 검색어 개별 삭제
     */
    private fun deleteRecentSearch(keyword: String) {
        viewModelScope.launch {
            val currentSearches = currentState.recentSearches.toMutableList()
            currentSearches.remove(keyword)
            preferencesManager.saveRecentSearches(currentSearches)
        }
    }

    /**
     * 최근 검색어 전체 삭제
     */
    private fun clearAllRecentSearches() {
        viewModelScope.launch {
            preferencesManager.saveRecentSearches(emptyList())
        }
    }
}
