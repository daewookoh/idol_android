/**
 * Copyright (C) 2023-2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 하나의 이미지/gif/비디오 일경우 보여주는 WidePhotoFragment
 *
 * */

package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.resource.gif.GifDrawable
import net.ib.mn.R
import net.ib.mn.databinding.FragmentWidePhotoBinding
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.RemoteFileModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.MediaExtension
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.common.util.appendVersion
import net.ib.mn.utils.ext.asyncPopBackStack
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.safeSetImageBitmap
import net.ib.mn.utils.safeSetImageDrawable
import androidx.core.view.isVisible
import net.ib.mn.utils.Logger

open class WidePhotoFragment : BaseWidePhotoFragment(),
    View.OnClickListener {

    private lateinit var rootLayoutView: View
    var isWideBanner: Boolean = false
    private var dX: Float = 0f
    private var dY: Float = 0f
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var centerWidth: Float = 0f
    private var centerHeight: Float = 0f
    private lateinit var glideRequestManager: RequestManager

    private var isSmallTalk = false

    lateinit var binding: FragmentWidePhotoBinding

    private var originalPlayerVolume = 1f
    private var isSoundChecked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (this.arguments == null) activity?.onBackPressed()
        isSmallTalk = this.requireArguments().getBoolean(PARAM_IS_SMALL_TALK)
        val model = baseWidePhotoViewModel.articleModel
        if (isSmallTalk) {
            model.imageUrl = model.files[0].originUrl
            model.thumbnailUrl = model.files[0].thumbnailUrl
            model.umjjalUrl = model.files[0].umjjalUrl
        }
        centerWidth = (resources.displayMetrics.widthPixels / 2).toFloat()
        centerHeight = (resources.displayMetrics.heightPixels / 2).toFloat()

        binding = FragmentWidePhotoBinding.inflate(inflater, container, false)
        rootLayoutView = binding.root

        return rootLayoutView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEdgeToEdge()
        init()
    }

    private fun init() {
        isSoundChecked = Util.getPreferenceBool(requireContext(), Const.IS_VIDEO_SOUND_ON, false)
        binding.ivSoundOnOff.isChecked = isSoundChecked

        getDataFromVM()
        photoOrGifStatusSetting()

        // 커뮤니티 게시물 목록으로 이동함. -> 다시 여기로 이동
        binding.btnHeartBox.visibility = if (Util.getPreferenceBool(
                activity,
                Const.PREF_HEART_BOX_VIEWABLE,
                false
            ) && !isAggregatingTime
        ) View.VISIBLE else View.INVISIBLE

        baseWidePhotoViewModel.addAdManagerView(context)

        with(binding) {
            btnClose.setOnClickListener(this@WidePhotoFragment)
            btnHeartBox.setOnClickListener(this@WidePhotoFragment)
            btnDownload.setOnClickListener(this@WidePhotoFragment)
            btnDownloadMp4.setOnClickListener(this@WidePhotoFragment)
            binding.photoView.setOnScaleChangeListener { scaleFactor, focusX, focusY ->
                // 이붙그램 확대하면 날짜 표시 숨김
                if (binding.photoView.scale < 1.0) {
                    tvDate.visibility = View.VISIBLE
                } else {
                    tvDate.visibility = View.GONE
                }
            }
            if (!baseWidePhotoViewModel.articleModel.files.isNullOrEmpty()) {
                btnShare.visibility = View.VISIBLE
                btnShare.setOnClickListener(this@WidePhotoFragment)
            } else {
                btnShare.visibility = View.GONE
            }
        }
    }

    private fun getDataFromVM() {
        baseWidePhotoViewModel.adManagerAdView.observe(
            this,
            SingleEventObserver { adManagerAdView ->
                binding.admobNativeAdContainer.addView(adManagerAdView)
            }
        )
    }

    private fun photoOrGifStatusSetting() {
        glideRequestManager = Glide.with(this)
        val article = baseWidePhotoViewModel.articleModel

        val attachedFile = if (article.files.isNullOrEmpty()) {
            RemoteFileModel(
                article.imageUrl
                    ?: article.thumbnailUrl,
                article.thumbnailUrl
                    ?: article.imageUrl,
                article.umjjalUrl
            )
        } else {
            article.files[0]
        }

        if (Const.FEATURE_VIDEO && !attachedFile.umjjalUrl.isNullOrEmpty()) {
            binding.photoView.visibility = View.GONE
            binding.pvWide.visibility = View.VISIBLE

            binding.pvWide.post {
                Util.showProgress(activity)
            }

            val isVideoMode =
                attachedFile.originUrl?.endsWith(MediaExtension.MP4.value) ?: false

            val videoUrl = if (isVideoMode) {
                binding.ivSoundOnOff.visibility = View.VISIBLE
                attachedFile.originUrl
            } else {
                binding.ivSoundOnOff.visibility = View.GONE
                attachedFile.umjjalUrl
            }

            val player = ExoPlayer.Builder(requireContext()).build()
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == PlaybackState.STATE_PLAYING) {
                        Util.closeProgress()
                        binding.pvWide.post {
                            updateDateView()
                        }
                    }
                }
            })

            if (isVideoMode) {

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build()

                player.setAudioAttributes(audioAttributes, true)

                binding.ivSoundOnOff.setOnCheckedChangeListener { _, isChecked ->
                    Util.setPreference(requireContext(), Const.IS_VIDEO_SOUND_ON, isChecked)
                    isSoundChecked = isChecked
                    if (isChecked) {
                        player.volume = originalPlayerVolume
                    } else {
                        player.volume = 0f
                    }
                }
            }

            val mediaItem = MediaItem.fromUri(videoUrl!!)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            player.repeatMode = Player.REPEAT_MODE_ALL

            binding.pvWide.player = player

            player.volume = if (!isSoundChecked) {
                0f
            } else {
                1f
            }

            if (isWideBanner) {
                binding.btnDownload.visibility = View.GONE
                binding.btnDownloadMp4.visibility = View.VISIBLE
            } else {
                binding.btnDownload.visibility = if (isVideoMode) View.GONE else View.VISIBLE
                binding.btnDownloadMp4.visibility = View.GONE
            }
        } else {
            binding.photoView.visibility = View.VISIBLE
            binding.pvWide.visibility = View.GONE

            binding.btnDownload.visibility = View.VISIBLE
            binding.btnDownloadMp4.visibility = View.GONE

            

            //사진 움직일 때
            @SuppressLint("ClickableViewAccessibility")
            if (!Util.isChinaBrand) {
                binding.photoView.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        if (binding.photoView.scale == 1.0f && event.pointerCount == 1) {

                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    dX = binding.rlPhotoArea.x - event.rawX
                                    dY = binding.rlPhotoArea.y - event.rawY
                                    startX = event.rawX
                                    startY = event.rawY
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    val diff =
                                        Util.getDistance(event.x, event.y, event.rawX, event.rawY)

                                    binding.rlPhotoArea.animate()
                                        .x(event.rawX + dX)
                                        .y(event.rawY + dY)
                                        .alpha(((centerWidth - diff) / centerWidth).toFloat())
                                        .setDuration(0)
                                        .start()
                                }

                                MotionEvent.ACTION_UP -> {
                                    val diff = Util.getDistance(startX, startY, event.rawX, event.rawY)

                                    if (diff < centerWidth * 0.7) {
                                        binding.rlPhotoArea.animate()
                                            .x(0f)
                                            .y(0f)
                                            .alpha(1f)
                                            .setDuration(300)
                                            .start()
                                    } else {
                                        binding.rlPhotoArea.animate()
                                            .alpha(0f)
                                            .withEndAction {
                                                if (activity?.supportFragmentManager != null && !activity!!.supportFragmentManager.isStateSaved) activity?.supportFragmentManager?.popBackStackImmediate()
                                                if (dialog != null && dialog!!.isShowing)
                                                    dismiss()
                                            }
                                            .setDuration(100)
                                            .start()
                                    }
                                }

                                else -> return false
                            }
                        } else {
                            binding.rlPhotoArea.animate()
                                .x(0f)
                                .y(0f)
                                .alpha(1f)
                                .setDuration(0)
                                .start()
                            return binding.photoView.attacher.onTouch(v, event)
                        }
                        return true
                    }
                })
            }

            val isGif = attachedFile.originUrl?.contains(".gif")
                ?: attachedFile.thumbnailUrl?.contains("gif") ?: false
            if (isGif) {
                glideRequestManager
                    .load(attachedFile.originUrl?.appendVersion(article.imageVer) ?: attachedFile.thumbnailUrl)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (isAdded) {
                                activity?.runOnUiThread {
                                    binding.photoView.safeSetImageDrawable(activity, resource)
                                    binding.photoView.post {
                                        updateDateView()
                                    }
                                }
                            }

                            return false
                        }
                    })
                    .submit()
            } else {
                glideRequestManager
                    .asBitmap()
                    .load(attachedFile.originUrl?.appendVersion(article.imageVer) ?: attachedFile.thumbnailUrl)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any,
                            target: Target<Bitmap>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (isAdded) {
                                activity?.runOnUiThread {
                                    // 사진 위치가 엉뚱한데 나오는 현상이 있어 ui thread 안으로 넣어줌
                                    binding.photoView.safeSetImageBitmap(activity, resource)
                                    binding.photoView.post {
                                        updateDateView()
                                    }
                                }
                            }

                            return false
                        }
                    })
                    .submit()
            }
        }
        if (isInquiry) {
            binding.btnDownload.visibility = View.GONE
            binding.btnDownloadMp4.visibility = View.GONE
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnHeartBox -> {
                binding.btnHeartBox.visibility = View.INVISIBLE
                Util.showProgress(activity)
                mHeartBoxSendHandler.sendEmptyMessageDelayed(0, 1000)
            }

            binding.btnDownloadMp4 -> {
                setUiActionFirebaseGoogleAnalyticsDialogFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_widephoto_download"
                )
                if (!Util.isSdPresent()) {
                    if (activity != null && isAdded)
                        Toast.makeText(
                            activity,
                            getString(R.string.msg_unable_use_download_1),
                            Toast.LENGTH_SHORT
                        ).show()
                    return
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29 이상: MediaStore 사용
                    baseWidePhotoViewModel.downloadImage(
                        requireContext(),
                        baseWidePhotoViewModel.getImageUrl(isGif = false) ?: ""
                    )
                } else {
                    baseWidePhotoViewModel.requestPermission(
                        this,
                        baseActivity,
                        targetSdkVersion,
                        isGifDownload = false
                    )
                }
                if (!baseWidePhotoViewModel.articleModel.id.isNullOrEmpty()) {
                    baseWidePhotoViewModel.downloadCount(
                        context,
                        baseWidePhotoViewModel.articleModel.id.toLong(),
                        1
                    )
                }
            }

            binding.btnClose -> {
                activity?.supportFragmentManager?.asyncPopBackStack {
                    if (dialog != null && dialog!!.isShowing)
                        dismiss()
                }
            }

            binding.btnDownload -> {
                setUiActionFirebaseGoogleAnalyticsDialogFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_widephoto_download"
                )
                if (!Util.isSdPresent()) {
                    if (activity != null && isAdded)
                        Toast.makeText(
                            activity,
                            getString(R.string.msg_unable_use_download_1),
                            Toast.LENGTH_SHORT
                        ).show()
                    return
                }

                val isGif = baseWidePhotoViewModel.isGifFromArticleModel()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29 이상: MediaStore 사용
                    baseWidePhotoViewModel.downloadImage(
                        requireContext(),
                        baseWidePhotoViewModel.getImageUrl(isGif = isGif) ?: "",
                    )
                } else {
                    baseWidePhotoViewModel.requestPermission(this, baseActivity, targetSdkVersion, isGifDownload = isGif)
                }
                if (!baseWidePhotoViewModel.articleModel.id.isNullOrEmpty()) {
                    baseWidePhotoViewModel.downloadCount(
                        context,
                        baseWidePhotoViewModel.articleModel.id.toLong(),
                        1
                    )
                }
            }

            binding.btnShare -> {
                if (isSmallTalk) {
                    setUiActionFirebaseGoogleAnalyticsDialogFragment(
                        GaAction.SHARE_SMALL_TALK.actionValue,
                        GaAction.SHARE_SMALL_TALK.label
                    )
                } else {
                    setUiActionFirebaseGoogleAnalyticsDialogFragment(
                        GaAction.COMMENT_ARTICLE_SHARE.actionValue,
                        GaAction.COMMENT_ARTICLE_SHARE.label
                    )
                }
                val url = LinkUtil.getAppLinkUrl(
                    context = context ?: return,
                    params = listOf(
                        LinkStatus.ARTICLES.status,
                        baseWidePhotoViewModel.articleModel.id.toString()
                    )
                )
                UtilK.linkStart(context = context, url = url)
            }
        }
    }

    override fun onDestroyView() {
        binding.admobNativeAdContainer.removeAllViews()
        super.onDestroyView()
    }

    private fun updateDateView() {
        try {
            if (binding.photoView.isVisible) {
                val actualHeight: Int
                val imageViewHeight = binding.photoView.height
                val imageViewWidth = binding.photoView.width
                val drawable = binding.photoView.drawable

                val bitmapHeight: Int
                val bitmapWidth: Int

                when (drawable) {
                    is BitmapDrawable -> {
                        bitmapHeight = drawable.bitmap.height
                        bitmapWidth = drawable.bitmap.width
                    }
                    is GifDrawable -> {
                        bitmapHeight = drawable.intrinsicHeight
                        bitmapWidth = drawable.intrinsicWidth
                    }
                    else -> {
                        // Or handle other drawable types if necessary
                        return
                    }
                }

                actualHeight = if (imageViewHeight * bitmapWidth <= imageViewWidth * bitmapHeight) {
                    imageViewHeight
                } else {
                    bitmapHeight * imageViewWidth / bitmapWidth
                }

                val viewHeight = binding.photoView.height
                val y =
                    viewHeight / 2 + actualHeight / 2 + Util.convertDpToPixel(activity, 13f).toInt()
                val lp = binding.tvDate.layoutParams as RelativeLayout.LayoutParams
                lp.topMargin = y
                binding.tvDate.layoutParams = lp
            } else {
                // 움짤 표시인 경우
                val viewHeight = binding.pvWide.height
                val y = binding.flPlayer.y.toInt() + viewHeight + Util.convertDpToPixel(activity, 13f)
                    .toInt()
                val lp = binding.tvDate.layoutParams as RelativeLayout.LayoutParams
                lp.topMargin = y
                binding.tvDate.layoutParams = lp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.pvWide.player?.release()
    }

    override fun onPause() {
        super.onPause()
        binding.pvWide.player?.pause()
    }

    private fun setupEdgeToEdge() {
        dialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.llRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.rlPhotoArea.setPadding(
                binding.rlPhotoArea.paddingLeft,
                systemBars.top,
                binding.rlPhotoArea.paddingRight,
                binding.rlPhotoArea.paddingBottom
            )

            (binding.admobNativeAdContainer.layoutParams as? RelativeLayout.LayoutParams)?.let { params ->
                params.bottomMargin = systemBars.bottom
                binding.admobNativeAdContainer.layoutParams = params
            }

            insets
        }
    }

    override fun onResume() {
        super.onResume()
        binding.pvWide.player?.play()

        if (!isSoundChecked) {
            return
        }
    }

    companion object {
        private const val PARAM_IS_SMALL_TALK = "param_is_small_talk"

        fun getInstance(model: ArticleModel, isSmallTalk: Boolean = false): WidePhotoFragment {
            val fragment = WidePhotoFragment()
            val args = Bundle()
            args.putSerializable(PARAM_MODEL, model)
            args.putBoolean(PARAM_IS_SMALL_TALK, isSmallTalk)
            fragment.arguments = args

            return fragment
        }

        fun getInstance(isInquiry: Boolean, model: ArticleModel): WidePhotoFragment {
            val fragment = WidePhotoFragment()
            val args = Bundle()
            args.putSerializable(PARAM_MODEL, model)
            args.putBoolean(PARAM_IS_INQUIRY, isInquiry)
            fragment.arguments = args

            return fragment
        }
    }

}
