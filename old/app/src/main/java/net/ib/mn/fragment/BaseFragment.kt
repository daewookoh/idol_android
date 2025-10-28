package net.ib.mn.fragment

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import androidx.annotation.ContentView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.disposables.CompositeDisposable
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.BaseActivity.OnBackPressedListener
import net.ib.mn.utils.CacheUtil.getCacheDataSourceFactory
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.dataSavingMode
import net.ib.mn.viewmodel.BaseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

open class BaseFragment : Fragment, OnBackPressedListener {

    private val baseViewModel: BaseViewModel by viewModels()

    var player1: ExoPlayer? = null
    var player2: ExoPlayer? = null
    var player3: ExoPlayer? = null

    var playerListener1: Player.Listener? = null
    var playerListener2: Player.Listener? = null
    var playerListener3: Player.Listener? = null

    // 움짤 프사
    @JvmField
    var playerView1: PlayerView? = null
    @JvmField
    var playerView2: PlayerView? = null
    @JvmField
    var playerView3: PlayerView? = null

    @JvmField
    var mapExpanded = HashMap<Int, Boolean>()

    var currentUrl: String? = null
    var firebaseAnalytics: FirebaseAnalytics? = null

    var fragIsVisible: Boolean = false // isVisible이 이미 kotlin에서 사용중이라 isVisible -> fragIsVisible

    var compositeDisposable: CompositeDisposable = CompositeDisposable()

    private var mVisible = false
    protected lateinit var mGlideRequestManager: RequestManager

    // attach 되기 전에 불리는 경우가 있어서 nullable 처리
    protected val baseActivity: BaseActivity?
        get() = (activity as BaseActivity?)

    protected fun showMessage(msg: String?) {
        if (isAdded) {
            baseActivity?.showMessage(msg)
        }
    }

    constructor() : super()

    @ContentView
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        val lastVisibility = mVisible
        mVisible = userVisibleHint
        if (mVisible != lastVisibility) {
            onVisibilityChanged(mVisible)
        }
    }

    override fun onPause() {
        super.onPause()
        val lastVisibility = mVisible
        mVisible = false
        if (mVisible != lastVisibility) {
            onVisibilityChanged(mVisible)
        }
    }

    override fun onBackPressed() {
        // TODO Auto-generated method stub
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        val lastVisibility = mVisible
        mVisible = isVisibleToUser
        if (mVisible != lastVisibility) {
            onVisibilityChanged(mVisible)
        }
    }

    protected open fun onVisibilityChanged(isVisible: Boolean) {
        this.fragIsVisible = isVisible

        if (!isVisible) {
            stopExoPlayer(playerView1)
            stopExoPlayer(playerView2)
            stopExoPlayer(playerView3)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this)
        setFirebaseGoogleAnalyticsFragment()
    }

    protected fun setFirebaseGoogleAnalyticsFragment() {
        if (BuildConfig.CHINA) {
            return
        }
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun setUiActionFirebaseGoogleAnalyticsFragment(action: String?, label: String?) {
        if (BuildConfig.CHINA) {
            return
        }
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity())

            val params = Bundle()
            params.putString(Const.ANALYTICS_GA_DEFAULT_ACTION_KEY, action)
            firebaseAnalytics!!.logEvent(label!!, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun setButtonPressFirebaseEvent(label: String?) {
        if (BuildConfig.CHINA) {
            return
        }
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity())

            val params = Bundle()
            params.putString(
                Const.ANALYTICS_GA_DEFAULT_ACTION_KEY,
                Const.ANALYTICS_BUTTON_PRESS_ACTION
            )
            firebaseAnalytics!!.logEvent(label!!, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // keyName : ui_action말고, 다른 값을 넣을 때 사용할 변수명, keyValue : keyName에 넣을 값
    protected fun setUiActionFirebaseGoogleAnalyticsFragmentWithKey(
        action: String?,
        label: String?,
        keyName: String?,
        keyValue: Int
    ) {
        if (BuildConfig.CHINA) {
            return
        }
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity())

            val params = Bundle()
            params.putString("ui_action", action)
            params.putInt(keyName, keyValue)
            firebaseAnalytics!!.logEvent(label!!, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // keyName : ui_action말고, 다른 값을 넣을 때 사용할 변수명, keyValue : keyName에 넣을 값
    protected fun setUiActionFirebaseGoogleAnalyticsFragmentWithKey(
        action: String?,
        label: String?,
        keyName: String?,
        keyValue: String?
    ) {
        if (BuildConfig.CHINA) {
            return
        }
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity())
            val params = Bundle()
            params.putString("ui_action", action)
            params.putString(keyName, keyValue)
            firebaseAnalytics!!.logEvent(label!!, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getViewByPosition(pos: Int, listView: ListView): View {
        val firstListItemPosition = listView.firstVisiblePosition
        val lastListItemPosition = firstListItemPosition + listView.childCount - 1

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.adapter.getView(pos, null, listView)
        } else {
            val childIndex = pos - firstListItemPosition
            return listView.getChildAt(childIndex)
        }
    }

    /**
     * udp broadcast의 ts를 보고 갱신 필요한지 여부
     *
     * @param ts : Unix timestamp (second)
     * @return
     */
    private fun isDayChanged(ts: Long): Boolean {
        if (ts == 0L) return false

        var result = false
        val date = Date(ts * 1000)
        val cal = Calendar.getInstance()
        val tz = TimeZone.getTimeZone("Asia/Seoul")
        /* date formatter in local timezone */
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = tz
        val serverTime = sdf.format(date)

        var lastTime: String?
        try {
            lastTime = Util.getPreference(activity, Const.PREF_SERVER_TIME)
        } catch (e: NullPointerException) {
            e.printStackTrace()
            lastTime = serverTime
        }
        if (serverTime != null && lastTime!!.length >= 10 && serverTime.length >= 10) {
            // T 로 잘라서 날짜만 비교
            val dayServer = serverTime.substring(0, 10)
            val dayPrev = lastTime.substring(0, 10)
            if (!dayServer.equals(dayPrev, ignoreCase = true)) {
                result = true
            }
        }

        Util.log("* serverTime $serverTime")
        Util.log("* lastTime $lastTime")

        Util.setPreference(activity, Const.PREF_SERVER_TIME, serverTime)
        return result
    }

    @UnstableApi
    fun createExoPlayer(index: Int): ExoPlayer {
        val player: ExoPlayer


        val bandwidthMeter: BandwidthMeter =
            DefaultBandwidthMeter.Builder(requireActivity()).build()
        val trackSelector: TrackSelector = DefaultTrackSelector(requireActivity())
        val loadControl: LoadControl = DefaultLoadControl()

        //      renderersFactory,
        val renderersFactory = DefaultRenderersFactory(requireActivity())
        player = ExoPlayer.Builder(requireActivity(), renderersFactory)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()

        player.volume = 0f
        val _player = player

        val listener = createPlayerListener(index)
        if (index == 0) {
            playerListener1 = listener
        } else if (index == 1) {
            playerListener2 = listener
        } else if (index == 2) {
            playerListener3 = listener
        }

        player.addListener(listener)

        return player
    }

    fun createPlayerListener(index: Int): Player.Listener {
        return object : Player.Listener {
            override fun onRenderedFirstFrame() {
                if (activity == null || !isAdded) {
                    return
                }

                Util.log("=== onRenderedFirstFrame $index")
                // 여기는 player view가 gone 상태면 안불린다.
                val intent = Intent(Const.PLAYER_START_RENDERING)
                intent.putExtra("index", index)
                LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
            }
        }
    }

    @UnstableApi
    fun playExoPlayer(index: Int, view: PlayerView?, thumbnailView: ImageView, url: String?, thumbnailUrl: String? = null) {
        Util.log("===== playExoPlayer $index $url")
        Util.log("     view=$view")

        if (baseActivity == null) {
            return
        }

        if (!Const.USE_ANIMATED_PROFILE) {
            return
        }

        val player: ExoPlayer?
        if (index == 0) {
            if (player1 == null) {
                player1 = createExoPlayer(0)
            }

            player = player1
            // 이전 재생되던거 멈추고
            player!!.stop()
            // 이전 플레이어 제거
            if (playerView1 != null) {
                PlayerView.switchTargetView(player, playerView1, view)
            } else {
                view?.player = player
            }
            //            player.removeVideoListener(((MainActivity)getBaseActivity()).playerListener1);
//            player.addVideoListener(((MainActivity)getBaseActivity()).playerListener1);
            playerView1 = view
        } else if (index == 1) {
            if (player2 == null) {
                player2 = createExoPlayer(1)
            }
            player = player2
            // 이전 재생되던거 멈추고
            player!!.stop()
            // 이전 플레이어 제거
            if (playerView2 != null) {
                PlayerView.switchTargetView(player, playerView2, view)
            } else {
                view?.player = player
            }
            //            player.removeVideoListener(((MainActivity)getBaseActivity()).playerListener2);
//            player.addVideoListener(((MainActivity)getBaseActivity()).playerListener2);
            playerView2 = view
        } else {
            if (player3 == null) {
                player3 = createExoPlayer(2)
            }
            player = player3
            // 이전 재생되던거 멈추고
            player!!.stop()
            // 이전 플레이어 제거
            if (playerView3 != null) {
                PlayerView.switchTargetView(player, playerView3, view)
            } else {
                view?.player = player
            }
            //            player.removeVideoListener(((MainActivity)getBaseActivity()).playerListener3);
//            player.addVideoListener(((MainActivity)getBaseActivity()).playerListener3);
            playerView3 = view
        }

        val isDataSavingMode = dataSavingMode(requireActivity())

        // 버저닝된 thumbnailUrl을 직접 받아서 처리하게 변경하여 아래는 막음
//        val thumbnailUrl: String? = if (url?.endsWith("mp4") == true)
//            url.replace("mp4", "webp")
//        else url

        // 움짤 없으면
        if (isDataSavingMode || url == null || view == null || (!url.contains(".mp4") && !url.contains("_s_mv.jpg"))) {
            if (view != null) {
                view.visibility = View.GONE
            }
            if (thumbnailUrl != null) {
                mGlideRequestManager
                    .load(thumbnailUrl) // 깜빡거림이 생겨 CustomTarget으로 처리
                    .into(object: CustomTarget<Drawable?>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable?>?
                        ) {
                            thumbnailView.setImageDrawable(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            thumbnailView.setImageDrawable(placeholder)
                        }
                    })
            }
            thumbnailView.visibility = View.VISIBLE
            return
        }

        // tag를 설정해서 동영상 있는지 여부 확인
        view.tag = url

        val urlVideo: String = if (url.contains(".mp4"))
            url
        else url.replace("_s_mv.jpg", "_m_mv.mp4")

        // 스크롤 속도 문제로 GONE 해놓은걸 다시 VISIBLE로 바꿔서 재생 시작 이벤트 받게 처리
        view.visibility = View.VISIBLE
        thumbnailView.visibility = View.VISIBLE

        val _player = player
        view.post(Runnable {
            if (baseActivity == null) return@Runnable
            val url1 = urlVideo

            // ....._s_mv.jpg 가 있으면 이를  _m_mv.mp4로 변환
            val cacheDataSourceFactory = getCacheDataSourceFactory(requireContext())
            val extractorsFactory: ExtractorsFactory = DefaultExtractorsFactory()

            val videoSource: MediaSource =
                ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(url1)))

            _player.setMediaSource(videoSource)
            _player.repeatMode = Player.REPEAT_MODE_ALL

            view.useController = false
            _player.prepare()
            _player.playWhenReady = true

            Util.log("playing $url1")
        })

        player.addListener(object: Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when(state) {
                    Player.STATE_READY -> {
                        thumbnailView.visibility = View.INVISIBLE
                    }
                }
            }
        })
    }

    fun stopExoPlayer(view: PlayerView?) {
        Util.log("===== stopExoPlayer view=$view")

        if (view == null) return

        val player = view.player as ExoPlayer?

        if (player == null) {
            Util.log("         stopExoPlayer player is NULL")
            return
        }

        if (baseActivity == null) return


        // 썸네일 다시 보이기 및 리스너 제거해서 까만 화면 나오는 현상 제거
        val parent = view.parent as ViewGroup
        try {
            if (parent.findViewById<View?>(R.id.photo1) != null) {
                parent.findViewById<View>(R.id.photo1).visibility = View.VISIBLE
            }
            //            player.removeVideoListener(((MainActivity)getBaseActivity()).playerListener1);
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (parent.findViewById<View?>(R.id.photo2) != null) {
                parent.findViewById<View>(R.id.photo2).visibility = View.VISIBLE
            }
            //            player.removeVideoListener(((MainActivity)getBaseActivity()).playerListener2);
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (parent.findViewById<View?>(R.id.photo3) != null) {
                parent.findViewById<View>(R.id.photo3).visibility = View.VISIBLE
            }
            //            player.removeVideoListener(((MainActivity)getBaseActivity()).playerListener3);
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 검은색으로 깜빡이는 현상 방지
        parent.post {
            player.playWhenReady = false
            player.release()

            try {
                player1 = null
                player2 = null
                player3 = null

                playerView1 = null
                playerView2 = null
                playerView3 = null
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            // 스크롤할 때 느리지 않게
            view.visibility = View.GONE
        }
    }

    fun startExoPlayer(view: PlayerView?) {
        Util.log("===== startExoPlayer")
        if (view == null) {
            Util.log("   view is null")
            return
        }
        val player = view.player

        if (player == null) {
            Util.log("   player is null")
            return
        }

        player.playWhenReady = true
    }

    fun hasVideo(view: PlayerView?): Boolean {
        if (view?.tag == null) return false
        val url = view.tag.toString()
        return (url.endsWith("_s_mv.jpg") || url.endsWith("mp4"))
    }

    fun sendFirebaseAnalyticsUDPEvent(bundle: Bundle) {
        if (lastEvent != null && lastEvent!!.getString("log") == bundle.getString("log")) {
            return
        }
        lastEvent = bundle
        firebaseAnalytics!!.logEvent(Const.ANALYTICS_EVENT_UDP, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.dispose()
    }

    companion object {
        var ARTICLE_BOTTOM_SHEET_TAG: String = "article"

        // send udp event
        // 같은 이벤트 중복으로 올리지 않게 처리
        var lastEvent: Bundle? = null
    }
}
