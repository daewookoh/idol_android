package net.ib.mn.liveStreaming

import android.bluetooth.BluetoothHeadset
import android.content.*
import android.content.ClipboardManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.*
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import net.ib.mn.utils.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.databinding.DataBindingUtil
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.hls.HlsManifest
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.BaseActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.PlayRepositoryImpl
import net.ib.mn.databinding.ActivityLiveStreamingBinding
import net.ib.mn.databinding.ControllerLiveStreamingBinding
import net.ib.mn.databinding.LiveChattingBottomSheetBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.liveStreaming.LiveSocketManager.Companion.KEY_SHARED_LIVE_REPORT_LIST
import net.ib.mn.model.LiveChatMessageModel
import net.ib.mn.model.LiveResolutionModel
import net.ib.mn.model.LiveStreamListModel
import net.ib.mn.utils.*
import net.ib.mn.utils.Util.*
import net.ib.mn.utils.UtilK.Companion.addImage
import net.ib.mn.utils.UtilK.Companion.doEditorAction
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.link.LinkUtil
import org.json.JSONObject
import java.lang.Thread.sleep
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


/**
 * ProjectName: idol_app_renew
 *
 * Description: 라이브 스트리밍  세로화면과 가로 화면이 실행되는 엑티비티이다.
 * orientation전환은  유투브처럼  모션 레이아웃을  이용하여,   영상의  끊김이 없도록 실행한다.
 * */
@UnstableApi
@AndroidEntryPoint
class LiveStreamingActivity : BaseActivity(), Player.Listener {

    //hls streaming용 샘플
    private var streamingLink:String? = null
    private lateinit var simpleExoPlayer: ExoPlayer
    private val constraintSet = ConstraintSet()
    private var isFullScreenMode = false
    private lateinit var streamListModel: LiveStreamListModel

    //secure 토큰  ->  seuretoken을 이용해
    //hls 용  스트리밍 url을 얻어낸다.
    private var secureToken = ""

    private var isFullButtonClicked = false
    private var isExitButtonClicked = false

    private var resolutionList = ArrayList<LiveResolutionModel>()

    private lateinit var countTimer:Thread

    //현재 디바이스 회전 orientation 리스너
    private lateinit var orientationEventListener: OrientationEventListener

    //Socket
    lateinit var liveSocketManager:LiveSocketManager

    //신고한 메세지 관련
    private val listType = object : TypeToken<List<LiveChatMessageModel>>() {}.type
    //차단된 유저 ArrayList
    private var reportedChatList = ArrayList<LiveChatMessageModel>()
    private val gson = IdolGson.getInstance(true)

    //맨 처음 hls 연결후  track 을 가져오는건지
    private var isTrackFirst = true

    private lateinit var ivLiveStreamArtWork:ImageView

    private var mGlideRequestManager: RequestManager? = null

    //엑소 플레이어 관련
    lateinit var streamingUrl:Uri
    lateinit var loadControl: LoadControl
    lateinit var bandwidthMeter: BandwidthMeter
    lateinit var trackSelector: DefaultTrackSelector
    lateinit var renderersFactory: DefaultRenderersFactory
    lateinit var dataSourceFactory: DataSource.Factory
    lateinit var extendMediaItem: MediaItem
    lateinit var hlsMediaSource: HlsMediaSource

    private var errorTimerCount = 0//에러가 난경우 5초마다 다시 플레이를 시도하므로, count 값을 늘려 5가되면  플레이 재 실행한다.
    private var errorTimer:Thread? = null//error가 난 경우 타이머 스레드를 실행하여,  5초간격으로  플레이 재시도를 실행한다.

    //현재 error 카운트 계속 하고 있는지 체크 
    private var isErrorCounting= false

    //에러 타이머 5초씩 몇번 repeat되었는지 count새줌.
    private var errorTimerRepeatCount =1

    //좋아요 카운트를  모아서 1초마다 서버에 보내기위해 사용되는 thread
    private var likeCountCheckThread =Thread()

    //스크롤  맨밑 체크를 위한  value들
    private  var pastVisibleItems: Int =0
    private  var visibleItemCount: Int  =0
    private  var totalItemCount: Int=0
    //보이는 아이템중 가장 마지막 포지션
    private var lastComplete:Int =0
    private var isScrollEnd = true


    //스크롤  맨밑 체크를 위한  value들
    private  var pastVisibleItemsLandscape: Int =0
    private  var visibleItemCountLandscape: Int  =0
    private  var totalItemCountLandscape: Int=0
    //보이는 아이템중 가장 마지막 포지션
    private var lastCompleteLandscape:Int =0
    private var isScrollEndLandscape = true


    //사용자가 서버에 보내기전 누른 좋아요 갯수
    private var tempStorageGivenHeart =0

    //헤드셋용 브로드캐스트 인텐트 필터
    private val headSetIntentFilter = IntentFilter()

    //user account
    var account: IdolAccount? = null


    //라이브 채팅 bottomSheet behavior
    private lateinit var liveChattingBottomBehavior:BottomSheetBehavior<ConstraintLayout>

    //desc 2라인 초과일경우 더보기 가 생성되는데 이때
    //더보기가 클릭되었는지 여부를 체크한다.
    private var isViewMoreClicked = false

    private lateinit var tvDescViewTreeListener:ViewTreeObserver.OnGlobalLayoutListener

    //가로 세로  채팅  라시아클러뷰 adapter
    private lateinit var liveStreamingPortraitChatAdapter: LiveStreamingChattingAdapter
    private lateinit var liveStreamingLandScapeChatAdapter: LiveStreamingChattingAdapter

    //라이브 채팅  메세지 리스트
    private var liveChattingMessageList = ArrayList<LiveChatMessageModel>()

    //exoplayer 오디오 특성 setting 용
    private lateinit var audioAttributes: AudioAttributes

    //채팅 visible
    private var isChatVisible = false

    //채팅 보내기 전 textView에 있는 text 전역으로 저장(가로,세로 모드 통일하기 위함)
    private var inputMessageNow = ""

    private var isShowKeyboard = false

    private lateinit var binding: ActivityLiveStreamingBinding
    private lateinit var controlBinding: ControllerLiveStreamingBinding
    private lateinit var bottomSheetBinding: LiveChattingBottomSheetBinding
    @Inject
    lateinit var playRepository: PlayRepositoryImpl
    @Inject
    lateinit var accountManager: IdolAccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_live_streaming)
        binding.container.applySystemBarInsets()

        controlBinding = DataBindingUtil.bind(binding.root.findViewById(R.id.live_controller_layout))!!
        controlBinding.liveControllerLayout.applySystemBarInsets()

        bottomSheetBinding = DataBindingUtil.bind(binding.root.findViewById(R.id.cl_live_chatting_bottom_sheet))!!
        initSet()
        setChatListRcyView()
        setClickEvent()
        setRecyclerViewScrollListener()
        setBottomSheetDialog()
        getLiveSecureToken()
        setLiveSocketEvent()
    }


    private fun setChatListRcyView(){

        //세로모드 채팅  adpter 연결
        liveStreamingPortraitChatAdapter = LiveStreamingChattingAdapter(LiveStreamingChattingAdapter.PORTRAIT_MODE,mGlideRequestManager,account)
        bottomSheetBinding.rcyLiveChattingContent.apply {
            adapter = liveStreamingPortraitChatAdapter
        }

        //가로모드 채팅 adapter 연결
        liveStreamingLandScapeChatAdapter = LiveStreamingChattingAdapter(LiveStreamingChattingAdapter.LANDSCAPE_MODE,mGlideRequestManager,account)
        binding.rcyLiveChattingContentLandscape.apply {
            adapter = liveStreamingLandScapeChatAdapter
        }
    }

    //헤드셋 연결관련 처리를 위한 브로드캐스트
    private val broadCastHeadSetWork = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if(!isTrackFirst && this@LiveStreamingActivity::simpleExoPlayer.isInitialized && simpleExoPlayer.isPlaying){
                if(intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,HEADSET_UNPLUGED)== HEADSET_UNPLUGED){//블루투스 unplug일때 스트리밍  pause 해줌.
                    binding.pvLiveStreaming.showController()
                    setStreamingPause()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        //0이상이면  아직 play 못하고 에러 가나서 재송출 시도중인것미으로,
        //view   visisble change 가 없는  replayPlayer를 사용
        if(errorTimerRepeatCount > 0){
            replayPlayer()
        }

        //라이브 중간에 pause 불렸다 다시 돌아오면,  컨트롤러를 보여준다.
        if(!isTrackFirst){
            binding.pvLiveStreaming.showController()
        }

        //중간에 백그라운드 갔다와서 엑티비티 재생성되면 visible  아이콘 풀리는 현상 때문에 한번더 넣어줌.
        changeChatVisibility(isChatVisible)

        headSetIntentFilter.addAction(Intent.ACTION_HEADSET_PLUG)
        headSetIntentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
        headSetIntentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        this.registerReceiver(broadCastHeadSetWork,headSetIntentFilter)


        //맨처음 로딩중일때 바로 pause해버리고  다시 화면으로 돌아오면,  프로그래스바 남아있는 상태에서
        //play되는 경우가 있어  돌아왔을때는 플레이 중이라면  프로그래스바  gone으로 예외 처리 적용
        if(this::simpleExoPlayer.isInitialized && simpleExoPlayer.isPlaying){
            binding.progressBarGroup.visibility = View.GONE
        }

        //좋아요 check thread 멈춰있으면 다시 실행
        if(likeCountCheckThread.isInterrupted){
            likeCountCheckThread.start()
        }

        //라이브 시간 카운트 타이머 실행
        if(countTimer.isInterrupted){
          countTimer.start()
        }

        //화면 회전  리스너 활성화
        orientationEventListener.enable()
    }


    //라이브 start 타임 기준으로 현재 시간과 비교하여,  1초마다 체크하여  라이브 시작후 몇분, 몇시간이 지났는지 알려준다.
    private fun liveTimeCount(tvLiveTime: TextView,tvLiveTimeFull:TextView,liveStreamListModel: LiveStreamListModel){
        tvLiveTime.text = "0 ${this.getString(R.string.time_minute)}"
        tvLiveTimeFull.text ="0 ${this.getString(R.string.time_minute)}"
        countTimer = thread{
            try {
                while (!countTimer.isInterrupted){
                    runOnUiThread {
                        if (liveStreamListModel.startAt != null) {
                            tvLiveTime.text = UtilK.timeBefore(liveStreamListModel.startAt!!,this)
                            tvLiveTimeFull.text = UtilK.timeBefore(liveStreamListModel.startAt!!,this)
                        }else{
                            tvLiveTimeFull.text = ""
                            tvLiveTime.text = ""
                        }
                    }
                    sleep(1000)
                }
            }catch (e:Exception) {
                e.printStackTrace()
            }
        }
    }


    //유의사항  이모지를  아이콘 이미지로  변경해준다.
    private fun replaceEmojiWithIcon(){
        var disclaimerText = this.resources.getString(R.string.live_disclaimer)
        try {

            disclaimerText = disclaimerText.replace("▶","[play-icon]")//조회수 이모지 replace
            disclaimerText = disclaimerText.replace("❤","[heart-icon]")//하트수 이모지 replace
            disclaimerText = disclaimerText.replace("\uD83D\uDC41","[concurrent_users-icon]")//동접자수 이모지 replace
            disclaimerText = disclaimerText.replace("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66","[max_users-icon]")//최대 동접자수 이모지 replace

            binding.agreementTv.text = disclaimerText

            //텍스트뷰의  height를 계산해서  text에 들어가는 image에  height를 조정해준다. -> width 는  height 에서  6정도 더 줘서  기존 아이콘 비율 맞춤.
            val metrics = binding.agreementTv.paint.fontMetrics
            val height = metrics.bottom - metrics.top

            //각각 해당 하는  글자에  아이콘  drawable 넣어줌.
            binding.agreementTv.addImage("[play-icon]",R.drawable.icon_streaming_hits, height.toInt()+4, height.toInt()-8)
            binding.agreementTv.addImage("[heart-icon]",R.drawable.icon_streaming_heart, height.toInt()+4, height.toInt()-8)
            binding.agreementTv.addImage("[concurrent_users-icon]",R.drawable.icon_streaming_users, height.toInt()+4, height.toInt()-8)
            binding.agreementTv.addImage("[max_users-icon]",R.drawable.icon_streaming_users_2, height.toInt()+4, height.toInt()-8)
        }catch (e:Exception){

            //혹시나 에러나면 -> text그대로 적용해줌.
            binding.agreementTv.text = this.resources.getString(R.string.live_disclaimer)
            e.printStackTrace()
        }

    }




    //화면 다시 시작할때 bundle에  저장해놓은 값 다시 적용
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        //해상도 리스트 지정된거  다시 받아옴.
        resolutionList = savedInstanceState.getSerializable(KEY_RESOLUTION_LIST) as ArrayList<LiveResolutionModel>
        streamListModel = savedInstanceState.getSerializable(KEY_LIVE_MODEL) as LiveStreamListModel

        //이미 트랙 한번 가져온건지 체크 ->  엑티비티 재생성시 여길 타므로,
        //isTrackFirst 은 false  (화면 처음 들어왓을떄 이미  true 값으로 관련 로직 진행함)
        isTrackFirst = false

        isChatVisible =  savedInstanceState.getBoolean(KEY_IS_CHAT_VISIBLE,false)
        isFullScreenMode = savedInstanceState.getBoolean(KEY_IS_FUll_SCREEN,false)

        //채팅 visible 여부 체크하여,  뷰 처리
        changeChatVisibility(isChatVisible)

        //full스크린 상태에서 다크모드 변환시 landscape ui모드 풀리는 현상때문에 넣어줌.
        if(isFullScreenMode){
            showLandScapeUi()

            //landscape 모드에서  화면 다시 생성될때 focus되어서 키보드 다시올라오는 현상 방지
            binding.messageInputLiveChatLandscape.clearFocus()
        }else{
            showPortraitUi()
        }

    }


    //혹시 화면  다시 시작 하는 경우를 대비해서 필요 값 저장
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_CHAT_VISIBLE,isChatVisible)//채팅 visible 여부 저장
        outState.putBoolean(KEY_IS_FUll_SCREEN,isFullScreenMode)//full screen mode 여부 저장
        outState.putSerializable(KEY_RESOLUTION_LIST,resolutionList)//해상도 현재 상태 저장
        outState.putSerializable(KEY_LIVE_MODEL,streamListModel)//현재 라이브 모델값 저장
    }

    override fun onDestroy() {
        super.onDestroy()

        //다크모드 등  엑티비티 재생성될때
        //이전 simpleexoplayer release 안되서 두개씩 도는 현상 때문에 destroy에서 release해줌.
        if(this::simpleExoPlayer.isInitialized){
            simpleExoPlayer.release()
        }

        //라이브 socket disconnect처리
        liveSocketManager.disconnectSocket()
        liveSocketManager.destroyInstance()

        //좋아요 체크 스레드 중지
        likeCountCheckThread.interrupt()

        //라이브 시간 카운트 타이머 정지
        countTimer.interrupt()

        //에러 타이머 종료
        errorTimerStop()

    }

    //error 타이머  종료 시킴.
    private fun errorTimerStop(){
        errorTimer?.interrupt()
        errorTimer = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(isFullScreenMode && hasFocus){
             hideSystemUI()
        }
    }


    //초기 세팅
    private fun initSet() {

        //landscape 모드 관련  뷰들 gone처리
        controlBinding.liveLandscapeControllerGroup.visibility = View.GONE
        binding.btnStreamingLikeFull.visibility = View.GONE
        binding.btnStreamingLikeEndFull.visibility = View.GONE

        //가로모드 채팅 view들 visible 처리
        controlBinding.btnChatHide.visibility = View.GONE
        controlBinding.btnChatShow.visibility = View.GONE

        //가로모드  채팅  관련 뷰 그룹 visisble 처리
        binding.landscapeLiveChatGroup.visibility = View.GONE

        mGlideRequestManager = Glide.with(this)

        account = IdolAccount.getAccount(this)

        //유의사항  이모지를 아이콘으로 변경해준다.
        replaceEmojiWithIcon()

        //editext 속성 설정
        bottomSheetBinding.messageInputLiveChat.setInputEditText(bottomSheetBinding.submitBtnLiveChat,false)
        binding.messageInputLiveChatLandscape.setInputEditText(binding.submitBtnLiveChatLandscape,true)

        //가로모드 editext focus되면 키보드 올라오는 현상 방지위해 시작시 clearfocus한번 불러줌.
        binding.messageInputLiveChatLandscape.clearFocus()

        //라이브 채팅 관련  셋팅
        setLiveChatting()


        //키보드 show
        KeyboardVisibilityUtil(window, onShowKeyboard = {//키보드가 보여지면 항상 맨아래로  스크롤 해준다.

            bottomSheetBinding.rcyLiveChattingContent.smoothScrollToPosition(liveStreamingPortraitChatAdapter.itemCount)//가장 마지막 인덱스로 스크롤
            binding.rcyLiveChattingContentLandscape.smoothScrollToPosition(liveStreamingLandScapeChatAdapter.itemCount)//가장 마지막 인덱스로 스크롤
            bottomSheetBinding.scrollToBottomBtnChat.visibility = View.GONE
            isScrollEnd = true
            isScrollEndLandscape = true
            isShowKeyboard = true
         }, onHideKeyboard = {
            isShowKeyboard = false

            //가로모드 메세지 입력 위해  올라온 키보드 hide시킬떄 -> 가로모드 메세지 입력 뷰 gone처리하고 editext  focus처리 해준다.
            if(isFullScreenMode && binding.messageInputLiveChatLandscape.isFocused){
                binding.footerLiveChatLandscape.visibility= View.GONE
                binding.messageInputLiveChatLandscape.clearFocus()
                inputMessageNow = binding.messageInputLiveChatLandscape.text.toString()
                binding.messageInputLiveChatNoKeyboard.text = inputMessageNow  //가로모드 시스템 키보드 내려갈 때 기존 가로모드 아래에 있는 TextView에 저장

                hideSystemUI()  //가로모드에서 키보드 올린 상태에서 키보드 내리면 시스템UI 없어지게 불러줌
            }
        })




        //해당 화면에서 처음 실행될때만 값 가져옴.
        //그외에  화면  갑자기 재실행 될경우는  bundle에 저장한 값으로 씀
        if(isTrackFirst){
            streamListModel = intent.getSerializableExtra(PARAM_LIVE_STREAM_LIST_MODEL) as LiveStreamListModel
        }

        liveSocketManager = LiveSocketManager.getInstance(liveId = streamListModel.id, this)

        liveSocketManager.createSocket()
        liveSocketManager.connectSocket()


        //처음 들어왔을때는 이전 메세지 30개를 받아오게 요청한다.
        liveSocketManager.socket?.emit(
            LiveSocketManager.LIVE_REQUEST_MESSAGES,
            JSONObject().put("cmd", LiveSocketManager.LIVE_REQUEST_MESSAGES)
                .put("live_id", streamListModel.id)
                .put("limit", 30)
        )



        //좋아요 갯수 체크 스레드 실행
        startLikeCountCheckThread()

        streamingLink = streamListModel.playUrl.toString()

        //라이브 좋아요 버튼 + 세로 채팅 좋아요 버튼 visible 여부 세팅
        setLikeButtonVisibleStatus(binding.btnStreamingLike,binding.btnStreamingLikeEnd,streamListModel.id)
        setLikeButtonVisibleStatus(bottomSheetBinding.btnStreamingLikeLiveChat,bottomSheetBinding.btnStreamingLikeEndLiveChat,streamListModel.id)

        //exoplayer 프로그래스바  visible되었을때  pause/play 버튼  안보이게 함.
        //버퍼링 체크보다  viewTreeObserver가  더 정확해서 사용함.
        binding.progressBar.viewTreeObserver.addOnGlobalLayoutListener {

            //프로그래스바가 보이는 경우-> play/pause 버튼  없애줌.
            if(binding.progressBar.visibility == View.VISIBLE){
                if(isTrackFirst){
                    binding.tvLiveStalled.visibility = View.GONE
                }
                controlBinding.btnPause.visibility = View.GONE
                controlBinding.btnPlay.visibility = View.GONE
            }else{//프로그래스바가 안보이고,  exoplayer 플레이 중이라면, 뷰를 play 상태로 보여준다.
                if(this::simpleExoPlayer.isInitialized && simpleExoPlayer.isPlaying){
                    controlBinding.btnPause.visibility = View.VISIBLE
                    controlBinding.btnPlay.visibility = View.GONE
                }
            }
        }

        //desc 적용 -> 더보기 적용
        setDescViewMore(streamListModel.desc.toString())

        //타이머 적용
        liveTimeCount(tvLiveTime = binding.tvLiveTime,tvLiveTimeFull = controlBinding.tvLiveTimeFull ,liveStreamListModel = streamListModel)

        //controller auto로 show 되는거 방지
        binding.pvLiveStreaming.controllerAutoShow = false


        //레벨 적용
        ("Lv."+streamListModel.levelLimit).apply {
            binding.tvLimitLevel.text = this
            controlBinding.tvControllerLimitLevel.text = this
        }

        //타이틀 적용
        streamListModel.title.apply {
            binding.tvLiveTitle.text = this
            controlBinding.tvControllerLiveTitle.text = this
        }



        val locale = LocaleUtil.getAppLocale(this@LiveStreamingActivity)
        //조회수 적용
        streamListModel.totalViews.apply {
            binding.tvStreamingHits.text = NumberFormat.getNumberInstance(locale).format(this)//컴마 적용
            bottomSheetBinding.tvStreamingHitsLiveChat.text = NumberFormat.getNumberInstance(locale).format(this)//컴마 적용
            controlBinding.tvControllerStreamingHits.text = NumberFormat.getNumberInstance(locale).format(this)
        }

        //하트수 적용
        streamListModel.heart.apply {
            binding.tvStreamingHeart.text = NumberFormat.getNumberInstance(locale).format(this)
            bottomSheetBinding.tvStreamingHeartLiveChat.text = NumberFormat.getNumberInstance(locale).format(this)
            controlBinding.tvControllerStreamingHeart.text = NumberFormat.getNumberInstance(locale).format(this)
        }

        //동접자수 적용
        streamListModel.views.apply {
            binding.tvStreamingUsers.text = NumberFormat.getNumberInstance(locale).format(this)
            bottomSheetBinding.tvStreamingUsersLiveChat.text = NumberFormat.getNumberInstance(locale).format(this)
            controlBinding.tvControllerStreamingUsers.text = NumberFormat.getNumberInstance(locale).format(this)
        }

        //최대 동접자수 적용
        streamListModel.maxViews.apply {
            binding.tvStreamingUsers2.text = NumberFormat.getNumberInstance(locale).format(this)
            bottomSheetBinding.tvStreamingUsers2LiveChat.text = NumberFormat.getNumberInstance(locale).format(this)
            controlBinding.tvControllerStreamingUsers2.text = NumberFormat.getNumberInstance(locale).format(this)
        }


        //화면 회전 리스너
        orientationEventListener = object :OrientationEventListener(this,SensorManager.SENSOR_DELAY_NORMAL){
            override fun onOrientationChanged(orientation: Int) {
                when {
                    //아래  경우에는  디바이스 회전 방향에 따라 회전이 가능하면(시스템 설정이 회전가능일때) 회전하도록 한다.
                    orientation >= 315 || orientation <45 -> {//세로 약0도 각도 일때
                        if(Settings.System.getInt(this@LiveStreamingActivity.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 0 ){
                            this@LiveStreamingActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                        }else if( Settings.System.getInt(this@LiveStreamingActivity.contentResolver, Settings.System.ACCELEROMETER_ROTATION) ==1 && !isFullButtonClicked) {
                            isFullButtonClicked = false
                            isExitButtonClicked = false
                            isFullScreenMode = false
                            this@LiveStreamingActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }

                    orientation in 45..134 ->{//가로방향 약  270 각도 일때
                        if(Settings.System.getInt(this@LiveStreamingActivity.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 0){
                            if(isFullScreenMode){//회전 블락당해도  fullscreenmode에서는  회전이 가능하게
                                this@LiveStreamingActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }else{
                              this@LiveStreamingActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                            }
                        }else if( Settings.System.getInt(this@LiveStreamingActivity.contentResolver, Settings.System.ACCELEROMETER_ROTATION) ==1 && !isExitButtonClicked) {
                            isFullButtonClicked = false
                            isExitButtonClicked = false
                            isFullScreenMode = true
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        }
                    }

                    (orientation in 225..314)-> {//가로방향 약 90도 각도 또는 270 각도 일때

                        if(Settings.System.getInt(this@LiveStreamingActivity.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 0){
                            if(isFullScreenMode){
                                this@LiveStreamingActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }else{
                                this@LiveStreamingActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                            }
                        }else if( Settings.System.getInt(this@LiveStreamingActivity.contentResolver, Settings.System.ACCELEROMETER_ROTATION) ==1 && !isExitButtonClicked) {
                            isFullButtonClicked = false
                            isExitButtonClicked = false
                            isFullScreenMode = true
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }
                }
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = item.groupId

        when(item.itemId){
            MENU_COPY ->{
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                var text = liveChattingMessageList[position].content//해당 포지션의  content 복사
                try {
                    text = text.replace("@\\{\\d+\\:([^\\}]+)\\}".toRegex(), "")
                } catch (e: Exception) {
                }

                val clip = ClipData.newPlainText("Copied text", text)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }

            MENU_DELETE ->{ //삭제 버튼 눌렀을 경우
                //라이브 소켓 연결되었을 경우  메세지 보냄.
                if(liveSocketManager.isSocketConnected){
                    liveSocketManager.socket?.emit(
                            LiveSocketManager.LIVE_DELETE_MESSAGE,
                            JSONObject()
                                    .put("cmd", LiveSocketManager.LIVE_DELETE_MESSAGE)
                                    .put("seq", liveSocketManager.sequenceNum)
                                    .put("live_id", streamListModel.id)
                                    .put("server_ts",liveChattingMessageList[position].serverTs)
                    )
                }
            }
            MENU_REPORT ->{ //신고 버튼 눌렀을 경우
                //라이브 소켓 연결되었을 경우  메세지 보냄.
                if(liveSocketManager.isSocketConnected){
                    liveSocketManager.socket?.emit(
                            LiveSocketManager.LIVE_REPORT_MESSAGE,
                            JSONObject()
                                .put("cmd", LiveSocketManager.LIVE_REPORT_MESSAGE)
                                .put("seq", liveSocketManager.sequenceNum)
                                .put("live_id", streamListModel.id)
                                .put("server_ts",liveChattingMessageList[position].serverTs)
                    )
                }
                Util.showDefaultIdolDialogWithBtn1(this,
                null,
                resources.getString(R.string.report_done), {Util.closeIdolDialog()})

                //신고의 경우는 요청한 사람만  뷰를 업데이트 해주면됨으로,  신고시 바로 처리해준다.
                //리포트 채팅 리스트  값  있는 경우  reportedChatList에 넣어줌.
                if(Util.getPreference(this,KEY_SHARED_LIVE_REPORT_LIST+streamListModel.id).isNotEmpty()){
                    //차단된 유저 array로 저장 가져옴.
                    reportedChatList = gson.fromJson(Util.getPreference(this, KEY_SHARED_LIVE_REPORT_LIST+streamListModel.id).toString(), listType)
                }
                liveChattingMessageList[position].isReported = true

                //리포트 true된걸 넣어서 reportedChatList업데이트 해주고,  preference 저장값도 업데이트 해준다.
                reportedChatList.add(liveChattingMessageList[position])
                Util.setPreferenceArray(this,KEY_SHARED_LIVE_REPORT_LIST+streamListModel.id,reportedChatList)

                //리스트 업데이트
                liveStreamingLandScapeChatAdapter.getLiveStreamAllMessageList(liveChattingMessageList)
                liveStreamingPortraitChatAdapter.getLiveStreamAllMessageList(liveChattingMessageList)
            }
        }
        return super.onContextItemSelected(item)
    }


    //editext 속성 설정
    private fun EditText.setInputEditText(submitBtn:View,isLandScape:Boolean){
        this.apply {
            if (isLandScape) {
                this.imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            } else {
                this.imeOptions = EditorInfo.IME_ACTION_SEND
            }
            this.setRawInputType(InputType.TYPE_CLASS_TEXT)

            //채팅 imeoption 메뉴 클릭시 -> 보내기 버튼 클릭되도록 수정
            this.doEditorAction(submitBtn)
        }
    }

    //desc가 2줄을 초과하면  그 이후 내용은 더보기로 처리해준다.
    private fun setDescViewMore(originalString:String){

        binding.tvLiveDesc.text = originalString//아래서 두줄 넘는지 체크위해 일단 original text를 넣어줌.
        tvDescViewTreeListener = ViewTreeObserver.OnGlobalLayoutListener {
            if(binding.tvLiveDesc.lineCount>2 && !isViewMoreClicked){//두줄이 넘고 더보기 클릭이 안되어있으면, 더보기 뷰 처리 진행
                binding.tvLiveDesc.text = originalString

                try{
                    var textOfMaxTwoLine = binding.tvLiveDesc.text.substring(binding.tvLiveDesc.layout.getLineStart(0), binding.tvLiveDesc.layout.getLineEnd(1))
                    val ssViewMore = if (Util.isRTL(this)){
                        getString(R.string.view_more) + " ..."
                    } else{
                        "... " + getString(R.string.view_more)
                    }

                    //\n으로  두번째 줄에서 처리되면  3번째줄로 더보기가 가지므로, 마지막 \n은 없애준다.
                    if(textOfMaxTwoLine.endsWith("\n")){
                        textOfMaxTwoLine= textOfMaxTwoLine.dropLast(2)
                    }

                    binding.tvLiveDesc.text = textOfMaxTwoLine+ssViewMore
                    binding.tvLiveDesc.maxLines = Integer.MAX_VALUE//혹시나 라인 2를 초과하는지 아래서 체크위해  maxLine을 최대로 적용해준다.

                    //만약에 위처럼 했는데 2번째 줄까지 text가 꽉차서 더보기가 3번째로 내려가는 경
                    //2째줄 초과인지 체크해서 desc 텍스트에서 더보기 text length만큼 뺀다음 다시 처리해준다.
                    if(binding.tvLiveDesc.lineCount>2){
                        textOfMaxTwoLine= textOfMaxTwoLine.substring(0,textOfMaxTwoLine.lastIndex-ssViewMore.length)
                        binding.tvLiveDesc.text = textOfMaxTwoLine+ssViewMore
                    }

                    val spannable = SpannableString(binding.tvLiveDesc.text)
                    spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.gray300)), textOfMaxTwoLine.lastIndex+1, binding.tvLiveDesc.text.lastIndex+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(RelativeSizeSpan(0.9f), textOfMaxTwoLine.lastIndex+1, binding.tvLiveDesc.text.lastIndex+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    binding.tvLiveDesc.text = spannable
                }catch (e:Exception){

                    //예외가 발생하면, original string 넣어주고,  max라인 적용해서 더보기 없이 보여준다.
                    //originla string은  맨위에서 넣어줌.
                    binding.tvLiveDesc.maxLines = Integer.MAX_VALUE
                    e.printStackTrace()
                }

            }

            //딱한번만  적용되면 됨으로, 위 로직이 다 끝나면, 리스너 remove해준다.
            binding.tvLiveDesc.viewTreeObserver.removeOnGlobalLayoutListener(tvDescViewTreeListener)
        }

        //위 로직 실행 위해 리스너 실행 .
        binding.tvLiveDesc.viewTreeObserver.addOnGlobalLayoutListener(tvDescViewTreeListener)

        //desc 클릭되었을때 아직 더보기가 클릭되지 않은 경우-> desc 텍스트뷰 뷰트리 observer remove해주고,
        //max 라인은  최대로 늘려준다. 그리고 original text를  넣어서 보여준다.
        binding.tvLiveDesc.setOnClickListener {
            if(!isViewMoreClicked){//더보기 한번도 안눌렸을떄
                isViewMoreClicked = true
                binding.tvLiveDesc.maxLines = Integer.MAX_VALUE//전체를 보여주기위해 maxline 최대로
                binding.tvLiveDesc.text = originalString
            }
        }

    }


    //라이브 채팅 관련 세팅
    private fun setLiveChatting() {
        liveChattingBottomBehavior = BottomSheetBehavior.from(bottomSheetBinding.clLiveChattingBottomSheet)
        liveChattingBottomBehavior.isDraggable = false//일단  bottomsheet은 드래그 안되게

        //드래그가 필요할시 사용한다.
        liveChattingBottomBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
        })

        //세로모드 라이브 채팅 눌렀을때  -> 채팅화면 bottomsheet expand함.
        binding.clLiveChatting.setOnClickListener {
            isChatVisible = true
            liveChattingBottomBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        //x버튼 누르면 채팅 bottomsheet 화면  collapse함.
        bottomSheetBinding.llIconLiveChattingClose.setOnClickListener {
            isChatVisible = false
            liveChatScreenReset()
        }
    }

    //라이브 좋아요 멕스 여부를 체크해서 -> false이면 아직 좋아요 누르기가 가능하므로,
    //좋아요 버튼 visible true이면  좋아요 max도달이므로, invisible처리 함.
    private fun setLikeButtonVisibleStatus(btnLike: View,btnEnd: View,liveId: Int?){
        if(Util.getPreferenceBool(this,Const.KEY_LIVE_ID_LIKE_MAX+liveId,false)){
            btnLike.visibility = View.GONE
            btnEnd.visibility = View.VISIBLE
        }else{
            btnLike.visibility = View.VISIBLE
            btnEnd.visibility = View.GONE
        }
    }


    //화면 회전될때  각각  각도에 맞는 ui 설정을 해준다.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            Logger.v("aaaaaa")
            showLandScapeUi()
        }else{
            Logger.v("aaaaaaaaa")
            showPortraitUi()
        }
    }


    //Live cdn접근용 secure토큰을  서버로 부터 받아온다.
    private fun getLiveSecureToken(){
        MainScope().launch {
            playRepository.getToken(
                streamListModel.id!!,
                { response ->
                    if (response.optBoolean("success")) {

                        secureToken = response.optString("token")

                        //해당 url 넣어서  exoplyaer 실행 혹시나 token이 없는 url 이  보내지는 경우가 있어서  토큰정보를 같이 보내줌.
                        if (streamingLink != null) {
                            setExoPlayer(streamingLink)
                        }

                    } else {
                        if (response.optInt("gcode") == ErrorControl.ERROR_88888) {

                            //바로  다이얼로그 보여주면, oncrate시점에서 다이얼로그 다시  사라져서  delay를 조금 줌.
                            Handler(Looper.getMainLooper()).postDelayed({
                                val errorMsg = ErrorControl.parseError(
                                    this@LiveStreamingActivity,
                                    response
                                )
                                Util.showDefaultIdolDialogWithBtn1(
                                    this@LiveStreamingActivity,
                                    null,
                                    errorMsg
                                ) { view ->
                                    Util.closeIdolDialog()
                                    finish()
                                }
                            }, 500)
                        }
                    }
                }, { throwable ->
                    showErrorWithClose(getString(R.string.error_abnormal_exception))
                }
            )
        }
    }


    //현재 라이브 공유
    fun shareLive(){
        val params = listOf(LinkStatus.LIVE.status, streamListModel.id.toString())
        val url = LinkUtil.getAppLinkUrl(context = this@LiveStreamingActivity, params = params)

        //라이브 공유 text
        val liveShareText = String.format(this.getString(if(BuildConfig.CELEB) R.string.live_share_title_celeb else R.string.live_share_title),streamListModel.title)+"\n${url}"

        setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "share_live"
        )

        UtilK.linkStart(this, url = url, msg = liveShareText)
    }



    //엑소 플레이어 해상도 변경
    fun setPlayerResolution(resolution: ArrayList<LiveResolutionModel>){

        try {
            //현재 해상도 모델 리스트에  bottomSheet에서 체크해온  해상도 모델 리스트 값을 적용하여  새로 체크된 해상도로 업데이트 해준다.
            this.resolutionList = resolution
            val resolutionModel = resolution.find { it.isSelected }//선택된 해상도 모델을 return

            val highestResolution = resolution[1]//가장 높은  해상도
            val lowestResolution = resolution[resolution.size-1]//가장 낮은 해상도

            //auto 해상도일때  가장 높은 해상도를 max에  가장 낮은 해상도를  min에  적용하여,  모든 범위의  해상도를  auto로 사용되게  한다.
            if(resolutionModel?.isAutoResolution == true){
                trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setMaxVideoSize(highestResolution.width, highestResolution.height)
                    .setMinVideoSize(lowestResolution.width, lowestResolution.height)
                )
            }else{//해상도 지정일때는  지정된 해상도 값을  max size로 넣어준다.
                trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setMaxVideoSize(resolutionModel?.width!!, resolutionModel.height)
                    .setMinVideoSize(resolutionModel.width, resolutionModel.height)
                )
            }
        }catch (e:Exception) {
            e.printStackTrace()
        }
    }

    //해상도 변경 bottomSheetDialog 실행
    fun showResolutionChangeDialog(){

        val sheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_LIVE_STREAMING_RESOLUTION,resolutionList)
        val tag = "live_resolution_filter"
        val oldFrag = this.supportFragmentManager.findFragmentByTag(tag)

        if (oldFrag == null) {
            sheet.show((this).supportFragmentManager, tag)
        }
    }


    private fun setBottomSheetDialog(){
        val sheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_LIVE_STREAMING)
        controlBinding.btnMoreMenu.setOnClickListener {
            val tag = "live_filter"
            val oldFrag = this.supportFragmentManager.findFragmentByTag(tag)
            if (oldFrag == null) {
                sheet.show((this).supportFragmentManager, tag)
            }
        }
    }

    //클릭동작 모음
    private fun setClickEvent() {
        controlBinding.btnFullScreen.setOnClickListener {
            if (isFullScreenMode) {//전체화면이면  세로모드로
                showPortraitUi()
                //클릭시에는  시스템  각도로 바로 영향을 받지 않게  버튼으로 가로또는 세로 방향으로 지정되었는지 체크해준다.
                isExitButtonClicked = true
                isFullButtonClicked = false
            } else {//전체화면 아니면  가로모드로
                showLandScapeUi()
                //클릭시에는  시스템  각도로 바로 영향을 받지 않게  버튼으로 가로또는 세로 방향으로 지정되었는지 체크해준다.
                isFullButtonClicked = true
                isExitButtonClicked = false
            }
        }

        //스트리밍 화면 누르면 키보드 내려줌
        binding.pvLiveStreaming.setOnClickListener {
            if(isFullScreenMode) {
                Util.hideSoftKeyboard(this, binding.messageInputLiveChatLandscape)
            }
            else{
                Util.hideSoftKeyboard(this, bottomSheetBinding.messageInputLiveChat)
            }
        }

        //세로모드에서 스트리밍 화면 - 채팅 화면 가운데 정보화면 누르면 키보드 내려줌
        bottomSheetBinding.clLiveChattingBottomSheet.setOnClickListener {
            Util.hideSoftKeyboard(this, bottomSheetBinding.messageInputLiveChat)
        }

        //가로모드일 때 채팅 아이템 누르면 키보드 내려줌
        liveStreamingLandScapeChatAdapter.setOnItemClickListener(object:LiveStreamingChattingAdapter.OnItemClickListener{
            override fun getItemClick() {
                Util.hideSoftKeyboard(this@LiveStreamingActivity,binding.messageInputLiveChatLandscape)
            }
        })

        //세로모드일 때 채팅 아이템 누르면 키보드 내려줌
        liveStreamingPortraitChatAdapter.setOnItemClickListener(object:LiveStreamingChattingAdapter.OnItemClickListener{
            override fun getItemClick() {
                Util.hideSoftKeyboard(this@LiveStreamingActivity,bottomSheetBinding.messageInputLiveChat)
            }
        })

        //가로모드 하단  키보드 없는  채팅 입력 창 누른 경우
        binding.messageInputLiveChatNoKeyboard.setOnClickListener {
            inputMessageNow = binding.messageInputLiveChatNoKeyboard.text.toString()
            binding.messageInputLiveChatLandscape.setText(inputMessageNow)
            binding.footerLiveChatLandscape.visibility= View.VISIBLE
            binding.messageInputLiveChatLandscape.requestFocus()
            Util.showSoftKeyboard(this,binding.messageInputLiveChatLandscape)
        }

        //가로모드 하단 키보드 없는 채팅 전송 버튼 누른 경우
        binding.submitBtnLiveChatNoKeyboard.setOnClickListener {
            val account = account ?: return@setOnClickListener
            if(!binding.messageInputLiveChatNoKeyboard.text.isNullOrEmpty()) {    //채팅 메세지가 빈 값이 아닌 경우만 보내게
                binding.messageInputLiveChatNoKeyboard.text = Util.BadWordsFilterToHeart(this,binding.messageInputLiveChatNoKeyboard.text.toString()) //보내기 전에 BadWords 적용
                //내가 보낸 메세지임으로 메세지와 함께 isMineChat true로 보냄.
                sendChatting(
                        LiveChatMessageModel(
                                userId = account.userId,
                                content = binding.messageInputLiveChatNoKeyboard.text.toString(),
                                isMineChat = true,
                                clientTs = System.currentTimeMillis(),
                                senderImage = account.profileUrl,
                                senderNickName = account.userName
                        )
                )
                inputMessageNow = ""
                binding.messageInputLiveChatNoKeyboard.text = ""
            }
        }

        //가로모드 채팅보이기 버튼 누른경우(누르면 채팅이 없어짐)
        controlBinding.btnChatShow.setOnClickListener {
            //가끔  리스트화면에서 세로로 눕혀놓고 바로 들어와 가로모드가 되면 isFullScreenMode가 안바뀌는 경우가 있어서.
            //가로모드 채팅보이기이므로  isFullScreenMode mode true로 한번더 바꿔줌.
            isFullScreenMode = true
            isChatVisible = false


            //채팅 숨기기를 누른경우는  키보드 없애줌.
            Util.hideSoftKeyboard(this,binding.messageInputLiveChatLandscape)

            binding.scrollToBottomBtnChatLandscape.visibility = View.GONE

            changeChatVisibility(isChatVisible)
        }


        //가로모드 채팅 숨기기 버튼 누른 경우(누르면 채팅이 생김)
        controlBinding.btnChatHide.setOnClickListener {
            isFullScreenMode = true
            isChatVisible = true

            if(!isScrollEndLandscape){  //스크롤이 맨 아래가 아니라면 가로모드 채팅을 다시 열었을 때 스크롤 내려주는 버튼 보이게
                binding.scrollToBottomBtnChatLandscape.visibility = View.VISIBLE
            }
            changeChatVisibility(isChatVisible)
        }



        controlBinding.btnBack.setOnClickListener {

            when {

                isFullScreenMode -> {//전체 모드일떄는 뒤로가기 누르면, 세로화면인 경우로 다시 돌림.
                    showPortraitUi()
                    Util.hideSoftKeyboard(this,binding.messageInputLiveChatNoKeyboard)
                    isExitButtonClicked = true
                    isFullButtonClicked = false
                }


                else -> {// 채팅 화면 안올라와져 있고, 전체화면 아니면 뒤로가기 실행

                    //라이브 socket disconnect처리
                    liveSocketManager.disconnectSocket()
                    liveSocketManager.destroyInstance()

                    //좋아요 체크 스레드 정지
                    likeCountCheckThread.interrupt()

                    //라이브 시간 카운트 타이머 정지
                    countTimer.interrupt()

                    //에러 타이머 종료
                    errorTimerStop()

                    try {
                        //뒤로가기 눌렀을때는 완전히  플레이  종료이므로,  exoplayer release 처리 해준다.
                        simpleExoPlayer.release()
                    } catch (e: Exception) {
                        //혹시나 simpleExoPlayer initalize 안된 중에 뒤로가기하면  null point나므로 예외처리 추가
                        e.printStackTrace()
                    }

                    super.onBackPressed()
                }
            }
        }

        controlBinding.btnPlay.setOnClickListener {
           if(!simpleExoPlayer.isPlaying || !simpleExoPlayer.isLoading){
              if(errorTimerRepeatCount >= 12){//error로 인한 재송출 시도  1분이상 한경우 release했으므로, play버튼 누르면  다시  exoplayer set 해줌.

                  errorTimerRepeatCount = 0
                  isErrorCounting = false

                  controlBinding.btnPlay.visibility = View.GONE
                  binding.progressBarGroup.visibility = View.VISIBLE

                  setExoPlayer(streamingLink)
              }else{
                  setStreamingPlay()
              }
           }
        }

        controlBinding.btnPause.setOnClickListener {
            if(simpleExoPlayer.isPlaying|| simpleExoPlayer.isLoading){
                setStreamingPause()
            }
        }

        //portrait 모드일때 좋아요 클릭시 처리
        binding.btnStreamingLike.setOnClickListener {
            binding.llLottieLikeHeartContainer.addView(addLikeHeartAnimation())
        }

        //portrait 모드일때 좋아요 클릭시 처리
        bottomSheetBinding.btnStreamingLikeLiveChat.setOnClickListener {
            bottomSheetBinding.llLottieLikeHeartContainerLiveChat.addView(addLikeHeartAnimation())
        }



        //landscape모드 일때  좋아요 클릭시 처리
        binding.btnStreamingLikeFull.setOnClickListener {

            //전체화면 좋아요 누를때  컨트롤러 보여져있는 상태이면, 계속  유지하게 함.
            if(binding.pvLiveStreaming.isControllerFullyVisible){
                binding.pvLiveStreaming.showController()
            }

            binding.llLottieLikeHeartFullContainer.addView(addLikeHeartAnimation())
        }

        //end 는 클릭 동작이 필요없지만,  지정안해주면,
        //binding.btnStreamingLikeEndFull 누를 때마다 controller visible 상태가 계속 바뀌므로,  클릭리스너만 달아줌.
        binding.btnStreamingLikeEndFull.setOnClickListener {
            //전체화면 좋아요 누를때  컨트롤러 보여져있는 상태이면, 계속  유지하게 함.
            if(binding.pvLiveStreaming.isControllerFullyVisible){
                binding.pvLiveStreaming.showController()
            }
        }


        //세로모드 채팅 전송 버튼
        bottomSheetBinding.submitBtnLiveChat.setOnClickListener {
            val account = account ?: return@setOnClickListener

            if(!bottomSheetBinding.messageInputLiveChat.text.isNullOrEmpty()) {    //채팅 메세지가 빈 값이 아닌 경우만 보내게
                bottomSheetBinding.messageInputLiveChat.setText(Util.BadWordsFilterToHeart(this,bottomSheetBinding.messageInputLiveChat.text.toString()))  //보내기 전에 BadWords 적용
                //내가 보낸 메세지임으로 메세지와 함께 isMineChat true로 보냄.
                sendChatting(
                        LiveChatMessageModel(
                                userId = account.userId,
                                content = bottomSheetBinding.messageInputLiveChat.text.toString(),
                                isMineChat = true,
                                clientTs = System.currentTimeMillis(),
                                senderImage = account.profileUrl,
                                senderNickName = account.userName
                        )
                )
                bottomSheetBinding.messageInputLiveChat.text?.clear()
                inputMessageNow = ""
                binding.messageInputLiveChatNoKeyboard.text =""
            }
        }

        //가로모드 채팅 버튼(키보드 있는 것)
        binding.submitBtnLiveChatLandscape.setOnClickListener {
            val account = account ?: return@setOnClickListener

            if(!binding.messageInputLiveChatLandscape.text.isNullOrEmpty()) {  //채팅 메세지가 빈 값이 아닌 경우만 보내게
                binding.messageInputLiveChatLandscape.setText(Util.BadWordsFilterToHeart(this,binding.messageInputLiveChatLandscape.text.toString()))  //보내기 전에 BadWords 적용
                //내가 보낸 메세지임으로 메세지와 함께 isMineChat true로 보냄.
                sendChatting(
                        LiveChatMessageModel(
                                userId = account.userId,
                                content = binding.messageInputLiveChatLandscape.text.toString(),
                                isMineChat = true,
                                clientTs = System.currentTimeMillis(),
                                senderImage = account.profileUrl,
                                senderNickName = account.userName
                        )
                )
            }
            binding.messageInputLiveChatLandscape.text?.clear()
            inputMessageNow = ""
            binding.messageInputLiveChatNoKeyboard.text=""
            hideSystemUI()  //채팅 보낸 후 SystemUI 제거
        }
        //세로모드 스크롤 위로 했을 때 아래로 내려주는 버튼
        bottomSheetBinding.scrollToBottomBtnChat.setOnClickListener {
            isScrollEnd = true
            bottomSheetBinding.rcyLiveChattingContent.smoothScrollToPosition(liveChattingMessageList.lastIndex)//가장 마지막 인덱스로 스크롤
        }

        //가로모드 스크롤 위로 했을 때 아래로 내려주는 버튼
        binding.scrollToBottomBtnChatLandscape.setOnClickListener{
            isScrollEndLandscape = true
            binding.rcyLiveChattingContentLandscape.smoothScrollToPosition(liveChattingMessageList.lastIndex)//가장 마지막 인덱스로 스크롤
        }

    }


    // TODO: 2022/01/02 일단 뷰동작만 넣어줌.
    //체팅 보낼때  동작
    private fun sendChatting(liveStreamChatMessageModel: LiveChatMessageModel){

        liveChattingMessageList.add(liveStreamChatMessageModel)
        liveStreamingPortraitChatAdapter.getLiveStreamMessageList(liveStreamChatMessageModel)
        liveStreamingLandScapeChatAdapter.getLiveStreamMessageList(liveStreamChatMessageModel)
        bottomSheetBinding.rcyLiveChattingContent.smoothScrollToPosition(liveChattingMessageList.lastIndex)//가장 마지막 인덱스로 스크롤
        binding.rcyLiveChattingContentLandscape.smoothScrollToPosition(liveChattingMessageList.lastIndex)//가장 마지막 인덱스로 스크롤
        bottomSheetBinding.messageInputLiveChat.apply {
            text?.clear()
            this.clearFocus()
        }
        binding.messageInputLiveChatLandscape.apply {
            text?.clear()
            this.clearFocus()
        }

        //라이브 소켓 연결되었을 경우  메세지 보냄.
        if(liveSocketManager.isSocketConnected){
            liveSocketManager.socket?.emit(
                LiveSocketManager.LIVE_SEND_MESSAGES,
                JSONObject()
                    .put("cmd", LiveSocketManager.LIVE_SEND_MESSAGES)
                    .put("seq", liveSocketManager.sequenceNum)
                    .put("live_id", streamListModel.id)
                    .put("client_ts",liveStreamChatMessageModel.clientTs)
                    .put("content_type", LiveSocketManager.LIVE_CHAT_TYPE_TEXT)
                    .put("content", liveStreamChatMessageModel.content)
            )
        }

        isScrollEnd = true
        isScrollEndLandscape = true
        binding.footerLiveChatLandscape.visibility= View.GONE
        Util.hideSoftKeyboard(this,bottomSheetBinding.messageInputLiveChat)
        Util.hideSoftKeyboard(this,binding.messageInputLiveChatLandscape)
    }

    // 채팅 뷰 visible setting
    private fun changeChatVisibility(isChatVisible:Boolean){
        //채팅 visible 상태일떄
            Logger.v("aaaaaa ->$isChatVisible isfullscreen ->$isFullScreenMode")
        if(isChatVisible){

            if(isFullScreenMode){//가로모드면  가로모드 세로모드 전부 처리해주고, 세로모드면 어차피 가로모드 전환시 처리되니까  세로모드것만 처리해줌.
                binding.landscapeLiveChatGroup.visibility = View.VISIBLE//가로모드 채팅 보여줌
                binding.footerLiveChatLandscape.visibility = View.GONE
                controlBinding.btnChatShow.visibility = View.VISIBLE
                controlBinding.btnChatHide.visibility = View.GONE
                liveChattingBottomBehavior.state = BottomSheetBehavior.STATE_EXPANDED//세로모드 채팅뷰 올림
            }else{
                liveChattingBottomBehavior.state = BottomSheetBehavior.STATE_EXPANDED//세로모드 채팅뷰 올림
            }
        }else{//채팅 invisible 상태일때
            if(isFullScreenMode){
                binding.landscapeLiveChatGroup.visibility = View.GONE
                controlBinding.btnChatShow.visibility = View.GONE
                controlBinding.btnChatHide.visibility = View.VISIBLE
                binding.footerLiveChatLandscape.visibility = View.GONE
                liveChattingBottomBehavior.state = BottomSheetBehavior.STATE_HIDDEN//세로모드 채팅뷰 내림
            }else{
                liveChattingBottomBehavior.state = BottomSheetBehavior.STATE_HIDDEN//세로모드 채팅뷰 올림
            }
        }
    }

    private fun setRecyclerViewScrollListener() {
        //세로모드
        bottomSheetBinding.rcyLiveChattingContent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)


                visibleItemCount =
                    (bottomSheetBinding.rcyLiveChattingContent.layoutManager as LinearLayoutManager).childCount
                totalItemCount = (bottomSheetBinding.rcyLiveChattingContent.layoutManager as LinearLayoutManager).itemCount
                pastVisibleItems =
                    (bottomSheetBinding.rcyLiveChattingContent.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                lastComplete =
                    (bottomSheetBinding.rcyLiveChattingContent.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                isScrollEnd = visibleItemCount + pastVisibleItems >= totalItemCount

                //스크롤 위치에 따라 스크롤 내려주는 이미지 버튼 visible
                if(!isScrollEnd && !isShowKeyboard){
                    bottomSheetBinding.scrollToBottomBtnChat.visibility = View.VISIBLE
                }
                else{
                    bottomSheetBinding.scrollToBottomBtnChat.visibility = View.GONE
                }
            }

        })


        //가로모드
        binding.rcyLiveChattingContentLandscape.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                visibleItemCountLandscape =
                        (binding.rcyLiveChattingContentLandscape.layoutManager as LinearLayoutManager).childCount
                totalItemCountLandscape = (binding.rcyLiveChattingContentLandscape.layoutManager as LinearLayoutManager).itemCount
                pastVisibleItemsLandscape =
                        (binding.rcyLiveChattingContentLandscape.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                lastCompleteLandscape =
                        (binding.rcyLiveChattingContentLandscape.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                isScrollEndLandscape = visibleItemCountLandscape + pastVisibleItemsLandscape >= totalItemCountLandscape

                //스크롤 위치에 따라 스크롤 내려주는 이미지 버튼 visible
                if(!isScrollEndLandscape && isFullScreenMode && isChatVisible){
                    binding.scrollToBottomBtnChatLandscape.visibility = View.VISIBLE
                }
                else{
                    binding.scrollToBottomBtnChatLandscape.visibility = View.GONE
                }

            }
        })

    }


    //라이브 좋아요 하트 애니메이션뷰 동적 추가 -> 연타 효과 주기위해서..
    private fun addLikeHeartAnimation() : View{
        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.item_live_like_heart_animation,null,false)
        val likeHeartAnimationView = view.findViewById<LottieAnimationView>(R.id.lottie_like_heart)

        //하트 눌릴때마다  뷰업데이트
        heartViewUpdate()

        likeHeartAnimationView.playAnimation()
        return view
    }


    //1초마다 서버에게 알릴  사용자의 좋아요 -> 카운트를 위한 thread 실행
    private fun startLikeCountCheckThread (){
        likeCountCheckThread = thread{
            try {
                while (!likeCountCheckThread.isInterrupted){
                    sleep(1000)

                    //체크된 좋아요 누른 횟수가 1개 이상이면,  서버에  보낸다.
                    //0개 일때는 굳이 보낼 필요 없으므로...
                    if(tempStorageGivenHeart>0){
                        updateLike(streamListModel.id)//1초마다  좋아요 업데이트
                    }
                }
            }catch (e:Exception) {
                e.printStackTrace()
            }
        }
    }

    //좋아요 클릭시  update를 진행한다.
    private fun updateLike(liveId:Int?){
        if (liveId != null) {
            MainScope().launch {
                playRepository.like(
                    liveId,
                    tempStorageGivenHeart,
                    { response ->
                        if(response?.optBoolean("success",false) == true){//success true

                            //서버에 알렸으므로 좋아요 체크 카운트 다시 초기화
                            tempStorageGivenHeart = 0

                            if(response.optInt("gcode") == 7101) {//마지막 좋아요  눌렸을때   좋아요 뷰들 모두  invisible 처리 해준다.

                                Util.setPreference(this@LiveStreamingActivity,Const.KEY_LIVE_ID_LIKE_MAX+liveId,true)

                                //좋아요 최대치 넘었으므로,  좋아요 버튼들 없애줌.
                                binding.btnStreamingLikeFull.visibility = View.GONE
                                binding.btnStreamingLike.visibility = View.INVISIBLE
                                bottomSheetBinding.btnStreamingLikeLiveChat.visibility = View.INVISIBLE

                                //좋아요 가능 회수 끝났으므로 end 뷰  visible
                                if(isFullScreenMode){
                                    binding.btnStreamingLikeEndFull.visibility = View.VISIBLE
                                }else{
                                    binding.btnStreamingLikeEndFull.visibility = View.GONE
                                    binding.btnStreamingLikeEnd.visibility = View.VISIBLE
                                    bottomSheetBinding.btnStreamingLikeEndLiveChat.visibility = View.VISIBLE
                                }

                            }
                        }else{//suscess 실패
                            //좋아요 최대수 넘었을때 ( ex 5번 좋아요면  6번째일 경우) -> 만약에 뷰가 visivble되어있는 경우(로컬 체크 값 삭제된 경우) 예외 처리 용도
                            if(response?.optInt("gcode") == 7100){
                                Util.setPreference(this@LiveStreamingActivity,Const.KEY_LIVE_ID_LIKE_MAX+liveId,true)

                                //좋아요 최대치 넘었으므로,  좋아요 버튼들 없애줌.
                                binding.btnStreamingLikeFull.visibility = View.GONE
                                binding.btnStreamingLike.visibility = View.INVISIBLE
                                bottomSheetBinding.btnStreamingLikeLiveChat.visibility = View.INVISIBLE

                                //좋아요 가능 회수 끝났으므로 end 뷰  visible
                                if(isFullScreenMode){
                                    binding.btnStreamingLikeEndFull.visibility = View.VISIBLE
                                }else{
                                    binding.btnStreamingLikeEndFull.visibility = View.GONE
                                    binding.btnStreamingLikeEnd.visibility = View.VISIBLE
                                    bottomSheetBinding.btnStreamingLikeEndLiveChat.visibility = View.VISIBLE
                                }
                            }else{
                                UtilK.showExceptionDialog(this@LiveStreamingActivity,
                                    null)
                            }
                        }
                    }, { throwable ->
                        UtilK.showExceptionDialog(this@LiveStreamingActivity,
                            null)
                    }
                )
            }
        }
    }


    //컨트롤러와 세로모드 하트 수에 + 1 해줌.
    private fun heartViewUpdate(){
        //1초마다 서버에게 알릴  좋아요 카운트
        tempStorageGivenHeart++
        streamListModel.heart = streamListModel.heart?.plus(1)
        val locale = LocaleUtil.getAppLocale(this)
        binding.tvStreamingHeart.text = NumberFormat.getNumberInstance(locale).format((streamListModel.heart?.toInt()))
        bottomSheetBinding.tvStreamingHeartLiveChat.text = NumberFormat.getNumberInstance(locale).format((streamListModel.heart?.toInt()))
        controlBinding.tvControllerStreamingHeart.text =  NumberFormat.getNumberInstance(locale).format(streamListModel.heart?.toInt())
    }

    //pause 상태 세팅
    private fun setStreamingPause(){

        if(this::simpleExoPlayer.isInitialized){
            controlBinding.btnPause.visibility = View.GONE
            controlBinding.btnPlay.visibility = View.VISIBLE
          simpleExoPlayer.playWhenReady = false
          simpleExoPlayer.removeListener(this)
        }
    }

    //play 상태 세팅
    private fun setStreamingPlay(){

        if(this::simpleExoPlayer.isInitialized){
            controlBinding.btnPause.visibility = View.VISIBLE
            controlBinding.btnPlay.visibility = View.GONE
            simpleExoPlayer.seekToDefaultPosition()
            simpleExoPlayer.prepare()
            simpleExoPlayer.playWhenReady = true
            simpleExoPlayer.addListener(this)
        }
    }

    //시스템  ui 가려주기(상태바 등등)
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//30 이상부터는 systemUiVisibility가 안먹힘으로  WindowInsetsCompat 요걸로 처리 함.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.container).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            //https://developer.android.com/training/system-ui/immersive
            val decorView = window.decorView
            decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    //시스템  ui 보여주기(상태바 등등)
    private fun showSystemUI() {//30 이상부터는 systemUiVisibility가 안먹힘으로  WindowInsetsCompat 요걸로 처리 함.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(
                window,
                binding.container
            ).show(WindowInsetsCompat.Type.systemBars())
        } else {
            val decorView = window.decorView
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }


    //exoplayer 세팅
    private fun setExoPlayer(streamingLink: String?) {


        streamingUrl = Uri.parse(streamingLink)
        loadControl = DefaultLoadControl()
        bandwidthMeter = DefaultBandwidthMeter.Builder(this).build()
        trackSelector = DefaultTrackSelector(this)
        renderersFactory = DefaultRenderersFactory(this)

        simpleExoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()

        val factory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Referer" to ServerUrl.HOST))//Referer host주소 값으로.

        //hls uri  적용 할때,   token 값이  없는 경우가 생겨서
        //이경우에는  체크하여   토큰 값을 다시  넣어  uri를 구성해준다.
        dataSourceFactory = ResolvingDataSource.Factory(
            factory,
            { dataSpec: DataSpec ->
                var uri = dataSpec.uri
                val app = if(BuildConfig.CELEB) "actor" else "idol"
                //토큰 정보가 없는 경우 403이 뜸으로  token을  넣어서 다시 uri 만든다.
                uri = if(!dataSpec.uri.toString().contains("token") && dataSpec.uri.toString().contains(app)){
                    UtilK.urlDecode(uri.buildUpon().appendQueryParameter("token",secureToken).build().toString())
                }else{
                    UtilK.urlDecode(uri.toString().replaceAfter("?token=",secureToken))
                }
                Logger.v("uri 체크  -> $uri")

                dataSpec.withUri(uri)
            })

        extendMediaItem = MediaItem.Builder().apply {
            this.setUri(streamingUrl).setMimeType(MimeTypes.APPLICATION_M3U8)
        }.build()



        hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory).setAllowChunklessPreparation(true).createMediaSource(
            extendMediaItem
        )

        binding.pvLiveStreaming.player = simpleExoPlayer
        binding.pvLiveStreaming.keepScreenOn = true
        binding.pvLiveStreaming.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        binding.pvLiveStreaming.setKeepContentOnPlayerReset(true)//player가 멈췃을때   현재  content를  보여주도록  true값 설정

        binding.pvLiveStreaming.setControllerVisibilityListener(object : PlayerView.ControllerVisibilityListener {
            override fun onVisibilityChanged(visibility: Int) {
                if(visibility == View.GONE){
                    changeFullLikeBtnWhenControllerVisible(likeBtn = binding.btnStreamingLikeEndFull,false)
                    changeFullLikeBtnWhenControllerVisible(likeBtn = binding.btnStreamingLikeFull,false)
                }else if(visibility == View.VISIBLE){
                    changeFullLikeBtnWhenControllerVisible(likeBtn =binding.btnStreamingLikeEndFull,true)
                    changeFullLikeBtnWhenControllerVisible(likeBtn =binding.btnStreamingLikeFull,true)
                }
            }

        })


        //재생화면 진입하며 로딩중일때   black screen말고 커버이미지 보이게 하기 위해서 exoplayer artwork 사용
        val artworkId = resources.getIdentifier("exo_artwork", "id", packageName)
        ivLiveStreamArtWork = binding.pvLiveStreaming.findViewById(artworkId)
        ivLiveStreamArtWork.visibility = View.VISIBLE//visible해줘야지 보임.

        //맨처음 hls track 시작하기전에는  라이브 모델의 imageurl을  보여준다.
        if(isTrackFirst){
          mGlideRequestManager
            ?.load(streamListModel.imageUrl)
            ?.skipMemoryCache(true)
            ?.into(ivLiveStreamArtWork)
        }


        //햔재 exoplayer audio 특성 세팅
        audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

        //audio focus true를 줘서  현재 라이브 스트리밍 audio에 포커스가 가도록 처리
        simpleExoPlayer.setAudioAttributes(audioAttributes,true)

        simpleExoPlayer.setMediaSource(hlsMediaSource)
        simpleExoPlayer.prepare()
        simpleExoPlayer.playWhenReady = true
        simpleExoPlayer.addListener(this)

    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)

        //중간에 라이브 종료되거나 멈췄을때  아래 조건을 타므로
        //이때  player를  release하고  다시  player를 넣어준다.
        //이유는  이렇게 되었을때  404요청을 계속하고  error timer 동작이 안되서  release하고 다시 player set 해줌.
        if(!isPlaying && simpleExoPlayer.playbackState == ExoPlayer.STATE_BUFFERING){
            simpleExoPlayer.release()
            setExoPlayer(streamingLink)
        }else if(isPlaying){//play하는 경우에는  errortimer repeat count를 0으로 초기화해줌.
            errorTimerRepeatCount = 0
        }
    }


    //전체화면 좋아요의 경우 controller 보여줌 여부에 따라
    //위치가 달라져야 하므로  아래와 같이 구성 .
    //end 와 full 모두 적용이어서 아래처럼  만듬.
    private fun changeFullLikeBtnWhenControllerVisible(likeBtn: View,isControllerVisible:Boolean){
        if(isControllerVisible){//컨트롤러가 보일때
            likeBtn.apply {
                this.updateLayoutParams<ConstraintLayout.LayoutParams> { verticalBias = 0.9f }
                UtilK.setMargins(this,
                    left = 0,
                    top = 0,
                    right = 0,
                    bottom = Util.convertDpToPixel(this@LiveStreamingActivity,47f).toInt()
                )
            }
        }else{//안보일떄
            likeBtn.apply {
                this.updateLayoutParams<ConstraintLayout.LayoutParams> { verticalBias = 0.95f }
                UtilK.setMargins(this,0,0,0,0)
            }
        }
    }

    //비디오 해상도  바뀔때
    override fun onVideoSizeChanged(videoSize: VideoSize) {
        super.onVideoSizeChanged(videoSize)
        Logger.v("video size change -> height " + videoSize.height + " width" + videoSize.width)
    }


    override fun onTracksChanged(tracks: Tracks) {
        super.onTracksChanged(tracks)

        if(isTrackFirst){//true값이면 맨처음 화면 들어왔을때  hls실행될때이므로, 이때  지원가능 해상도 목록을 받아와서 모델 리스트에 넣어준다.

            isTrackFirst = false
            //track이 change 된 이벤트가 올때,  hls 연결 된것이 확인되므로, 아래와 같이 진행
            //현재  해상도 목록 받아오기 위해  exoplayer 의 manifest를  가져옴.
            val manifest  = simpleExoPlayer.currentManifest
            val hlsManifest: HlsManifest = manifest as HlsManifest
            resolutionList = ArrayList()

            //manifeset에서  지원 해상도 뽑아서 차례대로 넣어줌.
            val variants = hlsManifest.multivariantPlaylist.variants
            for(i in 0 until variants.size){

                if(variants[i].format.height>=0){//해상도 -으로 오는 경우 있는데 그경우는 제외해줌.
                    Logger.v("resolution ->"+variants[i].format.height)
                    resolutionList.add(
                        LiveResolutionModel(
                        width = variants[i].format.width,
                        height = variants[i].format.height,
                        isAutoResolution = false,
                        isSelected = false)
                    )
                }
            }

            //해상도 값 받아오면 height 기준으로 높은순에서 낮은순으로 정렬 해준다.
            resolutionList.sortByDescending{ it.height }


            //데이터 절약 모드 + wifi 연결 안되었을때
            if(Util.getPreferenceBool(this, Const.PREF_DATA_SAVING, false) &&
                !InternetConnectivityManager.getInstance(this).isWifiConnected){

                 //데이터 절약모드이므로, 오토의 경우는 맨위로 올려주지만, 선택 여부는 false로 해준다.
                resolutionList.add(0,
                    LiveResolutionModel(isAutoResolution = true, isSelected = false)
                )

                //가장 낮은 해상도
                val lowestResolutionElement = resolutionList.last()
                lowestResolutionElement.apply { this.isSelected = true }//가장낮은 해상도를 선택한걸로 체크해줌.

                //데이터 절약 모드이므로, 처음에 auto로 잡혀있는 해상도를 가장 낮은 해상도의 값으로 적용해준다.
                trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setMaxVideoSize(lowestResolutionElement.width, lowestResolutionElement.height)
                    .setMinVideoSize(lowestResolutionElement.width, lowestResolutionElement.height)
                )

            }else{
                //위에서 정렬이 끝나고  맨 처음 값에  auto 를 넣어줌.
                //auto 용 기본 값 넣어줌 처음엔 auto가  select로 들어감.
                resolutionList.add(0,
                    LiveResolutionModel(isAutoResolution = true, isSelected = true)
                )
            }


        }else{//처음 들어온 경우가 아니면, 해상도 모델 리스트에서 selected된 모델을 찾아  해당 해상도의 height 와 Width를 적용해준다.

            val resolution = resolutionList.find { it.isSelected }

            //auto 해상도일때  video size 적용된거 모두 clear
            if(resolution?.isAutoResolution == true){

                trackSelector.setParameters(trackSelector.buildUponParameters().clearVideoSizeConstraints())
            }else{//해상도 지정일때는  지정된 해상도 값을  max size로 넣어준다.

                if (resolution != null) {
                    trackSelector.setParameters(trackSelector.buildUponParameters()
                        .setMaxVideoSize(resolution.width, resolution.height)
                        .setMinVideoSize(resolution.width, resolution.height)
                    )
                }
            }
        }

    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        super.onPlayerErrorChanged(error)
        //exoplayer  에러 났을때, 에러카운트 안하고 있으면  error Timer 실행
        if(!isErrorCounting){
            isErrorCounting = true
            playErrorTimer ()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        //exoplayer  에러 났을때, 에러카운트 안하고 있으면  error Timer 실행
        if(!isErrorCounting){
            isErrorCounting = true
            playErrorTimer ()
        }
    }

    //eror timer 실행
    //5초 주기로  count한뒤 재송출 요청하고,
    //이렇게  1분동안 (errorTimerRepeatCount =12)까지 해본다음에 안되면 다 멈추고 play버튼 나오게 해준다.
    //play버튼 다시 누르면  위 로직 다시 재 실행
    private fun playErrorTimer (){
        errorTimer = thread{
            try {
                errorTimerRepeatCount ++

                //5초 주기로  12번씩 하면 play버튼 보여주고 exoplayer 다시 실행할 준비 함.
                if(errorTimerRepeatCount >= 12){
                    runOnUiThread {
                        binding.progressBarGroup.visibility = View.GONE
                        controlBinding.btnPlay.visibility = View.VISIBLE
                        simpleExoPlayer.release()
                    }
                    errorTimerStop()
                }

                while (true){
                    sleep(800)//1초 간격으로
                    errorTimerCount++
                    if(errorTimerCount == 5){//총 5초가되면  엑소플레이어 다시 실행 시도.
                        errorTimerCount = 0//값은 다시  0으로 초기화
                        runOnUiThread {
                            replayPlayer()//재송출  요청
                        }
                        break
                    }
                }

                //에러 카운트 하고 있는지 여부 false 줘서 에러 나오면 다시 error timer 실행하게 해줌.
                isErrorCounting = false
                errorTimerStop()

            }catch (e:Exception) {
                errorTimerStop()
                e.printStackTrace()
            }
        }
    }


    //엑소플레이어 다시 실행 시도
    private fun replayPlayer(){
        if(this::simpleExoPlayer.isInitialized){
            simpleExoPlayer.seekToDefaultPosition()
            simpleExoPlayer.prepare()
            simpleExoPlayer.playWhenReady = true
            simpleExoPlayer.addListener(this)
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

        //버퍼링 걸릴때, 프로그래스바 보여주기
        if (playbackState == Player.STATE_BUFFERING) {
            binding.progressBarGroup.visibility = View.VISIBLE
            if(isErrorCounting){
                binding.tvLiveStalled.visibility = View.VISIBLE
            }else{
                binding.tvLiveStalled.visibility = View.GONE
            }

        } else if (playbackState == Player.STATE_READY) {
            binding.progressBarGroup.visibility = View.GONE
        }
    }


    //세로모드일때  ui 보여주기
    private fun showPortraitUi() {
        //세로모드 될 때 가모드에 있던 message를 inputMessageNow에 저장.
        inputMessageNow = binding.messageInputLiveChatLandscape.text.toString()
        bottomSheetBinding.messageInputLiveChat.setText(inputMessageNow)

        binding.scrollViewLiveInfo.visibility = View.VISIBLE
        binding.btnStreamingLikeFull.visibility = View.GONE
        binding.btnStreamingLikeEndFull.visibility = View.GONE

        controlBinding.liveLandscapeControllerGroup.visibility = View.GONE

        //가로모드 채팅 view들 visible 처리
        controlBinding.btnChatHide.visibility = View.GONE
        controlBinding.btnChatShow.visibility = View.GONE

        //가로모드  채팅  관련 뷰 그룹 visisble 처리
        binding.landscapeLiveChatGroup.visibility = View.GONE

        //세로모드 올 때 채팅 인덱스 맨 아래를 보여주니까 gone 처리
        bottomSheetBinding.scrollToBottomBtnChat.visibility = View.GONE

        //가로모드에서 세로모드 될 때 scroll newState값이 늦게 오면 보여서 GONE처리
        binding.scrollToBottomBtnChatLandscape.visibility = View.GONE

        //가로모드 댓글 입력 창도 gone처리 .
        binding.footerLiveChatLandscape.visibility =View.GONE

        //가로모드때 키보드가 열려있으면 세로모드 변환할때  닫아줌.
        Util.hideSoftKeyboard(this,binding.messageInputLiveChatLandscape)

        //라이브 좋아요 버튼 + 라이브 채팅 세로모드 좋아요 버튼 visisble 처리
        setLikeButtonVisibleStatus(binding.btnStreamingLike,binding.btnStreamingLikeEnd,streamListModel.id)
        setLikeButtonVisibleStatus(bottomSheetBinding.btnStreamingLikeLiveChat,bottomSheetBinding.btnStreamingLikeEndLiveChat,streamListModel.id)

        //상태바등이 hide된 상태므로 다시 보야준다.
        showSystemUI()

        //parent뷰와의 bottom to bottom 관계는 다시 없애고
        //엑소 플레이어뷰의 비율을  16:9로 맞춰준다.
        constraintSet.clone(binding.container)
        constraintSet.setDimensionRatio(binding.pvLiveStreaming.id, "16:9")
        constraintSet.clear(R.id.pv_live_streaming, ConstraintSet.BOTTOM)
        constraintSet.applyTo(binding.container)

        //orientation portrait 지정
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        isFullScreenMode = false
    }

    //라이브 채팅 화면에서 뒤로가기나, x버튼 눌렀을떄
    //기존 editext에 적어놨던 채팅 내용 없애주고, 키보드 숨김 처리 해준다.
    private fun liveChatScreenReset(){
        if( isKeyboardVisible(rootView = binding.container)){ //세로모드 채팅 키보드 열려있는데 x누름
            Util.hideSoftKeyboard(this,bottomSheetBinding.messageInputLiveChat)
            bottomSheetBinding.messageInputLiveChat.text?.clear()
            bottomSheetBinding.messageInputLiveChat.clearFocus()
            Handler(Looper.getMainLooper()).postDelayed({
                liveChattingBottomBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }, BOTTOM_SHEET_HIDE_DELAY_MILL)
        }else{  //세로모드 채팅 키보드 내려있는데 x누름
            bottomSheetBinding.messageInputLiveChat.text?.clear()
            bottomSheetBinding.messageInputLiveChat.clearFocus()
            liveChattingBottomBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
        inputMessageNow = ""
        isChatVisible = false
    }

    private fun isKeyboardVisible(rootView: View) =
        ViewCompat.getRootWindowInsets(rootView)!!.isVisible(WindowInsetsCompat.Type.ime())

    //가로모드일때 ui들 보여주기
    private fun showLandScapeUi() {
        //가로모드 될 때 세로모드에 있던 message를 inputMessageNow에 저장. 그리고 가로모드 message 둘다에 inputMessage 저장
        inputMessageNow = bottomSheetBinding.messageInputLiveChat.text.toString()
        binding.messageInputLiveChatNoKeyboard.text = inputMessageNow
        binding.messageInputLiveChatLandscape.setText(inputMessageNow)


        // TODO: 2021/10/31 여기 일단  textview만 넣어놨는데 정보 들어가는부분이라서 ui 다시 세팅
        binding.scrollViewLiveInfo.visibility = View.GONE

        //라이브 좋아요 버튼 visisble 처리
        setLikeButtonVisibleStatus(binding.btnStreamingLikeFull,binding.btnStreamingLikeEndFull,streamListModel.id)

        controlBinding.liveLandscapeControllerGroup.visibility = View.VISIBLE

        //가로모드  채팅  관련 뷰 그룹 visisble 처리 -> 채팅 여부 보여짐 값에따라
        //visible 처리 해준다.
        changeChatVisibility(isChatVisible)

        //세로모드때 키보드가 열려있으면 가로모드 변환할때  닫아줌.
        Util.hideSoftKeyboard(this,bottomSheetBinding.messageInputLiveChat)

        //시스템 ui는  hide
        hideSystemUI()

        //가로모드 되면,  exoplayerview 16:9 비율로 넣어놔서  넘어가는 경우가 발생함.
        //그래서 가로모드때는  bottom to bottom 관계를 parent로 묶어 화면에 맞춰줌.
        constraintSet.clone(binding.container)
        constraintSet.setDimensionRatio(binding.pvLiveStreaming.id, null)
        constraintSet.connect(
            R.id.pv_live_streaming,
            ConstraintSet.BOTTOM,
            R.id.container,
            ConstraintSet.BOTTOM,
            0
        )
        constraintSet.applyTo(binding.container)

        //엑티비티 orientation landscape으로
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        isFullScreenMode = true
    }


    override fun onBackPressed() {
        when {

            //채팅 창이 올라와져 있는 경우 백버튼 누르면, collapse시켜준다. 전체화면이 아닐 때, 전체화면이면 일단 새로전환이 우선임.
            liveChattingBottomBehavior.state == BottomSheetBehavior.STATE_EXPANDED && !isFullScreenMode -> {
                liveChatScreenReset()
            }

            isFullScreenMode -> {//전체 모드일떄는 뒤로가기 누르면, 세로화면인 경우로 다시 돌림.
                showPortraitUi()
                isExitButtonClicked = true
                isFullButtonClicked = false
            }


            else -> {// 채팅 화면 안올라와져 있고, 전체화면 아니면 뒤로가기 실행

                //라이브 socket disconnect처리
                liveSocketManager.disconnectSocket()
                liveSocketManager.destroyInstance()

                //좋아요 체크 스레드 정지
                likeCountCheckThread.interrupt()

                //라이브 시간 카운트 타이머 정지
                countTimer.interrupt()

                //에러 타이머 종료
                errorTimerStop()

                try {
                    //뒤로가기 눌렀을때는 완전히  플레이  종료이므로,  exoplayer release 처리 해준다.
                    simpleExoPlayer.release()
                } catch (e: Exception) {
                    //혹시나 simpleExoPlayer initalize 안된 중에 뒤로가기하면  null point나므로 예외처리 추가
                    e.printStackTrace()
                }

                super.onBackPressed()
            }
        }
    }


    //라이브 화면 데이터 업데이트용  소켓리스너
    private fun setLiveSocketEvent(){

        liveSocketManager.setUpdateLiveListener(object : LiveSocketManager.UpDateLiveListener{

            //auth fail 나면  알림
            override fun broadCastAuthFailed() {

                //authfail뜨면 서버에서  auth 가 사라진 상황일수 있으므로,
                //다시 api를 불러  update해주고,  sendauth를 불러준다.(api는 가장 간단하게 쓰는  userself를 사용함)
                //그냥 sendauth를 불러주면  서버에 auth가 없어서,  계속 fail뜸
                accountManager.fetchUserInfo(this@LiveStreamingActivity, {
                    liveSocketManager.sendAuth()
                })
            }

            override fun receiveChatMessage(liveStreamChatMessageModel: LiveChatMessageModel) {
               runOnUiThread {


                   //받은 메세지가   이미  채팅 리스트에 있는 경우 -> 삭제 또는 신고 횟수 0이상일때  들어옴.
                   if(liveChattingMessageList.any { it.serverTs == liveStreamChatMessageModel.serverTs }
                       && (liveStreamChatMessageModel.deleted == true || liveStreamChatMessageModel.isReported)
                   ){

                       //삭제인경우는  deleted true를  report 한 경우는 isReported true 적용
                       //삭제, 신고 메세지 중  삭제메세지를 우선적으로  적용해줌.
                       if(liveStreamChatMessageModel.deleted == true){
                           //deleted값을 true로 바꿔준다.
                           liveChattingMessageList.find { it.serverTs == liveStreamChatMessageModel.serverTs }?.deleted = true
                       }else if(liveStreamChatMessageModel.isReported){
                           liveChattingMessageList.find { it.serverTs == liveStreamChatMessageModel.serverTs }?.isReported = true
                       }

                       //리스트 업데이트
                       liveStreamingLandScapeChatAdapter.getLiveStreamAllMessageList(liveChattingMessageList)
                       liveStreamingPortraitChatAdapter.getLiveStreamAllMessageList(liveChattingMessageList)

                   }else{//삭제 외 일반적인 receive 메세지

                       //내가 보낸 메세지이고, clientts가 같은 경우는 이미  리스트에 해당 메세지가 존재한것이므로, 뷰 업데이트 안해주고 server ts만 업데이트 해줌.
                       //내가 보낸경우 receive오는데 얘는 처리 안해주고,  맨처음 들어올떄 30개는 처리해주기 위함. -> 맨처음 들어올때는 기존 채팅들이  없을테니까
                       // it.clientTs == liveStreamChatMessageModel.clientTs 이게 false로 나옴.
                       if((liveStreamChatMessageModel.userId == account?.userId
                                       && liveChattingMessageList.any { it.clientTs == liveStreamChatMessageModel.clientTs })
                       ){
                           liveChattingMessageList.find { it.clientTs == liveStreamChatMessageModel.clientTs }?.serverTs  = liveStreamChatMessageModel.serverTs
                           liveStreamingLandScapeChatAdapter.getLiveStreamAllMessageList(liveChattingMessageList)
                           liveStreamingPortraitChatAdapter.getLiveStreamAllMessageList(liveChattingMessageList)

                       }else{

                           //맨처음 30개 받아온 예들은  내가 보낸 메세지들의 경우는 isMineChat정보가 없으므로,
                           //isMineChat true값을 줘서  닉네임 색 빨갛게 나오게해줌.
                           if(liveStreamChatMessageModel.userId == account?.userId){
                               liveStreamChatMessageModel.isMineChat = true
                           }


                           if(!(liveChattingMessageList.any { it.serverTs == liveStreamChatMessageModel.serverTs && it.userId == liveStreamChatMessageModel.userId})){
                               liveChattingMessageList.add(liveStreamChatMessageModel)
                               liveStreamingPortraitChatAdapter.getLiveStreamMessageList(liveStreamChatMessageModel)
                               liveStreamingLandScapeChatAdapter.getLiveStreamMessageList(liveStreamChatMessageModel)
                           }

                           if(isScrollEnd){//scroll 마지막일때는 받아오면 항상 맨아래로
                               bottomSheetBinding.rcyLiveChattingContent.smoothScrollToPosition(liveStreamingPortraitChatAdapter.itemCount)//가장 마지막 인덱스로 스크롤
                           }

                           if(isScrollEndLandscape){
                               binding.rcyLiveChattingContentLandscape.smoothScrollToPosition(liveStreamingLandScapeChatAdapter.itemCount)//가장 마지막 인덱스로 스크롤
                           }
                       }
                   }
               }
            }


            //5초 주기로  라이브 정보 받아와서 model값 변경해주고
            //뷰 업데이트 실행
            override fun updateLiveInfo(updatedLiveInfo: JSONObject) {

                try {
                    runOnUiThread {

                        //모델 값 변경
                        streamListModel.heart.apply {
                            //현재 하트수가 ->  update수보다 같거나 작으면 업데이트 해줌. 하트수가 더크면  그냥 납둬줌.
                           if(this != null && this <= updatedLiveInfo.getLong("heart")) {
                               streamListModel.heart = updatedLiveInfo.getLong("heart")
                           }
                        }
                        streamListModel.views = updatedLiveInfo.getLong("views")
                        streamListModel.totalViews = updatedLiveInfo.getLong("total_views")
                        streamListModel.maxViews = updatedLiveInfo.getLong("max_views")

                        //조회수 적용
                        streamListModel.totalViews.apply {
                            binding.tvStreamingHits.text =
                                NumberFormat.getNumberInstance(Locale.getDefault())
                                    .format(this)//컴마 적용
                            bottomSheetBinding.tvStreamingHitsLiveChat.text =
                                NumberFormat.getNumberInstance(Locale.getDefault())
                                    .format(this)//라이브 채팅 세로모드쪽 조회수
                            controlBinding.tvControllerStreamingHits.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
                        }

                        //하트수 적용
                        streamListModel.heart.apply {
                            binding.tvStreamingHeart.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)

                            //라이브 채팅 세로모드쪽 하트수
                            bottomSheetBinding.tvStreamingHeartLiveChat.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
                            controlBinding.tvControllerStreamingHeart.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
                        }

                        //동접자수 적용
                        streamListModel.views.apply {
                            binding.tvStreamingUsers.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)

                            //라이브 채팅 세로모드쪽 동접자수
                            bottomSheetBinding.tvStreamingUsersLiveChat.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
                            controlBinding.tvControllerStreamingUsers.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
                        }

                        //최대 동접자수 적용
                        streamListModel.maxViews.apply {
                            binding.tvStreamingUsers2.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)

                            //라이브 채팅 세로모드쪽 최대 동접자수
                            bottomSheetBinding.tvStreamingUsers2LiveChat.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
                            controlBinding.tvControllerStreamingUsers2.text =
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
                        }

                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }



            //라이브 토큰  업데이트
            override fun updateLiveToken(updatedToken: JSONObject) {

                //securetoken ->소켓으로 받아온  toaken으로  새롭게 갈아끼워줌.
                secureToken = updatedToken.getString("content")

            }

            //라이브 시스템 command 알림.
            override fun broadCastSystemCommand(type: String, liveId: Int?) {
                    when (type) {
                        "LEAVE_LIVE" -> {
                            //라이브 아이디 같으면 현재 방 종료이므로  팝업 띄우고 종료 처리 -> 확인 누르면  이전 화면으로 돌아감.
                            if (streamListModel.id == liveId) {
                                runOnUiThread {
                                    //라이브 종료 오면,  플레이어 Release 시켜줌.
                                    if (this@LiveStreamingActivity::simpleExoPlayer.isInitialized) {
                                        simpleExoPlayer.release()
                                    }

                                    Util.showDefaultIdolDialogWithBtn1(
                                        this@LiveStreamingActivity,
                                        null,
                                        this@LiveStreamingActivity.getString(R.string.live_end)
                                    ) { view ->

                                        try {

                                            //신고처리했던  라이브 채팅 리스트도 날려줌,
                                            Util.removePreference(this@LiveStreamingActivity,KEY_SHARED_LIVE_REPORT_LIST+liveId)

                                            //라이브 socket disconnect처리
                                            liveSocketManager.disconnectSocket()
                                            liveSocketManager.destroyInstance()

                                            //좋아요 체크 스레드 정지
                                            likeCountCheckThread.interrupt()

                                            //라이브 시간 카운트 타이머 정지
                                            countTimer.interrupt()

                                            //에러 타이머 종료
                                            errorTimerStop()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        Util.closeIdolDialog()
                                        finish()
                                    }

                                }
                            }
                        }//LEAVE_LIVE 일떄 end
                    }//when절 end
            }//broadCastSystemCommand  end

        })



    }

    override fun onPause() {
        super.onPause()
        Logger.v("실행됨 onSaveInstanceState")
        setStreamingPause()

        try {//해드셋 동작관련  브로드 케스트  unregister
            this.unregisterReceiver(broadCastHeadSetWork)
        }catch (e:Exception){
            e.printStackTrace()
        }

        //화면 회전  리스너 비활성화
        orientationEventListener.disable()
    }

    companion object {

        //키보드 올라와 있는 상태일때 채팅창 내리는 경우 delay되는 시간
        const val BOTTOM_SHEET_HIDE_DELAY_MILL = 120L

        const val KEY_IS_FUll_SCREEN = "isFullScreenMode"
        const val KEY_RESOLUTION_LIST = "resoultion_list"
        const val KEY_LIVE_MODEL = "live_model"
        const val KEY_TRACK_FIRST = "isTrackFirst"
        const val KEY_IS_CHAT_VISIBLE = "isChatVisible"
        const val KEY_LIVE_CHAT_LIST = "live_chat_list"

        const val HEADSET_UNPLUGED = 0

        const val PARAM_LIVE_STREAM_LIST_MODEL = "live_stream_list_model"

        const val MENU_COPY = 1//복사
        const val MENU_DELETE = 2//삭제
        const val MENU_REPORT = 3//신고


        @JvmStatic
        fun createIntent(context: Context, liveStreamListModel: LiveStreamListModel?): Intent {

            val intent = Intent(context, LiveStreamingActivity::class.java)
            val args = Bundle()
            args.putSerializable(PARAM_LIVE_STREAM_LIST_MODEL, liveStreamListModel)
            intent.putExtras(args)
            return intent
        }
    }
}