package net.ib.mn.activity

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ContentView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.ActionBarContainer
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
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
import com.exodus.bridge.SharedBridgeManager.Companion.clearData
import com.exodus.bridge.SharedBridgeManager.Companion.sharedData
import com.exodus.bridge.SharedData
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.activity.AuthActivity
import net.ib.mn.activity.PushStartActivity
import net.ib.mn.activity.StartupActivity
import net.ib.mn.chatting.SocketManager
import net.ib.mn.chatting.SocketManager.Companion.getInstance
import net.ib.mn.chatting.chatDb.ChatDB
import net.ib.mn.chatting.chatDb.ChatMembersList
import net.ib.mn.chatting.chatDb.ChatMessageList
import net.ib.mn.chatting.chatDb.ChatRoomInfoList
import net.ib.mn.chatting.chatDb.ChatRoomList
import net.ib.mn.databinding.DialogLevelUpBinding
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment.Companion.newInstance
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.CacheUtil.getCacheDataSourceFactory
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.LocaleUtil.setLocale
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Logger.Companion.w
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.dataSavingMode
import net.ib.mn.utils.UtilK.Companion.restartApplication
import net.ib.mn.utils.UtilK.Companion.setName
import net.ib.mn.utils.modelToString
import java.io.File
import java.io.IOException
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat

open class BaseActivity : AppCompatActivity {
    // 움짤
    var player1: ExoPlayer? = null
    var player2: ExoPlayer? = null
    var player3: ExoPlayer? = null

    var playerListener1: Player.Listener? = null
    var playerListener2: Player.Listener? = null
    var playerListener3: Player.Listener? = null

    var firebaseAnalytics: FirebaseAnalytics? = null

    protected var mTempFileForCrop: File? = null

    var socketManager: SocketManager? = null

    private var mGlideRequestManager: RequestManager? = null

    private val liveData = sharedData

    interface OnBackPressedListener {
        fun onBackPressed()
    }

    constructor() : super()

    private var dataObserver: Observer<SharedData>? = null

    @ContentView
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)


    //    final VunglePub vunglePub = VunglePub.getInstance();
    var isAlive: Boolean = false
        private set

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Const.ARTICLE_SERVICE_UPLOAD == intent.action) {
                val reward = intent.getIntExtra("reward_heart", 0)

                if (reward <= 0) {
                    return
                }

                showBottomDialog(reward)
            }
        }
    }

    private fun showBottomDialog(heart: Int) {
        val mBottomSheetDialogFragment =
            newInstance(RewardBottomSheetDialogFragment.FLAG_ARTICLE_WRITE, heart) {}
        val tag = "reward_article_write"
        val oldTag = supportFragmentManager.findFragmentByTag(tag)
        if (oldTag == null) {
            mBottomSheetDialogFragment.show(supportFragmentManager, tag)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        dataObserver = Observer<SharedData> { sharedData: SharedData? ->
            if (sharedData != null) {
                val gcode = sharedData.gcode
                val mcode = sharedData.mcode
                val msg = sharedData.msg
                w("Activity=" + javaClass.simpleName)
                w(sharedData.toString())
                if (gcode == ErrorControl.ERROR_88888) {
                    val isMaintenance = mcode == 1

                    // 중복으로 뜨지 않게 data 지워줌
                    clearData()

                    Util.showDefaultIdolDialogWithBtn1(
                        this@BaseActivity,
                        null,
                        msg,
                        if (isMaintenance) R.drawable.img_maintenance else 0
                    ) { v: View? ->
                        if (this@BaseActivity.javaClass == PushStartActivity::class.java) {
                            restartApplication(this@BaseActivity)
                        }
                        Util.closeIdolDialog()
                        if (isMaintenance) {
                            this@BaseActivity.finishAffinity()
                        }
                    }
                }
            }
        }

        //웹뷰 디렉토리  맞춤.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName(this)
            val packageName = this.packageName
            if (packageName != processName) {
                try {
                    WebView.setDataDirectorySuffix(processName!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 앱 언어 설정
        setLocale(this)
        mGlideRequestManager = Glide.with(this)

        val actionbar = supportActionBar
        if (actionbar != null) {
            actionbar.elevation = 0f
            actionbar.setDisplayUseLogoEnabled(true)
            actionbar.setLogo(if (BuildConfig.CELEB) R.drawable.ic_shadow else R.drawable.header_logo)
            actionbar.setBackgroundDrawable(
                ContextCompat.getColor(
                    this, R.color.navigation_bar
                ).toDrawable()
            )
            if (!isTaskRoot && this !is MainActivity) {
                actionbar.setDisplayHomeAsUpEnabled(true)
                actionbar.setHomeButtonEnabled(true)
            }

            // back 버튼과 타이틀 사이 패딩이 v24부터 16dp가 추가되어 이를 강제로 0으로 설정. 여러가지 방법을 찾아보았으나 먹히지않아 이렇게 처리.
            try {
                val f = actionbar.javaClass.getDeclaredField("mContainerView")
                f.isAccessible = true
                val container = f[actionbar] as ActionBarContainer

                val f2 = container.javaClass.getDeclaredField("mActionBarView")
                f2.isAccessible = true
                val toolbar = f2[container] as Toolbar

                toolbar.contentInsetStartWithNavigation = 0
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        if (!BuildConfig.DEBUG) {
            setFirebaseGoogleAnalyticsActivity()
        }

        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor =
            ContextCompat.getColor(this, R.color.navigation_bar)

        setDarkMode()
    }

    private fun getProcessName(context: Context?): String? {
        if (context == null) return null
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (manager.runningAppProcesses != null) {
            for (processInfo in manager.runningAppProcesses) {
                if (processInfo.pid == Process.myPid()) {
                    return processInfo.processName
                }
            }
        }
        return null
    }

    // 움짤
    @UnstableApi
    fun createExoPlayer(index: Int): ExoPlayer {
        val player: ExoPlayer


        val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter.Builder(this).build()
        val trackSelector: TrackSelector = DefaultTrackSelector(this)
        val loadControl: LoadControl = DefaultLoadControl()

        //      renderersFactory,
        val renderersFactory = DefaultRenderersFactory(this)
        player = ExoPlayer.Builder(this, renderersFactory)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()

        player.volume = 0f

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
                Util.log("=== onRenderedFirstFrame $index")
                // 여기는 player view가 gone 상태면 안불린다.
                val intent = Intent(Const.PLAYER_START_RENDERING)
                intent.putExtra("index", index)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
        }
    }

    //galleryActivity, NewCommentActivity 등 각 Activity에 생성되어있는 playExoPlayer 통합해야함
    @UnstableApi
    fun basePlayExoPlayer(index: Int, view: PlayerView?, thumbnailView: ImageView, url: String?) {
        Util.log("===== playExoPlayer $index $url")
        Util.log("     view=$view")

        if (!Const.USE_ANIMATED_PROFILE) {
            return
        }

        val player: ExoPlayer
        if (index == 0) {
            if (player1 == null) {
                player1 = createExoPlayer(0)
            }

            player = player1!!
            // 이전 재생되던거 멈추고
            player.stop()
            // 이전 플레이어 제거
            if (playerView1 != null) {
                PlayerView.switchTargetView(player, playerView1, view)
            } else {
                view!!.player = player
            }
            playerView1 = view
        } else if (index == 1) {
            if (player2 == null) {
                player2 = createExoPlayer(1)
            }
            player = player2!!
            // 이전 재생되던거 멈추고
            player.stop()
            // 이전 플레이어 제거
            if (playerView2 != null) {
                PlayerView.switchTargetView(player, playerView2, view)
            } else {
                view!!.player = player
            }
            playerView2 = view
        } else {
            if (player3 == null) {
                player3 = createExoPlayer(2)
            }
            player = player3!!
            // 이전 재생되던거 멈추고
            player.stop()
            // 이전 플레이어 제거
            if (playerView3 != null) {
                PlayerView.switchTargetView(player, playerView3, view)
            } else {
                view!!.player = player
            }
            playerView3 = view
        }

        val isDataSavingMode = dataSavingMode(this)

        // 움짤 없으면
        if (isDataSavingMode
            || url == null || view == null || (!url.contains(".mp4") && !url.contains("_s_mv.jpg"))
        ) {
            if (view != null) {
                view.visibility = View.GONE
            }
            if (url != null) {
                val thumbnailUrl = if (url.contains(".mp4"))
                    url.replace("mp4", "webp")
                else
                    url
                mGlideRequestManager
                    ?.load(thumbnailUrl) // 깜빡거림이 생겨 아래 막음
                    //                        .disallowHardwareConfig()
                    //                        .dontAnimate()
                    //                        .dontTransform()
                    // 이놈이 crash 주범
                    //                        .placeholder(thumbnailView.getDrawable())
                    ?.into(thumbnailView)
            }
            thumbnailView.visibility = View.VISIBLE
            return
        }

        // tag를 설정해서 동영상 있는지 여부 확인
        view.tag = url

        val urlVideo = if (url.contains(".mp4"))
            url
        else
            url.replace("_s_mv.jpg", "_m_mv.mp4")

        // 스크롤 속도 문제로 GONE 해놓은걸 다시 VISIBLE로 바꿔서 재생 시작 이벤트 받게 처리
        view.visibility = View.VISIBLE
        thumbnailView.visibility = View.VISIBLE

        // 검은색 셔터 화면 안나오게 처리
        val imageShutter: ImageView
        val shutter = view.findViewById<View>(R.id.exo_shutter)
        if (shutter is ImageView) {
            imageShutter = shutter
        } else {
            val shutterParent = shutter.parent as FrameLayout
            shutterParent.removeView(shutter)
            imageShutter = ImageView(this)

            val lp2 = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            imageShutter.layoutParams = lp2
            imageShutter.id = R.id.exo_shutter
            imageShutter.setBackgroundColor(-0x1)
            //            imageShutter.setScaleType(ImageView.ScaleType.FIT_CENTER);
            shutterParent.addView(imageShutter, 0)
        }

        mGlideRequestManager
            ?.load(url)
            ?.disallowHardwareConfig()
            ?.dontAnimate()
            ?.dontTransform()
            ?.placeholder(R.drawable.bg_loading)
            ?.into(imageShutter)

        val _player = player
        view.post {
            val url1 = urlVideo
            // ....._s_mv.jpg 가 있으면 이를  _m_mv.mp4로 변환
            val cacheDataSourceFactory = getCacheDataSourceFactory(this)
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
        }
    }

    fun baseStopExoPlayer(view: PlayerView?) {
        Util.log("===== stopExoPlayer view=$view")

        if (view == null) return

        val player = view.player as ExoPlayer?

        if (player == null) {
            Util.log("         stopExoPlayer player is NULL")
            return
        }

        // 썸네일 다시 보이기 및 리스너 제거해서 까만 화면 나오는 현상 제거
        val parent = view.parent as ViewGroup
        try {
            if (parent.findViewById<View?>(R.id.photo1) != null) {
                parent.findViewById<View>(R.id.photo1).visibility =
                    View.VISIBLE
            }
            //            player.removeVideoListener(((MainActivity)getBaseActivity()).playerListener1);
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (parent.findViewById<View?>(R.id.photo2) != null) {
                parent.findViewById<View>(R.id.photo2).visibility =
                    View.VISIBLE
            }
            //            player.removeVideoListener(((MainActivity)getBaseActivity()).playerListener2);
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (parent.findViewById<View?>(R.id.photo3) != null) {
                parent.findViewById<View>(R.id.photo3).visibility =
                    View.VISIBLE
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        v("CurrentLocale::" + newConfig.modelToString())
    }

    protected fun restartActivity() {
        val intent = intent
        finish()
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        //		AdColony.pause();
//        vunglePub.onPause();
        val liveData = sharedData
        liveData.removeObserver(dataObserver!!)

        Util.log("idoltalk::BaseActivity onPause")

        //백그라운드처리 모든화면에서 가능하게.
//        try {
        Const.CHATTING_IS_PAUSE = true


        //            if (socketManager?.getSocket() != null && socketManager?.isSocketConnected()) {
//                incrementSequenceNumber();
//                socketManager?.getSocket().emit(Const.CHAT_CHAANGE_STATE,
//                        new JSONObject()
//                                .put("cmd", Const.CHAT_CHAANGE_STATE)
//                                .put("seq", socketManager?.getSquenceNum())
//                                .put("state", "BACKGROUND"));
//                Util.log("idoltalk::BaseActivity onPause background");
//            }


//            if (socketManager?.isSocketConnected()) {
//                socketManager?.disconnectSocket();
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    override fun onResume() {
        super.onResume()
        //		AdColony.resume(this);
//        vunglePub.onResume();
        val liveData = sharedData
        liveData.observe(this, dataObserver!!)

        socketManager = getInstance(this, null, null)

        //아직 UserModel이 없다는것은 로그인을 안했다는뜻이므로, 로그인하면 소켓연결 가능하게함.
        Util.log("idoltalk::BaseActivity onResume " + this.javaClass)


        //앱시작 화면에서는 무조건 소켓끊어주기.
        if (this.javaClass == AuthActivity::class.java || this.javaClass == StartupActivity::class.java) {
            socketManager!!.disconnectSocket()
            socketManager!!.socket = null
            ChatDB.destroyInstance()
            ChatRoomList.destroyInstance()
            ChatMembersList.destroyInstance()
            ChatMessageList.destroyInstance()
            ChatRoomInfoList.destroyInstance()
        }


        try {
            var isSocketConnected = false
            if (socketManager!!.socket != null) {
                isSocketConnected = socketManager!!.socket!!.connected()
            }
            if (!isSocketConnected &&
                (this.javaClass != StartupActivity::class.java && this.javaClass != AuthActivity::class.java && this.javaClass != PushStartActivity::class.java)
            ) { // 하나라도 해당되면 연결이 되면 안되니까 &&로바꿔줌.
                socketManager!!.createSocket()
                socketManager!!.connectSocket()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        Const.CHATTING_IS_PAUSE = false
    }

    //sequence넘버 emit을 보낼때 마다 1증가시켜준다.
    fun incrementSequenceNumber() {
        var sequencNum = socketManager!!.squenceNum
        ++sequencNum
        socketManager!!.squenceNum = sequencNum
        Util.log("idoltalk::current seq " + socketManager!!.squenceNum)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val fragmentList = supportFragmentManager.fragments
        if (fragmentList != null) {
            //TODO: Perform your logic to pass back press here
            for (fragment in fragmentList) {
                if (fragment is OnBackPressedListener) {
                    (fragment as OnBackPressedListener).onBackPressed()
                }
            }
        }
    }

    protected fun onUserInfoUpdated(account: IdolAccount?) {
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun showMessage(msg: String?) {
        if (!isFinishing) {
            Util.showDefaultIdolDialogWithBtn1(
                this,
                null,
                msg
            ) { v: View? -> Util.closeIdolDialog() }
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val uiMode = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    fun showErrorWithClose(msg: String?) {
        if (!isFinishing) {
            Util.showDefaultIdolDialogWithBtn1(
                this,
                null,
                msg
            ) { v: View? ->
                Util.closeIdolDialog()
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isAlive = true
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(Const.ARTICLE_SERVICE_UPLOAD))
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onStop()
        Util.log("BaseActivity:::" + javaClass.name)
        val checkGoPush = intent.getBooleanExtra("go_push_start", false)
        if (checkGoPush) {
            return
        }
        isAlive = false
    }

    override fun onDestroy() {
        if (FLAG_CLOSE_DIALOG) {
            Util.closeIdolDialog()
        } else {
            FLAG_CLOSE_DIALOG = true
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            var grantedCount = 0
            for (grantResult in grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    grantedCount++
                }
            }

            // Check if the only required permission has been granted
            if (grantedCount == grantResults.size) {
                // read phone state permission has been granted, preview can be displayed
                makeText(this, getString(R.string.msg_download_ok), Toast.LENGTH_SHORT).show()
            } else {
//                Log.i(TAG, "read phone state permission was NOT granted.");
                makeText(this, getString(R.string.msg_download_fail), Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == 100) {
            // do nothing
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    protected fun setFirebaseGoogleAnalyticsActivity() {
        if (BuildConfig.CHINA) {
            return
        }
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        setCrashlyticsUserInfo(getAccount(this@BaseActivity))
    }

    fun setUiActionFirebaseGoogleAnalyticsActivity(action: String?, label: String) {
        if (BuildConfig.CHINA) {
            return
        }
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)

            val params = Bundle()
            params.putString(Const.ANALYTICS_GA_DEFAULT_ACTION_KEY, action)
            firebaseAnalytics!!.logEvent(label, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setUiActionFirebaseGoogleAnalyticsActivityWithKey(
        action: String?,
        label: String,
        mapOfParams: Map<String?, String?>
    ) {
        if (BuildConfig.CHINA) {
            return
        }
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)

            val params = Bundle()
            params.putString(Const.ANALYTICS_GA_DEFAULT_ACTION_KEY, action)
            for (key in mapOfParams.keys) {
                val value = mapOfParams[key]
                params.putString(key, value)
            }
            firebaseAnalytics!!.logEvent(label, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setCommunityTitle(idol: IdolModel, addtext: String?) {
        val actionbar = supportActionBar
        val layout = ActionBar.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        //        idol.setLocalizedName(this);
        val text = idol.getName(this)
        val titleView = View.inflate(this, R.layout.view_title, null)
        val title = titleView.findViewById<TextView>(R.id.title)
        val group = titleView.findViewById<TextView>(R.id.group)
        val add = titleView.findViewById<TextView>(R.id.addtext)
        if (BuildConfig.CELEB) {
            title.setTextColor(resources.getColor(R.color.gray1000))
            group.setTextColor(resources.getColor(R.color.gray1000))
            add.setTextColor(resources.getColor(R.color.gray1000))
        }
        add.visibility = View.GONE

        setName(this, idol, title, group)

        if (addtext != null) {
            add.visibility = View.VISIBLE
            add.text = addtext.trim { it <= ' ' } + " - "
        }

        actionbar!!.setCustomView(titleView, layout)
        actionbar.setDisplayShowCustomEnabled(true)
    }

    //화면 넘어갈 경우 있어서 텍스트 3개로 나누어져 있는 것(setCommunityTitle) 하나로 합침
    fun setCommunityTitle2(idol: IdolModel?, addtext: String?) {
        val idol = idol ?: return
        val actionbar = supportActionBar
        val layout = ActionBar.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        val text = idol.getName(this)
        val titleView = View.inflate(this, R.layout.view_title, null)
        val add = titleView.findViewById<TextView>(R.id.addtext)
        add.visibility = View.GONE

        if (BuildConfig.CELEB) {
            add.setTextColor(resources.getColor(R.color.gray1000))

            if (addtext != null) {
                add.visibility = View.VISIBLE
                add.text = addtext.trim { it <= ' ' } + (" - $text")
            }
        } else {
            var underBarCheck = false

            var groupName = ""
            var soloName = ""
            var fullName = ""
            if (idol.type.equals("S", ignoreCase = true)) {
                //그룹 내 멤버일 때
                if (text.contains("_")) {
                    soloName = Util.nameSplit(this, idol)[0]
                    groupName = Util.nameSplit(this, idol)[1]
                    fullName = if (Util.isRTL(this)) {
                        "$groupName $soloName"
                    } else {
                        "$soloName $groupName"
                    }
                } else {
                    fullName = text
                }
            } else {
                fullName = text
                if (fullName.contains("_")) {
                    underBarCheck = true
                }
            }

            if (addtext != null) {
                add.visibility = View.VISIBLE

                //full name  앞뒤에 공백 있는 경우 있는데  indexoutofbounds 남으로 trim 해줌.
                val addtext = addtext + (" - " + fullName.trim { it <= ' ' })
                val ss = SpannableString(addtext.trim { it <= ' ' })
                if (!underBarCheck && text.contains("_")) {
                    if (Util.isRTL(this)) {
                        ss.setSpan(
                            RelativeSizeSpan(0.5f),
                            addtext.length - fullName.length,
                            addtext.length - soloName.length - 1,
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                        ) // -1넣은 이유는 fullName에 넣은 공백마저 크기가 줄기 때문
                    } else {
                        ss.setSpan(
                            RelativeSizeSpan(0.5f),
                            addtext.length - groupName.length,
                            addtext.length,
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                add.text = ss
            }
        }

        actionbar!!.setCustomView(titleView, layout)
        actionbar.setDisplayShowCustomEnabled(true)
    }

    fun showLevelUpDialog(level: Int) {
        val dialog = Dialog(
            this,
            android.R.style.Theme_Translucent_NoTitleBar
        )

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        dialog.window!!.attributes = lpWindow
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val dialogBinding = DialogLevelUpBinding.inflate(
            layoutInflater
        )
        dialog.setContentView(dialogBinding.root)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialogBinding.btnOk.setOnClickListener { v: View? -> dialog.cancel() }

        dialogBinding.tvLevel.text = level.toString()

        dialog.window!!.setBackgroundDrawable(
            Color.TRANSPARENT.toDrawable()
        )
        dialog.show()

        try {
            val viewAnimation: View = dialogBinding.levelUpAnimationView
            viewAnimation.visibility = View.VISIBLE
            viewAnimation.setBackgroundResource(R.drawable.animation_quiz_solve)
            viewAnimation.bringToFront()

            val frameAnimation = viewAnimation.background as AnimationDrawable
            frameAnimation.start()
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }
    }

    private fun setCrashlyticsUserInfo(account: IdolAccount?) {
        if (BuildConfig.CHINA) {
            return
        }
        if (account != null) {
            FirebaseCrashlytics.getInstance().setUserId(account.email!!)
            FirebaseCrashlytics.getInstance().setCustomKey("token", account.token!!)
            FirebaseCrashlytics.getInstance().setCustomKey("domain", account.domain!!)
            FirebaseCrashlytics.getInstance().setCustomKey("nickname", account.userName)
            if (account.most == null) {
                FirebaseCrashlytics.getInstance().setCustomKey("most", "none")
            } else {
                FirebaseCrashlytics.getInstance().setCustomKey("most", account.most!!.getName(this))
            }
        } else {
            FirebaseCrashlytics.getInstance().setUserId("none_account")
        }
    }

    fun openLegacyImageEditor(receivedUri: Uri?, useSquareImage: Boolean) {
        val cropIntent = Intent("com.android.camera.action.CROP")
        // 아래를 해줘야 50x50 미만은 편집할 수 없다는거 나오는게 방지됨.
        cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        cropIntent.setDataAndType(receivedUri, "image/*")
        cropIntent.putExtra("crop", "true")
        if (useSquareImage) {
            cropIntent.putExtra("aspectX", 1)
            cropIntent.putExtra("aspectY", 1)
        }
        cropIntent.putExtra("scale", "true")
        cropIntent.putExtra("output", createTempFile())
        cropIntent.putExtra("outputFormat", "PNG")
        val packageManager = packageManager
        if (cropIntent.resolveActivity(packageManager) != null) {
            try {
                startActivityForResult(cropIntent, PHOTO_CROP_REQUEST)
            } catch (e: Exception) {
                Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    getString(R.string.msg_use_internal_editor)
                ) { Util.closeIdolDialog() }
            }
        } else {
            Util.showDefaultIdolDialogWithBtn1(
                this,
                null,
                getString(R.string.cropper_not_found)
            ) { Util.closeIdolDialog() }
        }
    }

    protected fun createTempFile(): Uri? {
        // create temporal file for saving croped image
        try {
            mTempFileForCrop = try {
                File.createTempFile("crop", ".png", externalCacheDir)
            } catch (e: IOException) {
                File.createTempFile("crop", ".png", cacheDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return Uri.fromFile(mTempFileForCrop)
    }

    private fun setDarkMode() {
        // dark mode
        val darkmode = Util.getPreferenceInt(
            this,
            Const.KEY_DARKMODE,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        AppCompatDelegate.setDefaultNightMode(darkmode)
    }

    protected fun setBackPressed(callback: OnBackPressedCallback) {
        onBackPressedDispatcher.addCallback(callback)
    }


    companion object {
        const val MEZZO_PLAYER_REQ_CODE: Int = 900
        @JvmField
        var FLAG_CLOSE_DIALOG: Boolean = true

        const val REQUEST_WRITE_EXTERNAL_STORAGE: Int = 1000
        const val REQUEST_POST_NOTIFICATIONS: Int = 1001
        const val REQUEST_READ_EXTERNAL_STORAGE: Int = 1002
        const val REQUEST_READ_MEDIA_IMAGES: Int = 1003
        const val REQUEST_READ_MEDIA_VIDEO: Int = 1004

        const val PHOTO_SELECT_REQUEST: Int = 8000
        const val PHOTO_CROP_REQUEST: Int = 7000
        const val VIDEO_SELECT_REQUEST: Int = 6000

        const val EXTRA_NEXT_ACTIVITY: String = "next_activity"
        const val EXTRA_IDOL: String = "idol"
        const val EXTRA_ARTICLE: String = "article"
        const val EXTRA_SUPPORT: String = "support"
        const val EXTRA_SUPPORT_STATUS: String = "support_status"
        const val EXTRA_BOARD_STATUS: String = "board_status"
        const val EXTRA_RECORDS_STATUS: String = "records_status"
        const val EXTRA_STORE: String = "store"
        const val EXTRA_NOTICE_ID: String = "notice_number"
        const val EXTRA_NOTICE_TITLE: String = "notice_title"
        const val EXTRA_IS_NOTICE: String = "is_notice"
        const val EXTRA_IS_AWARD: String = "is_award"
        const val EXTRA_IS_HOF: String = "is_hof"
        const val EXTRA_IS_MIRACLE: String = "is_miracle"
        const val EXTRA_IS_HEART: String = "is_heart"
        const val EXTRA_IS_ROOKIE: String = "is_rookie"
        const val EXTRA_IS_LIVE: String = "is_live"
        const val EXTRA_IS_IMAGEPICK: String = "is_image"
        const val EXTRA_IS_MENU: String = "is_menu"
        const val EXTRA_IS_MY_HEART_INFO: String = "is_my_heart_info"
        const val EXTRA_IS_FREE_BOARD_REFRESH: String = "is_free_board_refresh" // 자게 이동 후 리프레시
        const val EXTRA_ONEPICK_STATUS: String = "onepick_status"
        const val EXTRA_IS_FROM_ALTERNATE_LINK_FRAGMENT_ACTIVITY: String =
            "is_from_alternate_link_fragment_activity"
        const val EXTRA_TITLE: String = "title"
        const val EXTRA_LINK_STATUS: String = "link_status"
        const val EXTRA_BANNERGRAM_ID: String = "banner_gram_id"
        const val EXTRA_THEME_PICK: String = "theme_pick"
        const val EXTRA_IMAGE_PICK: String = "image_pick"
        const val EXTRA_LIVE_STREAMING_INFO: String = "live_streaming"
        const val EXTRA_IS_FROM_PUSH: String = "is_from_push"

        const val EXTRA_IDOL_STATUS_CHANGE: String = "idol_status_change"
        const val PARAM_NEXT_INTENT: String = "next_intent"

        const val PARAM_IS_SOLO: String = "paramIsSolo"
        const val PARAM_IS_MALE: String = "paramIsMale"

        // 움짤 프사
        var playerView1: PlayerView? = null
        var playerView2: PlayerView? = null
        var playerView3: PlayerView? = null
    }
}
