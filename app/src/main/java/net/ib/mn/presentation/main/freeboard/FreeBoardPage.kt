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
import net.ib.mn.domain.model.ArticleUser
import net.ib.mn.domain.model.TagModel
import net.ib.mn.ui.components.ExoBoardItem
import net.ib.mn.ui.components.ExoBoardItemType
import net.ib.mn.ui.components.ExoSearchBox
import net.ib.mn.ui.theme.ColorPalette

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeBoardContent(
    state: FreeBoardContract.State,
    onIntent: (FreeBoardContract.Intent) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tags Section (Scrollable Tabs)
            if (state.tags.isNotEmpty()) {
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
                            text = if (state.selectedLanguageId.isEmpty()) {
                                stringResource(R.string.filter_all_language)
                            } else {
                                state.selectedLanguage ?: stringResource(R.string.filter_all_language)
                            },
                            onClick = { onIntent(FreeBoardContract.Intent.OnLanguageFilterClick) }
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
                            onClick = { /* Show filter bottom sheet */ }
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
                            items(
                                count = state.articles.size,
                                key = { index -> state.articles[index].id },
                                // 성능 최적화: contentType 지정으로 아이템 재사용 최적화
                                contentType = { "article" }
                            ) { index ->
                                val article = state.articles[index]

                                // 성능 최적화: 개별 아이템을 key로 recomposition 최소화
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
        // 최애 탭에서는 플로팅 버튼 숨김
        if (state.selectedTagId != FreeBoardContract.State.TAG_ID_MY_FAVORITE) {
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
            .padding(bottom = 107.dp)
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
