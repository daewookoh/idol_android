package net.ib.mn.presentation.community.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.ArticleType
import net.ib.mn.ui.components.ExoArticle
import net.ib.mn.ui.components.ExoArticleVoteDialog
import net.ib.mn.ui.components.ExoBoardNoticeItem
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.DateTimeUtil

/**
 * CommunityFeedSubPage - 커뮤니티 피드 탭
 *
 * @param rankingItem 선택된 아이돌 정보
 * @param viewModel ViewModel
 */
@Composable
fun CommunityFeedSubPage(
    rankingItem: RankingItem,
    viewModel: CommunityFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // 투표 다이얼로그 상태
    var showVoteDialog by remember { mutableStateOf(false) }
    var selectedArticleId by remember { mutableStateOf("") }
    var selectedArticleHeart by remember { mutableLongStateOf(0L) }

    // 좋아요 클릭 throttling - 마지막 클릭 시간 저장 (articleId -> lastClickTime)
    val likeClickTimes = remember { mutableMapOf<String, Long>() }

    // 초기 로드 (isMost는 ViewModel에서 PreferencesManager를 통해 직접 확인)
    LaunchedEffect(rankingItem.id) {
        val idolId = rankingItem.id.toIntOrNull() ?: 0
        if (idolId > 0) {
            viewModel.loadFeed(idolId)
        }
    }

    // 무한 스크롤 - 마지막 아이템 도달 시 다음 페이지 로드
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && uiState.hasNextPage && !uiState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadNextPage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        if (uiState.isLoading && uiState.articles.isEmpty()) {
            // 초기 로딩
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorPalette.main)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // 공지사항 (ExoBoardNoticeItem 사용)
                if (uiState.notices.isNotEmpty()) {
                    items(
                        items = uiState.notices,
                        key = { "notice_${it.id}" }
                    ) { notice ->
                        ExoBoardNoticeItem(
                            notice = notice,
                            onItemClick = {
                                // TODO: 공지사항 클릭 처리
                            },
                            showDivider = uiState.notices.indexOf(notice) != uiState.notices.lastIndex
                        )
                    }
                }

                // 필터 헤더
                item(key = "header") {
                    FeedFilterHeader(
                        viewType = uiState.viewType,
                        orderBy = uiState.orderBy,
                        onViewTypeChange = { viewModel.setViewType(it) },
                        onOrderByChange = { viewModel.setOrderBy(it) }
                    )
                }

                // 게시글 목록
                itemsIndexed(
                    items = uiState.articles,
                    key = { _, article -> article.id }
                ) { index, article ->
                    // 로컬 상태로 즉시 UI 업데이트 (Old 프로젝트의 ViewHolder 방식)
                    var localIsLiked by remember(article.id) { mutableStateOf(article.isUserLike) }
                    var localLikeCount by remember(article.id) { mutableStateOf(article.likeCount) }

                    // 80% 이상 보일 때만 비디오 재생
                    // LazyColumn 인덱스: notices(0~n-1) + header(n) + articles(n+1~)
                    val actualIndex = index + uiState.notices.size + 1
                    val isVisible by remember {
                        derivedStateOf {
                            val layoutInfo = listState.layoutInfo
                            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == actualIndex }
                            if (itemInfo == null) {
                                false
                            } else {
                                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                val itemTop = itemInfo.offset
                                val itemBottom = itemInfo.offset + itemInfo.size
                                val visibleTop = maxOf(itemTop, layoutInfo.viewportStartOffset)
                                val visibleBottom = minOf(itemBottom, layoutInfo.viewportEndOffset)
                                val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0)
                                val visibilityRatio = if (itemInfo.size > 0) visibleHeight.toFloat() / itemInfo.size else 0f
                                visibilityRatio >= 0.8f
                            }
                        }
                    }

                    ExoArticle(
                        type = ArticleType.FEED,
                        profileImageUrl = article.user?.imageUrlCommunity ?: "",
                        userName = article.user?.nickname ?: "",
                        userLevel = article.user?.level ?: 0,
                        userEmoticonUrl = article.user?.emoticon?.emojiUrl,
                        createdAt = DateTimeUtil.formatFullDate(article.createdAt),
                        content = article.content ?: "",
                        mediaFiles = article.mediaFiles,
                        linkUrl = article.linkUrl,
                        linkTitle = article.linkTitle,
                        imageUrl = article.imageUrl,
                        umjjalUrl = article.umjjalUrl,
                        heartCount = article.heart.toInt(),
                        likeCount = localLikeCount,
                        commentCount = article.commentCount,
                        isLiked = localIsLiked,
                        isPrivate = article.isMostOnly == "Y",
                        isPopular = article.isPopular,
                        showTranslation = true,
                        isVisible = isVisible,
                        onProfileClick = {
                            // TODO: 프로필 클릭 처리
                        },
                        onMoreClick = {
                            // TODO: 더보기 클릭 처리
                        },
                        onHeartClick = {
                            selectedArticleId = article.id
                            selectedArticleHeart = article.heart
                            showVoteDialog = true
                        },
                        onLikeClick = {
                            // 1초 이내 재클릭 방지
                            val currentTime = System.currentTimeMillis()
                            val lastClickTime = likeClickTimes[article.id] ?: 0L
                            if (currentTime - lastClickTime < 1000L) {
                                return@ExoArticle
                            }
                            likeClickTimes[article.id] = currentTime

                            // 즉시 로컬 상태 업데이트 (Old의 setLikeIcon과 동일)
                            val newLiked = !localIsLiked
                            localIsLiked = newLiked
                            localLikeCount = if (newLiked) localLikeCount + 1 else (localLikeCount - 1).coerceAtLeast(0)

                            // API 호출
                            viewModel.postLike(article.id, newLiked)
                        },
                        onCommentClick = {
                            // TODO: 댓글 클릭 처리
                        },
                        onTranslateClick = {
                            // TODO: 번역 처리
                        },
                        onMediaClick = { mediaIndex ->
                            // TODO: 미디어 클릭 처리
                        }
                    )
                }

                // 다음 페이지 로딩 인디케이터
                if (uiState.isLoadingMore) {
                    item(key = "loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ColorPalette.main,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // 에러 메시지
        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    style = ExoTypo.body14.copy(color = ColorPalette.textDimmed)
                )
            }
        }

        // 투표 다이얼로그
        if (showVoteDialog && selectedArticleId.isNotEmpty()) {
            ExoArticleVoteDialog(
                articleId = selectedArticleId,
                articleHeart = selectedArticleHeart,
                onVote = { hearts, onSuccess, onError ->
                    viewModel.voteArticle(
                        articleId = selectedArticleId,
                        hearts = hearts,
                        onSuccess = { response ->
                            android.util.Log.d("CommunityFeedSubPage", "Vote success: ${response.msg}")
                            onSuccess(response)
                        },
                        onError = { errorMsg ->
                            android.util.Log.e("CommunityFeedSubPage", "Vote failed: $errorMsg")
                            onError(errorMsg)
                        }
                    )
                },
                onDismiss = {
                    showVoteDialog = false
                }
            )
        }
    }
}

/**
 * 피드 필터 헤더
 * - 왼쪽: 목록/그리드 보기 전환
 * - 중앙: 배경화면만 보기 체크박스 (그리드 모드에서만)
 * - 오른쪽: 정렬 필터
 */
@Composable
private fun FeedFilterHeader(
    viewType: ViewType,
    orderBy: OrderByType,
    onViewTypeChange: (ViewType) -> Unit,
    onOrderByChange: (OrderByType) -> Unit
) {
    var showOrderByMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽: 뷰 타입 전환 버튼
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 목록 보기 버튼
                Icon(
                    painter = painterResource(
                        if (viewType == ViewType.LIST)
                            R.drawable.btn_layout_vertical_on
                        else
                            R.drawable.btn_layout_vertical_off
                    ),
                    contentDescription = "List view",
                    modifier = Modifier
                        .size(17.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onViewTypeChange(ViewType.LIST)
                        },
                    tint = Color.Unspecified
                )

                Spacer(modifier = Modifier.width(15.dp))

                // 그리드 보기 버튼
                Icon(
                    painter = painterResource(
                        if (viewType != ViewType.LIST)
                            R.drawable.btn_layout_grid_on
                        else
                            R.drawable.btn_layout_grid_off
                    ),
                    contentDescription = "Grid view",
                    modifier = Modifier
                        .size(17.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onViewTypeChange(ViewType.GRID)
                        },
                    tint = Color.Unspecified
                )
            }

            // 오른쪽: 배경화면 체크박스 + 정렬 필터
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 배경화면만 보기 (그리드 모드에서만 표시)
                if (viewType != ViewType.LIST) {
                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onViewTypeChange(
                                    if (viewType == ViewType.WALLPAPER) ViewType.GRID else ViewType.WALLPAPER
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                if (viewType == ViewType.WALLPAPER)
                                    R.drawable.checkbox_on
                                else
                                    R.drawable.checkbox_off
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.show_background_only),
                            style = ExoTypo.body12.copy(color = ColorPalette.textGray)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))
                }

                // 정렬 필터
                Box {
                    Row(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showOrderByMenu = true
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.btn_filter),
                            contentDescription = "Filter",
                            modifier = Modifier.size(10.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = getOrderByLabel(orderBy),
                            style = ExoTypo.body12.copy(color = ColorPalette.textGray)
                        )
                    }

                    DropdownMenu(
                        expanded = showOrderByMenu,
                        onDismissRequest = { showOrderByMenu = false }
                    ) {
                        OrderByType.values().forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = getOrderByLabel(type),
                                        fontSize = 14.sp,
                                        color = if (orderBy == type) ColorPalette.main else ColorPalette.textDefault
                                    )
                                },
                                onClick = {
                                    onOrderByChange(type)
                                    showOrderByMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 하단 구분선
        HorizontalDivider(
            thickness = 0.3.dp,
            color = ColorPalette.gray110
        )
    }
}

/**
 * 정렬 타입에 따른 레이블 반환
 */
@Composable
private fun getOrderByLabel(orderBy: OrderByType): String {
    return when (orderBy) {
        OrderByType.HEART -> stringResource(R.string.order_by_heart)
        OrderByType.TIME -> stringResource(R.string.freeboard_order_newest)
        OrderByType.COMMENTS -> stringResource(R.string.freeboard_order_comments)
        OrderByType.LIKES -> stringResource(R.string.order_by_like)
    }
}
