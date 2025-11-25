package net.ib.mn.presentation.main.freeboard

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.TagModel
import net.ib.mn.domain.repository.ArticlesRepository
import net.ib.mn.util.Constants
import javax.inject.Inject

private const val TAG = "FreeBoardViewModel"

@HiltViewModel
class FreeBoardViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val articlesRepository: ArticlesRepository,
    private val gson: Gson
) : BaseViewModel<FreeBoardContract.State, FreeBoardContract.Intent, FreeBoardContract.Effect>() {

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
                // Load tags from preferences
                val tagsJson = preferencesManager.boardTags.first()
                val tags = parseTags(tagsJson)

                // 저장된 탭 ID 로드 (없으면 HOT가 기본)
                val savedTagId = preferencesManager.getFreeBoardSelectedTagId()
                val initialTagId = savedTagId ?: FreeBoardContract.State.TAG_ID_HOT

                Log.d(TAG, "loadInitialData: savedTagId=$savedTagId, initialTagId=$initialTagId")

                setState {
                    copy(
                        tags = tags.map { it.copy(selected = it.id == initialTagId) },
                        selectedTagId = initialTagId
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
            val locale = currentState.selectedLanguageId.takeIf { it.isNotEmpty() }
            val selectedTagId = currentState.selectedTagId

            Log.d(TAG, "loadArticles: selectedTagId=$selectedTagId, orderBy=$orderBy, keyword=$keyword, locale=$locale")

            val flow = when (selectedTagId) {
                FreeBoardContract.State.TAG_ID_HOT -> {
                    Log.d(TAG, "Calling getFreeBoardHot")
                    articlesRepository.getFreeBoardHot(
                        orderBy = orderBy,
                        keyword = keyword,
                        locale = locale
                    )
                }
                FreeBoardContract.State.TAG_ID_ALL -> {
                    Log.d(TAG, "Calling getFreeBoardAll")
                    articlesRepository.getFreeBoardAll(
                        orderBy = orderBy,
                        keyword = keyword,
                        locale = locale
                    )
                }
                else -> {
                    Log.d(TAG, "Calling getArticles with tagId=$selectedTagId")
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
                        Log.d(TAG, "ApiResult.Loading")
                    }
                    is ApiResult.Success -> {
                        val response = result.data
                        Log.d(TAG, "ApiResult.Success: articles=${response.articles.size}, totalCount=${response.totalCount}, nextUrl=${response.nextUrl}")
                        nextUrl = response.nextUrl

                        val existingArticles = uiState.value.articles
                        val newArticles = if (isLoadMore) {
                            existingArticles + response.articles
                        } else {
                            response.articles
                        }

                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                articles = newArticles,
                                totalCount = response.totalCount,
                                hasMore = response.nextUrl != null,
                                isEmpty = newArticles.isEmpty()
                            )
                        }
                        Log.d(TAG, "State updated: articles=${newArticles.size}, isEmpty=${newArticles.isEmpty()}")
                    }
                    is ApiResult.Error -> {
                        Log.e(TAG, "ApiResult.Error: ${result.message}", result.exception)
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
            Log.d(TAG, "onTagSelected: saved tagId=${tag.id}")
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
            Log.d(TAG, "loadMore: nextUrl is null or empty, skipping")
            return
        }

        val currentState = uiState.value
        if (currentState.isLoading || currentState.isLoadingMore) {
            Log.d(TAG, "loadMore: already loading, skipping")
            return
        }

        if (!currentState.hasMore) {
            Log.d(TAG, "loadMore: hasMore is false, skipping")
            return
        }

        Log.d(TAG, "loadMore: loading nextUrl=$currentNextUrl")

        viewModelScope.launch {
            setState { copy(isLoadingMore = true) }

            articlesRepository.getArticlesNext(currentNextUrl).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        Log.d(TAG, "loadMore: ApiResult.Loading")
                    }
                    is ApiResult.Success -> {
                        val response = result.data
                        Log.d(TAG, "loadMore: Success - articles=${response.articles.size}, nextUrl=${response.nextUrl}")
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
                        Log.e(TAG, "loadMore: Error - ${result.message}")
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
}
