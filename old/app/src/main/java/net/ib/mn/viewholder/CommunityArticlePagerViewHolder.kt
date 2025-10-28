/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.viewholder

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.OptIn
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.LoopingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.databinding.CommunityVpItemBinding
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.CacheUtil
import net.ib.mn.utils.CacheUtil.getCacheDataSourceFactory
import net.ib.mn.utils.Const
import net.ib.mn.utils.MediaExtension
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.common.util.appendVersion
import net.ib.mn.utils.convertTimeMillsToTimerFormat
import net.ib.mn.utils.ext.getDuration
import net.ib.mn.utils.ext.safeActivity
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit


@UnstableApi
class CommunityArticlePagerViewHolder(
    val binding: CommunityVpItemBinding,
    private val context: Context,
    private val articlePhotoListener: ArticlePhotoListener?,
    private val lifecycleScope: LifecycleCoroutineScope,
) : RecyclerView.ViewHolder(binding.root), Player.Listener {

    private var glideRequestManager: RequestManager = Glide.with(context)

    private lateinit var bandwidthMeter: BandwidthMeter
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var loadControl: LoadControl
    private lateinit var renderersFactory: DefaultRenderersFactory
    private var player: ExoPlayer? = null
    private var cacheDataSource = CacheUtil.getCacheDataSourceFactory(context)

    private lateinit var extractorsFactory: ExtractorsFactory
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
    private lateinit var videoSource: MediaSource

    private var isVideoMode = false
    var url: String? = null

    // 타이머가 진행됬는지 여부 체크
    private var isTimerPlayed = false

    @OptIn(dagger.hilt.android.UnstableApi::class)
    fun bind(articleModel: ArticleModel, position: Int) = with(binding) {
        attachPhoto.apply {
            loadInfo = null
            setLoadInfo(R.id.TAG_LOAD_LARGE_IMAGE, false)
            setLoadInfo(R.id.TAG_IS_UMJJAL, false)
            setImageBitmap(null)
        }

        if (articleModel.isViewable == CommunityArticleViewHolder.BADGE_STATUS_OF_MANAGER) {
            attachPhoto.visibility = View.GONE
            llPreviewInfo.visibility = View.GONE
        }

        isVideoMode =
            articleModel.files[position].originUrl?.endsWith(MediaExtension.MP4.value) ?: false

        setVideo(articleModel, position, isVideoMode)
        setLink(articleModel)
        setOnClick(articleModel, position)
    }

    private fun setVideo(articleModel: ArticleModel, position: Int, isVideoMode: Boolean) = with(binding) {
        val isDataSavingMode = UtilK.dataSavingMode(context)
        val file = articleModel.files[position]
        if (Const.FEATURE_VIDEO && file.umjjalUrl != null) {

            setExoPlayerMedia()

            attachPhoto.setLoadInfo(R.id.TAG_IS_UMJJAL, true)
            attachFrame.visibility = View.VISIBLE

            // 가로세로 길이 고정
            val layoutParams = attachFrame.layoutParams as LinearLayoutCompat.LayoutParams
            layoutParams.height = Util.getDeviceWidth(context.safeActivity)
            attachFrame.layoutParams = layoutParams

            if (!isDataSavingMode) {
                // exoplayer requires android 4.1+
                attachExoplayerView.visibility = View.VISIBLE

                // video cache
                url = file.umjjalUrl

                // exoplayer requires android 4.1+
                videoSource = buildMediaSource(url)

                attachExoplayerView.useController = false
                attachExoplayerView.requestFocus()

                // item 재사용시 이전에 설정된 player 제거해야 문제 안생김
                attachExoplayerView.player = null

                setVideoPlayerShutter(articleModel)
                attachExoplayerView.tag = videoSource

                attachExoplayerView.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(p0: View) {
                        initExoPlayer()
                    }

                    override fun onViewDetachedFromWindow(p0: View) {
                        cleanUp()
                        removeListener()
                    }
                })
            }

            if (file.thumbnailUrl == null) {
                attachPhoto.setImageResource(R.drawable.image_placeholder)
                attachPhoto.visibility = View.GONE
                Util.log("--- set Thumbnail GONE")
                return@with
            }

            // show thumbnail
            attachPhoto.setImageBitmap(null)
            attachPhoto.setLoadInfo(R.id.TAG_THUMBNAIL_URL, file.thumbnailUrl!!)
            Util.log("--- set Thumbnail VISIBLE")

            setGifViewPosition(file.thumbnailUrl!!, isVideoMode)

            if (isVideoMode) {
                tvArticleTimer.setTimer(articleModel, position)

                attachExoplayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                if (UtilK.dataSavingMode(context)) {
                    ivMp4.visibility = View.VISIBLE
                }
            } else {
                attachExoplayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }

            return@with
        }

        viewGif.visibility = View.GONE

        val layoutParams = attachFrame.layoutParams as LinearLayoutCompat.LayoutParams
        layoutParams.height = LinearLayoutCompat.LayoutParams.WRAP_CONTENT
        attachFrame.layoutParams = layoutParams
        attachExoplayerView.visibility = View.GONE

        when {
            isVideoMode -> {
                if (file.thumbnailUrl == null) {
                    attachFrame.visibility = View.GONE
                    attachPhoto.setImageResource(R.drawable.image_placeholder)
                    attachPhoto.visibility = View.GONE
                    llPreviewInfo.visibility = View.GONE
                }
            }
            else -> {
                if (file.originUrl == null && file.thumbnailUrl == null) {
                    attachFrame.visibility = View.GONE
                    attachPhoto.setImageResource(R.drawable.image_placeholder)
                    attachPhoto.visibility = View.GONE
                    llPreviewInfo.visibility = View.GONE
                    return@with
                }
                if (!articleModel.linkTitle.isNullOrEmpty() && articleModel.linkTitle != "None") {
                    attachFrame.visibility = View.INVISIBLE
                    attachPhoto.visibility = View.INVISIBLE
                    return@with
                }
            }
        }

        attachFrame.visibility = View.VISIBLE
        attachPhoto.visibility = View.VISIBLE
        llPreviewInfo.visibility = View.GONE

        // lazy image loading
        attachPhoto.setLoadInfo(
            R.id.TAG_THUMBNAIL_URL,
            file.thumbnailUrl ?: "",
        )
        attachPhoto.setLoadInfo(
            R.id.TAG_LOAD_LARGE_IMAGE,
            java.lang.Boolean.FALSE,
        )

        glideRequestManager
            .asBitmap()
            .load(file.thumbnailUrl?.appendVersion(articleModel.imageVer))
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Bitmap>,
                    isFirstResource: Boolean,
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Bitmap>,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    context.safeActivity?.runOnUiThread {
                        // 네트워크가 느린 경우 뒤늦게 썸네일이 로드되는 경우 처리
                        val loadInfo = attachPhoto.getLoadInfo(R.id.TAG_THUMBNAIL_URL) as String?

                        if (loadInfo == null || loadInfo != file.thumbnailUrl) {
                            return@runOnUiThread
                        }
                        attachPhoto.setImageBitmap(resource)

                        // 데이터 절약 모드, 비디오 모드 일떄 (GIF가 아닐떄) image url 원본 보여줄 필요 없음.
                        if (isDataSavingMode || isVideoMode) {
                            return@runOnUiThread
                        }

                        // 썸네일 이미지 불러왔으니 원본 불러오게 세팅
                        attachPhoto.setLoadInfo(
                            R.id.TAG_LOAD_LARGE_IMAGE,
                            java.lang.Boolean.TRUE,
                        )
                    }
                    return false
                }
            })
            .submit()

        attachPhoto.loadInfo = file.originUrl?.appendVersion(articleModel.imageVer)
    }

    private fun setLink(articleModel: ArticleModel) = with(binding) {
        // 2025.4.11 링크 URL 제거함 (https://myloveidol.slack.com/archives/C4T9G0DB3/p1744340327008139?thread_ts=1744338693.142439&cid=C4T9G0DB3)

        if (articleModel.linkTitle.isNullOrEmpty() || articleModel.linkTitle == "None") {
            llPreviewInfo.visibility = View.GONE
            return@with
        }

        llPreviewInfo.visibility = View.VISIBLE
        attachPhoto.visibility = View.GONE
        tvPreviewTitle.text = articleModel.linkTitle

        // IMAGE
        if (!articleModel.imageUrl.isNullOrEmpty()) {
            ivPreviewImage.visibility = View.VISIBLE
            ivPreviewImage.let {
                glideRequestManager
                    .load(articleModel.imageUrl?.appendVersion(articleModel.imageVer))
                    .into(it)
            }
        } else {
            ivPreviewImage.visibility = View.GONE
        }

        // DESCRIPTION
        if (!articleModel.linkDesc.isNullOrEmpty() &&
            articleModel.linkDesc != "None" &&
            articleModel.linkDesc?.trim() != ""
        ) {
            tvPreviewDescription.text = articleModel.linkDesc
            tvPreviewDescription.visibility = View.VISIBLE
        } else {
            tvPreviewDescription.visibility = View.GONE
        }

        attachFrame.visibility = View.GONE
    }

    private fun buildMediaSource(
        url: String?
    ): MediaSource {
        val cacheDataSourceFactory = getCacheDataSourceFactory(context)
        val extractorsFactory: ExtractorsFactory = DefaultExtractorsFactory()

        val videoSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(url)))

        return videoSource
    }

    private fun setVideoPlayerShutter(articleModel: ArticleModel) = with(binding) {
        // 검은색 셔터 화면 안나오게 처리
        val imageShutter: ImageView
        val shutterId = context.resources.getIdentifier("exo_shutter", "id", context.packageName)
        val shutter = attachExoplayerView.findViewById<View>(shutterId)

        if (shutter is ImageView) {
            imageShutter = shutter
        } else {
            val shutterParent = shutter?.parent as FrameLayout
            shutterParent.removeView(shutter)
            imageShutter = ImageView(context)
            val lp2 = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            imageShutter.layoutParams = lp2
            imageShutter.id = shutterId
            imageShutter.setBackgroundColor(-0x1)
            imageShutter.scaleType = ImageView.ScaleType.FIT_CENTER
            shutterParent.addView(imageShutter, 0)
        }

        if (articleModel.thumbnailUrl != null) {
            glideRequestManager
                .load(articleModel.thumbnailUrl)
                .into(imageShutter)
        }

        Util.log(
            ">>>>> shutter visibility: " + shutter.visibility + " alpha:" +
                shutter.alpha,
        )
    }

    private fun setGifViewPosition(thumbNailUrl: String, isVideoMode: Boolean) = with(binding) {
        glideRequestManager.asBitmap()
            .load(thumbNailUrl)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Bitmap>,
                    isFirstResource: Boolean,
                ): Boolean {
                    viewGif.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Bitmap>,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    context.safeActivity?.runOnUiThread {
                        // 네트워크가 느린 경우 뒤늦게 썸네일이 로드되는 경우 처리
                        val loadInfo =
                            attachPhoto.getLoadInfo(R.id.TAG_THUMBNAIL_URL) as String?

                        if (loadInfo == null || loadInfo != thumbNailUrl) {
                            return@runOnUiThread
                        }

                        Util.log(
                            "******* show thumbnail : $thumbNailUrl tag=${
                                attachPhoto.getLoadInfo(
                                    R.id.TAG_THUMBNAIL_URL,
                                )
                            }",
                        )
                        attachPhoto.setImageBitmap(resource)

                        if (isVideoMode) {
                            return@runOnUiThread
                        }
                        viewGif.visibility = View.VISIBLE

                        attachPhoto.post {
                            // gif icon 표시
                            var width = resource.width
                            var height = resource.height
                            val ratio = height.toDouble() / width.toDouble()
                            val dispWidth =
                                attachPhoto.width
                            val dispHeight =
                                attachPhoto.height

                            if (ratio < 1.0) { // 가로로 긴 움짤
                                width = dispWidth
                                height = (dispHeight * ratio).toInt()
                            } else {
                                width = (dispWidth / ratio).toInt()
                                height = dispHeight
                            }

                            val layoutParamsGif =
                                viewGif.layoutParams as RelativeLayout.LayoutParams
                            layoutParamsGif.rightMargin =
                                (
                                    Util.convertDpToPixel(
                                        context,
                                        10f,
                                    ) + (dispWidth - width) / 2
                                    ).toInt()
                            layoutParamsGif.bottomMargin =
                                (
                                    Util.convertDpToPixel(
                                        context,
                                        10f,
                                    ) + (dispHeight - height) / 2
                                    ).toInt()
                            Util.log("lp rightMargin=${layoutParamsGif.rightMargin} bottomMargin=${layoutParamsGif.bottomMargin}")
                            viewGif.layoutParams = layoutParamsGif
                        }
                    }
                    return false
                }
            })
            .into(attachPhoto)
    }

    private fun setPlayTimer() {
        if (!isTimerPlayed) {
            return
        }

        binding.tvArticleTimer.visibility = View.VISIBLE

        Observable.interval(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                binding.tvArticleTimer.convertTimeMillsToTimerFormat(
                    (
                        player?.duration
                            ?: 0
                        ) - (player?.currentPosition ?: 0L),
                )
            }, {})
            .addTo(compositeDisposable ?: return)
    }


    override fun onRenderedFirstFrame() {
        binding.attachPhoto.visibility = View.GONE
        binding.attachExoplayerView.visibility = View.VISIBLE
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                if (!playWhenReady) {
                    cleanUp()
                    initExoPlayer()
                }
            }

            Player.STATE_READY -> {
                binding.attachExoplayerView.visibility = View.VISIBLE
                binding.attachPhoto.visibility = View.GONE
            }
        }
    }

    fun removeListener() {
        player?.removeListener(this)
    }

    private fun setOnClick(articleModel: ArticleModel, position: Int) {
        with(binding) {
            attachPhoto.setOnClickListener {
                articlePhotoListener?.widePhotoClick(articleModel, position)
            }

            attachButton.setOnClickListener {
                articlePhotoListener?.widePhotoClick(articleModel, position)
            }

            llPreviewInfo.setOnClickListener {
                articlePhotoListener?.linkClick(articleModel.linkUrl ?: return@setOnClickListener)
            }
        }
    }

    fun initExoPlayer(isReset: Boolean = false) {
        if (player != null) {
            Observable.timer(VIDEO_PLAYER_DELAY_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    try {
                        player?.prepare()
                        binding.attachExoplayerView.player?.playWhenReady = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.subscribe().addTo(compositeDisposable)


            return
        }

        if (!this::renderersFactory.isInitialized ||
            !this::bandwidthMeter.isInitialized ||
            !this::loadControl.isInitialized ||
            !this::trackSelector.isInitialized ||
            !this::videoSource.isInitialized
        ) {
            return
        }

        if (isReset) {
            setExoPlayerMedia()

            val cacheDataSourceFactory = getCacheDataSourceFactory(context)
            val extractorsFactory: ExtractorsFactory = DefaultExtractorsFactory()

            videoSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(url)))
        }

        player = ExoPlayer.Builder(context, renderersFactory)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()

        player?.setMediaSource(videoSource)
        player?.repeatMode = Player.REPEAT_MODE_ALL
        player?.volume = 0f
        player?.addListener(this)

        binding.attachExoplayerView.player = player

        Observable.timer(VIDEO_PLAYER_DELAY_TIME, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                try {
                    player?.prepare()
                    binding.attachExoplayerView.player?.playWhenReady = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.subscribe().addTo(compositeDisposable)
    }

    private fun setExoPlayerMedia() {
        // 1. Create a default TrackSelector
        bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        trackSelector = DefaultTrackSelector(context)
        loadControl = DefaultLoadControl()
        renderersFactory = DefaultRenderersFactory(context)

    }

    fun getPlayer(): ExoPlayer? {
        if (player == null) initExoPlayer()
        return player
    }

    fun cleanUp() {
        Util.log("************************* release exoplayer:$player")

        binding.attachPhoto.visibility = View.VISIBLE
        player?.clearVideoSurface()
        player?.stop()
        player?.release()
        binding.attachExoplayerView.player = null
        player = null
        compositeDisposable.clear()

    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        cleanUp()
        initExoPlayer(isReset = true)
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        super.onPlayerErrorChanged(error)
        cleanUp()
        initExoPlayer(isReset = true)
    }

    fun resumeVideoPlayer() {
        if (compositeDisposable.isDisposed) {
            compositeDisposable = CompositeDisposable()
        }

        if (player != null) {
            player?.play()
            return
        }

        initExoPlayer()
    }

    //플레이어  pause 시켜줌.
    fun pauseVideoPlayer() {
        compositeDisposable.dispose()
        player?.pause()
    }

    private fun AppCompatTextView.setTimer(articleModel: ArticleModel, position: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val totalDuration = articleModel.files[position].originUrl?.getDuration()
                withContext(Dispatchers.Main) {
                    this@setTimer.visibility = View.VISIBLE
                    this@setTimer.convertTimeMillsToTimerFormat(totalDuration ?: 0L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val VIDEO_PLAYER_DELAY_TIME = 100L
    }
}