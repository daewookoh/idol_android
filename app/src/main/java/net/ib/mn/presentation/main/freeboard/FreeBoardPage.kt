package net.ib.mn.presentation.main.freeboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.TagModel
import net.ib.mn.presentation.common.ArticleItemType
import net.ib.mn.presentation.common.ExoArticleItem
import net.ib.mn.presentation.common.ExoArticleNavigation
import net.ib.mn.presentation.common.ExoArticleViewModel
import net.ib.mn.ui.components.ExoBottomSheet
import net.ib.mn.ui.components.ExoBottomSheetItem
import net.ib.mn.ui.components.ExoBottomSheetList
import net.ib.mn.ui.components.ExoSearchBox
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.BoardLanguage
import net.ib.mn.util.LocaleUtil

/**
 * Free Board 페이지 - 프리톡 메뉴 화면
 */
@Composable
fun FreeBoardPage(
    onNavigateToWrite: () -> Unit = {},
    onNavigateToArticleDetail: (ArticleModel, externalTabName: String?, onArticleUpdated: (ArticleModel) -> Unit) -> Unit = { _, _, _ -> },
    onNavigateToNoticeDetail: (ArticleModel) -> Unit = {},
    viewModel: FreeBoardViewModel = hiltViewModel(),
    articleViewModel: ExoArticleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Load initial data
    LaunchedEffect(Unit) {
        viewModel.sendIntent(FreeBoardContract.Intent.LoadInitialData)
    }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FreeBoardContract.Effect.NavigateToWrite -> {
                    onNavigateToWrite()
                }
                is FreeBoardContract.Effect.ShowLanguageFilterDialog -> {
                    // Show language filter dialog
                }
                is FreeBoardContract.Effect.ShowError -> {
                    // Show error toast
                }
                is FreeBoardContract.Effect.ShowToast -> {
                    // Show toast
                }
            }
        }
    }

    // ExoArticle 네비게이션 이벤트 처리
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        articleViewModel.navigationEvent.collect { event ->
            when (event) {
                is ExoArticleNavigation.ArticleDetail -> {
                    // externalIdol이 있으면 다국어 이름 전달
                    val externalTabName = state.externalIdol?.let {
                        LocaleUtil.getLocalizedIdolName(context, it)
                    }
                    onNavigateToArticleDetail(event.article, externalTabName) { updatedArticle ->
                        viewModel.updateArticle(updatedArticle)
                    }
                }
                is ExoArticleNavigation.NoticeDetail -> {
                    onNavigateToNoticeDetail(event.article)
                }
                else -> { /* 다른 이벤트는 무시 */ }
            }
        }
    }

    FreeBoardContent(
        state = state,
        onIntent = viewModel::sendIntent,
        articleViewModel = articleViewModel
    )
}

/**
 * FreeBoardContent - 재사용 가능한 게시판 콘텐츠
 *
 * @param state FreeBoardContract.State
 * @param onIntent Intent 핸들러
 * @param isExternalIdolMode 외부에서 idolId를 전달받은 모드 (태그탭 숨김, 글쓰기 버튼 표시)
 * @param articleViewModel ExoArticleViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeBoardContent(
    state: FreeBoardContract.State,
    onIntent: (FreeBoardContract.Intent) -> Unit,
    isExternalIdolMode: Boolean = false,
    articleViewModel: ExoArticleViewModel = hiltViewModel()
) {
    // state.searchKeyword와 동기화되는 검색 텍스트
    var searchText by remember { mutableStateOf(state.searchKeyword ?: "") }

    // state.searchKeyword가 변경되면 (탭 변경 등) searchText도 업데이트
    LaunchedEffect(state.searchKeyword) {
        searchText = state.searchKeyword ?: ""
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // 정렬 필터 바텀시트 상태
    var showOrderFilterSheet by remember { mutableStateOf(false) }

    // 언어 필터 바텀시트 상태
    var showLanguageFilterSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tags Section (Scrollable Tabs) - 외부 idol 모드에서는 숨김
            if (!isExternalIdolMode && state.tags.isNotEmpty()) {
                TagTabRow(
                    tags = state.tags,
                    selectedTagId = state.selectedTagId,
                    onTagSelected = { tag ->
                        onIntent(FreeBoardContract.Intent.OnTagSelected(tag))
                    }
                )
            }

            // 최애탭이면서 최애 미설정이고, 검색어가 없는 경우 검색바/필터 숨김
            val shouldHideSearchBar = state.selectedTagId == FreeBoardContract.State.TAG_ID_MY_FAVORITE &&
                !state.hasMostIdol &&
                state.searchKeyword.isNullOrEmpty()

            // Search Bar and Filters (최애 미설정 시 숨김)
            if (!shouldHideSearchBar) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorPalette.background100)
                        .padding(top = 10.dp)
                ) {
                    // Search Bar
                    ExoSearchBox(
                        value = searchText,
                        onValueChange = { searchText = it },
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onIntent(FreeBoardContract.Intent.OnSearchSubmit(searchText))
                        },
                        placeholder = stringResource(R.string.freeboard_search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )

                    // Filter Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Language Filter
                        FilterButton(
                            text = if (state.selectedLanguageId.isEmpty() || state.selectedLanguageId == "all") {
                                stringResource(R.string.filter_all_language)
                            } else {
                                state.selectedLanguage ?: stringResource(R.string.filter_all_language)
                            },
                            onClick = { showLanguageFilterSheet = true }
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Order Filter
                        FilterButton(
                            text = when (state.orderBy) {
                                FreeBoardContract.State.FILTER_DATE_ORDER -> stringResource(R.string.freeboard_order_newest)
                                FreeBoardContract.State.FILTER_COMMENT_ORDER -> stringResource(R.string.freeboard_order_comments)
                                FreeBoardContract.State.FILTER_LIKE_ORDER -> stringResource(R.string.order_by_like)
                                FreeBoardContract.State.FILTER_HITS_ORDER -> stringResource(R.string.order_hit)
                                else -> stringResource(R.string.freeboard_order_newest)
                            },
                            onClick = { showOrderFilterSheet = true }
                        )
                    }
                }
            }

            // Content Area
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(FreeBoardContract.Intent.Refresh) },
                modifier = Modifier.fillMaxSize()
            ) {
                // 최애탭이면서 최애 미설정이고, 검색어가 없는 경우
                val isMyFavoriteTabWithNoMostIdol = state.selectedTagId == FreeBoardContract.State.TAG_ID_MY_FAVORITE &&
                    !state.hasMostIdol &&
                    state.searchKeyword.isNullOrEmpty()

                when {
                    state.isLoading && !state.isRefreshing -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ColorPalette.main)
                        }
                    }
                    isMyFavoriteTabWithNoMostIdol -> {
                        // 최애 탭인데 최애가 설정되지 않은 경우 특별한 빈 화면 표시
                        NoMostIdolEmptyView()
                    }
                    state.isEmpty -> {
                        EmptyView(hasSearchKeyword = !state.searchKeyword.isNullOrEmpty())
                    }
                    else -> {
                        val listState = rememberLazyListState()

                        // 무한 스크롤 감지 - snapshotFlow 사용하여 스크롤 상태 변경 시마다 체크
                        LaunchedEffect(listState) {
                            snapshotFlow {
                                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                                val totalItems = listState.layoutInfo.totalItemsCount
                                lastVisibleItem?.index to totalItems
                            }.collect { (lastIndex, totalItems) ->
                                if (lastIndex != null && totalItems > 0 && lastIndex >= totalItems - 3) {
                                    if (!state.isLoading && !state.isLoadingMore && state.hasMore) {
                                        onIntent(FreeBoardContract.Intent.LoadMore)
                                    }
                                }
                            }
                        }

                        val showPopularIcon = state.selectedTagId == FreeBoardContract.State.TAG_ID_HOT

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ColorPalette.background100)
                        ) {
                            // 공지사항/고정글 (상단에 표시)
                            items(
                                count = state.notices.size,
                                key = { index -> "notice_${state.notices[index].id}" },
                                contentType = { "notice" }
                            ) { index ->
                                val notice = state.notices[index]
                                key(notice.id) {
                                    ExoArticleItem(
                                        article = notice.toArticleModel(),
                                        type = ArticleItemType.ADMIN_NOTICE,
                                        viewModel = articleViewModel
                                    )
                                }
                            }

                            // 게시글 목록
                            items(
                                count = state.articles.size,
                                key = { index -> state.articles[index].id },
                                contentType = { "article" }
                            ) { index ->
                                val article = state.articles[index]

                                key(article.id) {
                                    ExoArticleItem(
                                        article = article,
                                        type = ArticleItemType.FREE_BOARD,
                                        showPopularIcon = showPopularIcon,
                                        viewModel = articleViewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Write Button (FAB) - old 프로젝트와 동일한 UI
        // btn_write_contents drawable 자체에 그라데이션 배경 + 아이콘이 포함되어 있음
        // 기존 FreeBoardPage: 최애 탭에서는 플로팅 버튼 숨김
        // 외부 idol 모드 (CommunityFanTalkSubPage 등): 숨김
        val showWriteButton = !isExternalIdolMode &&
            state.selectedTagId != FreeBoardContract.State.TAG_ID_MY_FAVORITE
        if (showWriteButton) {
            Icon(
                painter = painterResource(R.drawable.btn_write_contents),
                contentDescription = "Write",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(53.dp)
                    .clickable { onIntent(FreeBoardContract.Intent.OnWriteClick) },
                tint = androidx.compose.ui.graphics.Color.Unspecified
            )
        }
    }

    // 정렬 필터 바텀시트
    if (showOrderFilterSheet) {
        val orderFilterItems = listOf(
            ExoBottomSheetItem(FreeBoardContract.State.FILTER_DATE_ORDER, stringResource(R.string.freeboard_order_newest)),
            ExoBottomSheetItem(FreeBoardContract.State.FILTER_LIKE_ORDER, stringResource(R.string.order_by_like)),
            ExoBottomSheetItem(FreeBoardContract.State.FILTER_COMMENT_ORDER, stringResource(R.string.freeboard_order_comments)),
            ExoBottomSheetItem(FreeBoardContract.State.FILTER_HITS_ORDER, stringResource(R.string.order_hit))
        )

        ExoBottomSheetList(
            items = orderFilterItems,
            selectedValue = state.orderBy,
            onItemSelected = { orderBy ->
                when (orderBy) {
                    FreeBoardContract.State.FILTER_DATE_ORDER -> onIntent(FreeBoardContract.Intent.OnFilterLatest)
                    FreeBoardContract.State.FILTER_LIKE_ORDER -> onIntent(FreeBoardContract.Intent.OnFilterLike)
                    FreeBoardContract.State.FILTER_COMMENT_ORDER -> onIntent(FreeBoardContract.Intent.OnFilterComments)
                    FreeBoardContract.State.FILTER_HITS_ORDER -> onIntent(FreeBoardContract.Intent.OnFilterViewCount)
                }
            },
            onDismissRequest = { showOrderFilterSheet = false }
        )
    }

    // 언어 필터 바텀시트
    if (showLanguageFilterSheet) {
        // 언어와 이름을 함께 저장하기 위해 Pair 사용
        val languageItems = BoardLanguage.all().map { language ->
            val label = stringResource(language.labelResId)
            ExoBottomSheetItem(
                value = Pair(language, label),
                label = label
            )
        }

        val selectedLanguage = BoardLanguage.all().find { it.code == state.selectedLanguageId } ?: BoardLanguage.ALL
        val selectedLabel = stringResource(selectedLanguage.labelResId)

        ExoBottomSheetList(
            items = languageItems,
            selectedValue = Pair(selectedLanguage, selectedLabel),
            onItemSelected = { (language, languageName) ->
                val name = if (language == BoardLanguage.ALL) null else languageName
                onIntent(FreeBoardContract.Intent.OnLanguageFilterSelected(name, language.code))
            },
            onDismissRequest = { showLanguageFilterSheet = false }
        )
    }
}

/**
 * 스크롤 가능한 태그 탭 Row
 */
@Composable
private fun TagTabRow(
    tags: List<TagModel>,
    selectedTagId: Int,
    onTagSelected: (TagModel) -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .background(ColorPalette.background100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tags.forEach { tag ->
                TagChip(
                    tag = tag,
                    isSelected = tag.id == selectedTagId,
                    onClick = { onTagSelected(tag) }
                )
            }
        }
    }
}

/**
 * 태그 칩 (old 프로젝트의 TagAdapter 스타일 참고)
 */
@Composable
private fun TagChip(
    tag: TagModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 인기글 탭은 이미지 drawable 사용 (HOT 텍스트가 이미지에 포함됨)
    if (tag.id == FreeBoardContract.State.TAG_ID_HOT) {
        Icon(
            painter = painterResource(
                if (isSelected) R.drawable.btn_popularpost_on else R.drawable.btn_popularpost_off
            ),
            contentDescription = "HOT",
            modifier = Modifier
                .height(28.dp)
                .clickable(onClick = onClick),
            tint = androidx.compose.ui.graphics.Color.Unspecified
        )
    } else {
        // 나머지 탭: 선택됨 = textChat 배경, 선택 안됨 = 투명 배경 + gray200 테두리
        val backgroundColor = if (isSelected) ColorPalette.textChat else androidx.compose.ui.graphics.Color.Transparent
        val textColor = if (isSelected) ColorPalette.textWhiteBlack else ColorPalette.textDimmed
        val borderColor = if (isSelected) androidx.compose.ui.graphics.Color.Transparent else ColorPalette.gray200

        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(backgroundColor)
                .then(
                    if (!isSelected) {
                        Modifier.border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getTagDisplayName(tag),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )
        }
    }
}

/**
 * 태그 표시 이름 반환
 */
@Composable
private fun getTagDisplayName(tag: TagModel): String {
    return when (tag.id) {
        FreeBoardContract.State.TAG_ID_HOT -> "" // 인기글은 이미지 사용
        FreeBoardContract.State.TAG_ID_ALL -> "ALL"
        else -> tag.name
    }
}

@Composable
private fun FilterButton(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.btn_filter),
            contentDescription = null,
            modifier = Modifier.size(10.dp),
            tint = ColorPalette.textGray
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = ColorPalette.textGray
        )
    }
}

/**
 * 일반 빈 화면 - 검색 결과 없음 또는 게시글 없음
 */
@Composable
private fun EmptyView(hasSearchKeyword: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
            .padding(bottom = 107.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasSearchKeyword) {
                stringResource(R.string.no_search_result)
            } else {
                stringResource(R.string.freeboard_empty)
            },
            fontSize = 14.sp,
            color = ColorPalette.textDefault,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 최애 미설정 시 보여주는 화면
 * old 프로젝트의 ll_empty_most 레이아웃과 동일한 UI
 */
@Composable
private fun NoMostIdolEmptyView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 아이콘 이미지
            Icon(
                painter = painterResource(R.drawable.img_favorite_idol),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = androidx.compose.ui.graphics.Color.Unspecified
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 타이틀: "최애를 설정해야 볼 수 있어요!"
            Text(
                text = stringResource(R.string.freeboard_nobias_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorPalette.textDefault,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 서브타이틀: CELEB 여부에 따라 다른 문구
            Text(
                text = if (net.ib.mn.BuildConfig.CELEB) {
                    stringResource(R.string.freeboard_nobias_subtitle_celeb)
                } else {
                    stringResource(R.string.freeboard_nobias_subtitle)
                },
                fontSize = 13.sp,
                color = ColorPalette.textGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FreeBoardPagePreview() {
    FreeBoardContent(
        state = FreeBoardContract.State(
            tags = listOf(
                TagModel(id = 0, name = "", adminOnly = "N", selected = true),
                TagModel(id = 9898, name = "ALL", adminOnly = "N", selected = false),
                TagModel(id = 1, name = "자유", adminOnly = "N", selected = false),
                TagModel(id = 2, name = "유머", adminOnly = "N", selected = false),
                TagModel(id = 3, name = "정보", adminOnly = "N", selected = false)
            ),
            selectedTagId = 0
        ),
        onIntent = {}
    )
}
