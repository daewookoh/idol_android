package net.ib.mn.presentation.community.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.presentation.common.ArticleType
import net.ib.mn.presentation.common.ExoArticle
import net.ib.mn.presentation.common.ExoArticleNavigation
import net.ib.mn.presentation.common.ExoArticleViewModel
import net.ib.mn.ui.components.ExoBoardNoticeItem
import net.ib.mn.ui.components.ExoBottomSheetItem
import net.ib.mn.ui.components.ExoBottomSheetList
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * CommunityFeedSubPage - 커뮤니티 피드 탭
 *
 * @param rankingItem 선택된 아이돌 정보
 * @param onFirstArticleVideoPlaying 첫 번째 아티클의 비디오/움짤 재생 상태 콜백 (Top3 비디오 제어용)
 * @param onNavigateToProfile 프로필 화면 이동 콜백
 * @param onNavigateToArticleDetail 게시글 상세 화면 이동 콜백
 * @param viewModel ViewModel (상위에서 주입하여 상태 공유)
 * @param articleViewModel ExoArticle 공용 ViewModel
 */
@Composable
fun CommunityFeedSubPage(
    rankingItem: RankingItem,
    onFirstArticleVideoPlaying: (Boolean) -> Unit = {},
    onNavigateToProfile: (userId: Int, nickname: String, imageUrl: String?, level: Int, mostIdolName: String?) -> Unit = { _, _, _, _, _ -> },
    onNavigateToArticleDetail: (ArticleModel) -> Unit = {},
    viewModel: CommunityFeedViewModel = hiltViewModel(),
    articleViewModel: ExoArticleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // ExoArticle 네비게이션 이벤트 처리
    LaunchedEffect(Unit) {
        articleViewModel.navigationEvent.collect { event ->
            when (event) {
                is ExoArticleNavigation.Profile -> {
                    onNavigateToProfile(
                        event.userId,
                        event.nickname,
                        event.imageUrl,
                        event.level,
                        event.mostIdolName
                    )
                }
                is ExoArticleNavigation.ArticleDetail -> {
                    onNavigateToArticleDetail(event.article)
                }
                is ExoArticleNavigation.MediaDetail -> {
                    // TODO: 미디어 상세 화면으로 이동
                }
                is ExoArticleNavigation.Community -> {
                    // TODO: 커뮤니티 화면으로 이동
                }
            }
        }
    }

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

                // ViewType에 따라 목록 또는 그리드 표시
                if (uiState.viewType == ViewType.LIST) {
                    // 목록 보기 - 기존 ExoArticle 사용
                    itemsIndexed(
                        items = uiState.articles,
                        key = { _, article -> article.id }
                    ) { index, article ->
                        // 80% 이상 보일 때만 비디오 재생
                        val actualIndex = index + uiState.notices.size + 1
                        val isVisible by remember {
                            derivedStateOf {
                                val layoutInfo = listState.layoutInfo
                                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == actualIndex }
                                if (itemInfo == null) {
                                    false
                                } else {
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

                        // 첫 번째 아티클이 비디오/움짤이고 visible일 때 Top3 비디오 정지
                        val firstMedia = article.mediaFiles.firstOrNull()
                        val hasPlayableMedia = firstMedia != null && (firstMedia.isVideo || firstMedia.isGif)
                        if (index == 0 && hasPlayableMedia) {
                            LaunchedEffect(isVisible) {
                                onFirstArticleVideoPlaying(isVisible)
                            }
                        }

                        // ExoArticle - 모든 액션은 내부에서 처리
                        ExoArticle(
                            article = article,
                            type = ArticleType.FEED,
                            isVisible = isVisible,
                            showTranslation = true,
                            onDeleted = { deletedArticleId ->
                                viewModel.removeArticle(deletedArticleId)
                            },
                            onArticleUpdated = { updatedArticle ->
                                // ViewModel 직접 업데이트
                                viewModel.updateArticle(updatedArticle)
                            },
                            viewModel = articleViewModel
                        )
                    }
                } else {
                    // 그리드 보기 (GRID / WALLPAPER) - 3열 그리드
                    // Old 프로젝트처럼 articles를 3개씩 묶어서 한 행에 표시
                    val chunkedArticles = uiState.articles.chunked(3)
                    val isWallpaperMode = uiState.viewType == ViewType.WALLPAPER

                    items(
                        items = chunkedArticles,
                        key = { rowArticles -> "grid_row_${rowArticles.firstOrNull()?.id ?: ""}" }
                    ) { rowArticles ->
                        FeedGridRow(
                            articles = rowArticles,
                            isWallpaperMode = isWallpaperMode,
                            onItemClick = { article ->
                                // TODO: 이미지 상세 보기
                            }
                        )
                    }
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
            }
        }

        // 하단 구분선
        HorizontalDivider(
            thickness = 0.3.dp,
            color = ColorPalette.gray110
        )
    }

    // 정렬 필터 바텀시트
    if (showOrderByMenu) {
        val orderByItems = OrderByType.values().map { type ->
            ExoBottomSheetItem(
                value = type,
                label = getOrderByLabel(type)
            )
        }

        ExoBottomSheetList(
            items = orderByItems,
            selectedValue = orderBy,
            onItemSelected = { onOrderByChange(it) },
            onDismissRequest = { showOrderByMenu = false }
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

/**
 * 그리드 뷰 한 행 (3개의 이미지)
 * Old 프로젝트의 CommunityImgViewHolder / CommunityWallpaperViewHolder와 동일
 *
 * @param articles 최대 3개의 게시글
 * @param isWallpaperMode 배경화면 모드 (true면 centerCrop 안함)
 * @param onItemClick 아이템 클릭 이벤트
 */
@Composable
private fun FeedGridRow(
    articles: List<ArticleModel>,
    isWallpaperMode: Boolean = false,
    onItemClick: (ArticleModel) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // 최대 3개의 이미지 표시
        articles.forEachIndexed { index, article ->
            FeedGridItem(
                article = article,
                isWallpaperMode = isWallpaperMode,
                modifier = Modifier.weight(1f),
                onClick = { onItemClick(article) }
            )
        }

        // 3개 미만인 경우 빈 공간 채우기
        repeat(3 - articles.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 그리드 뷰 개별 아이템
 * - 일반 그리드: 정사각형 (1:1)
 * - 배경화면 모드: 세로형 (3:5)
 *
 * @param article 게시글 데이터
 * @param isWallpaperMode 배경화면 모드 (세로 길게)
 * @param onClick 클릭 이벤트
 */
@Composable
private fun FeedGridItem(
    article: ArticleModel,
    isWallpaperMode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // 대표 썸네일 URL 결정 (움짤이 아닌 썸네일 우선)
    val firstMedia = article.mediaFiles.firstOrNull()
    val imageUrl = firstMedia?.thumbnailUrl
        ?: firstMedia?.fileUrl
        ?: article.thumbnailUrl
        ?: article.imageUrl
        ?: ""

    // 이미지 개수 (2개 이상이면 badge 표시)
    val imageCount = article.mediaFiles.size

    // 배경화면 모드: 3:5 비율, 일반 그리드: 정사각형
    val aspectRatioValue = if (isWallpaperMode) 3f / 5f else 1f

    Box(
        modifier = modifier
            .aspectRatio(aspectRatioValue)
            .background(ColorPalette.gray100)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // 항상 Crop으로 빈 영역 없이
        )

        // 이미지 개수 badge (2개 이상일 때만 표시)
        if (imageCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "+$imageCount",
                    style = ExoTypo.body11.copy(color = Color.White)
                )
            }
        }
    }
}
