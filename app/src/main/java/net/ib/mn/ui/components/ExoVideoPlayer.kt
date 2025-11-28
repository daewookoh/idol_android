package net.ib.mn.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import net.ib.mn.util.MediaCacheUtil

/**
 * ExoVideoPlayer - 비디오/움짤(MP4) 재생 컴포넌트
 *
 * media3-ui-compose의 PlayerSurface 사용 - 컨트롤러 UI 없이 비디오만 표시
 *
 * @param videoUrl 재생할 비디오 URL
 * @param isVisible 화면에 보이는지 여부
 * @param modifier Modifier
 * @param isMuted 음소거 여부 (기본: true)
 * @param isLooping 반복 재생 여부 (기본: true)
 * @param onFirstFrameRendered 첫 프레임이 렌더링되면 호출되는 콜백 (깜빡임 방지용)
 */
@OptIn(UnstableApi::class)
@Composable
fun ExoVideoPlayer(
    videoUrl: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    isMuted: Boolean = true,
    isLooping: Boolean = true,
    onFirstFrameRendered: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val cacheDataSourceFactory = MediaCacheUtil.getCacheDataSourceFactory(context)
            val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))
            setMediaSource(mediaSource)
            repeatMode = if (isLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            volume = if (isMuted) 0f else 1f
            prepare()
        }
    }

    // 첫 프레임 렌더링 콜백 (Old의 onRenderedFirstFrame과 동일)
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                onFirstFrameRendered?.invoke()
            }
        })
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
            .resizeWithContentScale(ContentScale.Crop, presentationState.videoSizeDp)
    )
}
