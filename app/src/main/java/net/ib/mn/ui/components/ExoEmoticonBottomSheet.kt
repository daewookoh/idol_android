package net.ib.mn.ui.components

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.repository.EmoticonRepository
import net.ib.mn.util.IdolImageUtil
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.EmoticonDetailModel
import net.ib.mn.domain.model.EmoticonModel
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import javax.inject.Inject

/**
 * 이모티콘 바텀시트 UI 상태
 */
data class EmoticonBottomSheetUiState(
    val emoticonSets: List<EmoticonModel> = emptyList(),
    val emoticonDetails: Map<Int, List<EmoticonDetailModel>> = emptyMap(),
    val isLoading: Boolean = false,
    val selectedTabIndex: Int = 0,
    val cdnUrl: String = ""
)

/**
 * 이모티콘 바텀시트 ViewModel
 */
@HiltViewModel
class EmoticonBottomSheetViewModel @Inject constructor(
    private val emoticonRepository: EmoticonRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "EmoticonBottomSheet"
    }

    private val _uiState = MutableStateFlow(EmoticonBottomSheetUiState())
    val uiState: StateFlow<EmoticonBottomSheetUiState> = _uiState.asStateFlow()

    init {
        loadCdnUrl()
        loadEmoticonSets()
    }

    /**
     * CDN URL 로드
     */
    private fun loadCdnUrl() {
        viewModelScope.launch {
            val cdnUrl = preferencesManager.cdnUrl.first() ?: ""
            _uiState.value = _uiState.value.copy(cdnUrl = cdnUrl)
            logD(TAG, "CDN URL loaded: $cdnUrl")
        }
    }

    /**
     * 이모티콘 ID로 올바른 URL 생성
     * old 프로젝트의 UtilK.fileImageUrl() 로직과 동일
     */
    fun getEmoticonUrl(emoticonId: Int): String {
        val cdnUrl = _uiState.value.cdnUrl
        return if (cdnUrl.isNotEmpty()) {
            IdolImageUtil.getEmoticonImageUrl(cdnUrl, emoticonId).toSecureUrl()
        } else {
            "" // cdnUrl이 없으면 빈 문자열 반환
        }
    }

    /**
     * 이모티콘 세트 목록 로드
     */
    private fun loadEmoticonSets() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            emoticonRepository.getEmoticonSets().collect { result ->
                when (result) {
                    is ApiResult.Loading -> { /* skip */ }
                    is ApiResult.Success -> {
                        // old 프로젝트와 동일하게 역순 정렬
                        val sets = result.data.emoticonSets.reversed()
                        _uiState.value = _uiState.value.copy(
                            emoticonSets = sets,
                            isLoading = false
                        )
                        // 모든 세트의 상세 로드 (탭 아이콘용)
                        sets.forEach { set ->
                            loadEmoticonDetails(set.id)
                            logD(TAG, "EmoticonSet: id=${set.id}, title=${set.title}, emojiUrl=${set.emojiUrl}")
                        }
                        logD(TAG, "Loaded ${sets.size} emoticon sets (reversed)")
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        logE(TAG, "Failed to load emoticon sets: ${result.error.message}")
                    }
                }
            }
        }
    }

    /**
     * 특정 이모티콘 세트의 상세 로드
     */
    fun loadEmoticonDetails(emoticonSetId: Int) {
        // 이미 로드된 경우 스킵
        if (_uiState.value.emoticonDetails.containsKey(emoticonSetId)) {
            return
        }

        viewModelScope.launch {
            emoticonRepository.getEmoticonSet(emoticonSetId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> { /* skip */ }
                    is ApiResult.Success -> {
                        val details = result.data.emoticons.sortedBy { it.order }
                        _uiState.value = _uiState.value.copy(
                            emoticonDetails = _uiState.value.emoticonDetails + (emoticonSetId to details)
                        )
                        logD(TAG, "Loaded ${details.size} emoticons for set $emoticonSetId")
                    }
                    is ApiResult.Error -> {
                        logE(TAG, "Failed to load emoticon details: ${result.error.message}")
                    }
                }
            }
        }
    }

    /**
     * 탭 선택
     */
    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
        val sets = _uiState.value.emoticonSets
        if (index < sets.size) {
            loadEmoticonDetails(sets[index].id)
        }
    }
}

/**
 * ExoEmoticonBottomSheet - 이모티콘 선택 바텀시트
 * old 프로젝트의 EmoticonFragment와 동일한 레이아웃
 *
 * @param onEmoticonSelected 이모티콘 선택 콜백
 * @param onEmoticonDoubleClick 이모티콘 더블 클릭 콜백 (같은 이모티콘 빠르게 두번 클릭 시)
 * @param onDismissRequest 닫기 요청
 * @param viewModel ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExoEmoticonBottomSheet(
    onEmoticonSelected: (EmoticonDetailModel) -> Unit,
    onEmoticonDoubleClick: ((EmoticonDetailModel) -> Unit)? = null,
    onDismissRequest: () -> Unit,
    viewModel: EmoticonBottomSheetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(
        initialPage = uiState.selectedTabIndex,
        pageCount = { uiState.emoticonSets.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val tabListState = rememberLazyListState()

    // 더블 클릭 감지를 위한 상태 (old 프로젝트와 동일)
    var lastClickedEmoticonId by remember { mutableIntStateOf(-1) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var firstClickTime by remember { mutableLongStateOf(0L) }
    var isDoubleClickReady by remember { mutableIntStateOf(0) } // 0: 초기, 1: 첫 클릭 후

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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = ColorPalette.background100,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(
                        color = ColorPalette.gray150,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
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
                        EmoticonBottomSheetTabItem(
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

                // 탭 아래 구분선
                HorizontalDivider(
                    color = ColorPalette.gray150,
                    thickness = 1.dp
                )

                // 이모티콘 그리드
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
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(emoticons) { emoticon ->
                                // CDN URL 기반으로 이모티콘 이미지 URL 생성
                                val emoticonUrl = viewModel.getEmoticonUrl(emoticon.id)
                                EmoticonBottomSheetItem(
                                    emoticon = emoticon,
                                    emoticonUrl = emoticonUrl,
                                    onClick = {
                                        val currentTime = SystemClock.elapsedRealtime()

                                        // old 프로젝트와 동일한 더블 클릭 로직
                                        if (lastClickedEmoticonId == emoticon.id) {
                                            // 같은 이모티콘 클릭
                                            lastClickTime = currentTime
                                            if (isDoubleClickReady == 1 && (lastClickTime - firstClickTime < 300)) {
                                                // 빠르게 두번 클릭 -> 바로 전송 후 닫기
                                                onEmoticonDoubleClick?.invoke(emoticon)
                                                onDismissRequest()
                                                isDoubleClickReady = 0
                                            } else {
                                                // 느리게 클릭 -> 첫 클릭으로 처리
                                                firstClickTime = currentTime
                                                onEmoticonSelected(emoticon)
                                                onDismissRequest()
                                                isDoubleClickReady = 1
                                            }
                                        } else {
                                            // 다른 이모티콘 클릭 -> 선택 후 닫기
                                            lastClickedEmoticonId = emoticon.id
                                            firstClickTime = currentTime
                                            onEmoticonSelected(emoticon)
                                            onDismissRequest()
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
private fun EmoticonBottomSheetTabItem(
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
private fun EmoticonBottomSheetItem(
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
