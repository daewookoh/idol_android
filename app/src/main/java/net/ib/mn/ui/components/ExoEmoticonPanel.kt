package net.ib.mn.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.EmoticonDetailModel
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.IdolImageUtil.toSecureUrl

/**
 * ExoEmoticonPanel - 이모티콘 선택 패널 (댓글 입력창 아래에 표시)
 * old 프로젝트의 EmoticonFragment와 동일한 레이아웃
 *
 * @param visible 표시 여부
 * @param selectedEmoticonId 현재 선택된 이모티콘 ID (-1이면 선택 없음)
 * @param onEmoticonSelected 이모티콘 선택 콜백
 * @param onEmoticonDoubleClick 이모티콘 더블 클릭 콜백 (같은 이모티콘 빠르게 두번 클릭 시)
 * @param viewModel ViewModel (ExoEmoticonBottomSheetViewModel 재사용)
 * @param modifier Modifier
 */
@Composable
fun ExoEmoticonPanel(
    visible: Boolean,
    selectedEmoticonId: Int = -1,
    onEmoticonSelected: (EmoticonDetailModel) -> Unit,
    onEmoticonDoubleClick: ((EmoticonDetailModel) -> Unit)? = null,
    viewModel: EmoticonBottomSheetViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val tabListState = rememberLazyListState()

    // 더블 클릭 감지를 위한 상태 (old 프로젝트와 동일)
    var lastClickedEmoticonId by remember { mutableIntStateOf(-1) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var firstClickTime by remember { mutableLongStateOf(0L) }
    var isDoubleClickReady by remember { mutableIntStateOf(0) } // 0: 초기, 1: 첫 클릭 후

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(200)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(100)  // 빠르게 닫힘
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(ColorPalette.background100)
        ) {
            // 상단 구분선 (old: emoticon_divider1)
            HorizontalDivider(
                color = ColorPalette.gray150,
                thickness = 1.dp
            )

            if (uiState.isLoading && uiState.emoticonSets.isEmpty()) {
                // 로딩 상태
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ColorPalette.main,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (uiState.emoticonSets.isNotEmpty()) {
                val pagerState = rememberPagerState(
                    initialPage = uiState.selectedTabIndex,
                    pageCount = { uiState.emoticonSets.size }
                )

                // 탭 선택시 페이저 이동
                LaunchedEffect(uiState.selectedTabIndex) {
                    if (pagerState.currentPage != uiState.selectedTabIndex) {
                        pagerState.animateScrollToPage(uiState.selectedTabIndex)
                    }
                    // 탭 스크롤
                    tabListState.animateScrollToItem(
                        index = maxOf(0, uiState.selectedTabIndex - 1)
                    )
                }

                // 페이저 이동시 탭 선택
                LaunchedEffect(pagerState.currentPage) {
                    if (uiState.selectedTabIndex != pagerState.currentPage) {
                        viewModel.selectTab(pagerState.currentPage)
                    }
                }

                // 탭 바 (old: rv_chat_emoticon - 가로 스크롤)
                LazyRow(
                    state = tabListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    itemsIndexed(uiState.emoticonSets) { index, emoticonSet ->
                        // 탭 아이콘: emojiUrl이 비어있으면 첫 번째 이모티콘의 CDN URL 사용
                        val tabIconUrl = emoticonSet.emojiUrl.ifEmpty {
                            uiState.emoticonDetails[emoticonSet.id]?.firstOrNull()?.let { firstEmoticon ->
                                viewModel.getEmoticonUrl(firstEmoticon.id)
                            } ?: ""
                        }.toSecureUrl()
                        EmoticonTabItem(
                            emojiUrl = tabIconUrl,
                            title = emoticonSet.title,
                            isSelected = uiState.selectedTabIndex == index,
                            onClick = {
                                viewModel.selectTab(index)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }

                // 탭 아래 구분선 (old: emoticon_divider2)
                HorizontalDivider(
                    color = ColorPalette.gray150,
                    thickness = 1.dp
                )

                // 이모티콘 그리드 (old: vp_chat_emoticon)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val emoticonSetId = uiState.emoticonSets.getOrNull(page)?.id ?: return@HorizontalPager
                    val emoticons = uiState.emoticonDetails[emoticonSetId] ?: emptyList()

                    if (emoticons.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ColorPalette.main,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        // old: GridLayoutManager 4열, 상단 정렬
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalArrangement = Arrangement.Top
                        ) {
                            items(emoticons) { emoticon ->
                                // CDN URL 기반으로 이모티콘 이미지 URL 생성
                                val emoticonUrl = viewModel.getEmoticonUrl(emoticon.id)
                                EmoticonPanelItem(
                                    emoticon = emoticon,
                                    emoticonUrl = emoticonUrl,
                                    onClick = {
                                        val currentTime = SystemClock.elapsedRealtime()

                                        // old 프로젝트와 동일한 더블 클릭 로직
                                        if (lastClickedEmoticonId == emoticon.id) {
                                            // 같은 이모티콘 클릭
                                            lastClickTime = currentTime
                                            if (isDoubleClickReady == 1 && (lastClickTime - firstClickTime < 300)) {
                                                // 빠르게 두번 클릭 -> 바로 전송
                                                onEmoticonDoubleClick?.invoke(emoticon)
                                                isDoubleClickReady = 0
                                            } else {
                                                // 느리게 클릭 -> 첫 클릭으로 처리
                                                firstClickTime = currentTime
                                                onEmoticonSelected(emoticon)
                                                isDoubleClickReady = 1
                                            }
                                        } else {
                                            // 다른 이모티콘 클릭 -> 선택만
                                            lastClickedEmoticonId = emoticon.id
                                            firstClickTime = currentTime
                                            onEmoticonSelected(emoticon)
                                            isDoubleClickReady = 1
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 이모티콘 탭 아이템 (old: emoticon_tab_item.xml과 동일)
 */
@Composable
private fun EmoticonTabItem(
    emojiUrl: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = emojiUrl.toSecureUrl(),
            contentDescription = title,
            modifier = Modifier
                .width(38.dp)
                .height(30.dp),
            contentScale = ContentScale.Fit,
            alpha = if (isSelected) 1f else 0.5f
        )
    }
}

/**
 * 개별 이모티콘 아이템 (old: emoticon_item.xml과 동일 - 60dp x 60dp)
 *
 * @param emoticon 이모티콘 상세 모델
 * @param emoticonUrl CDN URL 기반 이모티콘 이미지 URL
 * @param onClick 클릭 콜백
 */
@Composable
private fun EmoticonPanelItem(
    emoticon: EmoticonDetailModel,
    emoticonUrl: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = emoticonUrl,
            contentDescription = emoticon.title,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    }
}
