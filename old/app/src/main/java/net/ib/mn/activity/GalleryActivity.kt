package net.ib.mn.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.Target
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.GalleryAdapter
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.addon.InternetConnectivityManager.Companion.getInstance
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.databinding.ActivityGalleryBinding
import net.ib.mn.fragment.WideBannerFragment.Companion.getInstance
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.GalleryModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.CacheUtil.getCacheDataSourceFactory
import net.ib.mn.utils.Const
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class GalleryActivity : BaseActivity(), AbsListView.OnScrollListener {
    private var mMoreView: View? = null
    private var mAdapter: GalleryAdapter? = null
    private var mIdol: IdolModel? = null
    private var offset = 0
    private var lastitemVisibleFlag = false
    private var lastPosition = -1
    private var outPosition = -1
    private var viewTopPosition = 0
    private var viewVisibleItem = 0
    private var mGlideRequestManager: RequestManager? = null
    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl

    private lateinit var binding: ActivityGalleryBinding

    // 움짤프사 및 카테고리
    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!Const.USE_ANIMATED_PROFILE) {
                return
            }

            val index = intent.getIntExtra("index", 0)
            // 움짤 주소가 있을 때에만 처리
            try {
                if (index == 0 && playerView1 != null && hasVideo(playerView1)) {
                    (playerView1!!.parent as ViewGroup).findViewById<View>(R.id.photo1).visibility =
                        View.INVISIBLE
                    playerView1!!.visibility = View.VISIBLE
                } else if (index == 1 && playerView2 != null && hasVideo(playerView2)) {
                    (playerView2!!.parent as ViewGroup).findViewById<View>(R.id.photo2).visibility =
                        View.INVISIBLE
                    playerView2!!.visibility = View.VISIBLE
                } else if (index == 2 && playerView3 != null && hasVideo(playerView3)) {
                    (playerView3!!.parent as ViewGroup).findViewById<View>(R.id.photo3).visibility =
                        View.INVISIBLE
                    playerView3!!.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGalleryBinding.inflate(layoutInflater)
        binding.rlContainer.applySystemBarInsets()
        setContentView(binding.root)
        mGlideRequestManager = Glide.with(this)

        mIdol = intent.getSerializableExtra(CommunityActivity.PARAM_IDOL) as IdolModel?
        offset = 0

        mMoreView = LayoutInflater.from(this).inflate(R.layout.activity_gallery_more, null)

        val actionbar = supportActionBar
        //        mIdol.setLocalizedName(this);
        setCommunityTitle2(mIdol, getString(R.string.gallery_title))
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeButtonEnabled(false)

        mAdapter = GalleryAdapter(this, mGlideRequestManager)

        binding.list.apply {
            setOnItemClickListener(AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                // WidePhotoActivity 재사용을 위해
                val article = ArticleModel()
                val model = mAdapter!!.getItem(position) ?: return@OnItemClickListener
                if (model.bannerUrl != null) {
                    if (model.bannerUrl!!.endsWith(".mp4")) {
                        // mp4로 끝나면 움짤
                        val gifUrl = model.bannerUrl!!.replace("_m_mv.mp4", "_o_mv.gif")
                        article.umjjalUrl = model.bannerUrl
                        article.imageUrl = gifUrl
                    } else {
                        article.imageUrl = model.bannerUrl
                    }
                    article.createdAt = model.createdAt ?: Date()
                    article.refDate = model.refDate
                    getInstance(article)
                        .show(supportFragmentManager, "wide_photo")
                    getInstance(article).showDialogGuide(baseContext)
                    //                    startActivity(WideBannerActivity.createWideBannerIntent(GalleryActivity
//                    .this, article));
                }
            })
            setOnScrollListener(this@GalleryActivity)
            addFooterView(mMoreView)
            setAdapter(mAdapter)
        }
        mMoreView?.setOnClickListener(View.OnClickListener { v: View? ->
            offset += 30
            loadList()
        })

        binding.loadingView.setVisibility(View.VISIBLE)
        loadList()
    }

    public override fun onResume() {
        super.onResume()

        val filter = IntentFilter()

        filter.addAction(Const.REFRESH)

        if (Const.USE_ANIMATED_PROFILE) {
            filter.addAction(Const.PLAYER_START_RENDERING)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter)
    }

    public override fun onPause() {
        super.onPause()

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
        } catch (e: Exception) {
        }

        // 움짤 멈추기
        if (Const.USE_ANIMATED_PROFILE) {
            stopExoPlayer(playerView1)
            stopExoPlayer(playerView2)
            stopExoPlayer(playerView3)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            player1?.release()
            player2?.release()
            player3?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkVisibility(exoPlayerView: PlayerView?): Boolean {
        if (exoPlayerView != null && exoPlayerView.visibility == View.VISIBLE) {
            // videoview 가운데 부분의 화면상 위치 구하고
            val videoHeight = exoPlayerView.height
            val location = IntArray(2)
            exoPlayerView.getLocationInWindow(location)
            val y = location[1]

            // 리스트뷰의 화면상 위치 구해서
            binding.list.getLocationInWindow(location)
            val listviewTop = location[1]
            val listviewBottom = listviewTop + binding.list.height

            // 화면에 조금이라도 걸쳐져 있으면
            return !(y < listviewTop || y + videoHeight > listviewBottom)
        }
        return false
    }

    private fun autoPlay() {
        var pos = 0
        val tmp = viewTopPosition
        for (i in viewTopPosition until
            (if (viewTopPosition + viewVisibleItem > mAdapter!!.count)
                mAdapter!!.count - 1
            else
                viewTopPosition + viewVisibleItem
            )
        ) {
            val item = mAdapter!!.getItem(i)
            if (item == null || item.imageUrl == null || mAdapter!!.getItem(
                    i
                )?.imageUrl2 == null || item?.imageUrl3 == null
            ) {
                if (outPosition == i) {
                    pos++
                    continue
                }
                if (lastPosition != i) {
                    lastPosition = i
                    outPosition = lastPosition
                }
            } else {
                Util.log(
                    "viewTopPosition=" + i + " viewVisibleItem=" + viewVisibleItem
                            + " lastPosition=" + lastPosition + " outPosition=" + outPosition
                )
                if (item.imageUrl.endsWith("_s_mv.jpg")
                    || item.imageUrl2?.endsWith("_s_mv.jpg") == true
                    || item.imageUrl3.endsWith("_s_mv.jpg")
                ) {
                    Util.log(
                        "has image viewTopPosition=" + i + " viewVisibleItem=" + viewVisibleItem
                                + " lastPosition=" + lastPosition + " outPosition=" + outPosition
                    )
                    if (outPosition == i) {
                        pos++
                        continue
                    }
                    if (lastPosition != i) {
                        lastPosition = i
                        outPosition = lastPosition
                        val listPos = pos
                        val adapterPos = i
                        stopExoPlayer(playerView1)
                        stopExoPlayer(playerView2)
                        stopExoPlayer(playerView3)
                        Util.log("playing gogogo index $pos")
                        Handler().postDelayed({
                            var offset = 0
                            if (tmp != viewTopPosition) {
                                offset =
                                    if (tmp < viewTopPosition) viewTopPosition - tmp
                                    else tmp - viewTopPosition
                            }
                            if (binding.list.getChildAt(listPos - offset) != null) {
                                Util.log("playing gogogo run")
                                val item = mAdapter!!.getItem(adapterPos) ?: return@postDelayed
                                if (item.imageUrl?.endsWith("_s_mv.jpg") == true) {
                                    playExoPlayer(
                                        0,
                                        binding.list.getChildAt(listPos - offset)
                                            .findViewById(R.id.playerview1),
                                        binding.list.getChildAt(listPos - offset)
                                            .findViewById(R.id.photo1),
                                        item.imageUrl
                                    )
                                }
                                if (item.imageUrl2?.endsWith("_s_mv.jpg") == true) {
                                    playExoPlayer(
                                        1,
                                        binding.list.getChildAt(listPos - offset)
                                            .findViewById(R.id.playerview2),
                                        binding.list.getChildAt(listPos - offset)
                                            .findViewById(R.id.photo2),
                                        item.imageUrl2
                                    )
                                }
                                if (item.imageUrl3?.endsWith("_s_mv.jpg") == true) {
                                    playExoPlayer(
                                        2,
                                        binding.list.getChildAt(listPos - offset)
                                            .findViewById(R.id.playerview3),
                                        binding.list.getChildAt(listPos - offset)
                                            .findViewById(R.id.photo3),
                                        item.imageUrl3
                                    )
                                }
                            }
                        }, 200)
                    }
                    break
                }
            }

            pos++
        }
    }

    private fun loadList() {
        val listener: (JSONObject) -> Unit =  { response ->
            val gson = getInstance(true)
            val listType = object : TypeToken<List<GalleryModel?>?>() {
            }.type
            val idols = gson.fromJson<List<GalleryModel>>(
                response.optJSONArray("objects").toString(), listType
            )
            for (idol in idols) {
                mAdapter!!.add(idol)
            }
            if (offset + 30 >= response.optJSONObject("meta").optInt("total_count")) {
                binding.list.removeFooterView(mMoreView)
            }
            if (mAdapter!!.count > 0) {
                binding.list.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
                mAdapter!!.notifyDataSetChanged()
            } else {
                binding.list.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            }
            binding.loadingView.visibility = View.GONE
            Handler().postDelayed({ autoPlay() }, 100)
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            val msg = throwable.message
            var errorText = getString(R.string.error_abnormal_default)
            if (!TextUtils.isEmpty(msg)) {
                errorText += msg
            }
            makeText(
                this@GalleryActivity, errorText,
                Toast.LENGTH_SHORT
            ).show()
            if (mAdapter!!.count > 0) {
                binding.list.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
                mAdapter!!.notifyDataSetChanged()
            } else {
                binding.list.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            }
            binding.loadingView.visibility = View.GONE
        }

        MainScope().launch {
            trendsRepository.recent(
                mIdol!!.getId(),
                null,
                offset,
                listener,
                errorListener
            )
        }
    }

    @OptIn(UnstableApi::class)
    fun playExoPlayer(
        index: Int,
        view: PlayerView,
        thumbnailView: ImageView,
        url: String?
    ) {
        if (mContext == null) {
            return
        }

        if (!Const.USE_ANIMATED_PROFILE) {
            return
        }

        val isDataSavingMode = (Util.getPreferenceBool(this, Const.PREF_DATA_SAVING, false)
                && !getInstance(this).isWifiConnected)

        val player: ExoPlayer?
        if (index == 0) {
            if (player1 == null) {
                player1 = createExoPlayer(0)
            }
            player = this.player1
            player?.let {
                // 이전 재생되던거 멈추고
                player.stop()
                player.setVideoTextureView(TextureView(this))
                // 이전 플레이어 제거
                if (playerView1 != null) {
                    PlayerView.switchTargetView(player, playerView1, view)
                } else {
                    view.player = player
                }
                playerView1 = view
            }
        } else if (index == 1) {
            if (player2 == null) {
                player2 = createExoPlayer(1)
            }
            player = this.player2
            player?.let {
                // 이전 재생되던거 멈추고
                player.stop()
                player.setVideoTextureView(TextureView(this))
                // 이전 플레이어 제거
                if (playerView2 != null) {
                    PlayerView.switchTargetView(player, playerView2, view)
                } else {
                    view.player = player
                }
                playerView2 = view
            }
        } else {
            if (player3 == null) {
                player3 = createExoPlayer(2)
            }
            player = this.player3
            player?.let {
                // 이전 재생되던거 멈추고
                player.stop()
                player.setVideoTextureView(TextureView(this))
                // 이전 플레이어 제거
                if (playerView3 != null) {
                    PlayerView.switchTargetView(player, playerView3, view)
                } else {
                    view.player = player
                }
                playerView3 = view
            }
        }

        // 움짤 없으면
        if (url == null || !url.endsWith("_s_mv.jpg")) {
            view.visibility = View.INVISIBLE
            thumbnailView.visibility = View.VISIBLE
            return
        }

        // tag를 설정해서 동영상 있는지 여부 확인
        view.tag = url

        val urlVideo: String = if (isDataSavingMode
        ) url
        else url.replace("_s_mv.jpg", "_m_mv.mp4")

        // 스크롤 속도 문제로 GONE 해놓은걸 다시 VISIBLE로 바꿔서 재생 시작 이벤트 받게 처리
        view.visibility = View.VISIBLE
        thumbnailView.visibility = View.VISIBLE
        //
        // 검은색 셔터 화면 안나오게 처리
        val imageShutter: ImageView
//        val shutter = view.findViewById<View>(R.id.exo_shutter)
        val shutterId = resources.getIdentifier("exo_shutter", "id", packageName)
        val shutter = view.findViewById<View>(shutterId)

        if (shutter is ImageView) {
            imageShutter = shutter
        } else {
            val shutterParent = shutter.parent as FrameLayout
            shutterParent.removeView(shutter)
            imageShutter = ImageView(mContext)

            val lp2 = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            imageShutter.layoutParams = lp2
            imageShutter.id = shutterId //R.id.exo_shutter
            imageShutter.setBackgroundColor(-0x1)
            imageShutter.scaleType = ImageView.ScaleType.FIT_CENTER
            shutterParent.addView(imageShutter, 0)
        }

        mGlideRequestManager
            ?.asBitmap()
            ?.load(url)
            ?.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            ?.into(imageShutter)

        val _player = player
        view.post {
            if (mContext == null) {
                return@post
            }
            val url1 = urlVideo

            // ....._s_mv.jpg 가 있으면 이를  _m_mv.mp4로 변환
            val cacheDataSourceFactory = getCacheDataSourceFactory(
                mContext!!
            )
            val extractorsFactory: ExtractorsFactory = DefaultExtractorsFactory()

            val videoSource: MediaSource =
                ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(url1)))

            _player?.setMediaSource(videoSource)
            _player?.repeatMode = Player.REPEAT_MODE_ONE

            view.useController = false
            _player?.prepare()
            _player?.playWhenReady = true
            Util.log("playing $url1")
        }
    }

    fun stopExoPlayer(view: PlayerView?) {
        if (view == null) return

        val player = view.player as ExoPlayer?

        if (player == null) {
            Util.log("         stopExoPlayer player is NULL")
            return
        }

        // 썸네일 다시 보이기 및 리스너 제거해서 까만 화면 나오는 현상 제거
        val parent = view.parent as ViewGroup
        try {
            parent.findViewById<View>(R.id.photo1).visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            parent.findViewById<View>(R.id.photo2).visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            parent.findViewById<View>(R.id.photo3).visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 검은색으로 깜빡이는 현상 방지
        parent.post {
            player.playWhenReady = false
            // 스크롤할 때 느리지 않게
            view.visibility = View.GONE
        }
    }

    fun hasVideo(view: PlayerView?): Boolean {
        if (view!!.tag == null) return false

        val url = view.tag.toString()
        return url.endsWith("_s_mv.jpg")
    }

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
        //        if(lastitemVisibleFlag) {
//        }
        //6.5.5 패치반영

        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            Util.log(
                "--------------------------------------------------------------------------------------------------- auto play"
            )
            autoPlay()
        }
    }

    override fun onScroll(
        view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int,
        totalItemCount: Int
    ) {
        if (Build.VERSION.SDK_INT < Const.EXOPLAYER_MIN_SDK) {
            return
        }
        viewTopPosition = firstVisibleItem
        viewVisibleItem = visibleItemCount
        lastitemVisibleFlag =
            (totalItemCount > 0) && (firstVisibleItem + visibleItemCount >= totalItemCount)

        if (!((lastPosition >= firstVisibleItem)
                    && lastPosition < firstVisibleItem + visibleItemCount) && lastPosition != -1
        ) {
            Util.log(
                "--------------------------------------------------------------------------------------------------- screen is out0"
            )
            lastPosition = -1
        }

        if (!(checkVisibility(playerView1) || checkVisibility(playerView2) || checkVisibility(
                playerView3
            ))
        ) {
            Util.log(
                "--------------------------------------------------------------------------------------------------- screen is out1"
            )
            stopExoPlayer(playerView1)
            stopExoPlayer(playerView2)
            stopExoPlayer(playerView3)
            outPosition = lastPosition
        } else {
            Util.log(
                "--------------------------------------------------------------------------------------------------- screen is out2"
            )
            outPosition = -1
        }
    }

    companion object {
        fun createIntent(context: Context?, model: IdolModel?): Intent {
            val intent = Intent(context, GalleryActivity::class.java)
            intent.putExtra(CommunityActivity.PARAM_IDOL, model as Parcelable?)
            mContext = context
            return intent
        }

        private var mContext: Context? = null

        // 움짤 프사
        var playerView1: PlayerView? = null
        var playerView2: PlayerView? = null
        var playerView3: PlayerView? = null
    }
}
