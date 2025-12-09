package net.ib.mn.presentation.search.result

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.SearchIdolModel
import net.ib.mn.domain.model.SearchSupportModel
import net.ib.mn.domain.model.SearchWallpaperModel
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.presentation.common.SearchBar
import net.ib.mn.ui.components.ExoScaffold

/**
 * 검색 결과 화면
 *
 * old 프로젝트의 SearchResultActivity를 Compose로 재구현
 * - 아이돌 검색 결과 (최대 3개 + 더보기)
 * - 서포트 검색 결과 (최대 3개 + 더보기)
 * - 배경화면 검색 결과
 * - 잡담게시판 검색 결과 (무한 스크롤)
 * - 커뮤니티 게시글 검색 결과 (무한 스크롤)
 *
 * Navigation 3 활용:
 * - LocalAppNavigator를 통해 네비게이션 직접 처리
 * - ViewModel에서 네비게이션 Intent/Effect 제거
 */
@Composable
fun SearchResultScreen(
    keyword: String,
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // keyword를 ViewModel에 전달
    LaunchedEffect(keyword) {
        if (keyword.isNotBlank()) {
            viewModel.sendIntent(SearchResultContract.Intent.Search(keyword))
        }
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SearchResultContract.Effect.NavigateBack -> {
                    navigator.popBackStack()
                }
                is SearchResultContract.Effect.NavigateToCommunity -> {
                    // TODO: 커뮤니티 화면으로 이동
                }
                is SearchResultContract.Effect.NavigateToSmallTalk -> {
                    // TODO: 잡담 화면으로 이동
                }
                is SearchResultContract.Effect.NavigateToSchedule -> {
                    // TODO: 스케줄 화면으로 이동
                }
                is SearchResultContract.Effect.NavigateToSupportDetail -> {
                    // TODO: 서포트 상세 화면으로 이동
                }
                is SearchResultContract.Effect.NavigateToWallpaperDetail -> {
                    // TODO: 배경화면 상세 화면으로 이동
                }
                is SearchResultContract.Effect.NavigateToArticleDetail -> {
                    // TODO: 게시글 상세 화면으로 이동
                }
                is SearchResultContract.Effect.ShowToast -> {
                    android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is SearchResultContract.Effect.ShowSetMostDialog -> {
                    // TODO: 최애 설정 확인 다이얼로그
                }
            }
        }
    }

    SearchResultContent(
        modifier = modifier,
        state = state,
        onNavigateBack = { navigator.popBackStack() },
        onSearch = { navigator.popBackStack() },
        onShowAllIdols = { viewModel.sendIntent(SearchResultContract.Intent.ShowAllIdols) },
        onShowAllSupports = { viewModel.sendIntent(SearchResultContract.Intent.ShowAllSupports) },
        onLoadMoreSmallTalks = { viewModel.sendIntent(SearchResultContract.Intent.LoadMoreSmallTalks) },
        onLoadMoreArticles = { viewModel.sendIntent(SearchResultContract.Intent.LoadMoreArticles) },
        onIdolClick = { idol ->
            // TODO: 커뮤니티 화면으로 이동
        },
        onIdolCommunityClick = { idol ->
            // TODO: 커뮤니티 화면으로 이동
        },
        onIdolSmallTalkClick = { idol ->
            // TODO: 잡담 화면으로 이동
        },
        onIdolScheduleClick = { idol ->
            // TODO: 스케줄 화면으로 이동
        },
        onToggleFavorite = { viewModel.sendIntent(SearchResultContract.Intent.ToggleFavorite(it)) },
        onSetMost = { viewModel.sendIntent(SearchResultContract.Intent.SetMost(it)) },
        onSupportClick = { support ->
            // TODO: 서포트 상세 화면으로 이동
        },
        onWallpaperClick = { wallpaper ->
            // TODO: 배경화면 상세 화면으로 이동
        },
        onArticleClick = { article ->
            // TODO: 게시글 상세 화면으로 이동
        }
    )
}

@Composable
private fun SearchResultContent(
    modifier: Modifier = Modifier,
    state: SearchResultContract.State,
    onNavigateBack: () -> Unit = {},
    onSearch: () -> Unit = {},
    onShowAllIdols: () -> Unit = {},
    onShowAllSupports: () -> Unit = {},
    onLoadMoreSmallTalks: () -> Unit = {},
    onLoadMoreArticles: () -> Unit = {},
    onIdolClick: (SearchIdolModel) -> Unit = {},
    onIdolCommunityClick: (SearchIdolModel) -> Unit = {},
    onIdolSmallTalkClick: (SearchIdolModel) -> Unit = {},
    onIdolScheduleClick: (SearchIdolModel) -> Unit = {},
    onToggleFavorite: (SearchIdolModel) -> Unit = {},
    onSetMost: (SearchIdolModel) -> Unit = {},
    onSupportClick: (SearchSupportModel) -> Unit = {},
    onWallpaperClick: (SearchWallpaperModel) -> Unit = {},
    onArticleClick: (ArticleModel) -> Unit = {}
) {
    val listState = rememberLazyListState()

    // 무한 스크롤 감지
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoading) {
            if (state.hasMoreSmallTalks && !state.isLoadingMoreSmallTalk) {
                onLoadMoreSmallTalks()
            }
            if (state.hasMoreArticles && !state.isLoadingMoreArticle) {
                onLoadMoreArticles()
            }
        }
    }

    ExoScaffold(
        modifier = modifier,
        useFullScreen = true,
        topBar = {
            SearchBar(
                searchQuery = state.keyword,
                onSearchQueryChange = {},
                onSearch = onSearch,
                onNavigateBack = onNavigateBack,
                readOnly = true,
                onClick = onSearch
            )
        }
    ) {
        // 로딩 상태
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colorResource(id = R.color.main)
                )
            }
        }
        // 검색 결과 없음
        else if (state.isEmpty) {
            EmptySearchResult(keyword = state.keyword)
        }
        // 검색 결과 표시
        else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                // 아이돌 섹션
                if (state.idols.isNotEmpty()) {
                    items(state.displayIdols) { idol ->
                        SearchIdolItem(
                            idol = idol,
                            onClick = { onIdolClick(idol) },
                            onCommunityClick = { onIdolCommunityClick(idol) },
                            onSmallTalkClick = { onIdolSmallTalkClick(idol) },
                            onScheduleClick = { onIdolScheduleClick(idol) },
                            onFavoriteClick = { onToggleFavorite(idol) },
                            onMostClick = { onSetMost(idol) }
                        )
                    }
                    if (state.showIdolsMore) {
                        item {
                            MoreButton(
                                onClick = onShowAllIdols,
                                text = stringResource(id = R.string.view_more)
                            )
                        }
                    }
                    item { SectionDivider() }
                }

                // 서포트 섹션
                if (state.supports.isNotEmpty()) {
                    items(state.displaySupports) { support ->
                        SearchSupportItem(
                            support = support,
                            onClick = { onSupportClick(support) }
                        )
                    }
                    if (state.showSupportsMore) {
                        item {
                            MoreButton(
                                onClick = onShowAllSupports,
                                text = stringResource(id = R.string.view_more)
                            )
                        }
                    }
                    item { SectionDivider() }
                }

                // 배경화면 섹션
                if (state.wallpapers.isNotEmpty()) {
                    items(state.wallpapers) { wallpaper ->
                        SearchWallpaperItem(
                            wallpaper = wallpaper,
                            onClick = { onWallpaperClick(wallpaper) }
                        )
                    }
                    item { SectionDivider() }
                }

                // 잡담게시판 섹션
                if (state.smallTalks.isNotEmpty()) {
                    items(state.smallTalks) { article ->
                        SearchArticleItem(
                            article = article,
                            onClick = { onArticleClick(article) }
                        )
                    }
                    if (state.isLoadingMoreSmallTalk) {
                        item { LoadingIndicator() }
                    }
                    item { SectionDivider() }
                }

                // 커뮤니티 게시글 섹션
                if (state.articles.isNotEmpty()) {
                    items(state.articles) { article ->
                        SearchArticleItem(
                            article = article,
                            onClick = { onArticleClick(article) }
                        )
                    }
                    if (state.isLoadingMoreArticle) {
                        item { LoadingIndicator() }
                    }
                }
            }
        }
    }
}

/**
 * 섹션 구분선
 */
@Composable
private fun SectionDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(colorResource(id = R.color.gray50))
    )
}

/**
 * 더보기 버튼
 */
@Composable
private fun MoreButton(
    onClick: () -> Unit,
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colorResource(id = R.color.gray50))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colorResource(id = R.color.gray580),
            fontSize = 14.sp
        )
    }
}

/**
 * 로딩 인디케이터
 */
@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = colorResource(id = R.color.main),
            strokeWidth = 2.dp
        )
    }
}

/**
 * 검색 결과 없음 UI
 */
@Composable
private fun EmptySearchResult(keyword: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.btn_navigation_search),
                contentDescription = null,
                tint = colorResource(id = R.color.gray300),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.no_search_result),
                color = colorResource(id = R.color.gray580),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 검색된 아이돌 아이템
 */
@Composable
private fun SearchIdolItem(
    idol: SearchIdolModel,
    onClick: () -> Unit,
    onCommunityClick: () -> Unit,
    onSmallTalkClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMostClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 이미지
            AsyncImage(
                model = idol.imageUrl,
                contentDescription = idol.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 이름
                Text(
                    text = idol.name,
                    color = colorResource(id = R.color.gray900),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 그룹명
                idol.groupName?.let { groupName ->
                    Text(
                        text = groupName,
                        color = colorResource(id = R.color.gray580),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 즐겨찾기 버튼
            Icon(
                painter = painterResource(
                    id = if (idol.isFavorite) R.drawable.btn_favorite_on else R.drawable.btn_favorite_off
                ),
                contentDescription = "Favorite",
                tint = if (idol.isFavorite) colorResource(id = R.color.main) else colorResource(id = R.color.gray300),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onFavoriteClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 버튼들 (커뮤니티, 잡담, 스케줄)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IdolActionButton(
                text = stringResource(id = R.string.enter_community),
                modifier = Modifier.weight(1f),
                onClick = onCommunityClick
            )
            IdolActionButton(
                text = stringResource(id = R.string.idoltalk),
                modifier = Modifier.weight(1f),
                onClick = onSmallTalkClick
            )
            IdolActionButton(
                text = stringResource(id = R.string.schedule),
                modifier = Modifier.weight(1f),
                onClick = onScheduleClick
            )
        }
    }

    HorizontalDivider(
        color = colorResource(id = R.color.gray100),
        thickness = 1.dp
    )
}

@Composable
private fun IdolActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorResource(id = R.color.gray50))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colorResource(id = R.color.gray580),
            fontSize = 13.sp
        )
    }
}

/**
 * 검색된 서포트 아이템
 */
@Composable
private fun SearchSupportItem(
    support: SearchSupportModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 썸네일
        AsyncImage(
            model = support.thumbnailUrl ?: support.imageUrl,
            contentDescription = support.title,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 제목
            Text(
                text = support.title,
                color = colorResource(id = R.color.gray900),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 진행률
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { support.progressRatio },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colorResource(id = R.color.main),
                    trackColor = colorResource(id = R.color.gray200)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${support.progressPercent}%",
                    color = colorResource(id = R.color.main),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 상태
            support.status?.let { status ->
                Text(
                    text = status,
                    color = if (support.isOngoing) colorResource(id = R.color.main) else colorResource(id = R.color.gray580),
                    fontSize = 12.sp
                )
            }
        }
    }

    HorizontalDivider(
        color = colorResource(id = R.color.gray100),
        thickness = 1.dp
    )
}

/**
 * 검색된 배경화면 아이템
 */
@Composable
private fun SearchWallpaperItem(
    wallpaper: SearchWallpaperModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 썸네일
        AsyncImage(
            model = wallpaper.thumbnailUrl ?: wallpaper.imageUrl,
            contentDescription = wallpaper.title,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 제목
            wallpaper.title?.let { title ->
                Text(
                    text = title,
                    color = colorResource(id = R.color.gray900),
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 아이돌 이름
            wallpaper.idol?.name?.let { name ->
                Text(
                    text = name,
                    color = colorResource(id = R.color.gray580),
                    fontSize = 12.sp
                )
            }
        }
    }

    HorizontalDivider(
        color = colorResource(id = R.color.gray100),
        thickness = 1.dp
    )
}

/**
 * 검색된 게시글 아이템
 */
@Composable
private fun SearchArticleItem(
    article: ArticleModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 제목
            article.title?.let { title ->
                Text(
                    text = title,
                    color = colorResource(id = R.color.gray900),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 내용
            article.content?.let { content ->
                Text(
                    text = content,
                    color = colorResource(id = R.color.gray580),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 메타 정보
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 작성자
                article.user?.nickname?.let { nickname ->
                    Text(
                        text = nickname,
                        color = colorResource(id = R.color.gray580),
                        fontSize = 12.sp
                    )
                    Text(
                        text = " · ",
                        color = colorResource(id = R.color.gray300),
                        fontSize = 12.sp
                    )
                }

                // 좋아요
                Icon(
                    painter = painterResource(id = R.drawable.icon_community_heart),
                    contentDescription = null,
                    tint = colorResource(id = R.color.gray300),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${article.likeCount}",
                    color = colorResource(id = R.color.gray580),
                    fontSize = 12.sp
                )

                Text(
                    text = " · ",
                    color = colorResource(id = R.color.gray300),
                    fontSize = 12.sp
                )

                // 댓글
                Icon(
                    painter = painterResource(id = R.drawable.icon_community_comment),
                    contentDescription = null,
                    tint = colorResource(id = R.color.gray300),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${article.commentCount}",
                    color = colorResource(id = R.color.gray580),
                    fontSize = 12.sp
                )
            }
        }

        // 썸네일 (있는 경우만)
        val thumbnailUrl = article.thumbnailUrl ?: article.imageUrl
        if (!thumbnailUrl.isNullOrEmpty()) {
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }

    HorizontalDivider(
        color = colorResource(id = R.color.gray100),
        thickness = 1.dp
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchResultContentPreview() {
    SearchResultContent(
        state = SearchResultContract.State(
            keyword = "BTS",
            idols = listOf(
                SearchIdolModel(
                    id = 1,
                    name = "BTS",
                    nameEn = "BTS",
                    nameJp = null,
                    nameZh = null,
                    nameZhTw = null,
                    type = "G",
                    category = "M",
                    groupId = null,
                    groupName = null,
                    imageUrl = null,
                    imageUrl2 = null,
                    heart = 100000000,
                    resourceUri = null,
                    birthday = null,
                    isLunarBirthday = null,
                    debutDay = null,
                    comebackDay = null,
                    burningDay = null,
                    angelCount = 10,
                    fairyCount = 5,
                    miracleCount = 3,
                    rookieCount = 1,
                    fdName = "아미",
                    fdNameEn = "ARMY",
                    chartCodes = null,
                    isFavorite = true,
                    isMost = false
                )
            ),
            supports = emptyList(),
            wallpapers = emptyList(),
            smallTalks = emptyList(),
            articles = emptyList()
        )
    )
}
