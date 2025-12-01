package net.ib.mn.presentation.community.profile.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette

/**
 * ProfilePhotoPage - 프로필 사진 탭
 *
 * Old 프로젝트의 FeedPhotoFragment를 참고하여 Compose로 구현
 * 유저가 올린 사진이 포함된 게시글을 3열 그리드로 표시
 *
 * @param userId 유저 ID
 * @param viewModel ViewModel
 */
@Composable
fun ProfilePhotoPage(
    userId: Int,
    viewModel: ProfilePhotoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(userId) {
        viewModel.loadPhotos(userId)
    }

    when (val state = uiState) {
        is ProfilePhotoUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorPalette.main)
            }
        }

        is ProfilePhotoUiState.Empty -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.feed_no_posts),
                    color = ColorPalette.textGray,
                    fontSize = 14.sp
                )
            }
        }

        is ProfilePhotoUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = ColorPalette.textGray,
                    fontSize = 14.sp
                )
            }
        }

        is ProfilePhotoUiState.Success -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                state = gridState,
                contentPadding = PaddingValues(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(
                    items = state.photos,
                    key = { it.id }
                ) { photo ->
                    ProfilePhotoItem(
                        imageUrl = photo.thumbnailUrl,
                        onClick = {
                            // TODO: 사진 상세 화면으로 이동
                        }
                    )
                }
            }
        }
    }
}

/**
 * ProfilePhotoItem - 사진 그리드 아이템
 */
@Composable
private fun ProfilePhotoItem(
    imageUrl: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
