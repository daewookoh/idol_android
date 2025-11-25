package net.ib.mn.presentation.main.freeboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

            // Search Bar and Filters
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

            // Content Area
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(FreeBoardContract.Intent.Refresh) },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && !state.isRefreshing -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ColorPalette.main)
                        }
                    }
                    state.isEmpty -> {
                        EmptyView(hasSearchKeyword = !state.searchKeyword.isNullOrEmpty())
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ColorPalette.gray80)
                        ) {
                            items(
                                count = state.articles.size,
                                key = { index -> state.articles[index].id }
                            ) { index ->
                                val article = state.articles[index]
                                val tagName = state.tags.find { it.id == article.tagId }?.name

                                ExoBoardItem(
                                    article = article,
                                    onItemClick = { /* Navigate to detail */ },
                                    onUserClick = { /* Navigate to user profile */ },
                                    onLikeClick = { /* Toggle like */ },
                                    onCommentClick = { /* Navigate to comments */ },
                                    onMoreClick = { /* Show more options */ },
                                    showTag = state.selectedTagId == FreeBoardContract.State.TAG_ID_HOT ||
                                            state.selectedTagId == FreeBoardContract.State.TAG_ID_ALL,
                                    tagName = tagName,
                                    showPopularIcon = state.selectedTagId == FreeBoardContract.State.TAG_ID_HOT
                                )
                            }
                        }
                    }
                }
            }
        }

        // Write Button (FAB)
        FloatingActionButton(
            onClick = { onIntent(FreeBoardContract.Intent.OnWriteClick) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 35.dp)
                .size(53.dp),
            containerColor = ColorPalette.main,
            contentColor = ColorPalette.textLight
        ) {
            Icon(
                painter = painterResource(R.drawable.btn_write_contents),
                contentDescription = "Write",
                modifier = Modifier.size(24.dp),
                tint = ColorPalette.textLight
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
                .padding(horizontal = 20.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                .padding(horizontal = 12.dp),
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
