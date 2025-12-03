package net.ib.mn.presentation.article

import android.os.Build
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.github.chrisbanes.photoview.PhotoView
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleFile
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.ad.AdaptiveBanner
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.MediaCacheUtil

/**
 * PhotoDetailScreen - 사진/미디어 상세 화면
 */
@Composable
fun PhotoDetailScreen(
    article: ArticleModel,
    initialIndex: Int = 0,
    onBackClick: () -> Unit = {},
    viewModel: PhotoDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val mediaFiles = article.mediaFiles

    val downloadState by viewModel.downloadState.collectAsState()
    val shouldShowBanner by viewModel.shouldShowBanner.collectAsState()
    val shouldShowHeartBox by viewModel.shouldShowHeartBox.collectAsState()
    val isHeartBoxLoading by viewModel.isHeartBoxLoading.collectAsState()

    // 하트박스 보상 다이얼로그 상태
    var heartBoxReward by remember { mutableStateOf<HeartBoxReward?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { event ->
            when (event) {
                ToastEvent.DownloadSuccess -> {
                    Toast.makeText(context, R.string.msg_save_ok, Toast.LENGTH_SHORT).show()
                }
                ToastEvent.DownloadError -> {
                    Toast.makeText(context, R.string.msg_unable_use_download_2, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 하트박스 보상 이벤트 수신
    LaunchedEffect(Unit) {
        viewModel.heartBoxRewardEvent.collect { reward ->
            heartBoxReward = reward
        }
    }

    // 하트박스 보상 다이얼로그
    heartBoxReward?.let { reward ->
        HeartBoxRewardDialog(
            heartBoxReward = reward,
            showVideoAdButton = true,  // TODO: 비디오 광고 시청 가능 여부 체크
            onDismiss = { heartBoxReward = null },
            onWatchVideoAd = {
                // TODO: 비디오 광고 화면으로 이동
            }
        )
    }

    val effectiveMediaFiles = if (mediaFiles.isEmpty() && !article.imageUrl.isNullOrEmpty()) {
        listOf(
            ArticleFile(
                originUrl = article.imageUrl,
                thumbnailUrl = article.thumbnailUrl ?: article.imageUrl,
                umjjalUrl = article.umjjalUrl
            )
        )
    } else {
        mediaFiles
    }

    if (effectiveMediaFiles.isEmpty()) {
        onBackClick()
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, effectiveMediaFiles.lastIndex),
        pageCount = { effectiveMediaFiles.size }
    )

    var isSoundOn by remember { mutableStateOf(false) }

    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { pageIndex ->
            val media = effectiveMediaFiles[pageIndex]
            val isCurrentPage = pagerState.currentPage == pageIndex

            MediaDetailItem(
                media = media,
                isVisible = isCurrentPage,
                isSoundOn = isSoundOn
            )
        }

        // 상단 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // 왼쪽: 닫기 버튼
            Icon(
                painter = painterResource(R.drawable.btn_img_closed),
                contentDescription = "Close",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBackClick() },
                tint = Color.Unspecified
            )

            // 중앙: 하트박스 Lottie 애니메이션 (old 프로젝트와 동일: heartBoxViewable && !isAggregatingTime)
            if (shouldShowHeartBox) {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset("heartbox.json"))

                if (isHeartBoxLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(40.dp)
                            .padding(4.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(80.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.onHeartBoxClick()
                            }
                    )
                }
            }

            // 오른쪽: 공유, 사운드, 다운로드 버튼
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.btn_img_share),
                    contentDescription = "Share",
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.shareArticle(context, article)
                        },
                    tint = Color.Unspecified
                )

                val currentMedia = effectiveMediaFiles.getOrNull(pagerState.currentPage)
                if (currentMedia?.isVideo == true) {
                    Icon(
                        painter = painterResource(
                            if (isSoundOn) R.drawable.btn_img_sound_on else R.drawable.btn_img_sound_off
                        ),
                        contentDescription = if (isSoundOn) "Sound On" else "Sound Off",
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isSoundOn = !isSoundOn },
                        tint = Color.Unspecified
                    )
                }

                val isDownloading = downloadState is DownloadState.Downloading
                if (!isDownloading) {
                    Icon(
                        painter = painterResource(R.drawable.btn_img_download),
                        contentDescription = "Download",
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val media = effectiveMediaFiles.getOrNull(pagerState.currentPage)
                                if (media != null) {
                                    viewModel.downloadMedia(
                                        context = context,
                                        media = media,
                                        articleId = article.id,
                                        mediaIndex = pagerState.currentPage
                                    )
                                }
                            },
                        tint = Color.Unspecified
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp).padding(8.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        if (effectiveMediaFiles.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 24.dp)
                    .background(
                        color = ColorPalette.textDefault.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(13.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${effectiveMediaFiles.size}",
                    style = ExoTypo.body14.copy(color = Color.White)
                )
            }
        }

        // 하단 배너 광고 (old 프로젝트와 동일: 중국 빌드가 아니고 데일리팩 미구독 시 표시)
        if (shouldShowBanner) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AdaptiveBanner(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 개별 미디어 상세 아이템
 */
@Composable
private fun MediaDetailItem(
    media: ArticleFile,
    isVisible: Boolean,
    isSoundOn: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            media.isVideo && isVisible -> {
                VideoDetailPlayer(
                    videoUrl = media.originUrl?.toSecureUrl() ?: media.umjjalUrl?.toSecureUrl() ?: "",
                    isVisible = isVisible,
                    isSoundOn = isSoundOn,
                    modifier = Modifier.fillMaxSize()
                )
            }
            media.isGif -> {
                GifDetailImage(
                    gifUrl = media.originUrl?.toSecureUrl() ?: media.umjjalUrl?.toSecureUrl() ?: "",
                    thumbnailUrl = media.thumbnailUrl?.toSecureUrl(),
                    isVisible = isVisible,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // PhotoView 사용 (Old 프로젝트와 동일한 자연스러운 줌)
                PhotoViewImage(
                    imageUrl = media.originUrl?.toSecureUrl() ?: media.displayUrl?.toSecureUrl() ?: "",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * PhotoView를 사용한 이미지 (핀치줌 지원)
 */
@Composable
private fun PhotoViewImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Coil로 이미지 로드
    LaunchedEffect(imageUrl) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
    }

    AndroidView(
        factory = { ctx ->
            PhotoView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { photoView ->
            bitmap?.let { photoView.setImageBitmap(it) }
        },
        modifier = modifier
    )
}

/**
 * 비디오 상세 플레이어
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoDetailPlayer(
    videoUrl: String,
    isVisible: Boolean,
    isSoundOn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val cacheDataSourceFactory = MediaCacheUtil.getCacheDataSourceFactory(context)
            val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(android.net.Uri.parse(videoUrl)))
            setMediaSource(mediaSource)
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
        }
    }

    LaunchedEffect(isSoundOn) {
        exoPlayer.volume = if (isSoundOn) 1f else 0f
    }

    LaunchedEffect(isVisible) {
        exoPlayer.playWhenReady = isVisible
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isVisible) exoPlayer.playWhenReady = true
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    val presentationState = rememberPresentationState(exoPlayer)

    PlayerSurface(
        player = exoPlayer,
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
        modifier = modifier
            .fillMaxSize()
            .resizeWithContentScale(ContentScale.Fit, presentationState.videoSizeDp)
    )
}

/**
 * GIF 상세 이미지
 */
@Composable
private fun GifDetailImage(
    gifUrl: String,
    thumbnailUrl: String?,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isVisible) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(gifUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = gifImageLoader,
                contentDescription = "GIF",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onState = { imageState = it }
            )
        }

        if (imageState !is AsyncImagePainter.State.Success && !thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
