package net.ib.mn.presentation.overlay.photodetail

import android.os.Build
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import net.ib.mn.ui.components.ExoBottomSheet
import net.ib.mn.ui.components.ExoBottomSheetType
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.MediaCacheUtil
import java.text.NumberFormat
import java.util.Locale

/**
 * PhotoDetailScreen - 사진/미디어 상세 화면
 * @param article 게시글 모델 (미디어 파일 포함)
 * @param initialIndex 초기 페이지 인덱스
 * @param showShareButton 공유 버튼 표시 여부 (배경화면 등에서는 false)
 * @param onBackClick 뒤로가기 콜백
 */
@Composable
fun PhotoDetailScreen(
    article: ArticleModel,
    initialIndex: Int = 0,
    showShareButton: Boolean = true,
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
                // 공유 버튼 (showShareButton이 true일 때만 표시)
                if (showShareButton) {
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
                }

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

/**
 * TrendsModel (이붙그램) 상세 화면
 * old 프로젝트의 WideBannerFragment 참고
 * - X 버튼 (닫기)
 * - 다운로드 버튼 (이미지: 일반 아이콘, mp4: MP4 아이콘)
 * - 날짜 표시 (refDate 또는 createdAt)
 * - 사운드 버튼 없음
 */
@OptIn(UnstableApi::class)
@Composable
fun PhotoDetailScreen(
    trendsModel: net.ib.mn.domain.model.TrendsModel,
    onBackClick: () -> Unit = {},
    viewModel: PhotoDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val downloadState by viewModel.downloadState.collectAsState()

    BackHandler(onBack = onBackClick)

    val mediaUrl = trendsModel.bannerUrl ?: return
    val isVideo = trendsModel.isVideo

    // 날짜 표시 (refDate 우선, 없으면 createdAt을 로캘 형식으로 포맷)
    // old 프로젝트: DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(context))
    val dateString = remember(trendsModel) {
        if (!trendsModel.refDate.isNullOrEmpty()) {
            trendsModel.refDate!!
        } else if (!trendsModel.createdAt.isNullOrEmpty()) {
            // createdAt은 "2025-12-02T16:01:14" 형식이므로 파싱 후 로캘 형식으로 변환
            try {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val date = inputFormat.parse(trendsModel.createdAt!!)
                val outputFormat = java.text.DateFormat.getDateInstance(
                    java.text.DateFormat.MEDIUM,
                    net.ib.mn.util.LocaleUtil.getAppLocale(context)
                )
                date?.let { outputFormat.format(it) } ?: ""
            } catch (e: Exception) {
                trendsModel.createdAt ?: ""
            }
        } else {
            ""
        }
    }

    // 다운로드용 URL (mp4면 mp4, gif면 원본 gif)
    val downloadUrl = if (isVideo) {
        // mp4는 그대로 다운로드
        mediaUrl
    } else if (trendsModel.gifUrl != null) {
        // gif인 경우 원본 gif URL
        trendsModel.gifUrl
    } else {
        mediaUrl
    }

    // 다운로드 성공/실패 토스트
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 미디어 + 날짜 Column (중앙 정렬, wrap_content)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.End
        ) {
            // 미디어 표시
            if (isVideo) {
                VideoDetailPlayer(
                    videoUrl = mediaUrl.toSecureUrl(),
                    isVisible = true,
                    isSoundOn = false,  // Trends는 사운드 없음
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (mediaUrl.endsWith(".gif", ignoreCase = true) || trendsModel.gifUrl != null) {
                // GIF 표시
                GifDetailImage(
                    gifUrl = (trendsModel.gifUrl ?: mediaUrl).toSecureUrl(),
                    thumbnailUrl = trendsModel.imageUrl?.toSecureUrl(),
                    isVisible = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PhotoViewImage(
                    imageUrl = mediaUrl.toSecureUrl(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 날짜 표시 (미디어 바로 아래, 우측 정렬)
            if (dateString.isNotEmpty()) {
                Text(
                    text = dateString,
                    style = ExoTypo.body12,
                    color = ColorPalette.gray300,
                    modifier = Modifier.padding(end = 13.dp, top = 5.dp)
                )
            }
        }

        // 상단 바: X 버튼, 다운로드 버튼
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

            // 오른쪽: 다운로드 버튼 (mp4면 MP4 아이콘, 아니면 일반 다운로드 아이콘)
            val isDownloading = downloadState is DownloadState.Downloading
            if (!isDownloading) {
                Icon(
                    painter = painterResource(
                        if (isVideo) R.drawable.btn_img_download_mp4 else R.drawable.btn_img_download
                    ),
                    contentDescription = "Download",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            downloadUrl?.let { url ->
                                viewModel.downloadTrendsMedia(context, url)
                            }
                        },
                    tint = Color.Unspecified
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .padding(8.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * 하트박스 보상 다이얼로그
 *
 * old 프로젝트의 RewardBottomSheetDialogFragment.setWidePhotoRewardDialog()와 동일한 로직
 * VoteCompleteBottomSheet와 동일한 레이아웃 구조 사용
 *
 * @param heartBoxReward 하트박스 보상 데이터
 * @param showVideoAdButton 비디오 광고 버튼 표시 여부 (현재 광고 시청 가능 상태)
 * @param onDismiss 다이얼로그 닫기
 * @param onWatchVideoAd 비디오 광고 보기 클릭 (heart=0이고 button=true일 때만 호출됨)
 */
@Composable
private fun HeartBoxRewardDialog(
    heartBoxReward: HeartBoxReward,
    showVideoAdButton: Boolean = true,
    onDismiss: () -> Unit,
    onWatchVideoAd: () -> Unit = {}
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    val heart = heartBoxReward.heart
    val button = heartBoxReward.button

    // 이미지 리소스 결정 (old 프로젝트와 동일)
    val imageRes = when {
        heart == 0 -> R.drawable.img_popup_heartbox_0
        heart < 20 -> R.drawable.img_popup_heartbox_7
        heart < 100 -> R.drawable.img_popup_heartbox_20
        heart < 1000 -> R.drawable.img_popup_heartbox_100
        else -> R.drawable.img_popup_heartbox_1000
    }

    // heart=0이고 button=true이면 비디오 광고 유도
    val showVideoAdOption = heart == 0 && button && showVideoAdButton

    ExoBottomSheet(
        onDismissRequest = onDismiss,
        type = ExoBottomSheetType.DESIGN
    ) {
        // old: cl_reward_root - paddingTop 10dp, transparent background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            // 콘텐츠 영역 (배경 포함)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 109.dp)  // 이미지 중앙 (168dp/2) + 25dp 낮춤
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(ColorPalette.textWhiteBlack)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 이미지 나머지 절반 + 18dp margin - 25dp
                Spacer(modifier = Modifier.height(59.dp + 18.dp))

                // old: tv_reward - 22sp bold
                val titleText = if (heart == 0) {
                    stringResource(R.string.lable_receive_no_heart)
                } else {
                    stringResource(R.string.reward_heartbox)
                }

                Text(
                    text = titleText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPalette.textChat,
                    textAlign = TextAlign.Center
                )

                // old: cl_heart - 하트 개수 (heart > 0일 때만)
                if (heart > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // old: tv_plus - 24sp, main color
                        Text(
                            text = "+",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.main
                        )
                        // old: tv_heart - 24sp, main color
                        Text(
                            text = numberFormat.format(heart),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.main
                        )
                        // old: iv_heart - 21x17dp, marginStart 2dp
                        Spacer(modifier = Modifier.width(2.dp))
                        Image(
                            painter = painterResource(R.drawable.img_popup_heart),
                            contentDescription = null,
                            modifier = Modifier.size(21.dp, 17.dp)
                        )
                    }
                }

                // old: tv_reward_detail - 14sp, dimmed color
                // 1000개 이상일 때 또는 heart=0이고 비디오 광고 가능할 때
                val detailText = when {
                    heart >= 1000 -> stringResource(R.string.reward_heartbox1000_sub)
                    showVideoAdOption -> stringResource(R.string.label_see_video_for_heartbox)
                    else -> null
                }

                if (detailText != null) {
                    Text(
                        text = detailText,
                        fontSize = 14.sp,
                        color = ColorPalette.textDimmed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
                    )
                }

                // old: tv_confirm - 55dp height, marginTop 26dp, marginHorizontal 24dp, radius 28dp
                val confirmText = if (showVideoAdOption) {
                    stringResource(R.string.receive_more_heart_after_video_ads, "30")
                } else {
                    stringResource(R.string.confirm)
                }

                Button(
                    onClick = {
                        if (showVideoAdOption) {
                            onWatchVideoAd()
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 26.dp)
                        .height(55.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPalette.main
                    )
                ) {
                    Text(
                        text = confirmText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 취소 버튼 (비디오 광고 옵션일 때만 - old: tv_cancel)
                if (showVideoAdOption) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.quit),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.mainLight
                        )
                    }
                }

                // old: bottom view - 30dp height
                Spacer(modifier = Modifier.height(30.dp))
            }

            // old: img_review - width=0dp(match_parent), height=168dp
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Heart Box Reward",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .align(Alignment.TopCenter)
            )

            // old: btn_close - 62dp, padding 25dp (icon ~12dp), marginTop 10dp from bg top, marginEnd 10dp
            // bg top = 109dp, so absolute top = 109dp + 10dp = 119dp
            // heart=0이고 button=true일 때만 표시
            if (showVideoAdOption) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 119.dp, end = 10.dp)
                        .size(62.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.btn_popup_close),
                        contentDescription = "Close",
                        modifier = Modifier.padding(25.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }
}
