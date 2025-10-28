/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.viewholder

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import net.ib.mn.databinding.CommunityItemYoutubeBinding
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.YoutubeHelper


@UnstableApi
class CommunityArticleYoutubeViewHolder(
    val binding: CommunityItemYoutubeBinding,
    private val context: Context,
    private val lifecycle: Lifecycle?,
) : RecyclerView.ViewHolder(binding.root) {
    var url: String? = null

    fun bind(articleModel: ArticleModel, position: Int) = with(binding) {
        lifecycle?.addObserver(youtubePlayerView)
        setLink(articleModel)
    }

    private fun setLink(articleModel: ArticleModel) = with(binding) {
        if (articleModel.linkTitle.isNullOrEmpty() || articleModel.linkTitle == "None") {
            return@with
        }
        
        val linkUrl = articleModel.linkUrl ?: return@with
        val videoId = YoutubeHelper.extractYoutubeVideoId(linkUrl) ?: return
        val startTime = YoutubeHelper.extractYoutubeVideoStartTime(linkUrl)
        youtubePlayerView.getYouTubePlayerWhenReady(object: YouTubePlayerCallback {
            override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                val validStartTime = startTime.coerceAtLeast(0).toFloat()
                youTubePlayer.cueVideo(videoId, validStartTime)
            }
        })
        youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                super.onError(youTubePlayer, error)
                Log.e("CommunityArticleYoutubeViewHolder", "Error")
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                super.onStateChange(youTubePlayer, state)
                Log.d("CommunityArticleYoutubeViewHolder", "State changed: $state")
            }

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                Log.d("CommunityArticleYoutubeViewHolder", "YouTube Player is ready")
            }
        })
    }

}