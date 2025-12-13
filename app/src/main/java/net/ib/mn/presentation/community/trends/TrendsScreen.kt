package net.ib.mn.presentation.community.trends

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.R
import net.ib.mn.domain.model.TrendsModel
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl

/**
 * TrendsScreen - 이붙그램 화면
 */
@Composable
fun TrendsScreen(
    idolId: Int,
    idolName: String = "",
    onBackClick: () -> Unit = {},
    onItemClick: (TrendsModel) -> Unit = {},
    viewModel: TrendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 타이틀 생성: "이붙그램 - 아이돌이름" 형식
    // 아이돌이름_그룹명 형식일 경우 "솔로이름 그룹명"으로 표시
    val galleryTitle = stringResource(R.string.gallery_title)
    val displayName = if (idolName.contains("_")) {
        val parts = idolName.split("_")
        "${parts[0]} ${parts.getOrElse(1) { "" }}".trim()
    } else {
        idolName
    }
    val title = if (displayName.isNotEmpty()) {
        "$galleryTitle - $displayName"
    } else {
        galleryTitle
    }

    // 초기 로드
    LaunchedEffect(idolId) {
        viewModel.loadTrends(idolId)
    }

    BackHandler(onBack = onBackClick)

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = title,
                onNavigationClick = onBackClick
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ColorPalette.main
                    )
                }
                uiState.items.isEmpty() && !uiState.isLoading -> {
                    // Empty state
                    Text(
                        text = stringResource(R.string.no_data),
                        style = ExoTypo.body14,
                        color = ColorPalette.textDefaultOpacity60,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.refDate ?: it.hashCode() }
                        ) { trendsItem ->
                            TrendsRow(
                                item = trendsItem,
                                onItemClick = onItemClick
                            )
                        }

                        // 더보기 버튼 또는 로딩
                        if (uiState.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = ColorPalette.main,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        // 더보기 버튼 (아래 화살표)
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(ColorPalette.background100)
                                                .clickable { viewModel.loadNextPage() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "더보기",
                                                tint = ColorPalette.textDefault,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 이붙그램 3개 행 (1개 TrendsModel의 imageUrl, imageUrl2, imageUrl3 표시)
 * image1이 없으면 왼쪽 빈칸, image2가 없으면 가운데 빈칸, image3이 없으면 오른쪽 빈칸
 */
@Composable
private fun TrendsRow(
    item: TrendsModel,
    onItemClick: (TrendsModel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 왼쪽: imageUrl
        if (item.imageUrl != null) {
            TrendsImageItem(
                imageUrl = item.imageUrl,
                bannerUrl = item.bannerUrl,
                imageVer = item.imageVer,
                onClick = { onItemClick(item) },
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
        }

        // 가운데: imageUrl2
        if (item.imageUrl2 != null) {
            TrendsImageItem(
                imageUrl = item.imageUrl2,
                bannerUrl = item.bannerUrl,
                imageVer = item.imageVer,
                onClick = { onItemClick(item) },
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
        }

        // 오른쪽: imageUrl3
        if (item.imageUrl3 != null) {
            TrendsImageItem(
                imageUrl = item.imageUrl3,
                bannerUrl = item.bannerUrl,
                imageVer = item.imageVer,
                onClick = { onItemClick(item) },
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
        }
    }
}

/**
 * 이붙그램 이미지 아이템
 */
@Composable
private fun TrendsImageItem(
    imageUrl: String,
    bannerUrl: String?,
    imageVer: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isClickable = bannerUrl != null

    // old 프로젝트와 동일하게 imageVer 추가
    val displayUrl = if (imageVer > 0) {
        "${imageUrl.toSecureUrl()}?ver=$imageVer"
    } else {
        imageUrl.toSecureUrl()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(ColorPalette.background100)
            .then(
                if (isClickable) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(displayUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Gallery Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
