package net.ib.mn.presentation.mostset

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoButton
import net.ib.mn.ui.components.ExoNameWithGroup
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.NumberFormatUtil

/**
 * 최애 설정 화면
 *
 * old 프로젝트의 FavoriteSettingActivity를 Compose로 재구현
 * - 현재 최애 아이돌 표시 (프로필 이미지 + 이름)
 * - 아이돌 검색 기능
 * - 검색 결과 리스트 (투표순 정렬)
 * - 즐겨찾기 추가/삭제
 * - 최애 설정/해제
 */
@Composable
fun MostSetScreen(
    modifier: Modifier = Modifier,
    viewModel: MostSetViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MostSetContract.Effect.HideKeyboard -> {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
                is MostSetContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MostSetContract.Effect.ShowToastRes -> {
                    Toast.makeText(context, effect.messageResId, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    MostSetContent(
        modifier = modifier,
        state = state,
        onSearchQueryChange = { viewModel.sendIntent(MostSetContract.Intent.UpdateSearchQuery(it)) },
        onSearch = { viewModel.sendIntent(MostSetContract.Intent.DoSearch) },
        onToggleFavorite = { viewModel.sendIntent(MostSetContract.Intent.ToggleFavorite(it)) },
        onSetMost = { viewModel.sendIntent(MostSetContract.Intent.SetMost(it)) },
        onComplete = { navigator.popBackStack() }
    )
}

@Composable
private fun MostSetContent(
    modifier: Modifier = Modifier,
    state: MostSetContract.State,
    onSearchQueryChange: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onToggleFavorite: (MostSetContract.IdolItem) -> Unit = {},
    onSetMost: (MostSetContract.IdolItem?) -> Unit = {},
    onComplete: () -> Unit = {}
) {
    // maxHeart 계산: 검색 결과 리스트 중 최대 heart 값
    val displayList = if (state.isSearchMode) state.searchResults else emptyList()
    val maxHeart = remember(displayList.size, displayList.firstOrNull()?.id) {
        displayList.maxOfOrNull { it.heart } ?: 0L
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ExoAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_favorite_setting),
                        style = ExoTypo.typo20Bold,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            )
        },
        bottomBar = {
            // 하단 완료 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(id = R.color.background_100))
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                ExoButton(
                    onClick = onComplete,
                    text = stringResource(id = R.string.complete),
                    enabled = state.mostIdolId != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp)
                )
            }
        },
        containerColor = colorResource(id = R.color.background_100)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
                .background(colorResource(id = R.color.background_100)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 현재 최애 프로필 이미지
            MostIdolProfile(
                imageUrl = state.mostIdolImageUrl,
                modifier = Modifier.size(125.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 최애 라벨 + 이름
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (BuildConfig.CELEB) {
                        stringResource(id = R.string.actor_most_favorite)
                    } else {
                        stringResource(id = R.string.most_favorite)
                    },
                    style = ExoTypo.typo14Bold.copy(
                        color = colorResource(id = R.color.main)
                    )
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = state.mostIdolName ?: stringResource(id = R.string.none),
                    style = ExoTypo.typo14.copy(
                        color = colorResource(id = R.color.text_default)
                    )
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 검색 바
            MostSetSearchBar(
                searchQuery = state.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearch = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 로딩 또는 리스트 (검색 모드에서만 표시)
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(id = R.color.main)
                    )
                }
            } else if (state.isSearchMode) {
                if (displayList.isEmpty()) {
                    // 검색 결과 없음
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_search_result),
                            style = ExoTypo.typo13.copy(
                                color = colorResource(id = R.color.text_gray)
                            )
                        )
                    }
                } else {
                    // 아이돌 리스트
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = displayList,
                            key = { it.id }
                        ) { idol ->
                            MostSetIdolItem(
                                idol = idol,
                                maxHeart = maxHeart,
                                onFavoriteClick = { onToggleFavorite(idol) },
                                onMostClick = { onSetMost(if (idol.isMost) null else idol) }
                            )
                        }
                    }
                }
            }
            // 검색 전에는 아무 리스트도 표시하지 않음
        }
    }
}

/**
 * 현재 최애 프로필 이미지
 */
@Composable
private fun MostIdolProfile(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colorResource(id = R.color.background_200)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .error(R.drawable.menu_profile_default)
                .placeholder(R.drawable.menu_profile_default)
                .build(),
            contentDescription = "Most idol profile",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

/**
 * 검색 바
 */
@Composable
private fun MostSetSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colorResource(id = R.color.background_300)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 텍스트 입력 영역
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (searchQuery.isEmpty()) {
                Text(
                    text = if (BuildConfig.CELEB) {
                        stringResource(id = R.string.actor_hint_search_idol)
                    } else {
                        stringResource(id = R.string.hint_search_idol)
                    },
                    style = ExoTypo.typo14.copy(
                        color = colorResource(id = R.color.gray300)
                    )
                )
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = ExoTypo.typo12.copy(
                    color = colorResource(id = R.color.text_default)
                ),
                singleLine = true,
                cursorBrush = SolidColor(colorResource(id = R.color.text_default)),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                )
            )
        }

        // 검색 버튼
        Box(
            modifier = Modifier
                .size(38.dp)
                .padding(end = 5.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onSearch() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.btn_navigation_search),
                contentDescription = "Search",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
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
 * 최애설정 아이돌 아이템
 * SearchResultScreen.kt의 SearchIdolItem과 동일한 구조
 * old 프로젝트의 item_searched_idol.xml 레이아웃 기반
 */
@Composable
private fun MostSetIdolItem(
    idol: MostSetContract.IdolItem,
    maxHeart: Long,
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
                .padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 이미지 (55dp 테두리 + 45dp 이미지)
            ExoProfileImage(
                imageUrl = idol.imageUrl ?: idol.imageUrl2,
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

                // 프로그레스바 + 뱃지 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp)
                ) {
                    // 프로그레스 계산 (4th root 알고리즘)
                    val progressPercent = remember(idol.heart, maxHeart) {
                        calculateProgressPercent(idol.heart, maxHeart)
                    }

                    // 프로그레스바 영역 (17dp height)
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
                            // 투표 수 (오른쪽 끝, 세로 가운데 정렬)
                            Text(
                                text = NumberFormatUtil.formatWithComma(idol.heart),
                                style = ExoTypo.typo11.copy(
                                    color = colorResource(id = R.color.text_heart_votes),
                                    lineHeight = 17.sp
                                ),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }

                    // 뱃지 영역
                    MostSetIdolBadges(
                        angelCount = idol.angelCount,
                        fairyCount = idol.fairyCount,
                        miracleCount = idol.miracleCount,
                        isRookie = idol.isRookie,
                        isSuperRookie = idol.isSuperRookie
                    )
                }
            }

            Spacer(modifier = Modifier.width(15.dp))

            // 하트(최애) 버튼 - 17dp, 좌우 10dp 터치영역
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

            // 별(즐겨찾기) 버튼 - 17dp, 좌측 10dp 터치영역
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

        // 하단 구분선
        HorizontalDivider(
            color = colorResource(id = R.color.gray110),
            thickness = 0.3.dp
        )
    }
}

/**
 * 최애설정 아이돌 뱃지 (Angel, Fairy, Miracle, Rookie)
 * SearchResultScreen의 SearchIdolBadges와 동일
 */
@Composable
private fun MostSetIdolBadges(
    angelCount: Int,
    fairyCount: Int,
    miracleCount: Int,
    isRookie: Boolean,
    isSuperRookie: Boolean
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
                    style = ExoTypo.typo7Bold.copy(
                        color = colorResource(id = R.color.text_angel)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 6.dp)
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
                    style = ExoTypo.typo7Bold.copy(
                        color = colorResource(id = R.color.text_fairy)
                    ),
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
                    style = ExoTypo.typo7Bold.copy(
                        color = colorResource(id = R.color.text_miracle)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                )
            }
        }

        // Rookie 배지
        if (isRookie || isSuperRookie) {
            Box(
                modifier = Modifier.size(13.dp, 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (isSuperRookie) R.drawable.charity_super_rookie_badge
                        else R.drawable.charity_rookie_badge
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = if (isSuperRookie) "S" else "R",
                    style = ExoTypo.typo7Bold.copy(
                        color = colorResource(
                            if (isSuperRookie) R.color.text_super_rookie else R.color.text_rookie
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                )
            }
        }
    }
}
