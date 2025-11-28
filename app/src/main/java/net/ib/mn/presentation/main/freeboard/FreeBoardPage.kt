package net.ib.mn.presentation.main.freeboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.ArticleUser
import net.ib.mn.domain.model.NoticeModel
import net.ib.mn.domain.model.TagModel
import net.ib.mn.ui.components.ExoBoardItem
import net.ib.mn.ui.components.ExoBoardItemType
import net.ib.mn.ui.components.ExoBoardNoticeItem
import net.ib.mn.ui.components.ExoSearchBox
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.BoardLanguage

/**
 * Free Board 페이지 - 프리톡 메뉴 화면
 */
@Composable
fun FreeBoardPage(
    onNavigateToWrite: () -> Unit = {},
    viewModel: FreeBoardViewModel = hiltViewModel()
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

    FreeBoardContent(
        state = state,
        onIntent = viewModel::sendIntent
    )
}

/**
 * FreeBoardContent - 재사용 가능한 게시판 콘텐츠
 *
 * @param state FreeBoardContract.State
 * @param onIntent Intent 핸들러
 * @param isExternalIdolMode 외부에서 idolId를 전달받은 모드 (태그탭 숨김, 글쓰기 버튼 표시)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeBoardContent(
    state: FreeBoardContract.State,
    onIntent: (FreeBoardContract.Intent) -> Unit,
    isExternalIdolMode: Boolean = false
) {
    // state.searchKeyword와 동기화되는 검색 텍스트
    var searchText by remember { mutableStateOf(state.searchKeyword ?: "") }

    // state.searchKeyword가 변경되면 (탭 변경 등) searchText도 업데이트
    LaunchedEffect(state.searchKeyword) {
        searchText = state.searchKeyword ?: ""
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // 정렬 필터 바텀시트 상태
    val orderFilterSheetState = rememberModalBottomSheetState()
    var showOrderFilterSheet by remember { mutableStateOf(false) }

    // 언어 필터 바텀시트 상태 (skipPartiallyExpanded = true로 처음부터 확장)
    val languageFilterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                                val isLastNotice = index == state.notices.size - 1

                                key(notice.id) {
                                    ExoBoardNoticeItem(
                                        notice = notice,
                                        onItemClick = { /* Navigate to notice detail (WebView) */ },
                                        showDivider = !isLastNotice // 마지막 공지는 구분선 표시 안함
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
                                    ExoBoardItem(
                                        article = article,
                                        onItemClick = { /* Navigate to detail */ },
                                        itemType = ExoBoardItemType.MINI,
                                        showPopularIcon = showPopularIcon
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
        ModalBottomSheet(
            onDismissRequest = { showOrderFilterSheet = false },
            sheetState = orderFilterSheetState,
            containerColor = ColorPalette.gray100
        ) {
            OrderFilterBottomSheetContent(
                currentOrderBy = state.orderBy,
                onFilterSelected = { orderBy ->
                    when (orderBy) {
                        FreeBoardContract.State.FILTER_DATE_ORDER -> onIntent(FreeBoardContract.Intent.OnFilterLatest)
                        FreeBoardContract.State.FILTER_LIKE_ORDER -> onIntent(FreeBoardContract.Intent.OnFilterLike)
                        FreeBoardContract.State.FILTER_COMMENT_ORDER -> onIntent(FreeBoardContract.Intent.OnFilterComments)
                        FreeBoardContract.State.FILTER_HITS_ORDER -> onIntent(FreeBoardContract.Intent.OnFilterViewCount)
                    }
                    scope.launch {
                        orderFilterSheetState.hide()
                        showOrderFilterSheet = false
                    }
                }
            )
        }
    }

    // 언어 필터 바텀시트 (화면의 70% 높이 고정, 내부 스크롤)
    if (showLanguageFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageFilterSheet = false },
            sheetState = languageFilterSheetState,
            containerColor = ColorPalette.gray100,
            sheetMaxWidth = BottomSheetDefaults.SheetMaxWidth
        ) {
            LanguageFilterBottomSheetContent(
                currentLanguageId = state.selectedLanguageId,
                onLanguageSelected = { language, languageName ->
                    onIntent(FreeBoardContract.Intent.OnLanguageFilterSelected(languageName, language.code))
                    scope.launch {
                        languageFilterSheetState.hide()
                        showLanguageFilterSheet = false
                    }
                },
                modifier = Modifier.fillMaxHeight(0.7f)
            )
        }
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

/**
 * 정렬 필터 바텀시트 내용
 * old 프로젝트의 bottom_sheet_board_filter.xml과 동일
 */
@Composable
private fun OrderFilterBottomSheetContent(
    currentOrderBy: String,
    onFilterSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // 최신순
        FilterOptionItem(
            text = stringResource(R.string.freeboard_order_newest),
            isSelected = currentOrderBy == FreeBoardContract.State.FILTER_DATE_ORDER,
            onClick = { onFilterSelected(FreeBoardContract.State.FILTER_DATE_ORDER) }
        )

        // 공감순
        FilterOptionItem(
            text = stringResource(R.string.order_by_like),
            isSelected = currentOrderBy == FreeBoardContract.State.FILTER_LIKE_ORDER,
            onClick = { onFilterSelected(FreeBoardContract.State.FILTER_LIKE_ORDER) }
        )

        // 댓글순
        FilterOptionItem(
            text = stringResource(R.string.freeboard_order_comments),
            isSelected = currentOrderBy == FreeBoardContract.State.FILTER_COMMENT_ORDER,
            onClick = { onFilterSelected(FreeBoardContract.State.FILTER_COMMENT_ORDER) }
        )

        // 조회순
        FilterOptionItem(
            text = stringResource(R.string.order_hit),
            isSelected = currentOrderBy == FreeBoardContract.State.FILTER_HITS_ORDER,
            onClick = { onFilterSelected(FreeBoardContract.State.FILTER_HITS_ORDER) }
        )
    }
}

/**
 * 언어 필터 바텀시트 내용
 */
@Composable
private fun LanguageFilterBottomSheetContent(
    currentLanguageId: String,
    onLanguageSelected: (BoardLanguage, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // 모든 언어 옵션들
        items(
            count = BoardLanguage.all().size,
            key = { index -> BoardLanguage.all()[index].code }
        ) { index ->
            val language = BoardLanguage.all()[index]
            val languageName = stringResource(language.labelResId)
            FilterOptionItem(
                text = languageName,
                isSelected = currentLanguageId == language.code ||
                    (currentLanguageId.isEmpty() && language == BoardLanguage.ALL),
                onClick = {
                    onLanguageSelected(
                        language,
                        if (language == BoardLanguage.ALL) null else languageName
                    )
                }
            )
        }
    }
}

/**
 * 필터 옵션 아이템
 */
@Composable
private fun FilterOptionItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) ColorPalette.main else ColorPalette.gray900
        )
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
