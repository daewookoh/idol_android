package net.ib.mn.chatting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.BaseActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.chatDb.ChatMessageList
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.core.data.model.ChatRoomCreateModel
import net.ib.mn.core.data.repository.ChatRepositoryImpl
import net.ib.mn.databinding.ActivityChattingRoomCreateBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.CreateChatRoomDialogFragment
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.fragment.BottomSheetFragment.Companion.newInstance
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONObject
import javax.inject.Inject


/**
 * ProjectName: idol_app_renew
 *
 * Description:
 * 채팅방 생성시 실행되는   화면이다.
 * 생성할  채팅방에 여러  조건들을 설정 할수 있다.
 * */

@AndroidEntryPoint
class ChattingCreateActivity: BaseActivity(), BaseDialogFragment.DialogResultHandler {

    @Inject
    lateinit var chatRepository: ChatRepositoryImpl
    @Inject
    lateinit var accountManager: IdolAccountManager

    //각 조건 목록이 들어갈  bottomSheetFragment
    private lateinit var chatConditionSelector: BottomSheetFragment

    private lateinit var idol:IdolModel

    private val maxPeopleCount =300//일단  최대  인원  count 고정
    private var mostOpenStatus = "N"//최애 공개 여부 값
    private var nicknameOpenStatus = "N" //닉네임 공개여부 값
    private var levelLimit =-1//레벨 제한

    private var titleLength =0// 채팅방 제목  whitespace제거후  length의 값.
    private lateinit var binding: ActivityChattingRoomCreateBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chatting_room_create)
        binding.svChattingRoomCreate.applySystemBarInsets()

        initSet()
        buttonClickEvent()
        textChangedEvent()
    }

    //엑티비티 실행시 초기 세팅
    private fun initSet(){
        //채팅 개설방 액션바
        supportActionBar?.title = getString(R.string.chat_make)

        //idol 값  받아옴.
        idol = intent.extras!!.get(PARAM_IDOL) as IdolModel


        //유저의 최애 타입의 따라 -> 공개여부에서 그룹/멤버 공개방 인지 최애만 공개방인지 보여주낟.
        val userMostType = IdolAccount.getAccount(this)?.userModel?.most?.type
        if(userMostType == "S" || BuildConfig.CELEB ){ // 셀럽은 무조건 공개여부   자기 최애만
            setChatOpenStatus(CHAT_OPEN_MOST)
            binding.tvChattingRoomOpenStatus.text = this.getString(R.string.chat_room_most_only)
        }else if(userMostType == "G"){
            setChatOpenStatus(CHAT_OPEN_COMMUNITY)
            binding.tvChattingRoomOpenStatus.text = this.getString(R.string.chat_room_most_and_group)
        }

        //팝업창 다시 안보기  설정 안했을때
        if (Util.getPreferenceBool(
                this,
                Const.PREF_SHOW_CREATE_CHAT_ROOM_INSTRUCTION,
                true
            )) {
            showPopUpDialog(POPUP_TYPE_INITIAL_CHAT_INSTRUCTION)
        }

        binding.editChatRoomName.setOnKeyListener(View.OnKeyListener { v, keyCode, event -> //Enter key Action
            if (keyCode == KeyEvent.KEYCODE_ENTER) { return@OnKeyListener true }
            return@OnKeyListener false
        })

        binding.btnChattingCreate.text= String.format(resources.getString(R.string.chat_room_make_button),ConfigModel.getInstance(this).chatRoomDiamond )
    }

    //text 변경 이벤트 모음
    private fun textChangedEvent(){

        //채팅방 이름 -title
        binding.editChatRoomName.doOnTextChanged { text, _, _, _->
            titleLength = text?.trim()?.length!!
            binding.tvChattingRoomNameCount.text = "${titleLength}/50"
            checkNecessaryDataFilled()
        }

        //채팅방 이름 editext focus 아웃 될때 -5자 이하면 팝업 띄어준다.
        binding.editChatRoomName.checkFocusOut()

        //채팅방 소개 -desc
        binding.editChatRoomInstruction.doOnTextChanged { text, _, _, _->
            binding.tvChattingRoomInstructionCount.text = "${text?.length}/100"
            checkNecessaryDataFilled()
        }
    }


    //editext focus 잃었을때
    private fun EditText.checkFocusOut() {
        this.setOnFocusChangeListener { v, hasFocus ->
            when (v) {

                //채팅방 이름  editext일때
                binding.editChatRoomName -> {
                    if (!hasFocus) {//focus를 읽은 경우  ->  5자 미만이면  다이얼로그 보여주낟.
                        if (titleLength < minimumRoomNameLength) {
                            Util.showDefaultIdolDialogWithBtn1(
                                this@ChattingCreateActivity,
                                null,
                                getString(R.string.chat_title_short)
                            ) {
                                Util.closeIdolDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    //버튼 클릭 이벤트 모음
    private fun buttonClickEvent(){


        //채팅방 익명, 또는 공개 여부
        binding.tvChattingRoomAnonymousStatus.setOnClickListener {

                chatConditionSelector = newInstance(BottomSheetFragment.FLAG_CHAT_ANONYMOUS_STATUS)
                val tag = "filter"
                val oldFrag = supportFragmentManager.findFragmentByTag(tag)
                if (oldFrag == null) {
                    chatConditionSelector.show(supportFragmentManager, tag)
                }
        }

        //채팅방 레벨 제한
        binding.tvChattingRoomLevelLimit.setOnClickListener {

               chatConditionSelector = newInstance(BottomSheetFragment.FLAG_CHAT_LEVEL_LIMIT)
               val tag = "filter"
               val oldFrag = supportFragmentManager.findFragmentByTag(tag)
               if (oldFrag == null) {
                   chatConditionSelector.show(supportFragmentManager, tag)
               }
        }

        //채팅방 생성 버튼 클릭시
        binding.btnChattingCreate.setOnClickListener {
            when {
                checkNecessaryDataFilled() -> {//필수항목이 모두 채워져 있을때
                    showPopUpDialog(POPUP_TYPE_CREATE_NEW_CHAT_ROOM)
                }
                titleLength < minimumRoomNameLength -> {//방  이름  5자 미만일때  5자 이상  유도 팝업 띄움
                    Util.showDefaultIdolDialogWithBtn1(
                        this, null, getString(R.string.chat_title_short)
                    ) {
                        Util.closeIdolDialog()
                    }
                }
                Util.hasBadWords(this,binding.editChatRoomName.text.toString()) ->{
                    Util.showDefaultIdolDialogWithBtn1(
                        this, null, getString(R.string.chat_title_bad_words)
                    ) {
                        Util.closeIdolDialog()
                    }
                }
                Util.hasBadWords(this,binding.editChatRoomInstruction.text.toString()) ->{
                    Util.showDefaultIdolDialogWithBtn1(
                        this, null, getString(R.string.chat_description_bad_words)
                    ) {
                        Util.closeIdolDialog()
                    }
                }

                else -> {//방이름  5개 채우고,  필수항목 안 채워져 있을때   필수항목 유도 팝업  띄움.
                    Util.showDefaultIdolDialogWithBtn1(
                        this, null, getString(R.string.schedule_require)
                    ) {
                        Util.closeIdolDialog()
                    }
                }
            }
        }

    }

    //다이얼로그 결과 값 받아오기
    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.CHAT_ROOM_CREATE.value -> {
                if (resultCode == BaseDialogFragment.RESULT_OK) {
                    Util.showProgress(this)
                    val locale =
                        LocaleUtil.getAppLocale(this).toString().split("_".toRegex()).toTypedArray()[0]
                    createChatRoom(
                        ChatRoomCreateModel(
                            idolId = idol.getId(),
                            locale = locale,
                            title = binding.editChatRoomName.text.toString(),
                            desc = binding.editChatRoomInstruction.text.toString(),
                            isMostOnly = mostOpenStatus,
                            isAnonymity = nicknameOpenStatus,
                            levelLimit = levelLimit,
                            maxPeople = maxPeopleCount
                        )
                    )

                    //구글 anlaystics -> 생성 버튼  필수항목 모두 입력후 눌렀을때
                    this@ChattingCreateActivity.setUiActionFirebaseGoogleAnalyticsActivity(
                        Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "chat_room_create"
                    )
                }
            }

        }
    }

    //채팅방 생성 api 호출
    private fun createChatRoom(chatRoomCreateModel: ChatRoomCreateModel){
        val presentClientTs = System.currentTimeMillis()
        MainScope().launch {
            chatRepository.createChatRoom(
                chatRoomCreateModel,
                { response ->
                    if (response.optBoolean("success")) {

                        Logger.v(response.toString())

                        Util.closeProgress()

                        accountManager.fetchUserInfo(this@ChattingCreateActivity)
                        //생성 성공 했으므로 ok

                        val roomInfo = response.optJSONObject("room")
                        Util.showDefaultIdolDialogWithBtn1(
                            this@ChattingCreateActivity,
                            null,
                            getString(R.string.chat_room_create_success)
                        ) {
                            Util.closeIdolDialog()
                            val isAnonymity = roomInfo?.getString("is_anonymity")
                            val nickname = roomInfo?.getString("nickname").toString()
                            val userId = roomInfo?.getInt("user_id")

                            val intent: Intent? = ChattingRoomActivity.createIntent(
                                context = this@ChattingCreateActivity,
                                roomId = roomInfo?.getInt("id") ?: 0,
                                userNickname = nickname,
                                userId = userId,
                                role = "O",//채팅방을 생성한거니까 role은 방장임을  의미하는 O를 줌
                                isAnonymity = isAnonymity,
                                title = chatRoomCreateModel.title
                            )

                            val account = IdolAccount.getAccount(this@ChattingCreateActivity)

                            //첫 참여 메세지 구성
                            val messageObject = JSONObject()
                                .put("room_id", roomInfo?.getInt("id") ?: 0)
                                .put("user_id", userId)
                                .put("client_ts", presentClientTs)
                                .put("server_ts", presentClientTs)
                                .put("content", getString(R.string.chat_first_join))
                                .put("content_type", MessageModel.CHAT_TYPE_TEXT)
                                .put("is_readable",true)
                                .put("is_first_join_msg",true)
                                .put("account_id", account?.userId ?: 0)

                            val message = IdolGson.getInstance(false)
                                .fromJson(messageObject.toString(), MessageModel::class.java)

                            //Db에링크도 저장.
                            ChatMessageList.getInstance(this@ChattingCreateActivity).setChatMessage(message){
                                Util.log("idoltalk::insert db(sendLinkMessage) -> $message")
                            }

                            //이전 엑티비티에  result 값을 보내기위해  flag 설정
                            intent?.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                            startActivity(intent)
                            finish()
                        }

                    } else {//response false 일때(다이아가 적을 때)
                        Util.closeProgress()
                        Logger.v("check response ->$response")
                        val errorMsg = response.getString("msg")
                        Util.showDefaultIdolDialogWithBtn1(
                            this@ChattingCreateActivity,
                            null,
                            errorMsg
                        ) {
                            Util.closeIdolDialog()
                        }
                    }
                },
                { throwable ->
                    Util.showDefaultIdolDialogWithBtn1(
                        this@ChattingCreateActivity,
                        null,
                        throwable.message
                    ) {
                        Util.closeIdolDialog()
                        finish()
                    }

                }
            )
        }
    }


    //채팅방 생성용  필수 항목들이  채워져 있는지 체크 및  욕설 포함 여부 체크
    private fun checkNecessaryDataFilled(): Boolean {
        with(binding) {
            val isRoomNameLengthValid = editChatRoomName.length() >= minimumRoomNameLength
            val isOpenStatusValid = tvChattingRoomOpenStatus.text.isNotEmpty()
            val isAnonyStatusValid = tvChattingRoomAnonymousStatus.text.isNotEmpty()
            val isLevelLimitValid = tvChattingRoomLevelLimit.text.isNotEmpty()
            val isInstructionValid = !Util.hasBadWords(this@ChattingCreateActivity, binding.editChatRoomInstruction.text.toString())
            val isRoomNameValid = !Util.hasBadWords(this@ChattingCreateActivity, binding.editChatRoomName.text.toString())

            val isValid = isRoomNameLengthValid && isOpenStatusValid && isAnonyStatusValid &&
                isLevelLimitValid && isInstructionValid && isRoomNameValid
            binding.btnChattingCreate.background = ContextCompat.getDrawable(
                this@ChattingCreateActivity,
                if(isValid) R.drawable.bg_radius_brand500 else R.drawable.bg_radius_gray300
            )
            return isValid
        }
    }

    //체팅방  익명, 공개 세팅
    fun setChatAnonymousStatus(status: Int){
        when(status){
            CHAT_OPEN_NICKNAME -> {
                nicknameOpenStatus = "N"
                binding.tvChattingRoomAnonymousStatus.setText(R.string.chat_room_nickname)
            }
            CHAT_ANONYMOUS -> {
                nicknameOpenStatus = "Y"
                binding.tvChattingRoomAnonymousStatus.setText(R.string.chat_room_anonymous)
            }
        }
        checkNecessaryDataFilled()
    }

    //팝업 다이얼로그 각 타입별로
    private fun showPopUpDialog(popUpType: Int){
        when(popUpType) {

            //채팅방 카운트  오버 함 (300개 이상일때)
            POPUP_TYPE_OVER_MAX_CHAT_ROOM_COUNT -> {
                Util.showDefaultIdolDialogWithBtn1(
                    this@ChattingCreateActivity,
                    null,
                    getString(R.string.chat_room_make_popup2)
                ) {
                    Util.closeIdolDialog()
                }
            }

            //채팅방 생성 팝업
            POPUP_TYPE_CREATE_NEW_CHAT_ROOM -> {
                val account = IdolAccount.getAccount(this)
                if (account == null && Util.mayShowLoginPopup(this)) {
                    return
                }
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "dia_use_create_Chat_Room"
                )

                val createChatRoomDia = ConfigModel.getInstance(this).chatRoomDiamond

                val color = "#" + Integer.toHexString(
                    ContextCompat.getColor(
                        this,
                        R.color.main
                    )
                ).substring(2)
                val msg = String.format(
                    resources.getString(R.string.chat_room_make_popup1_desc),
                    "<FONT color=$color>$createChatRoomDia</FONT>"
                )
                val spanned = HtmlCompat.fromHtml(
                    msg,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

                val createChatRoomDialog = CreateChatRoomDialogFragment.getInstance()
                createChatRoomDialog.setMessage(spanned)


                createChatRoomDialog.setActivityRequestCode(RequestCode.CHAT_ROOM_CREATE.value)
                createChatRoomDialog.show(supportFragmentManager, "create_chat_room")
            }


            POPUP_TYPE_INITIAL_CHAT_INSTRUCTION ->{
                Util.showChatRoomCreateDialogWithBtn2(
                    this,
                    { _ ->
                        Util.setPreference(
                            this,
                            Const.PREF_SHOW_CREATE_CHAT_ROOM_INSTRUCTION,
                            false
                        )
                        Util.closeIdolDialog()
                    }, { _ ->
                        Util.closeIdolDialog()
                    })
            }
        }
    }

    //세팅방  최애, 그룹 공개 세팅
    fun setChatOpenStatus(status: Int){
        when(status){
            CHAT_OPEN_MOST -> {
                mostOpenStatus = "Y"
                binding.tvChattingRoomOpenStatus.text = getString(R.string.chat_room_most_only)
                binding.tvChattingRoomOpenStatus.setTextColor(ContextCompat.getColor(this,R.color.text_dimmed))
            }
            CHAT_OPEN_COMMUNITY -> {
                mostOpenStatus = "N"
                binding.tvChattingRoomOpenStatus.text = getString(R.string.chat_room_most_and_group)
            }
        }
        checkNecessaryDataFilled()
    }


    //채팅 방 입장 레벨 세팅
    fun setChatLevelLimit(level: Int){
        if(level <= (IdolAccount.getAccount(this)?.level ?: 0)){
            levelLimit = level
            binding.tvChattingRoomLevelLimit.text = String.format(this.getString(R.string.chat_room_level_limit),level)
            binding.tvChattingRoomLevelLimit.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            checkNecessaryDataFilled()
        }else{
            Util.showDefaultIdolDialogWithBtn1(
                this,
                null,
                getString(R.string.chat_level_select_limit)
            ) {
                Util.closeIdolDialog()
            }
        }
    }



    companion object{
        const val PARAM_IDOL = "idol"
        const val CHAT_OPEN_MOST = 1
        const val CHAT_OPEN_COMMUNITY = 2
        const val CHAT_OPEN_NICKNAME = 3
        const val CHAT_ANONYMOUS = 4
        const val CHAT_ROOM_LIST_RECENT = 5
        const val CHAT_ROOM_LIST_MANY_TALK = 6
        const val POPUP_TYPE_LACK_OF_HEART = 7//하트 부족
        const val POPUP_TYPE_OVER_MAX_CHAT_ROOM_COUNT =8//채팅방  초과
        const val POPUP_TYPE_CREATE_NEW_CHAT_ROOM = 9 //채팅방 개설 가능
        const val POPUP_TYPE_INITIAL_CHAT_INSTRUCTION =10

        const val minimumRoomNameLength = 5 // 방제목 최소 길이

        fun createIntent(context: Context?, idol: IdolModel?): Intent {
            val intent  = Intent(context, ChattingCreateActivity::class.java)
            intent.putExtra(PARAM_IDOL, idol as Parcelable?)
            return intent
        }

    }
}