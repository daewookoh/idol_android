package net.ib.mn.presentation.community.profile.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette

/**
 * ProfilePhotoPage - 프로필 사진 탭 (3열 그리드)
 */
@Composable
fun ProfilePhotoPage(
    userId: Int,
    isMine: Boolean = false,
    isFeedPrivate: Boolean = false,
    isBlocked: Boolean = false,
    blockStatusChecked: Boolean = true,
    viewModel: ProfilePhotoViewModel = hiltViewModel()
) {
    // 차단 상태 확인 전에는 로딩 표시
    if (!blockStatusChecked) {
        LoadingContent()
        return
    }

    // 차단된 사용자일 경우 바로 Blocked 상태 표시
    if (isBlocked) {
        BlockedContent()
        return
    }

    // 비공개 피드일 경우 API 호출 없이 바로 Private 상태 표시
    if (isFeedPrivate) {
        PrivateContent()
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(userId) {
        viewModel.loadPhotos(userId, isMine)
    }

    // 무한 스크롤
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= gridState.layoutInfo.totalItemsCount - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState is ProfilePhotoUiState.Success) {
            if ((uiState as ProfilePhotoUiState.Success).hasMore) {
                viewModel.loadMore()
            }
        }
    }

    when (val state = uiState) {
        is ProfilePhotoUiState.Loading -> LoadingContent()
        is ProfilePhotoUiState.Empty -> EmptyContent(stringResource(R.string.feed_no_posts))
        is ProfilePhotoUiState.Private -> PrivateContent()
        is ProfilePhotoUiState.Error -> EmptyContent(state.message)
        is ProfilePhotoUiState.Success -> PhotoGrid(state.photos, gridState)
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ColorPalette.main)
    }
}

@Composable
private fun EmptyContent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = ColorPalette.textGray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PrivateContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.icon_feed_lock),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.feed_private),
                color = ColorPalette.textGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BlockedContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.icon_feed_lock),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.user_blocked),
                color = ColorPalette.textGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<ProfilePhotoItem>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        state = gridState
    ) {
        items(items = photos, key = { it.id }) { photo ->
            PhotoItem(photo) {
                // TODO: 사진 상세 화면으로 이동
            }
        }
    }
}

@Composable
private fun PhotoItem(photo: ProfilePhotoItem, onClick: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(ColorPalette.gray100)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // GIF/MP4 아이콘 (old와 동일: 28x18dp, marginEnd=10dp, marginBottom=10dp)
        when {
            photo.isGif -> Icon(
                painter = painterResource(R.drawable.icon_gif),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .size(width = 28.dp, height = 18.dp),
                tint = Color.Unspecified
            )
            photo.isVideo -> Icon(
                painter = painterResource(R.drawable.icon_mp4),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .size(width = 28.dp, height = 18.dp),
                tint = Color.Unspecified
            )
        }
    }
}
