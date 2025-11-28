package net.ib.mn.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import net.ib.mn.util.YoutubeHelper

@Composable
fun ExoYouTubePlayer(
    linkUrl: String,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoId = remember(linkUrl) { YoutubeHelper.extractVideoId(linkUrl) } ?: return
    val startTime = remember(linkUrl) { YoutubeHelper.extractStartTime(linkUrl) }

    AndroidView(
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                lifecycleOwner.lifecycle.addObserver(this)
                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, startTime.coerceAtLeast(0).toFloat())
                    }
                })
            }
        },
        modifier = modifier.fillMaxWidth(),
        onRelease = { it.release() }
    )
}
