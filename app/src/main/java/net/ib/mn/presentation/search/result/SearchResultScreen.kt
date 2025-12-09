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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
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
import net.ib.mn.data.remote.dto.MostIdol
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.SearchIdolModel
import net.ib.mn.domain.model.SearchSupportModel
import net.ib.mn.domain.model.SearchWallpaperModel
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.presentation.common.SearchBar
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoNameWithGroup
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.NumberFormatUtil

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
    viewModel: SearchResultViewModel = hiltViewModel(key = "search_result_$keyword")
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 최애 설정 다이얼로그 상태
    var showSetMostDialog by remember { mutableStateOf<SearchIdolModel?>(null) }

    // keyword를 ViewModel에 전달 (최초 진입 시에만 검색 실행)
    LaunchedEffect(Unit) {
        if (keyword.isNotBlank() && state.keyword.isEmpty()) {
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
    onArticleClick: (ArticleModel) -> Unit = {}
) {
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

                // 배경화면 섹션 (아이돌별로 그룹핑하여 가로 스크롤)
                if (state.wallpapers.isNotEmpty()) {
                    // 아이돌별로 그룹핑
                    val wallpapersByIdol = state.wallpapers.groupBy { it.idol?.name ?: "Unknown" }
                    wallpapersByIdol.forEach { (idolName, wallpapers) ->
                        item {
                            WallpaperSection(
                                idolName = idolName,
                                wallpapers = wallpapers,
                                onWallpaperClick = { onWallpaperClick(it) }
                            )
                        }
                    }
                    item { SectionDivider() }
                }

                // 잡담게시판(SmallTalk) 섹션
                if (state.smallTalks.isNotEmpty()) {
                    items(state.smallTalks) { article ->
                        SmallTalkItem(
                            article = article,
                            onClick = { onArticleClick(article) }
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
 * old 프로젝트의 item_support_main.xml 레이아웃 기반
 */
@Composable
private fun SearchSupportItem(
    support: SearchSupportModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.background_100))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 썸네일 (원형)
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = support.thumbnailUrl ?: support.imageUrl,
                    contentDescription = support.title,
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.menu_profile_default2),
                    error = painterResource(id = R.drawable.menu_profile_default2)
                )
            }

            // 서포트 정보
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                // 달성률 뱃지
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorResource(id = R.color.main200))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.support_achievement, support.progressPercent),
                        color = colorResource(id = R.color.main_light),
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // 아이돌 이름
                Text(
                    text = support.idol?.name ?: "",
                    color = colorResource(id = R.color.text_default),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(3.dp))

                // 서포트 제목
                Text(
                    text = support.title,
                    color = colorResource(id = R.color.text_default),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 서포트 내용/광고 타입
                support.content?.let { content ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = content,
                        color = colorResource(id = R.color.text_gray),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 기간
                if (support.startDate != null && support.endDate != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${support.startDate} ~ ${support.endDate}",
                        color = colorResource(id = R.color.text_gray),
                        fontSize = 12.sp
                    )
                }
            }

            // 화살표
            Icon(
                painter = painterResource(id = R.drawable.icon_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = colorResource(id = R.color.gray300)
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
 * 배경화면 섹션 (아이돌 이름별로 그룹핑)
 * old 프로젝트의 item_search_wallpaper_idol.xml 레이아웃 기반
 */
@Composable
private fun WallpaperSection(
    idolName: String,
    wallpapers: List<SearchWallpaperModel>,
    onWallpaperClick: (SearchWallpaperModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.background_100))
    ) {
        // 헤더: "아이돌이름 배경화면"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.background_100))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = idolName,
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

        // 가로 스크롤 배경화면 목록
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(colorResource(id = R.color.background_100)),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(wallpapers) { wallpaper ->
                AsyncImage(
                    model = wallpaper.thumbnailUrl ?: wallpaper.imageUrl,
                    contentDescription = wallpaper.title,
                    modifier = Modifier
                        .size(width = 120.dp, height = 168.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onWallpaperClick(wallpaper) },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * 검색된 배경화면 아이템 (리스트 형태 - 폴백용)
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
 * 잡담게시판(SmallTalk) 아이템
 * old 프로젝트의 item_small_talk.xml 레이아웃 기반
 */
@Composable
private fun SmallTalkItem(
    article: ArticleModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.background_100))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 17.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 왼쪽: 제목, 내용, 메타 정보
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                // 제목
                article.title?.let { title ->
                    Text(
                        text = title,
                        color = colorResource(id = R.color.text_default),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 내용
                article.content?.let { content ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = content,
                        color = colorResource(id = R.color.text_gray),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 작성자, 날짜
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    article.user?.nickname?.let { nickname ->
                        Text(
                            text = nickname,
                            color = colorResource(id = R.color.text_dimmed),
                            fontSize = 11.sp
                        )
                        Text(
                            text = " · ",
                            color = colorResource(id = R.color.text_dimmed),
                            fontSize = 11.sp
                        )
                    }
                    article.createdAt?.let { date ->
                        Text(
                            text = date,
                            color = colorResource(id = R.color.text_dimmed),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 좋아요, 댓글, 조회수
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 좋아요
                    Icon(
                        painter = painterResource(id = R.drawable.icon_board_like),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${article.likeCount}",
                        color = colorResource(id = R.color.text_gray),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // 댓글
                    Icon(
                        painter = painterResource(id = R.drawable.icon_board_comment),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${article.commentCount}",
                        color = colorResource(id = R.color.text_gray),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // 조회수
                    Icon(
                        painter = painterResource(id = R.drawable.icon_board_hits),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${article.viewCount}",
                        color = colorResource(id = R.color.text_gray),
                        fontSize = 12.sp
                    )
                }
            }

            // 오른쪽: 썸네일 (있는 경우만)
            val thumbnailUrl = article.thumbnailUrl ?: article.imageUrl
            if (!thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
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
