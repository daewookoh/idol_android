package net.ib.mn.chatting

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.text.InputFilter
import android.text.TextUtils
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnAttach
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.FeedActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.chatDb.*
import net.ib.mn.chatting.model.*
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.core.data.repository.ChatRepositoryImpl
import net.ib.mn.core.data.repository.ImagesRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ActivityChattingRoomBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.ReportDialogFragment
import net.ib.mn.dialog.ReportReasonDialogFragment
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.fragment.EmoticonFragment
import net.ib.mn.fragment.WidePhotoFragment
import net.ib.mn.model.*
import net.ib.mn.talk.MessageRemoveDialogFragment
import net.ib.mn.utils.*
import net.ib.mn.utils.GlobalVariable.TOP_STACK_ROOM_ID
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

@AndroidEntryPoint
class ChattingRoomActivity : BaseActivity(),
    BaseDialogFragment.DialogResultHandler,
    ChatMessageAdapter.OnPhotoClickListener,
    ChatMessageAdapter.ShowLastImageListener
{

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase
    @Inject
    lateinit var imagesRepository: ImagesRepository

    private var saveChatMsgForDisable =""
    private var account: IdolAccount? = null
    private var idol: IdolModel? = null
    private lateinit var glideRequestManager: RequestManager
    private var mContext: Context? = null

    //채팅방정보.
    private var menu: Menu? = null
    private var chattingInfo: ChattingInfoFragment? = null
    private var chatRoomInfoModel:ChatRoomInfoModel? = null
    // private var roomId = 0
    private var title: String? = null

    //채팅리스트
    private var chatRoomList: ChatRoomListModel? = null

    //채팅방 정보 툴바.
    lateinit var btnDelete: View
    lateinit var btnReport: View

    //현재 리사이클러뷰 스크롤이  맨아래 있는지 여부 체
    private var isScrollEnd = false

    //스크롤  맨밑 체크를 위한  value들
    private  var pastVisibleItems: Int =0
    private  var visibleItemCount: Int  =0
    private  var totalItemCount: Int=0
    //보이는 아이템중 가장 마지막 포지션
    private var lastComplete:Int =0

    //현재방  삭제 메세지를 받는 경우 true값을 주어  방삭제 팝업 확인 눌렀을때  finish처리를 한다.
    private var isRoomDeletedMsg = false

    private var firstMsg:MessageModel? = null
    //Socket
//    private lateinit var socket: Socket
//    private var isSocketConnected: Boolean = false

    //채팅관련 변수.
    private var messages = ArrayList<MessageModel>()
    private lateinit var mChatMessageAdapter: ChatMessageAdapter

    //리포트 사유적는 다이얼로그 프래그먼트
    private lateinit var reportReasonDialogFragment:ReportReasonDialogFragment

    //SequenceNumber
//    private var squenceNum = 2

    //채팅방 스크롤 관련 변수.
    private var shouldScrollToBottom: Boolean = true
    private var shouldShowScrollToBottomButton: Boolean = true
    private var isUserScrolling: Boolean = false

    //채팅방 사진.
    private var rawImage: String? = ""
    private val rawLinkImage = ""
    private var binImage: ByteArray? = null

    // 사진원본
    private var originSrcUri: Uri? = null
    private var originSrcWidth = 0
    private var originSrcHeight = 0

    //채팅방 유저정보 리스트.
    private var roomMembers = CopyOnWriteArrayList<ChatMembersModel>()

    //유저정보 익명방일떄.
    private var userId:Int?=null
    private var userNickname:String?=null
    private var isAnonymity:String?=null

    //삭제관련 변수.
    private var lastClickedMessagePosition = -1

    //페이징.
    private var limit: Int = 50
    private var offset = 0

    //requestRepeatMessage 카운트.
    private var endTs:Long = 0L
    private var startTs:Long = 0L

    private var endTs12:Long = 0L
    private var startTs12:Long = 0L

    //새 메세지 토스트 가 떴을때  해당  메세지를 보낸 유저의  userid를 체크 하기 위한 변수
    private var checkNewMessageToastMemberId = -1
    private var chatReport:Boolean = false

    private var isEmoticon : Boolean = false

    private lateinit var chatMembersInstance : ChatMembersList

    private lateinit var binding: ActivityChattingRoomBinding
    @Inject
    lateinit var chatRepository: ChatRepositoryImpl
    @Inject
    lateinit var videoAdUtil: VideoAdUtil

    // lateinit propery 초기화가 된 후 소켓 연결하기 위함
    private val _adapterReadyFlow = MutableStateFlow<Boolean?>(null)
    val adapterReadyFlow: StateFlow<Boolean?> = _adapterReadyFlow

    private fun sendMessageFailed(message: MessageModel?) {
        Thread {
            //10초후에 셋해줌.
            Thread.sleep(10000)

            //메시지 가지고옴. 10초간 딜레이가 있어서 setChatMessage에 쓰레드에 안넣어도됨.
            ChatMessageList.getInstance(this).getMessage(message){ returnMessage ->
                if (returnMessage != null) {
                    if (!returnMessage.status) {
                        returnMessage.statusFailed = true//true면 진짜 실패한거니까 업뎃해줍니다.
                        returnMessage.serverTs = returnMessage.clientTs //실패했으면 serverTs는 clientTs로 바꿔줌.
                        ChatMessageList.getInstance(this).updateStatusFailed(returnMessage) { count ->
                            if(count > 0){
                                runOnUiThread {
                                    Util.log("idoltalkRoom::sendMessage Failed complete")
                                    Util.closeProgress()// 프로그래스 돌고있으면 close 시켜줌
                                    mChatMessageAdapter.sendFailedMessage(returnMessage.clientTs)
                                }
                            } else{
                                Util.log("idoltalkRoom::sendMessage Failed Exception")
                            }
                        }
                    }
                }
            }
        }.start()
    }


    override fun onPause() {
        super.onPause()
        Util.log("idoltalkRoom::onPause")
    }

    override fun onResume() {
        super.onResume()

        if (account == null) account = IdolAccount.getAccount(this)
        Util.log("idoltalkRoom::::onResume ChattingRoom")
        //상위 BaseActivity에서 roomId가 변경될수 있어서 여기서 다시한번 변경.
        socketManager = SocketManager.getInstance(this, TOP_STACK_ROOM_ID, userId)

        //키보드 show 여부 감지후, scroll 이  맨아래라면 키보드가 올라가도  bottom 을 유지하게함.
        KeyboardVisibilityUtil(window, onShowKeyboard = {
            Logger.v("감지감지감지 -> ")
            if (isScrollEnd) {
                scrollToBottom()
            }
        }, onHideKeyboard = {})

        if(socketManager?.socket?.connected() != true){
            Util.log("idoltalkRoom::::소켓연결안되있어서 소켓불러주기.")
            socketManager?.socket?.on(Socket.EVENT_CONNECT, onConnect)
        }else{
            Util.log("idoltalkRoom::::소켓연결 되어있네.")
            if(::mChatMessageAdapter.isInitialized) {
                setSocket()
            } else {
                lifecycleScope.launch {
                    adapterReadyFlow.filterNotNull().first()
                    setSocket()
                }
            }
        }


        if(!Const.CHATTING_IS_PAUSE){
            //현재 방의  notification이  status 에  있으면  삭제해준다. 이건 연결과 상관없이 무조건 삭제되게...
            UtilK.deleteChatNotification(TOP_STACK_ROOM_ID,this)
        }
    }

    private val onConnect = Emitter.Listener {
        Util.log("idoltalkRoom::socket onConnect")
        setSocket()

        //sendAuth이제 안쓰므로 onConnect되면 바로 리스너 등록.
        if (!socketManager?.socket?.hasListeners(Const.CHAT_AUTH_COMPLETE)!!)
            socketManager?.socket?.on(Const.CHAT_AUTH_COMPLETE, socketManager?.onAuthComplete)
        if (!socketManager?.socket?.hasListeners(Const.CHAT_AUTH_FAILED)!!) {
            socketManager?.socket?.on(Const.CHAT_AUTH_FAILED, socketManager?.onAuthFailed)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Util.log("idoltalk::onDestroy")
        //TODO 현재는 테스트중 여기서말고 나가기 버튼눌렀을때 disconnect할것.
        messages.clear()

        //Const.TOP_STACK_ROOM_ID = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chatting_room)

        setupEdgeToEdgeForChat()

        Util.log("idoltalkRoom::onCreate ChattingRoom")

        account = IdolAccount.getAccount(this)
        //idol = intent.extras!!.get(PARAM_IDOL) as IdolModel
        binding.viewComment.inputComment.hint = ""

        //키보드 높이 리사이즈
        UtilK.resizeEmoticonKeyBoardHeight(this, binding.clRoot,binding.rlEmoticon)

        Const.IS_CHAT_ACTIVITY_FIRST_RUNNING = true
        Const.OG_REQUEST_COUNT_CHECK =0
        TOP_STACK_ROOM_ID = intent.extras!!.getInt(PARAM_CHAT_ROOM_ID)

        isAnonymity = intent.extras!!.getString(PARAM_CHAT_ROOM_ANONI)
        title = intent.extras!!.getString(PARAM_CHAT_ROOM_TITLE)

        //Logger.v("idol::${idol!!.getId()} roomId::${roomId}")
        mContext = this
        glideRequestManager = Glide.with(this)
        chatMembersInstance = ChatMembersList.getInstance(this)

        //userId 넣어주기.
        if (isAnonymity == "Y") {
            Util.log("idoltalkRoom::익명방입니다. $isAnonymity")
            userId = intent.extras!!.getInt(PARAM_ANONI_USER_ID)
            userNickname = intent.extras!!.getString(PARAM_ANONI_USER_NICKNAME)
        } else {
            Util.log("idoltalkRoom::닉네임 방입니다. $isAnonymity")
            userId = account!!.userId
        }


        if(!Const.CHATTING_IS_PAUSE){
            //현재 방의  notification이  status 에  있으면  삭제해준다. 이건 연결과 상관없이 무조건 삭제되게...
            UtilK.deleteChatNotification(TOP_STACK_ROOM_ID,this)
        }

        //채팅방 정보 toolbar.
        setChatRoomToolBar()
        checkChatReport()

        //메시지 어댑터 세팅.
        binding.messageViewChat.apply {
            layoutManager = LinearLayoutManagerWrapper(this@ChattingRoomActivity, LinearLayoutManager.VERTICAL, false).apply {
                //역순정렬인데 xml에 추가해도됨.
                stackFromEnd = false//위부터 쌓여서 내려오게
            }
            itemAnimator = null
        }

//        setSocket()
        //채팅방 정보.
        lifecycleScope.launch(Dispatchers.IO) {
            val fetchedRoomInfo = ChatRoomInfoList.getInstance(this@ChattingRoomActivity)
                .getChatRoomInfo(TOP_STACK_ROOM_ID)
            val fetchedRoomMembers = ChatMembersList.getInstance(this@ChattingRoomActivity)
                .getChatMemberList(TOP_STACK_ROOM_ID)

            withContext(Dispatchers.Main) {
                if (fetchedRoomInfo != null && !fetchedRoomMembers.isEmpty()) {
                    Util.log("idoltalkRoom:: roomInfo and roomMembers is not null")
                    try {
                        chattingInfo = ChattingInfoFragment.newInstance(
                            chatRoomInfoModel!!,
                            fetchedRoomMembers,
                            myRoleStatus(fetchedRoomMembers)
                        )
                        supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.drawer_menu_chat, chattingInfo!!)
                            .commit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    //현재 로컬에있는 유저가 방장인지 아닌지 확인해서 신고, 방삭제 아이콘 보여줌 여부 결정.
                    //기본방일땐 신고, 방삭제 못하게!!(방장이없으므로)
                    if (fetchedRoomInfo.isDefault == "Y") {
                        btnDelete.visibility = View.GONE
                        btnReport.visibility = View.GONE
                    } else { //기본방이 아닐때 삭제,신고 보여줌여부 표시.
                        for (i in 0 until fetchedRoomMembers.size) {
                            if (fetchedRoomMembers[i].role == "O") {
                                Util.log("idoltalkRoom::방장 ${fetchedRoomMembers[i].id}")
                                if (fetchedRoomMembers[i].id == userId) {
                                    btnDelete.visibility = View.VISIBLE
                                    btnReport.visibility = View.GONE
                                } else {
                                    btnDelete.visibility = View.GONE
                                    btnReport.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
            }

            //로컬에 저장된 메시지부터 보여주도록 한다
            //페이징 적용 (50개씩 보여줌 바꾸고 싶으면 limit 변경).
            val messageListInstance = ChatMessageList.getInstance(this@ChattingRoomActivity)
            val fetchedMessages =
                messageListInstance.fetchMessages(TOP_STACK_ROOM_ID, limit, offset)
            val fetchedMessageCount = messageListInstance.getMessageCount(TOP_STACK_ROOM_ID)
            messages.clear()
            val needRemoveList: ArrayList<MessageModel> =
                ArrayList()//db에 혹시나 잘못들어간  status false인 내가보낸 메세지들을 담아서 지워준다.

            for (element in fetchedMessages) {
                if (element.clientTs == element.serverTs && element.userId == userId && !element.status && !element.isFirstJoinMsg) {
                    needRemoveList.add(element)
                }
            }
            fetchedMessages.removeAll(needRemoveList.toSet())

            messages.addAll(fetchedMessages)
            messages.sortBy { data -> data.serverTs }
            if (fetchedMessageCount != null) {
                if (limit < fetchedMessageCount) {//메세지 카운트가  처음 limit 보다 높을때  ->  첫번째  메세지를  찾아서  삭제 해준다. -> 첫번째 페이징에서 안보여야됨
                    val firmsg = messages.find { it.isFirstJoinMsg }
                    if (firmsg != null) {
                        firstMsg = firmsg
                        messages.remove(firmsg)
                    }
                }
            }

            Logger.v("실행됨 ->> ")
            requestMessage()
            withContext(Dispatchers.Main) {
                mChatMessageAdapter = ChatMessageAdapter(
                    mContext!!,
                    glideRequestManager,
                    messages,
                    account!!,
                    fetchedRoomMembers,
                    isAnonymity,
                    userId,
                    UtilK.getScreenWidth(this@ChattingRoomActivity),
                    this@ChattingRoomActivity
                ) { model: MessageModel, v: View, position: Int ->
                    onPhotoClick(model, v, position)
                }

                binding.messageViewChat.adapter = mChatMessageAdapter
                Util.log("idoltalkRoom::localMessages size${messages.size}")

                //유저 피드로 이동한다.
                mChatMessageAdapter.setOnUserProfileClickListener(object :
                    ChatMessageAdapter.OnUserProfileClickListener {
                    override fun onProfileClick(chatMemberInfo: ChatMembersModel?) {
                        val userModel =
                            UserModel()//피드에 넘길때 UserModel형태로 넘기기위해  chatmember 모델에서  가져온 값들을  넣어서  usermodel 객체를   생성한다
                        var idolModel: IdolModel? = IdolModel()
                        Logger.v(chatMemberInfo.toString())

                        lifecycleScope.launch(Dispatchers.IO) {
                            chatMemberInfo?.let { memberInfo ->
                                idolModel = getIdolByIdUseCase(memberInfo.most)
                                    .mapDataResource { it?.toPresentation() }
                                    .awaitOrThrow()

                                if (!memberInfo.deleted) {
                                    userModel.imageUrl = memberInfo.imageUrl
                                    userModel.id = memberInfo.id
                                    userModel.nickname = memberInfo.nickname
                                    userModel.level = memberInfo.level
                                    userModel.most =
                                        idolModel//프로필 클릭된 유저의 최애의 경우는  현재  나의  최애와 같으므 내 최애를 넣는다.

                                    withContext(Dispatchers.Main) {
                                        startActivity(
                                            FeedActivity.createIntent(
                                                this@ChattingRoomActivity,
                                                userModel
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                })

                binding.messageViewChat.scrollToPosition(messages.size - 1)
                isScrollEnd = true

                _adapterReadyFlow.value = true
            }

            //채팅방 참여자 리스트.
            getChatRoomMembers()
        }

        binding.drawerLayoutChat.addDrawerListener(object : SimpleDrawerListener() {
            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_SETTLING) {
                    if (!binding.drawerLayoutChat.isDrawerOpen(GravityCompat.END)) {
                        Util.hideSoftKeyboard(this@ChattingRoomActivity,binding.drawerLayoutChat)
                        Logger.v("열림")
                    } else {

                        Logger.v("취소됨 ")
                        // Drawer started closing
                    }
                }
            }
        })

        //최대 200글자까지 가능.
        binding.viewComment.inputComment.apply {
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(TALK_MAX_LENGTH))
            setOnFocusChangeListener { v, hasFocus ->
                if(!hasFocus) Util.hideSoftKeyboard(this@ChattingRoomActivity,v)
            }
        }

        binding.scrollToBottomBtnChat.setOnClickListener {
            isScrollEnd =true
            scrollToBottom()
        }
        binding.newMessageWrapperChat.setOnClickListener {
            isScrollEnd =true
            scrollToBottom()
        }

        setRecyclerViewScrollListener()
        setRecyclerViewLayoutChangeListener()
        registerForContextMenu(binding.messageViewChat)
    }

    private fun setupEdgeToEdgeForChat() {
        val root = binding.drawerLayoutChat
        val toolbarContainer = binding.chatToolbar.root   // 위 XML의 FrameLayout
        val footer = binding.footerChat
        val emojiPanel = binding.rlEmoticon
        val recycler = binding.messageViewChat
        val scrollToBottom = binding.scrollToBottomBtnWrapperChat
        val newMessageToast = binding.newMessageWrapperChat
        val langChip = binding.llLanguageWrapperChat

        // 0) Drawer가 상태바 배경을 별도로 그리지 않게
        root.fitsSystemWindows = false
        binding.drawerLayoutChat.setStatusBarBackground(null)

        // 1) 컨텐츠를 시스템바 아래까지
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2) 상태바 아이콘 밝기 (라이트/다크 모드에 맞게)
        val night = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, root).isAppearanceLightStatusBars = !night

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            // 상단 inset을 툴바 컨테이너의 paddingTop으로 소비
            toolbarContainer.setPadding(
                toolbarContainer.paddingLeft, statusTop,
                toolbarContainer.paddingRight, toolbarContainer.paddingBottom
            )

            val footerH = if (footer.height != 0) footer.height else footer.measuredHeight

            if (emojiPanel.isVisible) {
                footer.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = 0 }
                emojiPanel.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = navBottom }
                recycler.setPadding(recycler.paddingLeft, recycler.paddingTop, recycler.paddingRight, 0)

                val extra = footerH + navBottom + dp(16)
                scrollToBottom.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = extra }
                newMessageToast.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = extra }
                langChip.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = extra }
            } else {
                val bottomInset = maxOf(navBottom, imeBottom)
                footer.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = bottomInset }
                emojiPanel.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = 0 }
                recycler.setPadding(recycler.paddingLeft, recycler.paddingTop, recycler.paddingRight, 0)

                val extra = footerH + bottomInset + dp(16)
                scrollToBottom.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = extra }
                newMessageToast.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = extra }
                langChip.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = extra }
            }

            if (footerH == 0) footer.doOnLayout { ViewCompat.requestApplyInsets(root) }
            insets
        }

        // 오른쪽 드로어 컨텐츠도 인셋
        ViewCompat.setOnApplyWindowInsetsListener(binding.menuScreenChat) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, status.top, v.paddingRight, nav.bottom)
            insets
        }

        if (root.isAttachedToWindow) ViewCompat.requestApplyInsets(root)
        else root.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    private fun View.updatePadding(
        left: Int = paddingLeft, top: Int = paddingTop,
        right: Int = paddingRight, bottom: Int = paddingBottom
    ) = setPadding(left, top, right, bottom)

    private fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun View.dp(v: Int) = context.dp(v)

    private fun setChatRoomToolBar() {
        // drawer 메뉴 toolbar
        binding.toolbarMenuChat.setNavigationIcon(R.drawable.ic_action_forward)
        binding.toolbarMenuChat.setNavigationOnClickListener { v: View? -> binding.drawerLayoutChat.closeDrawers() }

        binding.toolbarMenuChat.inflateMenu(R.menu.chatting_info_menu)
        btnDelete = binding.toolbarMenuChat.findViewById(R.id.menu_delete_room)
        btnReport = binding.toolbarMenuChat.findViewById(R.id.menu_report_room)
        btnDelete.setOnClickListener {
            //TODO 방 삭제 팝업 , API
            Util.showDefaultIdolDialogWithBtn2(this,
                getString(R.string.remove),
                getString(R.string.chat_room_delete_popup), {
                    Util.closeIdolDialog()
                    deleteRoom()
                }, {
                    Util.closeIdolDialog()
                })

        }

        //채팅방 신고 클릭 이벤트
        btnReport.setOnClickListener {
            if(chatReport){
                Util.showDefaultIdolDialogWithBtn1(this@ChattingRoomActivity,null,resources.getString(R.string.chat_report_error_2401) ){Util.closeIdolDialog()}
            }
            else {
                val report = ReportDialogFragment.getInstance(true)
                val reportHeart = ConfigModel.getInstance(this).reportHeart
                val color = "#" + Integer.toHexString(
                    ContextCompat.getColor(this,
                        R.color.main)).substring(2)
                var msg = String.format(
                    resources.getString(R.string.chat_room_report),
                    "<FONT color=$color>$reportHeart</FONT>")

                msg = msg.replace("\n", "<br>");
                val spanned = HtmlCompat.fromHtml(msg,
                    HtmlCompat.FROM_HTML_MODE_LEGACY)

                report.setMessage(spanned)
                report.setActivityRequestCode(RequestCode.CHAT_ROOM_REPORT.value)
                report.show(supportFragmentManager, "feed_report")
            }
        }

        setSupportActionBar(binding.chatToolbar.toolbarChat)

        val actionBar = supportActionBar

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }

        supportActionBar?.title = title
    }

    //들어왔을 때 신고된 채팅방 체크
    private fun checkChatReport(){
        MainScope().launch {
            chatRepository.isReportChatRoom(
                TOP_STACK_ROOM_ID,
                account!!.userId.toLong(),
                { response ->
                    if (response.optBoolean("success")) {
                        chatReport = response.optBoolean("reported")
                    }
                },
                { throwable ->
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                }
            )
        }
    }


    //방삭제 API.
    private fun deleteRoom() {
        Util.showProgress(this,false)
        MainScope().launch {
            chatRepository.deleteChatRoom(
                TOP_STACK_ROOM_ID,
                { response ->
                    Logger.v("delete ->>>>>>>>$response")

                    if (response?.getBoolean("success")!!) {

                        //채팅방 삭제니까  해당방안의  메세지및  chatting room list에서 방 데이터를  삭제해준다.
                        ChatRoomInfoList.getInstance(this@ChattingRoomActivity).deleteRoomInfo(TOP_STACK_ROOM_ID){
                            ChatRoomList.getInstance(this@ChattingRoomActivity).deleteRoom(TOP_STACK_ROOM_ID) {
                                ChatMessageList.getInstance(this@ChattingRoomActivity).deleteChatRoomMessages(roomId = TOP_STACK_ROOM_ID)
                            }
                        }
                    }else{
                        Toast.makeText(this@ChattingRoomActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                        Util.closeProgress()
                    }
                },
                { throwable ->
                    Toast.makeText(this@ChattingRoomActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                        .show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                    Util.closeProgress()
                }
            )
        }
    }

    //채팅방 멤버 1명 update(나가거나, 들어올 때)
    private fun getChatRoomMemberUpdate(userId : Int){
        MainScope().launch {
            chatRepository.getChatMember(
                TOP_STACK_ROOM_ID,
                userId.toLong(),
                { response ->
                    if (response.optBoolean("success")) {
                        updateChatMembers(response)
                    }
                },
                { throwable ->
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                }
            )
        }
    }

    //채팅방 참여 멤버 가져오기 API.
    private fun getChatRoomMembers() {
        val roomMembersTemp=CopyOnWriteArrayList<ChatMembersModel>()
        MainScope().launch {
            chatRepository.getChatMembers(
                TOP_STACK_ROOM_ID,
                { response ->
                    if (response.optBoolean("success")) {
                        val gson = IdolGson.getInstance()
                        val array = response.getJSONArray("users")

                        roomMembers.clear()

                        for (i in 0 until array.length()) {
                            val model = gson.fromJson(array.getJSONObject(i).toString(), ChatMembersModel::class.java)
                            model.roomId = TOP_STACK_ROOM_ID
                            model.accountId = account!!.userId
                            roomMembers.add(model)
                        }
                        //roomMembers -> 서버에서 주는 방 멤버. ChatMembersList -> 내가 처음 들어왔을 때 가지고 있던 멤버.
                        Logger.v("idoltalkRoom  members-> $roomMembers")
                        roomMembersTemp.addAll(roomMembers)
                        lifecycleScope.launch(Dispatchers.IO) lifecycle@ {
                            val chatMemberListInstance = ChatMembersList.getInstance(this@ChattingRoomActivity)

                            chatMemberListInstance.setChatMembers(roomMembers)

                            val fetchedMembers = chatMemberListInstance.getChatMemberList(
                                TOP_STACK_ROOM_ID)
                            Logger.v("idoltalkRoom roomMembersModel -> $fetchedMembers")

                            val deletedChatMembersModel: CopyOnWriteArrayList<ChatMembersModel> = CopyOnWriteArrayList()
                            fetchedMembers.forEachIndexed { index, chatMembersModel ->
                                //any 하나라도 틀린게(!=) 있으면 무조건 true로 리턴이 되므로 같은걸로(==) 비교해서 false가 하나라도 나오면 그걸로 비교를 해준다.
                                val modelExist = roomMembers.any{ it.id == chatMembersModel.id }
                                if(!modelExist){    //채팅방에 없는 유저인 경우 deletedChatMembersModel이란 ArrayList에 쌓아준다.
                                    chatMembersModel.deleted = true
                                    chatMembersModel.role = "N"
                                    roomMembers.add(chatMembersModel)    //서버에서 온 사용자에 내가 가지고있던 사용자(나간사람) add
                                    deletedChatMembersModel.add(chatMembersModel)
                                }
                                else {
                                    if (chatMembersModel.id == checkNewMessageToastMemberId) {
                                        withContext(Dispatchers.Main) {
                                            binding.newMessageNicknameChat.text =chatMembersModel.nickname
                                            glideRequestManager
                                                .load(chatMembersModel.imageUrl)//기본 이미지로 변경
                                                .apply(RequestOptions.circleCropTransform())
                                                .error(Util.noProfileImage(chatMembersModel.id))
                                                .fallback(Util.noProfileImage(chatMembersModel.id))
                                                .placeholder(Util.noProfileImage(chatMembersModel.id))
                                                .into(binding.newMessageProfileImgChat)
                                        }
                                    }
                                }
                                if(fetchedMembers.size-1 == index){   //deletedChatMembersModel에 다 쌓은 이후 삭제한 유저
                                    chatMemberListInstance.updateDeletedMemberList(deletedChatMembersModel,TOP_STACK_ROOM_ID)
                                    Logger.v("idoltalkRoom:: 디비 업데이트 완료")
                                    setDeletedChatMembers(fetchedMembers)
                                }
                            }

                            if (!this@ChattingRoomActivity::mChatMessageAdapter.isInitialized) {
                                return@lifecycle
                            }

                            withContext(Dispatchers.Main) {
                                mChatMessageAdapter.setMembers(roomMembers)
                            }
                        }

                        //방 정보 가져오기. getChatRoomMember 콜백이 더늦게 올 수 있어서 안에다 넣음..
                        getChatRoomInfo(roomMembersTemp)

                    } else {
                        Toast.makeText(this@ChattingRoomActivity, R.string.error_abnormal_default, Toast.LENGTH_SHORT).show()
                    }
                }, { throwable ->
                    Toast.makeText(this@ChattingRoomActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                }
            )
        }
    }

    private fun setDeletedChatMembers(roomMembersModel : CopyOnWriteArrayList<ChatMembersModel>){

        for(i in 0 until roomMembersModel.size){
            //탈퇴한 유저의 id 와 새 메세지  토스트 유저의 아이디가 같을때  토스트 메세지 삭제된 유저로 처리해준다.
            if (roomMembersModel[i].id == checkNewMessageToastMemberId) {
                runOnUiThread {
                    binding.newMessageNicknameChat.text = this@ChattingRoomActivity.getString(R.string.chat_leave_user)
                    glideRequestManager
                        .load(Util.noProfileImage(roomMembersModel[i].id))//기본 이미지로 변경
                        .apply(RequestOptions.circleCropTransform())
                        .error(Util.noProfileImage(roomMembersModel[i].id))
                        .fallback(Util.noProfileImage(roomMembersModel[i].id))
                        .placeholder(Util.noProfileImage(roomMembersModel[i].id))
                        .into(binding.newMessageProfileImgChat)
                }
            }
        }
    }
    //채팅방 정보요청.
    private fun getChatRoomInfo(roomMembers: CopyOnWriteArrayList<ChatMembersModel>) {

        Logger.v("TOP_STACK_ROOM_ID-> $TOP_STACK_ROOM_ID")
        MainScope().launch {
            chatRepository.getChatRoomInfo(
                TOP_STACK_ROOM_ID,
                { response ->
                    if (response?.optBoolean("success")!!) {
                        Logger.v("response real-> $response")
                        val gson = IdolGson.getInstance()
                        chatRoomInfoModel = gson.fromJson(response.getJSONObject("room").toString(), ChatRoomInfoModel::class.java)
                        Logger.v("response-> $chatRoomInfoModel")
                        chatRoomInfoModel?.accountId = account!!.userId
                        lifecycleScope.launch(Dispatchers.IO) {
                            val chatRoomInfoInstance =
                                ChatRoomInfoList.getInstance(this@ChattingRoomActivity)
                            chatRoomInfoInstance.setChatRoomInfo(chatRoomInfoModel)
                            Util.log("idoltalkRoom::채팅방 정보 넣기 완료.")

                            //현재 로컬에있는 유저가 방장인지 아닌지 확인해서 신고, 방삭제 아이콘 보여줌 여부 결정.
                            //기본방일땐 신고, 방삭제 못하게!!(방장이없으므로)
                            withContext(Dispatchers.Main) {
                                if (chatRoomInfoModel?.isDefault == "Y") {
                                    btnDelete.visibility = View.GONE
                                    btnReport.visibility = View.GONE
                                } else { //기본방이 아닐때 삭제,신고 보여줌여부 표시.
                                    btnDelete.visibility = View.GONE
                                    btnReport.visibility = View.VISIBLE
                                    for (i in 0 until roomMembers.size) {
                                        if (roomMembers[i].role == "O" && roomMembers[i].id == userId) {    //방장이고, 방장이 나라면
                                            Util.log("idoltalkRoom::방장 ${roomMembers[i].id}")
                                            btnDelete.visibility = View.VISIBLE
                                            btnReport.visibility = View.GONE
                                            break
                                        }
                                    }
                                }

                                //채팅방정보는 Null값이 되면안되므로 Api호출후에 instance생성.
                                try {
                                    chattingInfo = ChattingInfoFragment.newInstance(
                                        chatRoomInfoModel!!,
                                        roomMembers,
                                        myRoleStatus(roomMembers)
                                    )
                                    supportFragmentManager
                                        .beginTransaction()
                                        .replace(R.id.drawer_menu_chat, chattingInfo!!)
                                        .commit()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                            }
                        }
                    }else{//채팅방 정보 받아오기  success false 값일때  서버 msg 보내주고 리스트 화면으로 돌아간다.
                        val errorMessage= response.get("msg").toString()
                        Util.showDefaultIdolDialogWithBtn1(this@ChattingRoomActivity, null, errorMessage) {
                            setResult(Const.CHATTING_LIST_RESET)
                            finish()
                        }
                    }
                },
                { throwable ->
                    Toast.makeText(this@ChattingRoomActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                }
            )
        }
    }

    //이미지 보내기 API.
    private fun sendImage(binImage: ByteArray) {
        Util.showProgress(this, false)
        lifecycleScope.launch {
            imagesRepository.uploadImage(
                binImage,
                IMAGE_SIZE.toString(),
                { response ->
                    if (response.optBoolean("success")) {
                        //TODO:: 모델을 그냥 만들까 말까 고민.
                        Util.log("idoltalkRoom::Image was sended")
                        Util.log("idoltalkRoom::Image response -> $response")
                        val imageUrl = response.getString("image_url")
                        val thumbnailUrl = response.getString("thumbnail_url")
                        val umjjalUrl = response.getString("umjjal_url")
                        val thumbHeight = response.optInt("thumb_height")
                        val thumbWidth = response.optInt("thumb_width")

                        //success시 소켓에 넣어서 보내준다. API호출 -> 소켓 emit.
                        sendImageSocket(imageUrl, thumbnailUrl, umjjalUrl, thumbHeight, thumbWidth)
                    } else {
                        //TODO:: goce 오류 관련 코드 넣기.
                        Toast.makeText(this@ChattingRoomActivity, "데이터를 가져오는데 실패하셨습니다.", Toast.LENGTH_SHORT).show()
                    }
                }, {
                    Toast.makeText(this@ChattingRoomActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private class PagerAdapter(
        fm: FragmentActivity,
        emoFragList: ArrayList<EmoticonFragment>
    ) : FragmentStateAdapter(fm) {

        val fragList = emoFragList

        override fun getItemCount(): Int {
            return fragList.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragList[position]
        }
    }

    override fun onBackPressed() {
        if (binding.rlEmoticon.isVisible) { //뒤로가기 눌렀을때 이모티콘창 있을경우엔 뒤로가지말고 이모티콘창 닫아줌.
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            binding.rlEmoticon.visibility = View.GONE
            binding.drawerLayoutChat.post { ViewCompat.requestApplyInsets(binding.drawerLayoutChat) }
        } else { //화면 닫아줌.
            super.onBackPressed()
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode==RequestCode.CHAT_ROOM_REPORT.value){
            if(resultCode == ResultCode.REPORTED.value){
                reportReasonDialogFragment = ReportReasonDialogFragment.getInstance(ReportReasonDialogFragment.CHATTING_REPORT, TOP_STACK_ROOM_ID,null)
                reportReasonDialogFragment.show(supportFragmentManager, "report_reason")
            }
        } else if (requestCode == REQUEST_DELETE && resultCode == BaseDialogFragment.RESULT_OK) {// 메시지 삭제.
            try {

                Util.log("idoltalkRoom::삭제버튼이 눌렸습니다. ")
                if (socketManager?.socket?.connected() == true) {
                    //연결됐고, 완전실패하고, 삭제안된거는 emit안보내고 그냥삭제.
                    //statusFailed가 true인것만( Time out 10초지난것들).
                    if (messages[lastClickedMessagePosition].statusFailed && !messages[lastClickedMessagePosition].status) {
                        ChatMessageList.getInstance(this).deleteChatRoomMessage(messages[lastClickedMessagePosition]) {
                            runOnUiThread {
                                Util.log("idoltalkRoom:: 소켓연결이 됐고 전송실패한 메시지 Delete가 완료되었습니다. ${it}")
                                if (it != null) {
                                    mChatMessageAdapter.deleteChatMessage(it.serverTs)
                                }
                            }
                        }
                    } else {
                        //연결됐고, 실패안된것들.(실패안된것들은 onReceive에서 db삭제해준다).
                        incrementSequenceNumber()
                        socketManager?.socket?.emit(Const.CHAT_REQUEST_DELETE,
                            JSONObject()
                                .put("cmd", Const.CHAT_REQUEST_DELETE)
                                .put("seq", socketManager?.squenceNum)
                                .put("room_id", TOP_STACK_ROOM_ID)
                                .put("server_ts", messages[lastClickedMessagePosition].serverTs))
                    }
                } else {
                    //만약 연결이 안되어있을경우 전송실패메시지는 지울수있게 해준다.
                    if (messages[lastClickedMessagePosition].statusFailed && !messages[lastClickedMessagePosition].status) {
                        ChatMessageList.getInstance(this).deleteChatRoomMessage(messages[lastClickedMessagePosition]) {
                            runOnUiThread {
                                Util.log("idoltalkRoom:: 소켓연결이 안됐고 전송실패한 메시지 Delete가 완료되었습니다. ${it}")
                                if (it != null) {
                                    mChatMessageAdapter.deleteChatMessage(it.serverTs)
                                }
                            }
                        }
                    }
                }
                lastClickedMessagePosition = -1
            } catch (e: Exception) {
                Toast.makeText(this, R.string.msg_error_ok, Toast.LENGTH_SHORT).show()
            }
        }else if(resultCode == ResultCode.REPORT_REASON_UPLOADED.value){//리포트 사유 적기 완료 했을때
            chatReport = true//채팅 report 신고했음 으로 값 변경
        }

    }

    //더보기 메뉴. 커뮤니티 메뉴 재사용함.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        this.menu = menu
        menuInflater.inflate(R.menu.community_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.more) {
            if (!binding.drawerLayoutChat.isDrawerOpen(Gravity.RIGHT))
                binding.drawerLayoutChat.openDrawer(Gravity.RIGHT)
            else
                binding.drawerLayoutChat.closeDrawer(Gravity.RIGHT)
        }
        return super.onOptionsItemSelected(item)
    }

    /***** socket *****/

    private fun setSocket(){
        Util.log("local TOP_STACK_ROOM_ID Chatting $TOP_STACK_ROOM_ID")
        if (socketManager?.socket?.connected() == true){
            Util.log("idoltalkRoom::onCreate socket connected")

            //이모티콘 가져오기.
            getEmoticon(this,
                rootView = binding.root,
            )

            //입력 , 전송, 사진보내기가능.
            runOnUiThread {
                enabledSend(false)
            }

            lifecycleScope.launch {
                EventBus.receiveEvent<JSONObject>(Const.CHAT_RECEIVE_MESSAGES).collect { data ->
                    onReceiveMessage(data)
                }
            }
            lifecycleScope.launch {
                EventBus.receiveEvent<JSONObject>(Const.CHAT_SYSTEM_COMMAND).collect { data ->
                    onSystemCommand(data)
                }
            }
            lifecycleScope.launch {
                EventBus.receiveEvent<JSONObject>(Const.CHAT_SYSTEM_MESSAGE).collect { data ->
                    onSystemMessage(data)
                }
            }
            lifecycleScope.launch {
                EventBus.receiveEvent<JSONObject>(Socket.EVENT_CONNECT_ERROR).collect { data ->
                    try {
                        if(!data.getString("connected").toBoolean()){
                            disabledSend()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
            lifecycleScope.launch {
                EventBus.receiveEvent<JSONObject>(Const.CHAT_AUTH_COMPLETE).collect { data ->
                    try {
                        if(data.getString("connected").toBoolean()){
                            enabledSend(true)

                            //재연결 되었을떄 상대방 메시지 다시 가져오기.
                            Const.OG_REQUEST_COUNT_CHECK = 0
                            requestMessage()

                            //커넥션 끊겼을시 멤버를 못가져올 수 있으니까 다시한번 불러준다.
                            getChatRoomMembers()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
            lifecycleScope.launch {
                EventBus.receiveEvent<ArrayList<MessageModel>>(Const.CHAT_REQUEST_OG_MESSAGE).collect { data ->
                    ogRequestProcess(data)
                }
            }
            if(!Const.IS_CHAT_ACTIVITY_FIRST_RUNNING){
                requestMessage()
            }
        }
    }

    private fun ogRequestProcess(receiveMessageList: ArrayList<MessageModel>){
        try {
            if(Const.OG_REQUEST_COUNT_CHECK<3&& receiveMessageList.any { it.isLastCount }){
                Logger.v("카운트 ->>> "+Const.OG_REQUEST_COUNT_CHECK)

                if(Const.OG_REQUEST_COUNT_CHECK ==0){
                    Const.IS_CHAT_ACTIVITY_FIRST_RUNNING =false
                }

                ++Const.OG_REQUEST_COUNT_CHECK
                if(receiveMessageList.find{ it.isLastCount } != null){
                    startTs = receiveMessageList.find{ it.isLastCount }!!.serverTs - 1
                    requestMessage()
                }
            }

            if(receiveMessageList.size>0){
                mChatMessageAdapter.setOgChatMessage(receiveMessageList.sortedBy { it.serverTs }) {
//                    offset += receiveMessageList.size
                    scrollToBottom()
                }
            }


            Logger.v("112313213ㅊ  가는 ")

        }catch (e:Exception){
            Logger.v("112313213ㅊ 나와라 얍얍얍 ->"+e.message)
            e.printStackTrace()
        }


    }


    private fun requestMessage(){

        //requestMessage전송.
        //로컬에 저장되어있는 가장 최근 server_ts를 가져옵니다.
        lifecycleScope.launch(Dispatchers.IO) {
            val lastServerTs = ChatMessageList.getInstance(this@ChattingRoomActivity).getLatestServerts(TOP_STACK_ROOM_ID)
            incrementSequenceNumber()

            if (Const.OG_REQUEST_COUNT_CHECK == 0) {
                startTs = System.currentTimeMillis()
                endTs = if (lastServerTs != null) {
                    lastServerTs + 1
                } else {//lastserverts가 없는 경우 -> message가 없는 경우에는 ->  로그인시  저장해놓은  serverts를 기준으로  메세지를  불러온다.
                    Util.getPreferenceLong(this@ChattingRoomActivity, "user_login_ts", 0L) + 1
                }
            }

            try {
                Logger.v("idoltalkRoom::request 날림 ->$TOP_STACK_ROOM_ID")
                socketManager?.socket?.emit(
                    Const.CHAT_REQUEST_MESSAGES,
                    JSONObject().put("cmd", Const.CHAT_REQUEST_MESSAGES)
                        .put("seq", socketManager?.squenceNum)
                        .put("room_id",  TOP_STACK_ROOM_ID)
                        .put("start_ts", startTs)
                        .put("end_ts", endTs)
                )
            }catch (e : Exception){
                e.printStackTrace()
            }

        }
    }

    //메시지 보내기.
    private fun sendMessage(msg: String) {
        val filteredMsg = Util.BadWordsFilterToHeart(mContext, msg)
        val containedUrl = Util.checkUrls(filteredMsg)
        val ts = System.currentTimeMillis()

        incrementSequenceNumber()

        val messageObject = JSONObject()
            .put("seq", socketManager?.squenceNum )
            .put("room_id", TOP_STACK_ROOM_ID)
            .put("user_id", userId)
            .put("client_ts", ts)
            .put("content_type", MessageModel.CHAT_TYPE_TEXT)
            .put("content", filteredMsg)

        val message = IdolGson.getInstance(false)
            .fromJson(messageObject.toString(), MessageModel::class.java)
        message.status = false
        //serverTs가 pk라 무조건 ts는 있어야됨 해당메시지가 잘가면 서버 ts로 바뀜.
        message.serverTs = ts
        message.accountId = account!!.userId

        Util.log("idoltalkRoom::Model->$message")
        runOnUiThread {
            messages.add(message)
            ++offset
            Util.log("send::${messages.size}")
            try {
                mChatMessageAdapter.notifyItemInserted(messages.size - 1)
            }catch (e:Exception) {
                e.printStackTrace()
            }
        }

        //Db에저장.(DB에저장하는건 소켓연결 안해도되게 밖으로빼줌)
        ChatMessageList.getInstance(this).setChatMessage(message){
            Util.log("idoltalkRoom::insert db(sendMessage) -> $message")
        }

        //전송실패 처리.
        sendMessageFailed(message)

        if (socketManager?.socket?.connected() == true) {

            socketManager?.socket?.emit(
                Const.CHAT_SEND_MESSAGE,
                JSONObject()
                    .put("cmd", Const.CHAT_SEND_MESSAGE)
                    .put("seq", socketManager?.squenceNum)
                    .put("room_id", TOP_STACK_ROOM_ID)
                    .put("client_ts", ts)
                    .put("content_type", MessageModel.CHAT_TYPE_TEXT)
                    .put("content", filteredMsg)
            )

        }

        scrollToBottom()
        binding.viewComment.inputComment.text?.clear()
        //Util.hideSoftKeyboard(this, binding.messageInputChat)
    }

    //이미지 url socket에 넣어보내기. (sendImage -> sendImageSocket 순으로 불림).
    private fun sendImageSocket(imageUrl: String, thumbnailUrl: String, umjjalUrl: String, thumbHeight : Int, thumbWidth : Int) {

        val ts = System.currentTimeMillis()
        incrementSequenceNumber()

        //TODO:: JSON함수 분리...
        val contentObject = if(thumbHeight == 0 || thumbWidth == 0){
            JSONObject().put("url", imageUrl)
                .put("thumbnail", thumbnailUrl)
                .put("umjjal", umjjalUrl)
        } else{
            JSONObject().put("url", imageUrl)
                .put("thumbnail", thumbnailUrl)
                .put("umjjal", umjjalUrl)
                .put("thumb_height",thumbHeight.toString())
                .put("thumb_width",thumbWidth.toString())
        }

        val messageObject = JSONObject()
            .put("seq", socketManager?.squenceNum)
            .put("room_id", TOP_STACK_ROOM_ID)
            .put("user_id", userId)
            .put("client_ts", ts)
            .put("content_type", MessageModel.CHAT_TYPE_IMAGE)
            .put("content", contentObject.toString())

        val message = IdolGson.getInstance(false)
            .fromJson(messageObject.toString(), MessageModel::class.java)
        message.status = false
        message.serverTs = ts
        message.accountId = account!!.userId

        Util.log("idoltalkRoom::Model->$message")
        runOnUiThread {
            messages.add(message)
            ++offset
            Util.log("send::${messages.size}")
            try {
                mChatMessageAdapter.sendImageSocket()
                mChatMessageAdapter.notifyItemInserted(messages.size - 1)
            }catch (e:Exception) {
                e.printStackTrace()
            }
        }

        //Db에저장.
        ChatMessageList.getInstance(this).setChatMessage(message) {
            Util.log("idoltalkRoom::insert db(sendMessage) -> $message")
        }

        //전송실패 처리.
        sendMessageFailed(message)

        if (socketManager?.socket?.connected() == true) {

            socketManager?.socket?.emit(
                Const.CHAT_SEND_MESSAGE,
                JSONObject()
                    .put("cmd", Const.CHAT_SEND_MESSAGE)
                    .put("seq", socketManager?.squenceNum)
                    .put("room_id", TOP_STACK_ROOM_ID)
                    .put("client_ts", ts)
                    .put("content_type", MessageModel.CHAT_TYPE_IMAGE)
                    .put("content", contentObject.toString().replace("\\/", "/")) // Slash 이스케이프처리.
            )
        }

        Util.closeProgress()
        scrollToBottom()
        binding.viewComment.inputComment.text?.clear()
        //Util.hideSoftKeyboard(this, binding.messageInputChat)
    }

    //이모티콘 url socket에 넣어보내기
    private fun sendEmoticonSocket(imageUrl: String, thumbnailUrl: String) {

        val ts = System.currentTimeMillis()
        incrementSequenceNumber()

        //TODO:: JSON함수 분리...
        var contentObject = JSONObject().put("url", imageUrl)
            .put("thumbnail", thumbnailUrl)

        if(isEmoticon){
            contentObject.put("is_emoticon", "true")
        }


        //하나 전송하고 전역변수 초기화
        isEmoticon = false

        val messageObject = JSONObject()
            .put("seq", socketManager?.squenceNum)
            .put("room_id", TOP_STACK_ROOM_ID)
            .put("user_id", userId)
            .put("client_ts", ts)
            .put("content_type", MessageModel.CHAT_TYPE_IMAGE)
            .put("content", contentObject.toString())

        val message = IdolGson.getInstance(false)
            .fromJson(messageObject.toString(), MessageModel::class.java)
        message.status = false
        message.serverTs = ts
        message.accountId = account!!.userId

        Util.log("idoltalkRoom::Model->$message")
        runOnUiThread {
            messages.add(message)
            ++offset
            Util.log("send::${messages.size}")
            try {
                mChatMessageAdapter.notifyItemInserted(messages.size - 1)
            }catch (e:Exception) {
                e.printStackTrace()
            }
        }

        //Db에저장.
        ChatMessageList.getInstance(this).setChatMessage(message) {
            Util.log("idoltalkRoom::insert db(sendMessage) -> $message")
        }

        //전송실패 처리.
        sendMessageFailed(message)

        if (socketManager?.socket?.connected() == true) {

            socketManager?.socket?.emit(
                Const.CHAT_SEND_MESSAGE,
                JSONObject()
                    .put("cmd", Const.CHAT_SEND_MESSAGE)
                    .put("seq", socketManager?.squenceNum)
                    .put("room_id", TOP_STACK_ROOM_ID)
                    .put("client_ts", ts)
                    .put("content_type", MessageModel.CHAT_TYPE_IMAGE)
                    .put("content", contentObject.toString().replace("\\/", "/")) // Slash 이스케이프처리.
            )
        }

        Util.closeProgress()
        scrollToBottom()
        //Util.hideSoftKeyboard(this, binding.messageInputChat)
    }


    val gson = IdolGson.getInstance(true)
    private fun onReceiveMessage(data: JSONObject){

        //현재 방의  notification이  status 에  있으면  삭제해준다. 메시지 받을때마다 지워주기.
        if(!Const.CHATTING_IS_PAUSE){
            //현재 방의  notification이  status 에  있으면  삭제해준다. 이건 연결과 상관없이 무조건 삭제되게...
            UtilK.deleteChatNotification(TOP_STACK_ROOM_ID,this)
        }

        val gson = IdolGson.getInstance(true)
        try {
            Logger.v("message check::${data}")

            val gson = IdolGson.getInstance(true)

//            Util.log("idoltalkRoom::onReceive messages size is ${messages?.size}")

            try {
                val receiveMessage = gson.fromJson<MessageModel>(data.toString(), MessageModel::class.java)

                Logger.v("message check::${receiveMessage.content}")

                if (receiveMessage.roomId == TOP_STACK_ROOM_ID) {

                    //삭제된 메시지 아닌거 구별하기.
                    if (!receiveMessage.deleted && receiveMessage.reports == null) {
                        if (receiveMessage.userId == userId) {
                            Util.log("idoltalkRoom::유저아이디가 같군요.")

                            //내 메시지 일때.
                            //update로 return값이 null이면 없는것이므로 insert 아니면 그냥 update.
                            ChatMessageList.getInstance(this).getMessage(receiveMessage) {
                                if (it == null) {

                                    if (receiveMessage.contentType == MessageModel.CHAT_TYPE_LINK) {//메세지 type이  링크 타입일떄
                                        receiveMessage.isLinkUrl = true
                                    }
                                    receiveMessage.status = true
                                    receiveMessage.isReadable = true
                                    receiveMessage.statusFailed = false
                                    receiveMessage.accountId = account!!.userId
                                    ChatMessageList.getInstance(this).setChatMessage(receiveMessage) {
                                        Util.log("idoltalkRoom::update failed and insert (onReceive) -> ${receiveMessage}")
                                        runOnUiThread {
                                            messages.add(receiveMessage)
                                            ++offset
                                            try {
                                                mChatMessageAdapter.notifyItemInserted(messages.size - 1)
                                            }catch (e:Exception) {
                                                e.printStackTrace()
                                            }
                                            scrollToBottom()
                                            Util.closeProgress()
                                        }
                                    }
                                } else {

                                    for (j in messages.indices) {
                                        if (receiveMessage.contentType == MessageModel.CHAT_TYPE_LINK) {//메세지 type이  링크 타입일떄

                                            if (receiveMessage.userId == messages[j].userId && receiveMessage.clientTs == messages[j].clientTs) {
                                                val linkContent = gson.fromJson(receiveMessage.content, ChatMsgLinkModel::class.java)

                                                if (linkContent.imageUrl != null && linkContent.title != null) {// ㅅurl 이어도 썸네일 정보 없으면 일반 text로 보이게 수정
                                                    messages[j].isLinkUrl = true
                                                    messages[j].content = receiveMessage.content
                                                    messages[j].contentType = receiveMessage.contentType

                                                } else {
                                                    messages[j].contentType = it.contentType
                                                    messages[j].isLinkUrl = false
                                                    messages[j].content = it.content
                                                }

                                                messages[j].status = true
                                                messages[j].serverTs = receiveMessage.serverTs
                                                messages[j].isReadable = true
                                                messages[j].statusFailed = false

                                                messages[j].accountId = account!!.userId
                                                runOnUiThread {
                                                    mChatMessageAdapter.notifyItemChanged(j)
                                                    if (messages.size == messages.size + 1) {
                                                        Util.closeProgress()
                                                    }
                                                }
                                                ChatMessageList.getInstance(this)
                                                    .updateLinkMessage(messages[j]) {
                                                        runOnUiThread {
                                                            scrollToBottom()
                                                        }
                                                        Util.log("idoltalkRoom::update db(onReceive) 메세지 업데이-> ${messages[j]}")
                                                    }
                                            }
                                        } else {

                                            receiveMessage.content = it.content
                                            if (receiveMessage.userId == messages[j].userId && receiveMessage.clientTs == messages[j].clientTs) {
                                                messages[j].status = true
                                                messages[j].content = receiveMessage.content
                                                messages[j].serverTs = receiveMessage.serverTs
                                                messages[j].isReadable = true
                                                messages[j].statusFailed = false
                                                messages[j].accountId = account!!.userId
                                                runOnUiThread {
                                                    mChatMessageAdapter.notifyItemChanged(j)
                                                    if (messages.size == messages.size + 1) {
                                                        Util.closeProgress()
                                                    }
                                                }

                                                ChatMessageList.getInstance(this).update(messages[j]) { count ->
                                                    if (count > 0)
                                                        Util.log("idoltalkRoom::update db(onReceive) -> ${count}")
                                                    else { //익셉션 떳을땐 음수.
                                                        Util.log("idoltalkRoom::update db failed and SQLiteException ${it}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {

                            runOnUiThread {
//                                Logger.v("RXJava onReceive  내가 보낸거 아님 ->>>> ${receiveMessage.content}")
                                //내가보낸 메시지가 아닐때.
                                val recyclerViewState = binding.messageViewChat.layoutManager?.onSaveInstanceState()

                                val message = messages.find { it.serverTs == receiveMessage.serverTs && it.userId == receiveMessage.userId }

//                                Logger.v("RXJava onReceive  messgage ->>>>" + message)
                                if (message == null) {
                                    Logger.v("메세지 이거야이거 ->>> " + receiveMessage.content)
                                    messages.add(receiveMessage)
                                    ++offset
                                    messages.sortBy { data -> data.serverTs }

                                    val firmsg = messages.find { it.isFirstJoinMsg }
                                    if (firmsg != null) {
                                        firstMsg = firmsg
                                        messages.remove(firmsg)
                                        messages.add(0, firmsg)
                                    }

                                    try {
                                        mChatMessageAdapter.notifyItemInserted(messages.size - 1)
                                    }catch (e:Exception) {
                                        e.printStackTrace()
                                    }
                                    binding.messageViewChat.layoutManager?.onRestoreInstanceState(recyclerViewState)

                                    var membersModel: ChatMembersModel? = null
                                    for (i in 0 until roomMembers.size) {
                                        if (roomMembers[i].id == receiveMessage.userId) {
                                            membersModel = roomMembers[i]
                                        }
                                    }

                                    // 화면 크기만큼 스크롤을 안했다면 메시지 왔을 때, 자동으로 최하단으로 감
                                    if (shouldScrollToBottom) {
                                        scrollToBottom()
                                    } else {
                                        when (receiveMessage.contentType) {
                                            MessageModel.CHAT_TYPE_IMAGE -> {
                                                //이모티콘, 사진 분리.
                                                if(JSONObject(receiveMessage.content).optBoolean("is_emoticon",false))
                                                    setNewMessageToast(getString(R.string.emoji), membersModel)
                                                else
                                                    setNewMessageToast(getString(R.string.chat_photo), membersModel)
                                            }
                                            MessageModel.CHAT_TYPE_TEXT -> {
                                                setNewMessageToast(receiveMessage.content, membersModel)
                                            }

                                            MessageModel.CHAT_TYPE_LINK -> {
                                                val gson = IdolGson.getInstance(true)
                                                val linkContent = gson.fromJson(receiveMessage.content, ChatMsgLinkModel::class.java)
                                                setNewMessageToast(linkContent.originalMsg.toString(), membersModel)
                                            }
                                        }
                                    }
                                } else {
                                    if (receiveMessage.isLinkUrl) {
                                        for (j in messages.indices) {
                                            if (receiveMessage.userId == messages[j].userId && receiveMessage.serverTs == messages[j].serverTs) {

                                                val linkContent = gson.fromJson(
                                                    receiveMessage.content,
                                                    ChatMsgLinkModel::class.java
                                                )
                                                if (linkContent.imageUrl != null && linkContent.title != null && receiveMessage.isLinkUrl) {// ㅅurl 이어도 썸네일 정보 없으면 일반 text로 보이게 수정
                                                    messages[j].isLinkUrl = true

                                                    messages[j].content = receiveMessage.content
                                                    messages[j].contentType =
                                                        receiveMessage.contentType

                                                } else {
                                                    messages[j].contentType =
                                                        MessageModel.CHAT_TYPE_TEXT
                                                    messages[j].isLinkUrl = false
                                                    messages[j].content =
                                                        linkContent.originalMsg.toString()
                                                }

                                                messages[j].status = true
                                                messages[j].serverTs = receiveMessage.serverTs
                                                messages[j].isReadable = true
                                                messages[j].statusFailed = false
                                                messages[j].accountId = account!!.userId
                                                runOnUiThread {
                                                    mChatMessageAdapter.notifyItemChanged(j)
                                                    if (messages.size == messages.size + 1) {
                                                        Util.closeProgress()
                                                    }
                                                    if (shouldScrollToBottom) {
                                                        scrollToBottom()
                                                    }
                                                }
                                                ChatMessageList.getInstance(this)
                                                    .updateLinkMessage(messages[j]) {
                                                        if (shouldScrollToBottom) {
                                                            runOnUiThread {
                                                                scrollToBottom()
                                                            }
                                                        }
                                                    }

                                            }
                                        }

                                    }
                                }
                            }
                        }
                    } else if (receiveMessage.deleted) {

                        val message = messages.find { it.serverTs == receiveMessage.serverTs && it.userId == receiveMessage.userId }
                        if (message == null) {

                            messages.add(receiveMessage)
                            ++offset
                            messages.sortBy { data -> data.serverTs }

                            val firmsg = messages.find { it.isFirstJoinMsg }
                            if (firmsg != null) {
                                firstMsg = firmsg
                                messages.remove(firmsg)
                                messages.add(0, firmsg)
                            }

                            runOnUiThread {
                                try {
                                    mChatMessageAdapter.notifyItemInserted(messages.size - 1)
                                }catch (e:Exception) {
                                    e.printStackTrace()
                                }
                            }


                            // 화면 크기만큼 스크롤을 안했다면 메시지 왔을 때, 자동으로 최하단으로 감
                            if (shouldScrollToBottom) {
                                scrollToBottom()
                            }
                        }


                        for (j in messages.indices) {
                            if (receiveMessage.serverTs == messages[j].serverTs && !TextUtils.isEmpty(messages[j].content) && !messages[j].deleted && receiveMessage.userId == messages[j].userId) {
                                Util.log("idoltalkRoom::Delete가 같은거 몇개??")

                                //Db deleted업데이트.
                                ChatMessageList.getInstance(this).updateChatRoomMessage(receiveMessage) {
                                    Util.log("idoltalkRoom::Delete가 완료되었습니다. $receiveMessage")
                                    runOnUiThread {
                                        //삭제된 메세지 타입을 텍스트로 바꿔주낟. -> 어차피  이미지든 텍스트든 모두 삭제된 메세지 입니다로 바뀔 예정이므로,
                                        messages[j].contentType = MessageModel.CHAT_TYPE_TEXT
                                        mChatMessageAdapter.updateChatMessage(receiveMessage.serverTs)
                                    }
                                }
                            }
                        }


                    }
//                        }

                    runOnUiThread {
                        try {
                            if (receiveMessage.reports == null) {
                                mChatMessageAdapter.notifyItemChanged(messages.size - 2)
                            }
                        }catch (e:UninitializedPropertyAccessException){
                            e.printStackTrace()
                            Logger.v("exception::AdapterException")
                        }
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: JSONException) {
            Util.log("idoltalkRoom::onReceiveMessage ERROR ${e.message}")
        }
    }



    //시스템 커맨드 수신.(커맨드마다 분기됨).
    private fun onSystemCommand(data:JSONObject){
        Util.log("idoltalkRoom::systemCommand")
        try {
//            val data = args[0] as JSONObject
            Util.log("idoltalkRoom::$data")

            Util.log("idoltalkRoom::content_type->${data.getString("content_type")}")
            Util.log("idoltalkRoom::content->${data.getString("content")}")

            val contentType = data.getString("content_type")
            val content = data.getJSONObject("content")
            val type = content.getString("type")

            Util.log("idoltalkRoom::type->${type}")

            checkSystemMessage(type, content)

        } catch (e: JSONException) {
            Util.log("idoltalkRoom::systemCommand ERROR ${e.message}")
        }

    }

    //시스템 메시지 분기.
    private fun checkSystemMessage(type: String, content: JSONObject) {

        var roomId: Int? = null
        var userId: Int? = null
        var nickname : String? = null
        var imageUrl : String? = null
        var level : Int? = null
        var message: String? = null
        var ownerId : Int? = 0

        when(type){
            //방삭제됨.
            "LEAVE_ROOM" -> {
                roomId = content.getInt("room_id")
                //room list 에서  해당방  제거 해주고,  해당방의 메세지도 제거 해준다.
                ChatRoomInfoList.getInstance(this).deleteRoomInfo(roomId){
                    ChatRoomList.getInstance(this).deleteRoom(roomId!!) {
                        ChatMessageList.getInstance(this).deleteChatRoomMessages(roomId!!)
                        if(roomId == TOP_STACK_ROOM_ID){
                            isRoomDeletedMsg =true
                        }

                        Logger.v("delete ->>>>>>>> LEAVEROOM  isROomDELETEMSG ->"+isRoomDeletedMsg)

                    }
                }
            }

            //방정보 변경됨.
            "UPDATE_ROOMINFO" -> {
                roomId = content.getInt("room_id")
                if(TOP_STACK_ROOM_ID == roomId){
                    getChatRoomMembers()
                }
            }
            //사용자 입장/퇴장이 일어남.
            "ADD_JOINS" -> {
                roomId = content.getInt("room_id")
                userId = content.getInt("user_id")
                if(TOP_STACK_ROOM_ID == roomId){
                    getChatRoomMemberUpdate(userId)
                }
            }
            "DELETE_JOINS" -> {
                roomId = content.getInt("room_id")
                userId = content.getInt("user_id")
                ownerId = content.optInt("owner_id")    //방장이 나갔을 경우 오는 방장 userId

                //Delete할 멤버 userId, roomId로찾는다.
                if (TOP_STACK_ROOM_ID != roomId) {
                    return
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val chatMembersInstance = ChatMembersList.getInstance(this@ChattingRoomActivity)

                    chatMembersInstance.updateDeletedMember(userId, TOP_STACK_ROOM_ID)

                    val fetchedChatMember = chatMembersInstance.getChatRoomMember(userId!!, roomId!!)
                    fetchedChatMember?.deleted = true
                    fetchedChatMember?.role = "N"

                    val fetchedChatMemberList = chatMembersInstance.getChatMemberList(roomId!!)

                    withContext(Dispatchers.Main) {
                        binding.newMessageNicknameChat.text =
                            this@ChattingRoomActivity.getString(R.string.chat_leave_user)
                        glideRequestManager
                            .load(Util.noProfileImage(fetchedChatMember?.id ?: 0))//기본 이미지로 변경
                            .apply(RequestOptions.circleCropTransform())
                            .error(Util.noProfileImage(fetchedChatMember?.id ?: 0))
                            .fallback(Util.noProfileImage(fetchedChatMember?.id ?: 0))
                            .placeholder(Util.noProfileImage(fetchedChatMember?.id ?: 0))
                            .into(binding.newMessageProfileImgChat)

                        mChatMessageAdapter.setMembers(fetchedChatMemberList)
                        chattingInfo = chatRoomInfoModel?.let {
                            ChattingInfoFragment.newInstance(
                                it,
                                fetchedChatMemberList,
                                myRoleStatus(fetchedChatMemberList)
                            )
                        }
                        chattingInfo?.let {
                            if (!supportFragmentManager.isStateSaved) {
                                supportFragmentManager.beginTransaction()
                                    .replace(R.id.drawer_menu_chat, it).commit()
                            }
                        }
                    }

                    if (ownerId != 0) {
                        val fetchedOwnerMember = chatMembersInstance.getChatRoomMember(
                            ownerId, roomId!!
                        )
                        fetchedOwnerMember?.role = "O"

                        chatMembersInstance.updateChatMember(
                            ownerId,
                            roomId,
                            fetchedOwnerMember ?: return@launch
                        )

                        val fetchedUpdatedChatMemberList = chatMembersInstance.getChatMemberList(roomId!!)

                        withContext(Dispatchers.Main) {
                            mChatMessageAdapter.setMembers(fetchedUpdatedChatMemberList)
                            if (ownerId == this@ChattingRoomActivity.userId) {
                                chattingInfo = ChattingInfoFragment.newInstance(
                                    chatRoomInfoModel!!,
                                    fetchedUpdatedChatMemberList,
                                    myRoleStatus(fetchedUpdatedChatMemberList)
                                )
                                if (!supportFragmentManager.isStateSaved) {
                                    supportFragmentManager.beginTransaction()
                                        .replace(R.id.drawer_menu_chat, chattingInfo!!).commit()
                                }
                                btnDelete.visibility =
                                    View.VISIBLE //방장이 됐을 경우 오른쪽 상단에 신고버튼이 아닌 삭제버튼 보이도록 변경
                                btnReport.visibility = View.GONE
                            }
                        }
                    }
                }

            }
            //사용자의 프사변경,레벨업,닉네임 변경.(UserModel만 변경해주면됨.)
            "UPDATE_USER" -> {
                userId = content.getInt("user_id")
                roomId = content.getInt("room_id")
                nickname = content.optString("nickname")
                imageUrl = content.optString("image_url")
                level = content.optInt("level")

                if (TOP_STACK_ROOM_ID != roomId) {
                    return
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val chatMembersInstance = ChatMembersList.getInstance(this@ChattingRoomActivity)
                    val fetchedMember =
                        chatMembersInstance.getChatRoomMember(userId, TOP_STACK_ROOM_ID)

                    when {

                        nickname != "" -> {
                            fetchedMember?.nickname = nickname as String
                            binding.newMessageNicknameChat.text = fetchedMember?.nickname
                        }

                        imageUrl != "" -> {
                            fetchedMember?.imageUrl = imageUrl as String
                            withContext(Dispatchers.Main) {
                                glideRequestManager
                                    .load(fetchedMember?.imageUrl)//기본 이미지로 변경
                                    .apply(RequestOptions.circleCropTransform())
                                    .error(Util.noProfileImage(fetchedMember?.id ?: 0))
                                    .fallback(Util.noProfileImage(fetchedMember?.id ?: 0))
                                    .placeholder(Util.noProfileImage(fetchedMember?.id ?: 0))
                                    .into(binding.newMessageProfileImgChat)
                            }
                        }

                        level != 0 -> {
                            fetchedMember?.level = level as Int
                        }
                    }

                    if (this@ChattingRoomActivity::mChatMessageAdapter.isInitialized) {
                        chatMembersInstance.updateChatMember(
                            userId,
                            roomId,
                            fetchedMember ?: return@launch
                        )

                        val fetchedMemberList = chatMembersInstance.getChatMemberList(roomId)
                        withContext(Dispatchers.Main) {
                            mChatMessageAdapter.setMembers(fetchedMemberList)
                        }
                    }
                }
            }
        }

    }

    //시스템 메시지 수신.(토스트 메시지, 치명적인 메시지)
    private fun onSystemMessage(data:JSONObject){
        Util.log("idoltalkRoom::systemMessage")
        try {
//            val data = args[0] as JSONObject
            Util.log("idoltalkRoom::$data")

            Util.log("idoltalkRoom::content_type->${data.getString("content_type")}")
            Util.log("idoltalkRoom::content->${data.getString("content")}")

            val contentType = data.getString("content_type")
            val content = data.getString("content")

            if (contentType == MessageModel.CHAT_TYPE_TOAST) {

                if(Util.isAppOnForeground(this)){
                    Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                }

            } else if (contentType == MessageModel.CHAT_TYPE_FATAL) {
                Logger.v("delete ->>>>>>>> fatalMSG   ->"+content)

                Util.closeProgress()

                //이전에 다른 다이얼로그가  보여지고 있다면,  close시켜준다.
                if(Util.isDialogShowing()){ Util.closeIdolDialog()}
                Util.showDefaultIdolDialogWithBtn1(this, null, content) {
                    if(isRoomDeletedMsg) {//isroomdeletemsg 는  system message 의   같은방의  leaveroom msg가  왔을때 true로 변함으로,  이경우에는 finish를 호출한다.
                        Util.closeIdolDialog()
                        setResult(Const.CHATTING_LIST_RESET)
                        finish()
                    }
                    Util.closeIdolDialog()
                }
            }

        } catch (e: JSONException) {
            Util.log("idoltalkRoom::onSystemMessage ERROR ${e.message}")
        }
    }

    //이미지 버튼 연타방지.
    private var mLastClickTime = 0L

    //메시지 버튼 연타방지.
    private var mLastMssageTime = 0L

    //입력창 , 전송버튼 활성화.
    private fun enabledSend(isAuthComPlete:Boolean){

        if(saveChatMsgForDisable.isNotEmpty()){//잠시 저장된 메세지가 있는 경우 -> 활성화시  다시 editext에 넣어준다.
            binding.viewComment.inputComment.setText(saveChatMsgForDisable)
            saveChatMsgForDisable =""//저장용 변수는 다시 비워줌.
        }

        //소켓이 연결됬으면 메세지를 보낼수있음.
        //채팅 송신.
        binding.viewComment.inputComment.hint = ""
        binding.viewComment.inputComment.isEnabled = true
        binding.viewComment.inputComment.isFocusableInTouchMode = true
        binding.viewComment.inputComment.setSelection(binding.viewComment.inputComment.length())// editext 가장 마지막에 커서를 둠.

        //백그라운드에서 돌아올땐 키보드가 다시 돌아올수있으니까 focus없애줌.
        if(binding.viewComment.inputComment.hasFocus()){
            binding.viewComment.inputComment.clearFocus()
        }

        if(isAuthComPlete){
            Util.showSoftKeyboard(this@ChattingRoomActivity, binding.viewComment.inputComment)
        }else{
            binding.viewComment.inputComment.setOnClickListener {
                Util.showSoftKeyboard(this@ChattingRoomActivity, binding.viewComment.inputComment)
            }
        }

        binding.viewComment.btnSubmit.setOnClickListener {
            if(SystemClock.elapsedRealtime() - mLastMssageTime < 300){
                return@setOnClickListener
            }
            mLastMssageTime = SystemClock.elapsedRealtime()

            if(binding.clPreview.visibility == View.VISIBLE){
                isEmoticon = true
                emoModel?.let {
                    sendEmoticonSocket(it.imageUrl, it.thumbnail)
                }
                binding.clPreview.visibility = View.GONE  //이모티콘 하나 보내고 미리보기 닫기
            }

            if (binding.viewComment.inputComment.text != null
                && binding.viewComment.inputComment.text!!.trim().isNotEmpty()) {
                sendMessage(binding.viewComment.inputComment.text.toString())
            }
        }

        //사진전송도 소켓연결되어있으면 보낼수있음.
        binding.viewComment.btnGallery.setOnClickListener {
            if(SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return@setOnClickListener
            }
            mLastClickTime = SystemClock.elapsedRealtime()
            UtilK.getPhoto(this, true)
        }

        //키보드 높이를 가져옵니다.
        var keyboardHeight = Util.getPreferenceInt(this, Const.KEYBOARD_HEIGHT, -1)

        if(keyboardHeight == -1){ //초기화 안되었다는 뜻이므로 키보드 올라오게해줌.
            binding.viewComment.inputComment.callOnClick()
        }

        //만약 키보드 높이가 없다면.
        if(keyboardHeight != -1){
            //아니면 키보드 높이 가져와서 이모티콘창 높이계산.
            val params = binding.rlEmoticon.layoutParams
            params.height = keyboardHeight
            binding.rlEmoticon.layoutParams = params
        }

        // 이모티콘 버튼 클릭
        binding.viewComment.btnEmoticon.setOnClickListener {
            lifecycleScope.launch {
                if (binding.rlEmoticon.visibility == View.GONE) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                    Util.hideSoftKeyboard(this@ChattingRoomActivity, binding.viewComment.inputComment)
                    binding.rlEmoticon.visibility = View.VISIBLE
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

                    if (isScrollEnd) {//이모티콘 올라온경우도 scroll 마지막일때  리사이클러뷰 스크롤 bottom으로 내려줌.
                        scrollToBottom()
                    }
                    if(binding.viewComment.inputComment.text.toString().isEmpty()){
                        binding.viewComment.inputComment.clearFocus()
                    }
                } else {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                    Util.showSoftKeyboard(this@ChattingRoomActivity, binding.viewComment.inputComment)
//                    delay(100)
                    binding.rlEmoticon.visibility = View.GONE
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                }
            }
        }

        //채팅 아이콘 클릭시 이모티콘 나오게.
        binding.viewComment.inputComment.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {

                val DRAWABLE_LEFT = 0
                val DRAWABLE_TOP = 1
                val DRAWABLE_RIGHT = 2
                val DRAWABLE_BOTTOM = 3

                var directionValue = binding.viewComment.inputComment.right
                var drawablesValue = DRAWABLE_RIGHT

                if(Util.isRTL(this@ChattingRoomActivity)) {//아랍어일땐 반대로 방향지정해주기.
                    directionValue = binding.viewComment.inputComment.left
                    drawablesValue = DRAWABLE_LEFT
                }

                //아이콘 오른쪽에 넣었음. 만약왼쪽이면 부등호 반대로 해주세요.
                if (event.action == MotionEvent.ACTION_UP) {
                    lifecycleScope.launch {
                        if (binding.rlEmoticon.isVisible) { //이모티콘창 올라와있고 키보드 눌렀을때.
                            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                            Util.showSoftKeyboard(this@ChattingRoomActivity, binding.viewComment.inputComment)
                            delay(100)
                            binding.rlEmoticon.visibility = View.GONE
                            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                        } else {
                            binding.viewComment.inputComment.requestFocus()
                            Util.showSoftKeyboard(this@ChattingRoomActivity, binding.viewComment.inputComment)
                        }
                    }
                }
                return false
            }
        })

        //이모티콘 미리보기 닫기.
        binding.ivPreviewClose.setOnClickListener {
            binding.clPreview.visibility = View.GONE
        }

        bindInputListener(binding.root)
    }

    //입력창 , 전송버튼 비활성화.
    private fun disabledSend(){
        if(binding.viewComment.inputComment.text.toString().isNotEmpty()){//비활성화가 계속 불리므로 메세지가 있으면 , 현재 메세지  임시저장 진행  (이러면 처음 값만 저장함)
            saveChatMsgForDisable = binding.viewComment.inputComment.text.toString()
        }
        with(binding.viewComment) {
            inputComment.text?.clear()
            inputComment.hint = getString(R.string.chat_connecting)
            inputComment.isEnabled = false
            inputComment.isFocusableInTouchMode = false
            inputComment.setOnClickListener(null)
            btnSubmit.setOnClickListener(null)
            btnGallery.setOnClickListener(null)
        }
    }



    /*** Chatting Scroll ***/
    private fun loadNextFetchMessage(){
        offset += limit
        offset = if (offset < 0) 0 else offset
        Logger.v("Paging check::Paging offset ${offset} limit ${limit}")
        lifecycleScope.launch(Dispatchers.IO) {
            val messageListInstance = ChatMessageList.getInstance(this@ChattingRoomActivity)
            val fetchedMessages = messageListInstance.fetchMessages(TOP_STACK_ROOM_ID, limit, offset)
            val fetchedMessageCount = messageListInstance.getMessageCount(TOP_STACK_ROOM_ID)
            if(fetchedMessages.size > 0){
                val needRemoveList :ArrayList<MessageModel> = ArrayList()//db에 혹시나 잘못들어간  status false인 내가보낸 메세지들을 담아서 지워준다.
                for(element in fetchedMessages){
                    messages.remove(messages.find {it.serverTs == element.serverTs && it.userId == element.userId} )
                    if(element.clientTs == element.serverTs  && element.userId == userId && !element.status  && !element.isFirstJoinMsg){
                        needRemoveList.add(element)
                    }
                }
                fetchedMessages.removeAll(needRemoveList.toSet())

                messages.addAll(0, fetchedMessages)
                messages.sortBy { data -> data.serverTs }
                if(offset < fetchedMessageCount!! && fetchedMessageCount <=offset+50){
                    messages.remove(firstMsg)
                    firstMsg?.let { messages.add(0, it) }
                }
                withContext(Dispatchers.Main) {
                    mChatMessageAdapter.notifyDataSetChanged()
                    val linearLayoutManager = binding.messageViewChat.layoutManager as LinearLayoutManager
                    linearLayoutManager.scrollToPositionWithOffset(fetchedMessages.size, 0)
                }
            }
        }
    }

    private fun scrollToBottom() {
        try {
            val count = mChatMessageAdapter.itemCount as Int
            if (count > 1) {
                binding.messageViewChat.scrollToPosition(count - 1)
            } else {
                binding.messageViewChat.scrollToPosition(0)
            }

            binding.scrollToBottomBtnWrapperChat.visibility = View.GONE
            binding.newMessageWrapperChat.visibility = View.GONE

            //새 메세지 토스트 gone 되므로  다시 default 값으로 reset
            checkNewMessageToastMemberId=-1
            shouldScrollToBottom = true
            shouldShowScrollToBottomButton = false
        }catch (e:Exception) {
            e.printStackTrace()
        }

    }

    private fun setNewMessageToast(message: String, membersModel: ChatMembersModel?) {

        if (membersModel == null) {
            return
        }
        //멤버 모델이  null 값이 아닐때
        binding.newMessageChat.text = message
        binding.newMessageNicknameChat.text = membersModel.nickname

        if (chatRoomInfoModel?.isAnonymity == "Y") {//익명방일때는 프로필 사진을 보여주지 않는다.
            binding.newMessageProfileImgChat.visibility = View.GONE
        } else {
            binding.newMessageProfileImgChat.visibility = View.VISIBLE
            glideRequestManager
                .load(membersModel.imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(membersModel.id))
                .fallback(Util.noProfileImage(membersModel.id))
                .placeholder(Util.noProfileImage(membersModel.id))
                .into(binding.newMessageProfileImgChat)
        }
        binding.scrollToBottomBtnWrapperChat.visibility = View.GONE
        binding.newMessageWrapperChat.visibility = View.VISIBLE
        checkNewMessageToastMemberId = membersModel.id    // 새메세지  토스트 뜨면   멤버의 id  넣어줌.
    }

    private fun showScrollToBottomButton() {
        // 화면 크기만큼 올라가면 버튼이 생김
        if ((binding.messageViewChat.computeVerticalScrollRange()
                - binding.messageViewChat.computeVerticalScrollOffset())
            > binding.messageViewChat.computeVerticalScrollExtent() * 2) {

            if (shouldShowScrollToBottomButton) {
                binding.scrollToBottomBtnWrapperChat.visibility = View.VISIBLE
            }
            if (shouldScrollToBottom) shouldScrollToBottom = false
        } else {
            binding.scrollToBottomBtnWrapperChat.visibility = View.GONE
            binding.newMessageWrapperChat.visibility = View.GONE

            //새 메세지 토스트 gone 되므로  다시 default 값으로 reset
            checkNewMessageToastMemberId=-1
            if (!shouldScrollToBottom) shouldScrollToBottom = true
        }
    }

    private fun setRecyclerViewScrollListener() {
        binding.messageViewChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) isUserScrolling = true
                if (newState == RecyclerView.SCROLL_STATE_IDLE) isUserScrolling = false

                visibleItemCount =
                    (binding.messageViewChat.layoutManager as LinearLayoutManager).childCount
                totalItemCount = (binding.messageViewChat.layoutManager as LinearLayoutManager).itemCount
                pastVisibleItems =
                    (binding.messageViewChat.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                lastComplete =
                    (binding.messageViewChat.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()


                isScrollEnd = visibleItemCount + pastVisibleItems >= totalItemCount
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (isUserScrolling) {
//                    if (messageView.canScrollVertically(1)
//                            && messageView.computeVerticalScrollOffset() < 400
//                            && dy <= 0) {
//                        requestLog(isInit)
//                        description_view.scrollTo(0, description_view.scrollY - messageView.computeVerticalScrollRange())
//                    }
                    if (!binding.messageViewChat.canScrollVertically(-1) && dy <= 0) {
//                        requestLog(isInit)
                        isUserScrolling = false
                        Logger.v("Paging check::This is Top")
                        loadNextFetchMessage()
                    }

                    if (binding.messageViewChat.canScrollVertically(1)) {
                        showScrollToBottomButton()
                    }
                }
            }
        })

    }

    private fun setRecyclerViewLayoutChangeListener() {
        binding.messageViewChat.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (!shouldShowScrollToBottomButton) {
                scrollToBottom()
            }
            if (binding.messageViewChat.height < Util.convertDpToPixel(this, 100F)) {
                shouldShowScrollToBottomButton = false
                binding.scrollToBottomBtnWrapperChat.visibility = View.GONE
                binding.newMessageWrapperChat.visibility = View.GONE

                //새 메세지 토스트 gone 되므로  다시 default 값으로 reset
                checkNewMessageToastMemberId=-1
            } else {
                shouldShowScrollToBottomButton = true
            }
        }
    }

    private fun cropArticlePhoto(uri: Uri, isSharedImage: Boolean) {
        ImageUtil.cropArticlePhoto(this, uri, isSharedImage, false, ConfigModel.getInstance(this).articleMaxSize * 1024 * 1024,
            { fileData ->
                sendImage(fileData)
            }, {

            },
            { options ->
                originSrcUri = uri
                originSrcWidth = options.outWidth
                originSrcHeight = options.outHeight
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == PHOTO_SELECT_REQUEST
            && resultCode == Activity.RESULT_OK) {
            cropArticlePhoto(data!!.data!!, false)
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri
                try {
                    onArticlePhotoSelected(resultUri)
                } catch (e: SecurityException) {
                    Toast.makeText(this, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }else if (requestCode == MEZZO_PLAYER_REQ_CODE) {//비디오광고  본후 -> result 실행
            Util.handleVideoAdResult(
                this, false, true, requestCode, resultCode, data, "cHat_room_videoad"
            ) { adType: String? ->
                videoAdUtil.onVideoSawCommon(
                    this,
                    true,
                    adType,
                    null
                )
            }
        }
    }

    @Throws(SecurityException::class)
    private fun onArticlePhotoSelected(uri: Uri) {

        ImageUtil.onArticlePhotoSelected(this, uri, originSrcWidth, originSrcHeight, originSrcUri,
            {
            },
            { stream ->
                sendImage(stream.toByteArray())
                Util.log("idoltalkRoom::sendImage $rawImage")
        })
    }

    override fun onPhotoClick(model: MessageModel, v: View?, position: Int) {
        when (v?.id) {
            R.id.photo_chat -> {
                val myContent = JSONObject(model.content)
                val index = myContent.getString("url").lastIndexOf(".")
                val extension = myContent.getString("url").substring(index + 1)

                //imageSocket, emoticonSocket 나눠놔서 is_emoticon은 emoticon일 경우에만 존재함. 그래서 try/catch 묶음
                try {
                    if(myContent.getBoolean("is_emoticon"))
                        return
                }catch (e: Exception) { }

                val articleModel = ArticleModel()
                if (extension == "gif") {
                    articleModel.umjjalUrl = myContent.getString("umjjal")
                    articleModel.imageUrl = myContent.getString("url")
                } else {
                    articleModel.imageUrl = myContent.getString("url")
                }

                Util.log("articleModel:: image ${articleModel.imageUrl}")
                Util.log("articleModel:: umjjal ${articleModel.umjjalUrl}")
                WidePhotoFragment.getInstance(articleModel)
                    .show(supportFragmentManager, "wide_photo")
            }

        }
    }

    /** Delete , Copy, Report Message **/
    override fun onContextItemSelected(item: MenuItem): Boolean {

        Util.log("idoltalk::${item}")
        try {
            val menuItemId = item.itemId
            lastClickedMessagePosition = (binding.messageViewChat.adapter as ChatMessageAdapter).getClickedPosition()
            val message = messages[lastClickedMessagePosition]

            Logger.v("message - context menu -?>>>>> ${message}")
            when (menuItemId) {
                MENU_COPY -> {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                    //link url 의 경우 message content가 linkmodel로 되어있으므로 아래와 같이 처리해준다.
                    // 링크 url 의 경우는 ->  삭제 및 report 가 되지 않은 경우에만  클립보드 복사가 가능 -> 그외에는 삭제 또는 신고 메세지가 복사되어야 됨.
                    var text = if(message.isLinkUrl && !message.deleted && !message.reported){
                        val gson = IdolGson.getInstance(true)
                        val linkContent = gson.fromJson(message.content,ChatMsgLinkModel::class.java)
                        linkContent.originalMsg.toString()
                    }else{

                        //메세지가  삭제되었거나,  신고 삭제둘다 되었을때는 삭제한 메세지 string을  복사
                        if((!message.reported && message.deleted) || (message.reported && message.deleted)){
                            this.getString(R.string.chat_deleted_message)
                        }else if(message.reported && !message.deleted){//신고만 되었을때는  신고한 메세지 string 복사
                            this.getString(R.string.already_reported)
                        }else {//삭제 신고 두개다 false일때는 그냥 메세지 를 클립보드에 복사한다.
                            message.content
                        }
                    }

                    try {
                        text = text.replace("@\\{\\d+\\:([^\\}]+)\\}".toRegex(), "")
                    } catch (e: Exception) {
                    }

                    val clip = ClipData.newPlainText("Copied text", text)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(mContext, R.string.copied_to_clipboard, Toast.LENGTH_SHORT)
                        .show()
                }

                MENU_DELETE -> {
                    val frag = MessageRemoveDialogFragment.getInstance()
                    frag.setActivityRequestCode(REQUEST_DELETE)
                    frag.show(supportFragmentManager, "remove")
                }
                MENU_REPORT -> {
                    report(message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return super.onContextItemSelected(item)
    }

    fun report(message: MessageModel) {
        if (socketManager?.socket?.connected() == true && !message.reported) { //신고한경우.
            incrementSequenceNumber()
            socketManager?.socket?.emit(Const.CHAT_REPORT_MESSAGE,
                JSONObject()
                    .put("cmd", Const.CHAT_REPORT_MESSAGE)
                    .put("seq", socketManager?.squenceNum)
                    .put("room_id", TOP_STACK_ROOM_ID)
                    .put("server_ts", message.serverTs))

            //신고도 DB에서 비교해줌.
            //신고같은경우 남의것을 해줘야되서 socket연결확인후 db업데이트해주기.
            message.reported = true
            message.contentType = MessageModel.CHAT_TYPE_TEXT

            //신고전 스크롤 위치 기억.
            val recyclerViewState = binding.messageViewChat.layoutManager?.onSaveInstanceState()

            ChatMessageList.getInstance(this).updateReported(message){
                Util.log("idoltalkRoom::신고DB 업데이트 완료.")
                runOnUiThread {
                    (binding.messageViewChat.adapter as ChatMessageAdapter).reportMessage(message.serverTs){
                        if (it.userId != userId) {
                            Util.hideSoftKeyboard(this, binding.viewComment.inputComment)
                            Util.showDefaultIdolDialogWithBtn1(
                                this,
                                null,
                                resources?.getString(R.string.report_done),
                                {
                                    Util.closeIdolDialog()
                                    binding.messageViewChat.layoutManager?.onRestoreInstanceState(recyclerViewState)
                                },
                                true)
                        }
                    }
                }
            }

        } else if(message.reported){ //이미 신고한경우.
            Util.showDefaultIdolDialogWithBtn1(
                this,
                null,
                getString(R.string.chat_message_already_reported),
                { Util.closeIdolDialog() },
                true)
        }

    }

    private fun myRoleStatus(chatMembersList : CopyOnWriteArrayList<ChatMembersModel>) : String?{
        return chatMembersList.find { it.id == this@ChattingRoomActivity.userId }?.role
    }

    //TransactionTooLarge 해결
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Logger.v("idoltalkRoom:: onSaveInstance")
        outState.clear()
    }

    private fun updateChatMembers(response: JSONObject?) = lifecycleScope.launch(Dispatchers.IO) {

        val userObj = response?.getJSONObject("user")

        val user = IdolGson.getInstance().fromJson(
            userObj.toString(),
            ChatMembersModel::class.java
        )

        val chatMemberListInstance = ChatMembersList.getInstance(this@ChattingRoomActivity)
        val fetchedRoomMembers = chatMemberListInstance.getChatMemberList(
            TOP_STACK_ROOM_ID
        )

        fetchedRoomMembers.find { model -> model.id == user.id }?.apply {
            deleted = false
            role = "N"
            nickname = user.nickname
            level = user.level
            imageUrl = user.imageUrl
            chatMemberListInstance.updateChatMember(userId, TOP_STACK_ROOM_ID, this)
        }

        Logger.v("api :: ${fetchedRoomMembers.modelToString()}")
        if (!fetchedRoomMembers.any { it.id == user.id }) {
            user.role = "N"
            user.roomId =
                TOP_STACK_ROOM_ID //user에서 roomID 안줘서 roomID 안넣으면 디비에 저장이 안됨. 꼭 넣어야함.
            fetchedRoomMembers.add(user)
            chatMemberListInstance.setChatMembers(fetchedRoomMembers)
        }

        Logger.v("api :: ${fetchedRoomMembers.size}")

        withContext(Dispatchers.Main) {
            chattingInfo = ChattingInfoFragment.newInstance(
                chatRoomInfoModel!!,
                fetchedRoomMembers,
                myRoleStatus(fetchedRoomMembers)
            )

            supportFragmentManager.commit(allowStateLoss = true) {
                replace(R.id.drawer_menu_chat, chattingInfo!!)
            }

            roomMembers = fetchedRoomMembers
            mChatMessageAdapter.setMembers(fetchedRoomMembers)
        }
    }

    companion object{

        const val ID_CHATTING_MSG = 70//채팅 메세지  id
        const val PARAM_IDOL = "idol"
        const val PARAM_CHAT_ROOM_ID ="room_id"
        const val PARAM_ANONI_USER_ID ="user_id"
        const val PARAM_ANONI_USER_NICKNAME = "user_nickname"
        const val PARAM_CHAT_ROOM_USER_ROLE ="user_role"
        const val PARAM_CHAT_ROOM_ANONI = "is_anonymity"
        const val PARAM_CHAT_ROOM_SOCKET_URL="socket_url"
        const val PARAM_CHAT_ROOM_TITLE = "title"

        const val TALK_MAX_LENGTH = 500
        const val REQ_NUM_OF_LOG = 30
        const val MENU_COPY = 1
        const val MENU_DELETE = 2
        const val MENU_REPORT = 3
        const val REQUEST_DELETE = 16
        const val WRITE_ENABLE_LEVEL = 3

        //image size 400으로 맞춤.
        const val IMAGE_SIZE = 400

        val talkLanguages: Array<String> = arrayOf(
            "한국어",
            "ENG",
            "中文",
            "日本語",
            "Indonesia",
            "Português",
            "Español",
            "Tiếng Việt",
            "ไทย"
        )
        val talkLocales: Array<String> = arrayOf(
            "ko",
            "en",
            "zh",
            "ja",
            "in",
            "pt",
            "es",
            "vi",
            "th"
        )

        @JvmStatic
        fun createIntent(
            context: Context?,
            roomId: Int?,
            userNickname: String?,
            userId: Int?,
            role: String?,
            isAnonymity: String?,
            title: String?
        ): Intent {
            val intent = Intent(context, ChattingRoomActivity::class.java)
//            intent.putExtra(PARAM_IDOL, idol)
            intent.putExtra(PARAM_CHAT_ROOM_USER_ROLE, role)
            intent.putExtra(PARAM_CHAT_ROOM_ID, roomId)
            intent.putExtra(PARAM_ANONI_USER_ID, userId)
            intent.putExtra(PARAM_ANONI_USER_NICKNAME, userNickname)
            intent.putExtra(PARAM_CHAT_ROOM_ANONI, isAnonymity)
            intent.putExtra(PARAM_CHAT_ROOM_TITLE, title)
            return intent
        }

    }

    //마지막 이미지를 받았을 경우 스크롤 맨 마지막으로 내리기 위함.(처음 방 들어왔을 때, 이미지 보냈을 때만)
    override fun showLastImage() {
        binding.messageViewChat.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    scrollToBottom()
                    binding.messageViewChat.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
    }
}
