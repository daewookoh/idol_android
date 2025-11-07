package net.ib.mn.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import dagger.hilt.android.UnstableApi
import net.ib.mn.R

/**
 * ExoTop3 - 랭킹 리스트 상단 TOP3 배너 (IdolEntity 버전)
 *
 * @param idol 1위 아이돌 엔티티 (이미지/동영상 URL 포함)
 * @param isVisible 현재 화면에 표시 여부
 * @param onItemClick 아이템 클릭 콜백
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ExoTop3(
    idol: net.ib.mn.data.local.entity.IdolEntity,
    isVisible: Boolean = true,
    onItemClick: (Int) -> Unit = {}
) {
    ExoTop3Internal(
        id = "exo_top3_${idol.id}",
        imageUrls = net.ib.mn.util.IdolImageUtil.getTop3ImageUrls(idol),
        videoUrls = net.ib.mn.util.IdolImageUtil.getTop3VideoUrls(idol),
        isVisible = isVisible,
        onItemClick = onItemClick
    )
}

/**
 * ExoTop3 - RankingItemData 버전
 *
 * @param rankingItemData 랭킹 아이템 데이터
 * @param isVisible 현재 화면에 표시 여부
 * @param onItemClick 아이템 클릭 콜백
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ExoTop3(
    rankingItemData: net.ib.mn.ui.components.RankingItemData,
    isVisible: Boolean = true,
    onItemClick: (Int) -> Unit = {}
) {
    ExoTop3Internal(
        id = "exo_top3_${rankingItemData.id}",
        imageUrls = rankingItemData.top3ImageUrls,
        videoUrls = rankingItemData.top3VideoUrls,
        isVisible = isVisible,
        onItemClick = onItemClick
    )
}

/**
 * ExoTop3 - 랭킹 아이템 확장 시 표시용 (URL 직접 전달 버전)
 *
 * @param id 고유 ID
 * @param imageUrls 이미지 URL 리스트 (3개)
 * @param videoUrls 동영상 URL 리스트 (3개)
 * @param isVisible 현재 화면에 표시 여부
 * @param onItemClick 아이템 클릭 콜백
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ExoTop3(
    id: String,
    imageUrls: List<String?>,
    videoUrls: List<String?>,
    isVisible: Boolean = true,
    onItemClick: (Int) -> Unit = {}
) {
    ExoTop3Internal(
        id = id,
        imageUrls = imageUrls,
        videoUrls = videoUrls,
        isVisible = isVisible,
        onItemClick = onItemClick
    )
}

/**
 * ExoTop3 내부 구현
 *
 * Old 프로젝트의 ranking_header.xml 로직을 Compose로 구현
 *
 * 핵심 로직:
 * - 높이 = 너비 / 3 (정사각형의 1/3)
 * - 3개 이미지를 1/3씩 균등 분할
 * - Layer1: 스틸 이미지 (기본)
 * - Layer2: 움짤/동영상 (있는 경우만, 재생되면 스틸 숨김)
 * - 전역 관리: 한 화면에 여러 ExoTop3가 있을 때 최근 활성화된 것만 재생
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ExoTop3Internal(
    id: String,
    imageUrls: List<String?>,
    videoUrls: List<String?>,
    isVisible: Boolean = true,
    onItemClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val manager = LocalExoTop3Manager.current

    // 전역 활성 상태 확인
    val activePlayerId by manager.activePlayerId.collectAsState()
    val isActive = isVisible && activePlayerId == id

    // 화면에 보일 때 활성 플레이어로 등록
    DisposableEffect(id, isVisible) {
        if (isVisible) {
            manager.setActivePlayer(id)
        }

        onDispose {
            if (manager.isActive(id)) {
                manager.clearActivePlayer()
            }
        }
    }

    // 3개 이미지 보장
    val images = imageUrls.take(3).let { list ->
        list + List(3 - list.size) { null }
    }
    val videos = videoUrls.take(3).let { list ->
        list + List(3 - list.size) { null }
    }

    // 높이 = 너비 / 3 계산을 위한 BoxWithConstraints
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.gray100)
    ) {
        val screenWidth = with(LocalDensity.current) { maxWidth.toPx() }
        val itemWidth = screenWidth / 3
        val height = itemWidth // 정사각형이므로 높이 = 너비

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { height.toDp() })
        ) {
            // 3개 아이템 (각 1/3)
            for (index in 0..2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Layer1: 스틸 이미지
                    val stillImageUrl = images[index]
                    val videoUrl = videos[index]

                    var isVideoReady by remember { mutableStateOf(false) }

                    // 스틸 이미지 (최적화: 정확한 크기로 리사이징)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(stillImageUrl)
                            .size(itemWidth.toInt(), height.toInt())  // 정확한 크기로 다운샘플링
                            .scale(Scale.FILL)
                            .crossfade(true)
                            .crossfade(200)
                            .build(),
                        contentDescription = "Top ${index + 1} 이미지",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(getDefaultImageRes(index)),
                        placeholder = androidx.compose.ui.res.painterResource(getDefaultImageRes(index))
                    )

                    // Layer2: 동영상 (있는 경우만)
                    if (videoUrl != null) {
                        // .mp4 또는 _s_mv.jpg → _m_mv.mp4 변환
                        val actualVideoUrl = remember(videoUrl) {
                            when {
                                videoUrl.contains(".mp4") -> videoUrl
                                videoUrl.contains("_s_mv.jpg") -> videoUrl.replace("_s_mv.jpg", "_m_mv.mp4")
                                else -> null
                            }
                        }

                        if (actualVideoUrl != null) {
                            val exoPlayer = remember(actualVideoUrl) {
                                ExoPlayer.Builder(context)
                                    // 메모리 최적화: 작은 버퍼 사이즈 사용
                                    .setLoadControl(
                                        androidx.media3.exoplayer.DefaultLoadControl.Builder()
                                            .setBufferDurationsMs(
                                                15000,  // minBufferMs: 15초
                                                30000,  // maxBufferMs: 30초
                                                1000,   // bufferForPlaybackMs: 1초
                                                2000    // bufferForPlaybackAfterRebufferMs: 2초
                                            )
                                            .build()
                                    )
                                    .build().apply {
                                        repeatMode = Player.REPEAT_MODE_ALL
                                        // 비디오만 재생 (오디오 트랙 비활성화로 메모리 절약)
                                        volume = 0f
                                        setMediaItem(MediaItem.fromUri(actualVideoUrl))
                                        prepare()

                                        addListener(object : Player.Listener {
                                            override fun onPlaybackStateChanged(playbackState: Int) {
                                                if (playbackState == Player.STATE_READY) {
                                                    isVideoReady = true
                                                }
                                            }
                                        })
                                    }
                            }

                            // isActive 상태에 따라 재생/정지 제어
                            LaunchedEffect(isActive) {
                                exoPlayer.playWhenReady = isActive
                            }

                            DisposableEffect(Unit) {
                                onDispose {
                                    exoPlayer.release()
                                }
                            }

                            // PlayerView
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 기본 이미지 리소스 ID 반환 (이미지가 없을 때)
 * Old 프로젝트의 Util.noProfileImage() 로직
 */
@Composable
private fun getDefaultImageRes(index: Int): Int {
    return if (index % 2 == 0) {
        R.drawable.menu_profile_2
    } else {
        R.drawable.menu_profile_1
    }
}
