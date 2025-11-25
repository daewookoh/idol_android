package net.ib.mn.presentation.main.freeboard

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.TagModel

/**
 * Free Board Contract - State, Intent, Effect
 */
class FreeBoardContract {

    data class State(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val tags: List<TagModel> = emptyList(),
        val selectedTagId: Int = TAG_ID_HOT,
        val articles: List<ArticleModel> = emptyList(),
        val totalCount: Int = 0,
        val orderBy: String = FILTER_DATE_ORDER,
        val searchKeyword: String? = null,
        val selectedLanguage: String? = null,
        val selectedLanguageId: String = "",
        val showPopular: Boolean = false,
        val isEmpty: Boolean = false,
        val error: String? = null
    ) : UiState {
        companion object {
            const val TAG_ID_HOT = 0      // 인기글
            const val TAG_ID_ALL = 9898   // ALL

            const val FILTER_DATE_ORDER = "-created_at"
            const val FILTER_COMMENT_ORDER = "-num_comments"
            const val FILTER_LIKE_ORDER = "-like_count"
            const val FILTER_HITS_ORDER = "-view_count"
        }
    }

    sealed class Intent : UiIntent {
        data object LoadInitialData : Intent()
        data object Refresh : Intent()
        data object LoadMore : Intent()
        data class OnSearchSubmit(val keyword: String) : Intent()
        data class OnTagSelected(val tag: TagModel) : Intent()
        data object OnWriteClick : Intent()
        data object OnFilterLatest : Intent()
        data object OnFilterComments : Intent()
        data object OnFilterLike : Intent()
        data object OnFilterViewCount : Intent()
        data object TogglePopularFilter : Intent()
        data class OnLanguageFilterSelected(val language: String?, val languageId: String) : Intent()
        data object OnLanguageFilterClick : Intent()
    }

    sealed class Effect : UiEffect {
        data object NavigateToWrite : Effect()
        data object ShowLanguageFilterDialog : Effect()
        data class ShowError(val message: String) : Effect()
        data class ShowToast(val message: String) : Effect()
    }
}
