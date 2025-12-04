package net.ib.mn.presentation.main.freeboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.TagModel
import net.ib.mn.domain.repository.ArticlesRepository
import net.ib.mn.util.Constants
import net.ib.mn.util.logD
import net.ib.mn.util.logE

private const val TAG = "FreeBoardViewModel"

@HiltViewModel
class FreeBoardViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val articlesRepository: ArticlesRepository,
    private val gson: Gson,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<FreeBoardContract.State, FreeBoardContract.Intent, FreeBoardContract.Effect>() {

    companion object {
        // 기본 탭 인덱스 (HOT = 0)
        const val DEFAULT_TAB_INDEX = FreeBoardContract.State.TAG_ID_HOT
        // 외부에서 idol_id를 전달받을 때 사용하는 키
        const val ARG_IDOL_ID = "idolId"
    }

    // 외부에서 전달된 idolId (CommunityFanTalkSubPage 등에서 사용)
    // null이면 기존 FreeBoardPage 동작 (최애 탭에서 preferencesManager.getMostIdolId() 사용)
    private var externalIdolId: Int? = savedStateHandle.get<Int>(ARG_IDOL_ID)

    // 외부에서 idolId가 전달된 경우 (덕질게시판 전용 모드)
    val isExternalIdolMode: Boolean
        get() = externalIdolId != null && externalIdolId!! > 0

    /**
     * 외부에서 idolId를 설정 (CommunityFanTalkSubPage 등에서 사용)
     * LoadInitialData 호출 전에 설정해야 함
     */
    fun setExternalIdolId(idolId: Int) {
        if (idolId > 0) {
            externalIdolId = idolId
        }
    }

    private var nextUrl: String? = null

    override fun createInitialState() = FreeBoardContract.State()

    override fun handleIntent(intent: FreeBoardContract.Intent) {
        when (intent) {
            is FreeBoardContract.Intent.LoadInitialData -> loadInitialData()
            is FreeBoardContract.Intent.Refresh -> refresh()
            is FreeBoardContract.Intent.LoadMore -> loadMore()
            is FreeBoardContract.Intent.OnSearchSubmit -> onSearchSubmit(intent.keyword)
            is FreeBoardContract.Intent.OnTagSelected -> onTagSelected(intent.tag)
            is FreeBoardContract.Intent.OnWriteClick -> onWriteClick()
            is FreeBoardContract.Intent.OnFilterLatest -> onFilterLatest()
            is FreeBoardContract.Intent.OnFilterComments -> onFilterComments()
            is FreeBoardContract.Intent.OnFilterLike -> onFilterLike()
            is FreeBoardContract.Intent.OnFilterViewCount -> onFilterViewCount()
            is FreeBoardContract.Intent.TogglePopularFilter -> togglePopularFilter()
            is FreeBoardContract.Intent.OnLanguageFilterSelected -> onLanguageFilterSelected(intent.language, intent.languageId)
            is FreeBoardContract.Intent.OnLanguageFilterClick -> setEffect { FreeBoardContract.Effect.ShowLanguageFilterDialog }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }

            try {
                // 외부에서 idolId가 전달된 경우 (CommunityFanTalkSubPage 등)
                // 태그 없이 바로 해당 아이돌의 덕질게시판만 로드
                if (isExternalIdolMode) {
                    logD(TAG, "loadInitialData: externalIdolMode with idolId=$externalIdolId")
                    setState {
                        copy(
                            tags = emptyList(),
                            selectedTagId = FreeBoardContract.State.TAG_ID_MY_FAVORITE,
                            hasMostIdol = true
                        )
                    }
                    loadArticles()
                    return@launch
                }

                // 기존 FreeBoardPage 동작
                // Load tags from preferences
                val tagsJson = preferencesManager.boardTags.first()
                val tags = parseTags(tagsJson)

                // 저장된 탭 ID 로드 (없으면 HOT가 기본)
                // startup에서 최애 아이돌 ID로 설정됨
                val savedTagId = preferencesManager.getFreeBoardSelectedTagId()
                val initialTagId = savedTagId ?: DEFAULT_TAB_INDEX

                // 최애 설정 여부 확인
                val mostIdolId = preferencesManager.getMostIdolId()
                val hasMostIdol = mostIdolId != null &&
                    mostIdolId != Constants.SECRET_ROOM_IDOL_ID &&
                    mostIdolId > 0

                logD(TAG, "loadInitialData: savedTagId=$savedTagId, initialTagId=$initialTagId, mostIdolId=$mostIdolId, hasMostIdol=$hasMostIdol")

                setState {
                    copy(
                        tags = tags.map { it.copy(selected = it.id == initialTagId) },
                        selectedTagId = initialTagId,
                        hasMostIdol = hasMostIdol
                    )
                }

                // Load articles for initial tag
                loadArticles()
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { FreeBoardContract.Effect.ShowError(e.message ?: "Failed to load tags") }
            }
        }
    }

    private fun loadArticles(isLoadMore: Boolean = false) {
        viewModelScope.launch {
            if (!isLoadMore) {
                setState { copy(isLoading = true, articles = emptyList(), hasMore = true) }
                nextUrl = null
            }

            // setState 이후 최신 상태를 가져옴
            val currentState = uiState.value
            val orderBy = currentState.orderBy
            val keyword = currentState.searchKeyword?.takeIf { it.isNotEmpty() }
            // "all"이거나 빈 문자열이면 locale을 null로 보냄
            val locale = currentState.selectedLanguageId.takeIf { it.isNotEmpty() && it != "all" }
            val selectedTagId = currentState.selectedTagId

            logD(TAG, "loadArticles: selectedTagId=$selectedTagId, orderBy=$orderBy, keyword=$keyword, locale=$locale")

            // 최애 탭인데 최애가 설정되지 않은 경우 빈 화면 표시
            if (selectedTagId == FreeBoardContract.State.TAG_ID_MY_FAVORITE && !currentState.hasMostIdol) {
                logD(TAG, "loadArticles: 최애 탭인데 최애 미설정 -> 빈 화면 표시")
                setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        articles = emptyList(),
                        isEmpty = true
                    )
                }
                return@launch
            }

            val flow = when (selectedTagId) {
                FreeBoardContract.State.TAG_ID_HOT -> {
                    logD(TAG, "Calling getFreeBoardHot")
                    articlesRepository.getFreeBoardHot(
                        orderBy = orderBy,
                        keyword = keyword,
                        locale = locale
                    )
                }
                FreeBoardContract.State.TAG_ID_ALL -> {
                    logD(TAG, "Calling getFreeBoardAll")
                    articlesRepository.getFreeBoardAll(
                        orderBy = orderBy,
                        keyword = keyword,
                        locale = locale
                    )
                }
                FreeBoardContract.State.TAG_ID_MY_FAVORITE -> {
                    // 외부에서 idolId가 전달된 경우 해당 ID 사용, 아니면 최애 아이돌 ID 사용
                    val idolId = externalIdolId ?: (preferencesManager.getMostIdolId() ?: 0)
                    logD(TAG, "Calling getMyFavoriteArticles with idolId=$idolId (external=$externalIdolId)")
                    articlesRepository.getMyFavoriteArticles(
                        idolId = idolId,
                        orderBy = orderBy,
                        keyword = keyword,
                        locale = locale
                    )
                }
                else -> {
                    logD(TAG, "Calling getArticles with tagId=$selectedTagId")
                    articlesRepository.getArticles(
                        idolId = Constants.FREE_BOARD_IDOL_ID,
                        orderBy = orderBy,
                        tags = selectedTagId.toString(),
                        keyword = keyword,
                        locale = locale,
                        isPopular = if (currentState.showPopular) "Y" else null
                    )
                }
            }

            flow.collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        logD(TAG, "ApiResult.Loading")
                    }
                    is ApiResult.Success -> {
                        val response = result.data
                        logD(TAG, "ApiResult.Success: notices=${response.notices.size}, articles=${response.articles.size}, totalCount=${response.totalCount}, nextUrl=${response.nextUrl}")
                        nextUrl = response.nextUrl

                        val existingArticles = uiState.value.articles
                        val newArticles = if (isLoadMore) {
                            existingArticles + response.articles
                        } else {
                            response.articles
                        }

                        // 공지사항은 첫 로드에만 설정 (loadMore 시에는 유지)
                        val notices = if (isLoadMore) {
                            uiState.value.notices
                        } else {
                            response.notices
                        }

                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                notices = notices,
                                articles = newArticles,
                                totalCount = response.totalCount,
                                hasMore = response.nextUrl != null,
                                isEmpty = newArticles.isEmpty() && notices.isEmpty()
                            )
                        }
                        logD(TAG, "State updated: notices=${notices.size}, articles=${newArticles.size}, isEmpty=${newArticles.isEmpty() && notices.isEmpty()}")
                    }
                    is ApiResult.Error -> {
                        logE(TAG, "ApiResult.Error: ${result.message}", result.exception)
                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                isEmpty = articles.isEmpty()
                            )
                        }
                        setEffect { FreeBoardContract.Effect.ShowError(result.message ?: "Failed to load articles") }
                    }
                }
            }
        }
    }

    private fun parseTags(tagsJson: String?): List<TagModel> {
        if (tagsJson.isNullOrEmpty()) return createDefaultTags()

        return try {
            val listType = object : TypeToken<List<TagModel>>() {}.type
            val serverTags: List<TagModel> = gson.fromJson(tagsJson, listType)

            // 인기글(0)과 ALL(9898) 태그를 앞에 추가
            val resultTags = mutableListOf<TagModel>()
            resultTags.add(TagModel(id = FreeBoardContract.State.TAG_ID_HOT, name = "", adminOnly = "N", selected = true))
            resultTags.add(TagModel(id = FreeBoardContract.State.TAG_ID_ALL, name = "ALL", adminOnly = "N", selected = false))
            resultTags.addAll(serverTags)

            resultTags
        } catch (e: Exception) {
            createDefaultTags()
        }
    }

    private fun createDefaultTags(): List<TagModel> {
        return listOf(
            TagModel(id = FreeBoardContract.State.TAG_ID_HOT, name = "", adminOnly = "N", selected = true),
            TagModel(id = FreeBoardContract.State.TAG_ID_ALL, name = "ALL", adminOnly = "N", selected = false)
        )
    }

    private fun onTagSelected(tag: TagModel) {
        viewModelScope.launch {
            // 선택된 탭 ID 저장
            preferencesManager.setFreeBoardSelectedTagId(tag.id)
            logD(TAG, "onTagSelected: saved tagId=${tag.id}")
        }

        setState {
            copy(
                selectedTagId = tag.id,
                tags = tags.map { it.copy(selected = it.id == tag.id) }
            )
        }
        loadArticles()
    }

    private fun refresh() {
        setState { copy(isRefreshing = true) }
        loadArticles()
    }

    private fun loadMore() {
        val currentNextUrl = nextUrl
        if (currentNextUrl.isNullOrEmpty()) {
            logD(TAG, "loadMore: nextUrl is null or empty, skipping")
            return
        }

        val currentState = uiState.value
        if (currentState.isLoading || currentState.isLoadingMore) {
            logD(TAG, "loadMore: already loading, skipping")
            return
        }

        if (!currentState.hasMore) {
            logD(TAG, "loadMore: hasMore is false, skipping")
            return
        }

        logD(TAG, "loadMore: loading nextUrl=$currentNextUrl")

        viewModelScope.launch {
            setState { copy(isLoadingMore = true) }

            articlesRepository.getArticlesNext(currentNextUrl).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        logD(TAG, "loadMore: ApiResult.Loading")
                    }
                    is ApiResult.Success -> {
                        val response = result.data
                        logD(TAG, "loadMore: Success - articles=${response.articles.size}, nextUrl=${response.nextUrl}")
                        nextUrl = response.nextUrl

                        setState {
                            copy(
                                isLoadingMore = false,
                                articles = articles + response.articles,
                                totalCount = response.totalCount,
                                hasMore = !response.nextUrl.isNullOrEmpty()
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        logE(TAG, "loadMore: Error - ${result.message}")
                        setState { copy(isLoadingMore = false) }
                        setEffect { FreeBoardContract.Effect.ShowError(result.message ?: "Failed to load more") }
                    }
                }
            }
        }
    }

    private fun onSearchSubmit(keyword: String) {
        setState {
            copy(searchKeyword = keyword.trim().ifEmpty { null })
        }
        loadArticles()
    }

    private fun onWriteClick() {
        setEffect { FreeBoardContract.Effect.NavigateToWrite }
    }

    private fun onFilterLatest() {
        setState { copy(orderBy = FreeBoardContract.State.FILTER_DATE_ORDER) }
        loadArticles()
    }

    private fun onFilterComments() {
        setState { copy(orderBy = FreeBoardContract.State.FILTER_COMMENT_ORDER) }
        loadArticles()
    }

    private fun onFilterLike() {
        setState { copy(orderBy = FreeBoardContract.State.FILTER_LIKE_ORDER) }
        loadArticles()
    }

    private fun onFilterViewCount() {
        setState { copy(orderBy = FreeBoardContract.State.FILTER_HITS_ORDER) }
        loadArticles()
    }

    private fun togglePopularFilter() {
        setState { copy(showPopular = !showPopular) }
        loadArticles()
    }

    private fun onLanguageFilterSelected(language: String?, languageId: String) {
        setState {
            copy(
                selectedLanguage = language,
                selectedLanguageId = languageId
            )
        }
        loadArticles()
    }

    /**
     * 게시글 업데이트 (좋아요 등 상태 변경 시 목록 동기화)
     */
    fun updateArticle(updatedArticle: ArticleModel) {
        val currentArticles = uiState.value.articles.toMutableList()
        val index = currentArticles.indexOfFirst { it.id == updatedArticle.id }
        if (index >= 0) {
            currentArticles[index] = updatedArticle
            setState { copy(articles = currentArticles) }
        }
    }
}
