package net.ib.mn.presentation.main.freeboard

import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.TagModel
import javax.inject.Inject

@HiltViewModel
class FreeBoardViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val gson: Gson
) : BaseViewModel<FreeBoardContract.State, FreeBoardContract.Intent, FreeBoardContract.Effect>() {

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

                setState {
                    copy(
                        tags = tags,
                        selectedTagId = FreeBoardContract.State.TAG_ID_HOT,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { FreeBoardContract.Effect.ShowError(e.message ?: "Failed to load tags") }
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
        setState {
            copy(
                selectedTagId = tag.id,
                tags = tags.map { it.copy(selected = it.id == tag.id) },
                isLoading = true
            )
        }
        viewModelScope.launch {
            // TODO: Load articles for selected tag
            setState { copy(isLoading = false) }
        }
    }

    private fun refresh() {
        setState { copy(isRefreshing = true) }
        viewModelScope.launch {
            // TODO: Refresh data
            setState { copy(isRefreshing = false) }
        }
    }

    private fun loadMore() {
        // TODO: Load more data
    }

    private fun onSearchSubmit(keyword: String) {
        setState {
            copy(
                searchKeyword = keyword.trim().ifEmpty { null },
                isLoading = true
            )
        }
        viewModelScope.launch {
            // TODO: Search articles
            setState { copy(isLoading = false) }
        }
    }

    private fun onWriteClick() {
        setEffect { FreeBoardContract.Effect.NavigateToWrite }
    }

    private fun onFilterLatest() {
        setState {
            copy(
                orderBy = FreeBoardContract.State.FILTER_DATE_ORDER,
                isLoading = true
            )
        }
        viewModelScope.launch {
            // TODO: Reload with new filter
            setState { copy(isLoading = false) }
        }
    }

    private fun onFilterComments() {
        setState {
            copy(
                orderBy = FreeBoardContract.State.FILTER_COMMENT_ORDER,
                isLoading = true
            )
        }
        viewModelScope.launch {
            // TODO: Reload with new filter
            setState { copy(isLoading = false) }
        }
    }

    private fun onFilterLike() {
        setState {
            copy(
                orderBy = FreeBoardContract.State.FILTER_LIKE_ORDER,
                isLoading = true
            )
        }
        viewModelScope.launch {
            // TODO: Reload with new filter
            setState { copy(isLoading = false) }
        }
    }

    private fun onFilterViewCount() {
        setState {
            copy(
                orderBy = FreeBoardContract.State.FILTER_HITS_ORDER,
                isLoading = true
            )
        }
        viewModelScope.launch {
            // TODO: Reload with new filter
            setState { copy(isLoading = false) }
        }
    }

    private fun togglePopularFilter() {
        setState { copy(showPopular = !showPopular, isLoading = true) }
        viewModelScope.launch {
            // TODO: Reload with new filter
            setState { copy(isLoading = false) }
        }
    }

    private fun onLanguageFilterSelected(language: String?, languageId: String) {
        viewModelScope.launch {
            setState {
                copy(
                    selectedLanguage = language,
                    selectedLanguageId = languageId,
                    isLoading = true
                )
            }
            // TODO: Reload with new language filter
            setState { copy(isLoading = false) }
        }
    }
}
