package net.ib.mn.presentation.search.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import net.ib.mn.navigation.Screen
import net.ib.mn.presentation.common.ArticleItemType
import net.ib.mn.presentation.common.ExoArticleItem
import net.ib.mn.presentation.common.ExoArticleNavigation
import net.ib.mn.presentation.common.ExoArticleViewModel
import net.ib.mn.presentation.common.SearchBar
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoNameWithGroup
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.NumberFormatUtil
import net.ib.mn.util.getAdDatePeriod

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
    timestamp: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel = hiltViewModel(key = "search_result_${keyword}_$timestamp"),
    articleViewModel: ExoArticleViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // 최애 설정 다이얼로그 상태
    var showSetMostDialog by remember { mutableStateOf<SearchIdolModel?>(null) }

    // keyword를 ViewModel에 전달 (최초 진입 시에만 검색 실행)
    LaunchedEffect(Unit) {
        if (keyword.isNotBlank() && state.keyword.isEmpty()) {
            viewModel.sendIntent(SearchResultContract.Intent.Search(keyword))
        }
    }

    // ExoArticleItem의 네비게이션 이벤트 처리
    LaunchedEffect(Unit) {
        articleViewModel.navigationEvent.collect { event ->
            when (event) {
                is ExoArticleNavigation.ArticleDetail -> {
                    navigator.navigate(
                        Screen.ArticleDetail(
                            articleId = event.articleId,
                            isFeed = event.isFeed
                        )
                    )
                }
                is ExoArticleNavigation.Profile -> {
                    // TODO: 프로필 화면으로 이동
                }
                is ExoArticleNavigation.MediaDetail -> {
                    val imageUrls = event.article.mediaFiles.mapNotNull { it.originUrl }
                    val selectedUrl = imageUrls.getOrNull(event.mediaIndex) ?: imageUrls.firstOrNull()
                    if (selectedUrl != null) {
                        navigator.navigate(
                            Screen.PhotoDetail(imageUrl = selectedUrl)
                        )
                    }
                }
                is ExoArticleNavigation.Community -> {
                    navigator.navigate(Screen.Community(idolId = event.idolId))
                }
                is ExoArticleNavigation.EditArticle -> {
                    navigator.navigate(
                        Screen.ArticleWrite(
                            writeType = "FEED",
                            idolId = event.article.idol?.id,
                            editingArticleId = event.article.id
                        )
                    )
                }
                else -> { /* 다른 이벤트 무시 */ }
            }
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
                    showSetMostDialog = effect.idol
                }
            }
        }
    }

    // 최애 설정 확인 다이얼로그
    showSetMostDialog?.let { idol ->
        val currentIsMost = idol.isMost
        val message = if (currentIsMost) {
            // 최애 해제 시
            buildAnnotatedString {
                append(context.getString(R.string.msg_favorite_unregi_guide2))
                append("\n")
                append(context.getString(R.string.msg_favorite_unregi_guide1))
            }
        } else {
            // 최애 설정 시 - 아이돌 이름만 빨간색으로 표시
            val guide2Template = context.getString(R.string.msg_favorite_guide_2__)

            buildAnnotatedString {
                append(context.getString(R.string.msg_favorite_guide_1))
                append("\n")
                val parts = guide2Template.split("%s")
                if (parts.size >= 2) {
                    append(parts[0])
                    withStyle(style = SpanStyle(color = ColorPalette.main)) {
                        append(idol.name)
                    }
                    append(parts[1])
                } else {
                    withStyle(style = SpanStyle(color = ColorPalette.main)) {
                        append(idol.name)
                    }
                    append(guide2Template)
                }
            }
        }

        ExoConfirmDialog(
            title = context.getString(R.string.my_idol),
            message = message,
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirm = {
                showSetMostDialog = null
                viewModel.sendIntent(SearchResultContract.Intent.ConfirmSetMost(idol))
            },
            onDismiss = {
                showSetMostDialog = null
            }
        )
    }

    SearchResultContent(
        modifier = modifier,
        state = state,
        onNavigateBack = { navigator.popBackStack() },
        onSearchQueryChange = { viewModel.sendIntent(SearchResultContract.Intent.UpdateSearchQuery(it)) },
        onSearch = { query ->
            if (query.isNotBlank()) {
                focusManager.clearFocus()
                viewModel.sendIntent(SearchResultContract.Intent.Search(query))
            }
        },
        onCancel = { navigator.popBackStack() },
        onShowAllIdols = { viewModel.sendIntent(SearchResultContract.Intent.ShowAllIdols) },
        onShowAllSupports = { viewModel.sendIntent(SearchResultContract.Intent.ShowAllSupports) },
        onLoadMoreSmallTalks = { viewModel.sendIntent(SearchResultContract.Intent.LoadMoreSmallTalks) },
        onLoadMoreArticles = { viewModel.sendIntent(SearchResultContract.Intent.LoadMoreArticles) },
        onIdolClick = { idol, initialTab ->
            navigator.navigate(
                net.ib.mn.navigation.Screen.Community(
                    idolId = idol.id,
                    initialTab = initialTab
                )
            )
        },
        onToggleFavorite = { viewModel.sendIntent(SearchResultContract.Intent.ToggleFavorite(it)) },
        onSetMost = { viewModel.sendIntent(SearchResultContract.Intent.SetMost(it)) },
        onSupportClick = { support ->
            // TODO: 서포트 상세 화면으로 이동
        },
        onWallpaperClick = { wallpaper ->
            // TODO: 배경화면 상세 화면으로 이동 (더보기 클릭 시)
        },
        onWallpaperImageClick = { imageUrl ->
            // 배경화면은 공유 버튼 숨김
            navigator.navigate(Screen.PhotoDetail(imageUrl = imageUrl, showShareButton = false))
        }
    )
}

@Composable
private fun SearchResultContent(
    modifier: Modifier = Modifier,
    state: SearchResultContract.State,
    onNavigateBack: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onCancel: () -> Unit = {},
    onShowAllIdols: () -> Unit = {},
    onShowAllSupports: () -> Unit = {},
    onLoadMoreSmallTalks: () -> Unit = {},
    onLoadMoreArticles: () -> Unit = {},
    onIdolClick: (SearchIdolModel, Int) -> Unit = { _, _ -> },
    onToggleFavorite: (SearchIdolModel) -> Unit = {},
    onSetMost: (SearchIdolModel) -> Unit = {},
    onSupportClick: (SearchSupportModel) -> Unit = {},
    onWallpaperClick: (SearchWallpaperModel) -> Unit = {},
    onWallpaperImageClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // maxHeart 계산: 전체 아이돌 리스트 중 최대 heart 값 (효율적으로 한 번만 계산)
    val maxHeart = remember(state.idols.size, state.idols.firstOrNull()?.id) {
        state.idols.maxOfOrNull { it.heart } ?: 0L
    }

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
                onSearchQueryChange = onSearchQueryChange,
                onSearch = { onSearch(state.keyword) },
                onNavigateBack = onNavigateBack,
                onCancel = {
                    // 검색어 리셋 + 화면 닫기
                    onSearchQueryChange("")
                    focusManager.clearFocus()
                    onCancel()
                },
                focusRequester = focusRequester
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
                            maxHeart = maxHeart,
                            onIdolClick = { initialTab -> onIdolClick(idol, initialTab) },
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
                    // 서포트 헤더
                    item {
                        SupportSectionHeader()
                    }
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

                // 배경화면 섹션 (아이돌별로 가로 스크롤)
                if (state.wallpapers.isNotEmpty()) {
                    items(state.wallpapers) { wallpaper ->
                        WallpaperSection(
                            wallpaper = wallpaper,
                            onImageClick = onWallpaperImageClick,
                            onWallpaperClick = onWallpaperClick
                        )
                    }
                    item { SectionDivider() }
                }

                // 잡담게시판(SmallTalk) 섹션 - ExoArticleItem FREE_BOARD 타입 사용
                if (state.smallTalks.isNotEmpty()) {
                    items(
                        items = state.smallTalks,
                        key = { "smalltalk_${it.id}" }
                    ) { article ->
                        ExoArticleItem(
                            article = article,
                            type = ArticleItemType.FREE_BOARD,
                            isVisible = true,
                            showTranslation = false
                        )
                    }
                    if (state.hasMoreSmallTalks && !state.isLoadingMoreSmallTalk) {
                        item {
                            MoreButton(
                                onClick = onLoadMoreSmallTalks,
                                text = stringResource(id = R.string.view_more)
                            )
                        }
                    }
                    if (state.isLoadingMoreSmallTalk) {
                        item { LoadingIndicator() }
                    }
                    item { SectionDivider() }
                }

                // 커뮤니티 게시글 섹션 - ExoArticleItem FEED 타입 사용
                if (state.articles.isNotEmpty()) {
                    items(
                        items = state.articles,
                        key = { it.id }
                    ) { article ->
                        // 아이돌 정보에서 커뮤니티 이름 추출
                        val communityName = article.idol?.let { idol ->
                            LocaleUtil.getLocalizedIdolName(context, idol)
                        }
                        ExoArticleItem(
                            article = article,
                            type = ArticleItemType.FEED,
                            externalCommunityName = communityName,
                            isVisible = true,
                            showTranslation = true
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
 * 더보기 텍스트 영역
 * old: TextView, padding 12dp, 12sp, bold, gray580, 상하 0.5dp border
 */
@Composable
private fun MoreButton(
    onClick: () -> Unit,
    text: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 상단 border
        HorizontalDivider(
            thickness = 0.5.dp,
            color = colorResource(id = R.color.gray100)
        )

        // 텍스트 영역 (전체 클릭 가능)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.background_100))
                .clickable { onClick() }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = colorResource(id = R.color.gray580),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // 하단 border
        HorizontalDivider(
            thickness = 0.5.dp,
            color = colorResource(id = R.color.gray100)
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
 * 프로그레스 퍼센트 계산
 * 38% ~ 100% 범위, 4th root 사용
 */
private fun calculateProgressPercent(heartCount: Long, maxHeartCount: Long): Float {
    return if (maxHeartCount == 0L || heartCount == 0L) {
        0.38f
    } else {
        val voteRoot = kotlin.math.sqrt(kotlin.math.sqrt(heartCount.toDouble()))
        val maxRoot = kotlin.math.sqrt(kotlin.math.sqrt(maxHeartCount.toDouble()))
        val p = 38 + (voteRoot * 62 / maxRoot)
        (p / 100f).toFloat().coerceIn(0.38f, 1f)
    }
}

/**
 * 검색된 아이돌 아이템
 * old 프로젝트의 item_searched_idol.xml 레이아웃 기반
 * 프로그레스바: s_league_progress -> main 그라데이션
 */
@Composable
private fun SearchIdolItem(
    idol: SearchIdolModel,
    maxHeart: Long,
    onIdolClick: (initialTab: Int) -> Unit,
    onFavoriteClick: () -> Unit,
    onMostClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.background_100))
    ) {
        // 상단 영역 (프로필 + 이름 + 프로그레스바 + 버튼들)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onIdolClick(0) }
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 이미지 (55dp 테두리 + 45dp 이미지)
            ExoProfileImage(
                imageUrl = idol.imageUrl,
                type = ProfileImageType.MEDIUM_CIRCLE,
                rank = idol.id,
                miracleCount = idol.miracleCount,
                fairyCount = idol.fairyCount,
                angelCount = idol.angelCount
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 이름 + 그룹 + 프로그레스바 영역
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 이름 + 그룹 (ExoNameWithGroup 사용)
                val fullName = if (idol.groupName.isNullOrEmpty()) {
                    idol.name
                } else {
                    "${idol.name}_${idol.groupName}"
                }
                ExoNameWithGroup(
                    fullName = fullName,
                    nameFontSize = 15.sp,
                    groupFontSize = 10.sp,
                    nameColor = R.color.text_default,
                    groupColor = R.color.gray300
                )

                Spacer(modifier = Modifier.height(3.dp))

                // 프로그레스바 + 뱃지 영역 (old: paddingBottom 3dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp)
                ) {
                    // 프로그레스 계산 (4th root 알고리즘)
                    val progressPercent = remember(idol.heart, maxHeart) {
                        calculateProgressPercent(idol.heart, maxHeart)
                    }

                    // 프로그레스바 영역 (old: 17dp height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(17.dp)
                    ) {
                        // 그라데이션 프로그레스바 (s_league_progress -> main)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressPercent)
                                .height(17.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            colorResource(id = R.color.s_league_progress),
                                            colorResource(id = R.color.main)
                                        )
                                    ),
                                    shape = RoundedCornerShape(8.5.dp)
                                ),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            // 투표 수 (오른쪽 끝, 세로 가운데 정렬, lineHeight = 바 높이)
                            Text(
                                text = NumberFormatUtil.formatWithComma(idol.heart),
                                color = colorResource(id = R.color.text_heart_votes),
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }

                    // 뱃지 영역 (old: marginStart 5dp, marginTop -3dp)
                    SearchIdolBadges(
                        angelCount = idol.angelCount,
                        fairyCount = idol.fairyCount,
                        miracleCount = idol.miracleCount,
                        rookieCount = idol.rookieCount
                    )
                }
            }

            Spacer(modifier = Modifier.width(15.dp))

            // 하트(최애) 버튼 - 17dp (IdolDialog와 동일), 좌우 10dp 터치영역
            Icon(
                painter = painterResource(
                    id = if (idol.isMost) R.drawable.btn_favorite_on else R.drawable.btn_favorite_off
                ),
                contentDescription = "Most",
                tint = Color.Unspecified,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onMostClick() }
                    .padding(horizontal = 10.dp)
                    .size(17.dp)
            )

            // 별(즐겨찾기) 버튼 - 17dp (IdolDialog와 동일), 좌측 10dp 터치영역
            Icon(
                painter = painterResource(
                    id = if (idol.isFavorite) R.drawable.btn_bookmark_on else R.drawable.btn_bookmark_off
                ),
                contentDescription = "Favorite",
                tint = Color.Unspecified,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onFavoriteClick() }
                    .padding(start = 10.dp)
                    .size(17.dp)
            )
        }

        // 버튼들 (커뮤니티 입장, 채팅, 스케줄 보기) - old: 40dp height, 0.5dp border
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .padding(bottom = 10.dp)
                .height(40.dp)
                .border(
                    width = 0.5.dp,
                    color = colorResource(id = R.color.gray100),
                    shape = RoundedCornerShape(0.dp)
                )
        ) {
            // 커뮤니티 입장 (initialTab = 0: FEED)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onIdolClick(0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.enter_community),
                    color = colorResource(id = R.color.gray300),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 구분선
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .fillMaxHeight()
                    .background(colorResource(id = R.color.gray100))
            )

            // 채팅 (initialTab = 2: CHAT)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onIdolClick(2) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.chat),
                    color = colorResource(id = R.color.gray300),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 구분선
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .fillMaxHeight()
                    .background(colorResource(id = R.color.gray100))
            )

            // 스케줄 보기 (initialTab = 3: SCHEDULE)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onIdolClick(3) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.enter_schedule),
                    color = colorResource(id = R.color.gray300),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 하단 구분선
        HorizontalDivider(
            color = colorResource(id = R.color.gray110),
            thickness = 0.3.dp
        )
    }
}

/**
 * 검색 아이돌 뱃지 (Angel, Fairy, Miracle, Rookie)
 * old: marginStart 5dp, marginTop -3dp, 13dp x 16dp
 */
@Composable
private fun SearchIdolBadges(
    angelCount: Int,
    fairyCount: Int,
    miracleCount: Int,
    rookieCount: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .padding(start = 5.dp)
            .offset(y = (-2).dp)
    ) {
        // Angel 배지
        if (angelCount > 0) {
            Box(
                modifier = Modifier.size(13.dp, 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.charity_angel_badge),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = angelCount.toString(),
                    color = colorResource(id = R.color.text_angel),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                )
            }
        }

        // Fairy 배지
        if (fairyCount > 0) {
            Box(
                modifier = Modifier.size(13.dp, 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.charity_fairy_badge),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = fairyCount.toString(),
                    color = colorResource(id = R.color.text_fairy),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                )
            }
        }

        // Miracle 배지
        if (miracleCount > 0) {
            Box(
                modifier = Modifier.size(13.dp, 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.charity_miracle_badge),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = miracleCount.toString(),
                    color = colorResource(id = R.color.text_miracle),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                )
            }
        }

        // Rookie 배지
        if (rookieCount > 0) {
            val isSuper = rookieCount >= 3
            Box(
                modifier = Modifier.size(13.dp, 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (isSuper) R.drawable.charity_super_rookie_badge
                        else R.drawable.charity_rookie_badge
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = if (isSuper) "S" else rookieCount.toString(),
                    color = colorResource(
                        if (isSuper) R.color.text_super_rookie else R.color.text_rookie
                    ),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                )
            }
        }
    }
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
 * 서포트 섹션 헤더
 */
@Composable
private fun SupportSectionHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.background_100))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.support),
                color = colorResource(id = R.color.text_gray),
                fontSize = 13.sp
            )
        }
        HorizontalDivider(
            color = colorResource(id = R.color.gray110),
            thickness = 0.3.dp
        )
    }
}

/**
 * 검색된 서포트 아이템
 * old 프로젝트의 item_support_main.xml 및 SupportMainAdapter.kt 기반
 * - 104dp 테두리 + 74dp 이미지
 * - 성공/실패 시 SUCCESS/FAIL 리본 테두리 이미지 표시
 * - 달성률 배지 (진행중: main200 배경/main_light 텍스트, 종료: gray300 배경/흰색 텍스트 "종료")
 * - 아이돌 이름 + 그룹명 (14sp bold / 10sp bold)
 * - 서포트 제목 (12sp bold)
 * - 이모지 + 광고 타입명 (성공/실패시만)
 * - 기간: 진행중 "시작일 ~ 종료일", 성공 "날짜 포함 N주"
 * - 성공 시 좋아요/댓글 수 표시
 * - 화살표: 12dp, 진행중 icon_main_arrow, 종료 icon_main_arrow_finish
 */
@Composable
private fun SearchSupportItem(
    support: SearchSupportModel,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isSuccess = support.status == 1
    val isFailed = support.status == 2
    val isEnded = isSuccess || isFailed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.background_100))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
                .padding(horizontal = 10.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 이미지 (104dp 테두리 + 74dp 이미지)
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center
            ) {
                // 프로필 이미지 (먼저 그려서 뒤에 배치)
                AsyncImage(
                    model = support.thumbnailUrl ?: support.imageUrl,
                    contentDescription = support.title,
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(colorResource(id = R.color.gray100)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(
                        id = if (support.id % 2 == 0) R.drawable.menu_profile_1
                        else R.drawable.menu_profile_2
                    )
                )

                // 성공/실패 테두리 이미지 (SUCCESS/FAIL 리본) - 프로필 위에 오버레이
                if (isEnded) {
                    Icon(
                        painter = painterResource(
                            id = if (isSuccess) R.drawable.img_success
                            else R.drawable.img_finish_fail
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(104.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            // 서포트 정보
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // 달성률 배지 (진행중인 경우만 표시, old: 21dp height, brand500 배경)
                if (!isEnded) {
                    Box(
                        modifier = Modifier
                            .height(21.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(colorResource(id = R.color.main))
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.support_achievement, support.progressPercent),
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // 아이돌 이름 + 그룹명 (ViewModel에서 idolId로 조회한 정보)
                if (support.idolName != null) {
                    val fullName = buildIdolFullName(context, support)
                    ExoNameWithGroup(
                        fullName = fullName,
                        nameFontSize = 14.sp,
                        groupFontSize = 10.sp,
                        spacing = 4.dp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // 서포트 제목 (old: 12sp, marginTop 2.7dp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = support.title,
                    color = colorResource(id = R.color.text_default),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 이모지 + 광고 타입 (old: 12sp, marginTop 2.3dp)
                // 성공/실패시 카테고리별 이모지 표시: 🇰🇷(K), 🌎(F), 📱(M)
                if (support.adTypeName != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 이모지 (카테고리별)
                        val emoji = when (support.adTypeCategory) {
                            "K" -> "\uD83C\uDDF0\uD83C\uDDF7 " // 🇰🇷
                            "F" -> "\uD83C\uDF0E " // 🌎
                            "M" -> "\uD83D\uDCF1 " // 📱
                            else -> ""
                        }
                        if (emoji.isNotEmpty()) {
                            Text(
                                text = emoji,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = support.adTypeName,
                            color = colorResource(id = R.color.text_gray),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 기간 (old: 12sp, marginTop 2.3dp)
                Spacer(modifier = Modifier.height(2.dp))
                if (isSuccess) {
                    // 성공 시: "yyyy. M. d. 포함 N주" 형식
                    val periodText = buildSuccessPeriodText(context, support)
                    if (periodText.isNotEmpty()) {
                        Text(
                            text = periodText,
                            color = colorResource(id = R.color.text_gray),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                } else if (support.startDate != null && support.endDate != null) {
                    // 진행중: "yyyy. M. d. ~ yyyy. M. d." 형식
                    val formattedStart = formatSupportDate(context, support.startDate)
                    val formattedEnd = formatSupportDate(context, support.endDate)
                    Text(
                        text = "$formattedStart ~ $formattedEnd",
                        color = colorResource(id = R.color.text_gray),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                // 성공 시 좋아요/댓글 수
                if (isSuccess) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 좋아요
                        Icon(
                            painter = painterResource(id = R.drawable.icon_board_like),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = NumberFormatUtil.formatWithComma(support.likeCount.toLong()),
                            color = colorResource(id = R.color.text_gray),
                            fontSize = 10.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        // 댓글
                        Icon(
                            painter = painterResource(id = R.drawable.icon_board_comment),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = NumberFormatUtil.formatWithComma(support.commentCount.toLong()),
                            color = colorResource(id = R.color.text_gray),
                            fontSize = 10.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // 화살표 (old: 12dp, marginEnd 5.3dp)
            Icon(
                painter = painterResource(
                    id = if (isEnded) R.drawable.icon_main_arrow_finish
                    else R.drawable.icon_main_arrow
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(12.dp),
                tint = Color.Unspecified
            )
        }

        // 하단 구분선
        HorizontalDivider(
            color = colorResource(id = R.color.gray110),
            thickness = 0.3.dp
        )
    }
}

/**
 * 서포트 날짜 포맷팅 (old 프로젝트의 UtilK.getKSTDateString과 동일)
 * 로케일에 맞는 날짜 형식으로 변환
 */
private fun formatSupportDate(context: android.content.Context, dateString: String): String {
    return try {
        // ISO 8601 형식 파싱 (API는 UTC로 제공)
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)
        if (date != null) {
            val locale = LocaleUtil.getAppLocale(context)
            val formatter = if (locale == java.util.Locale.KOREA) {
                java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale)
            } else {
                java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale)
            }
            // 출력은 KST로
            (formatter as? java.text.SimpleDateFormat)?.timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            formatter.format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

/**
 * 서포트의 아이돌 이름+그룹명 생성
 * old 프로젝트의 UtilK.setName 로직 참고
 * - 로케일에 따라 적절한 이름 선택
 * - 솔로(type == "S")면 이름만 반환
 * - 그룹이면 "이름_그룹명" 형식
 */
private fun buildIdolFullName(context: android.content.Context, support: SearchSupportModel): String {
    // 로케일에 맞는 이름 선택
    val name = LocaleUtil.getLocalizedName(
        context = context,
        name = support.idolName.orEmpty(),
        nameEn = support.idolNameEn.orEmpty(),
        nameZh = support.idolNameZh.orEmpty(),
        nameZhTw = support.idolNameZhTw.orEmpty(),
        nameJp = support.idolNameJp.orEmpty()
    )

    if (name.isEmpty()) return ""

    // 이미 "_"가 포함되어 있으면 그대로 반환
    if (name.contains("_")) return name

    val isSolo = support.idolType.equals("S", ignoreCase = true)
    return if (!isSolo && !support.idolGroupName.isNullOrEmpty()) {
        "${name}_${support.idolGroupName}"
    } else {
        name
    }
}

/**
 * 성공 서포트 기간 텍스트 생성 (old: String.format(R.string.format_include_date, dateString, adPeriod))
 * "날짜 포함 N주" 형식 (영어: "N weeks including date")
 *
 * old 프로젝트: d_day 필드 사용 (SearchSupportModel에서는 createdAt)
 * adTypePeriod 예: "2W" (2주), "1M" (1개월)
 */
private fun buildSuccessPeriodText(context: android.content.Context, support: SearchSupportModel): String {
    // old 프로젝트: d_day 필드 사용 (SearchSupportModel에서는 createdAt)
    val dateStr = support.createdAt ?: return ""
    val formattedDate = formatSupportDate(context, dateStr)

    // 광고 기간이 있으면 "날짜 포함 N주" 형식
    val period = support.adTypePeriod
    return if (!period.isNullOrEmpty()) {
        val periodText = period.getAdDatePeriod(context)
        if (periodText.isNotEmpty()) {
            context.getString(R.string.format_include_date, formattedDate, periodText)
        } else {
            formattedDate
        }
    } else {
        formattedDate
    }
}

/**
 * 배경화면 섹션 (아이돌별로 가로 스크롤)
 * old 프로젝트의 item_search_wallpaper_idol.xml 레이아웃 기반
 * - 아이돌 이름 + "배경화면" 헤더
 * - 가로 스크롤로 배경화면 이미지 목록 표시
 * - 마지막에 "더보기" 버튼 표시
 * @param onImageClick 이미지 클릭 시 (이미지 URL) 콜백
 */
@Composable
private fun WallpaperSection(
    wallpaper: SearchWallpaperModel,
    onImageClick: (String) -> Unit,
    onWallpaperClick: (SearchWallpaperModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.background_100))
            .padding(bottom = 30.dp) // old: setMargins bottom 30
    ) {
        // 헤더: "아이돌이름 배경화면"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.background_100))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = wallpaper.idolName ?: "Unknown",
                color = colorResource(id = R.color.text_gray),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(id = R.string.background),
                color = colorResource(id = R.color.text_dimmed),
                fontSize = 13.sp
            )
        }

        HorizontalDivider(
            color = colorResource(id = R.color.gray110),
            thickness = 0.3.dp
        )

        // 가로 스크롤 배경화면 목록 (old: 200dp height, padding 15dp 좌우, 16dp 상하)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(colorResource(id = R.color.background_100)),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp) // old: marginStart 5dp
        ) {
            // 배경화면 이미지들 (old: 100dp x 166dp, radius 10dp 추가)
            itemsIndexed(wallpaper.imageUrls) { index, imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 100.dp, height = 166.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // 호버 효과 제거
                        ) {
                            onImageClick(imageUrl)
                        },
                    contentScale = ContentScale.Crop
                )
            }

            // 더보기 버튼 (old: cl_more, 100dp x 166dp, bg_radius10_gray100)
            if (wallpaper.totalCount > wallpaper.imageUrls.size) {
                item {
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 166.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorResource(id = R.color.gray100))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // 호버 효과 제거
                            ) {
                                onWallpaperClick(wallpaper)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.view_more),
                                color = colorResource(id = R.color.text_dimmed),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.btn_go),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
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
