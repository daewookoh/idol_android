package net.ib.mn.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.SearchSuggestModel
import net.ib.mn.domain.model.SearchTrendModel
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.navigation.Screen
import net.ib.mn.presentation.common.InAppBannerSection
import net.ib.mn.presentation.common.SearchBar
import net.ib.mn.ui.components.ExoScaffold

/**
 * 검색 화면
 *
 * old 프로젝트의 SearchHistoryActivity를 Compose로 재구현
 * - 검색 입력창 + 뒤로가기/검색 버튼
 * - 핫 트렌드 (인기 검색어)
 * - 최근 검색어
 * - 자동완성 추천
 * - 인앱 배너
 *
 * Navigation 3 활용:
 * - LocalAppNavigator를 통해 네비게이션 직접 처리
 * - ViewModel에서 네비게이션 Intent/Effect 제거
 */
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // 화면 진입 시 검색어 리셋
    LaunchedEffect(Unit) {
        viewModel.sendIntent(SearchContract.Intent.ResetSearchQuery)
    }

    // 검색 실행 함수 (키워드 저장 후 결과 화면으로 이동)
    fun doSearch(keyword: String) {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.isBlank()) return
        viewModel.sendIntent(SearchContract.Intent.SaveSearchAndNavigate(trimmedKeyword))
    }

    // Effect 처리 - UI 관련 이벤트만
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SearchContract.Effect.HideKeyboard -> {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
                is SearchContract.Effect.ShowToast -> {
                    android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is SearchContract.Effect.SearchSaved -> {
                    // 검색어 저장 완료 후 결과 화면으로 이동
                    navigator.navigate(Screen.SearchResult(effect.keyword))
                }
            }
        }
    }

    SearchContent(
        modifier = modifier,
        state = state,
        onSearchQueryChange = { viewModel.sendIntent(SearchContract.Intent.UpdateSearchQuery(it)) },
        onSearch = { doSearch(it) },
        onKeywordClick = { doSearch(it) },
        onDeleteRecentSearch = { viewModel.sendIntent(SearchContract.Intent.DeleteRecentSearch(it)) },
        onClearAllRecentSearches = { viewModel.sendIntent(SearchContract.Intent.ClearAllRecentSearches) },
        onBannerClick = { banner ->
            // 배너 클릭 - Navigation 3로 직접 처리
            val link = banner.link ?: return@SearchContent
            when {
                link.startsWith("idol:") -> {
                    val idolId = link.removePrefix("idol:").toIntOrNull() ?: return@SearchContent
                    // TODO: 커뮤니티 화면으로 이동
                }
                link.startsWith("http") -> {
                    navigator.navigate(Screen.WebView(link))
                }
            }
        },
        onNavigateBack = { navigator.popBackStack() },
        onFocusChange = { focused ->
            viewModel.sendIntent(SearchContract.Intent.SetSearchFocused(focused))
        },
        onCancel = {
            // 포커스 안된 상태(TRENDS 모드)에서 취소 버튼 클릭 시 화면 닫기
            if (state.displayMode == SearchContract.DisplayMode.TRENDS) {
                navigator.popBackStack()
            }
        }
    )
}

@Composable
private fun SearchContent(
    modifier: Modifier = Modifier,
    state: SearchContract.State,
    onSearchQueryChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onKeywordClick: (String) -> Unit = {},
    onDeleteRecentSearch: (String) -> Unit = {},
    onClearAllRecentSearches: () -> Unit = {},
    onBannerClick: (InAppBanner) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    ExoScaffold(
        modifier = modifier,
        useFullScreen = true,
        topBar = {
            SearchBar(
                searchQuery = state.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearch = { onSearch(state.searchQuery) },
                onNavigateBack = onNavigateBack,
                onCancel = {
                    // 검색어 리셋 + 언포커싱
                    onSearchQueryChange("")
                    focusManager.clearFocus()
                    onCancel()
                },
                focusRequester = focusRequester,
                onFocusChange = onFocusChange
            )
        }
    ) {
        // 컨텐츠 영역 (Old 프로젝트와 동일한 3가지 모드)
        when (state.displayMode) {
            SearchContract.DisplayMode.TRENDS -> {
                // 핫 트렌드 + 배너
                TrendsContent(
                    hotTrends = state.hotTrends,
                    recentSearches = state.recentSearches,
                    banners = state.banners,
                    onKeywordClick = onKeywordClick,
                    onDeleteRecentSearch = onDeleteRecentSearch,
                    onClearAllRecentSearches = onClearAllRecentSearches,
                    onBannerClick = onBannerClick
                )
            }
            SearchContract.DisplayMode.HISTORY -> {
                // 최근 검색어 (검색창 포커스 시)
                HistoryContent(
                    recentSearches = state.recentSearches,
                    onKeywordClick = onKeywordClick,
                    onDeleteRecentSearch = onDeleteRecentSearch,
                    onClearAllRecentSearches = onClearAllRecentSearches
                )
            }
            SearchContract.DisplayMode.SUGGESTIONS -> {
                // 자동완성 추천
                SuggestionsContent(
                    suggestions = state.suggestions,
                    onKeywordClick = onKeywordClick
                )
            }
        }
    }
}

/**
 * 핫 트렌드 + 배너 컨텐츠 (Old 프로젝트와 동일한 세로 리스트 형태)
 */
@Composable
private fun TrendsContent(
    hotTrends: List<SearchTrendModel>,
    recentSearches: List<String>,
    banners: List<InAppBanner>,
    onKeywordClick: (String) -> Unit,
    onDeleteRecentSearch: (String) -> Unit,
    onClearAllRecentSearches: () -> Unit,
    onBannerClick: (InAppBanner) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background_100))
    ) {
        // 인앱 배너
        if (banners.isNotEmpty()) {
            item {
                InAppBannerSection(
                    bannerList = banners,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    onBannerClick = onBannerClick
                )
            }
        }

        // 핫 트렌드 타이틀
        if (hotTrends.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.search_hot_trend),
                    color = colorResource(id = R.color.text_gray),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 25.dp)
                )
            }

            // 핫 트렌드 리스트 (세로 리스트 - Old 프로젝트와 동일)
            items(hotTrends) { trend ->
                HotTrendItem(
                    rank = trend.rank,
                    text = trend.text,
                    onClick = { onKeywordClick(trend.text) }
                )
            }
        }
    }
}

/**
 * 최근 검색어 컨텐츠 (Old 프로젝트와 동일한 형태)
 */
@Composable
private fun HistoryContent(
    recentSearches: List<String>,
    onKeywordClick: (String) -> Unit,
    onDeleteRecentSearch: (String) -> Unit,
    onClearAllRecentSearches: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background_100))
    ) {
        // 최근 검색어 타이틀
        item {
            Text(
                text = stringResource(id = R.string.search_history),
                color = colorResource(id = R.color.text_gray),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 20.dp, top = 25.dp)
            )
        }

        // 최근 검색어 리스트
        items(recentSearches) { keyword ->
            RecentSearchItem(
                keyword = keyword,
                onClick = { onKeywordClick(keyword) },
                onDelete = { onDeleteRecentSearch(keyword) }
            )
        }
    }
}

/**
 * 핫 트렌드 아이템 (Old 프로젝트의 search_hot_trend_item.xml과 동일)
 */
@Composable
private fun HotTrendItem(
    rank: Int,
    text: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank",
                color = colorResource(id = R.color.main_light),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(17.dp))
            Text(
                text = text,
                color = colorResource(id = R.color.text_gray),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = colorResource(id = R.color.gray100)
        )
    }
}

/**
 * 최근 검색어 아이템 (Old 프로젝트의 search_history_item.xml과 동일)
 */
@Composable
private fun RecentSearchItem(
    keyword: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.icon_search_clock),
                contentDescription = null,
                tint = colorResource(id = R.color.gray300),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(17.dp))
            Text(
                text = keyword,
                color = colorResource(id = R.color.text_gray),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                painter = painterResource(id = R.drawable.btn_navigation_delete),
                contentDescription = "Delete",
                tint = colorResource(id = R.color.gray300),
                modifier = Modifier
                    .padding(end = 10.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDelete() }
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = colorResource(id = R.color.gray100)
        )
    }
}

/**
 * 자동완성 추천 컨텐츠
 */
@Composable
private fun SuggestionsContent(
    suggestions: List<SearchSuggestModel>,
    onKeywordClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(suggestions) { suggest ->
            SuggestionItem(
                text = suggest.text,
                onClick = { onKeywordClick(suggest.text) }
            )
        }
    }
}

/**
 * 자동완성 추천 아이템
 */
@Composable
private fun SuggestionItem(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.btn_navigation_search),
            contentDescription = null,
            tint = colorResource(id = R.color.gray300),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = colorResource(id = R.color.gray580),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            painter = painterResource(id = R.drawable.icon_arrow_right),
            contentDescription = null,
            tint = colorResource(id = R.color.gray300),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchContentPreview() {
    SearchContent(
        state = SearchContract.State(
            searchQuery = "",
            displayMode = SearchContract.DisplayMode.TRENDS,
            hotTrends = listOf(
                SearchTrendModel("BTS", 1),
                SearchTrendModel("블랙핑크", 2),
                SearchTrendModel("아이브", 3),
                SearchTrendModel("뉴진스", 4),
                SearchTrendModel("세븐틴", 5)
            ),
            recentSearches = listOf(
                "에스파",
                "스트레이키즈",
                "르세라핌"
            )
        )
    )
}
