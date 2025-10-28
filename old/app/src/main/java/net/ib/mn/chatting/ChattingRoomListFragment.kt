package net.ib.mn.chatting

import android.app.Activity.RESULT_OK
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.chatDb.ChatMessageList
import net.ib.mn.chatting.model.ChatRoomListModel
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.core.data.repository.ChatRepositoryImpl
import net.ib.mn.databinding.ChattingListFragmentBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.CommunityActivityViewModel
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject


/**
 * ProjectName: idol_app_renew
 *
 * Description:
 * 아이돌  피드 화면에서  채팅룸 리스트  탭의 화면이다.
 * 전체  채팅방과  참여중인 채팅방이  분리되어 뿌려진다.
 *
 * */

@AndroidEntryPoint
class ChattingRoomListFragment : BaseFragment()
    ,ChattingRoomListAdapter.OnClickListener {

    val gson: Gson = IdolGson.getInstance()

    internal lateinit var manager: FragmentManager
    private lateinit var trans: FragmentTransaction

    private var actionbar: ActionBar? = null

    private val chatRoomEntireList=ArrayList<ChatRoomListModel>()
    private val chatRoomJoinedList = ArrayList<ChatRoomListModel>()
    lateinit var chatRoomListRcyAdapter: ChattingRoomListAdapter
    private var totalChatRoomCount =0
    private var totalJoinChatRoomCount =0

    private var idol: IdolModel? = null


    private var entireFilterValue = TYPE_CHAT_ROOM_LIST_FILTER_RECENT
    private var joinedFilterValue = TYPE_CHAT_ROOM_LIST_FILTER_RECENT


    //내 참여방 페이징 용  다음  resource url
    private var nextJoinChatRoomResourceUrl: String? = null
    private var joinedOffset =0
    private var joinedLimit =30

    //전체 채팅방  페이징용 다음 resource url
    private var nextEntireChatRoomResourceUrl: String? = null
    private var entireOffset = 0
    private var entireLimit = 30

    private val chatLessLevel = 5

    var mAccount : IdolAccount? = null

    private lateinit var binding: ChattingListFragmentBinding
    private val communityActivityViewModel: CommunityActivityViewModel by activityViewModels()

    @Inject
    lateinit var chatRepository: ChatRepositoryImpl

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.chatting_list_fragment,
            container,
            false)
        return binding.root

    }

    //채팅룸  recyclerview setting
    private fun setChattingRoomRecyclerView() {
        chatRoomListRcyAdapter =
            ChattingRoomListAdapter(this, requireActivity(), account = mAccount )


        binding.rcyChatRoomList.apply {
            adapter = chatRoomListRcyAdapter
            itemAnimator = null
        }

        binding.rcyChatRoomList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                //recyclerview 하단을 감지해서 보여준다.
                if (!recyclerView.canScrollVertically(1) && newState == SCROLL_STATE_IDLE) {
                    // TODO: 2021/03/27 여기서  다음 페이징 가지고와야됨.

                    if(!nextJoinChatRoomResourceUrl.equals("null")) {
                        joinedOffset += 30
                        getChatRoomJoinedList(entireFilter = entireFilterValue, joinedFilter = joinedFilterValue)
                    }else if(nextJoinChatRoomResourceUrl.equals("null")&&!nextEntireChatRoomResourceUrl.equals("null")){
                        Logger.v("여기")
                        Logger.v(nextEntireChatRoomResourceUrl+" 전체 url")
                        Logger.v(nextJoinChatRoomResourceUrl+ "조인 url ")
                        entireOffset += 30
                        getChattingRoomList(orderBy = entireFilterValue)

                    }



                }
            }
        })

    }

    //채팅방 리스트 필터 선택 처리
    //각 핉터에 맞게  순서를 update 해준다.
    fun setChatRoomFilterSelected(status: Int, isJoinedRoom: Boolean){
        when(status){
            //대화 많은 순
            TYPE_CHAT_ROOM_LIST_FILTER_MANY_TALK -> {
                if (isJoinedRoom) {
                    joinedFilterValue = TYPE_CHAT_ROOM_LIST_FILTER_MANY_TALK

                    joinedOffset =0
                    joinedLimit= 30
                    entireOffset =0
                    entireLimit =30
                    getChatRoomJoinedList(
                        entireFilter = entireFilterValue,
                        joinedFilter = joinedFilterValue
                    )
                } else {

                    Logger.v("이렇게 ->"+joinedOffset)
                    entireFilterValue = TYPE_CHAT_ROOM_LIST_FILTER_MANY_TALK
                    entireOffset =0
                    entireLimit =30
                    getChattingRoomList(orderBy = entireFilterValue)
                }
            }

            //최신순
            TYPE_CHAT_ROOM_LIST_FILTER_RECENT -> {
                if (isJoinedRoom) {
                    joinedFilterValue = TYPE_CHAT_ROOM_LIST_FILTER_RECENT
                    joinedOffset =0
                    joinedLimit= 30
                    entireOffset =0
                    entireLimit =30
                    getChatRoomJoinedList(
                        entireFilter = entireFilterValue,
                        joinedFilter = joinedFilterValue
                    )
                } else {
                    entireFilterValue = TYPE_CHAT_ROOM_LIST_FILTER_RECENT
                    entireOffset =0
                    entireLimit =30
                    Logger.v("이렇게 ->"+joinedOffset)

                    getChattingRoomList(orderBy = entireFilterValue)
                }

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSet(savedInstanceState)

        //맨처음 RoomList를  가져올때는 최신순으로 가져온다.
//        getChattingRoomList(orderBy = TYPE_CHAT_ROOM_LIST_FILTER_RECENT)
        getChatRoomJoinedList(entireFilter = entireFilterValue,joinedFilter = joinedFilterValue)

        setChattingRoomRecyclerView()
        getDataFromCommunityVM()
    }


    //초기 세팅
    private fun initSet(savedInstanceState: Bundle?){
        actionbar = (activity as AppCompatActivity).supportActionBar
        manager = activity?.supportFragmentManager!!
        trans = manager.beginTransaction()
        idol = arguments?.getSerializable(PARAM_IDOL) as IdolModel
        mAccount = IdolAccount.getAccount(requireActivity())


         //TODO: 2021/03/09  채팅방 새로고침 기능이 필요할때  주석 해재 및  false 부분  막을것
         //채팅방 리스트  새로고침 기능
         binding.chatRoomSwipeRefresh.setOnRefreshListener {

             //리스트 내용을 새롭게 다시 불러온다.
             joinedLimit = 30
             joinedOffset = 0
             entireLimit = 30
             entireOffset = 0
             getChatRoomJoinedList(
                 entireFilter = entireFilterValue,
                 joinedFilter = joinedFilterValue
             )

         }

        //Rx(SocketManager에서 값던져주면 받음).
        setRxEvent()
    }

    private fun getDataFromCommunityVM() {
        communityActivityViewModel.chattingRoomWrite.observe(
            viewLifecycleOwner,
            SingleEventObserver{
                if(it) {
                    if(IdolAccount.getAccount(requireActivity())?.userModel?.level ?: 0 < 5){//레벨 5 미만일
                        showPopUpDialog(POPUP_TYPE_LOW_LEVEL_CREATE_CHAT_ROOM)
                    }else if(IdolAccount.getAccount(requireActivity())?.most?.getId() != idol?.getId()) {//최애가 다를때  채팅방 생성은 불가하
                        showPopUpDialog(POPUP_TYPE_CREATE_ROOM_DIFFERENT_MOST)
                    }else{//레벨  5이상일때 + 최애가 같을때  채팅방 개설 설명 팝업을  띄어줌.
                        //채팅방 생성창  실행
                        startActivityForResult(
                            ChattingCreateActivity.createIntent(
                                requireActivity(),
                                idol
                            ), REQUEST_CREATE_CHATTING
                        )
                    }
                }
            }
        )
    }

    private fun setRxEvent() {
        lifecycleScope.launch {
            EventBus.receiveEvent<JSONObject>(Const.CHAT_SYSTEM_COMMAND).collect { data ->
                Util.log("idoltalkRoom::RXJava onSystemCommand->$data")
                val content = data.getJSONObject("content")
                val type = content.getString("type")
                if(type == "LEAVE_ROOM"){
                    getChatRoomJoinedList(entireFilter = entireFilterValue,joinedFilter = joinedFilterValue)
                }
            }
        }
    }


    //내가 참여한 채팅방 리스트
    private fun getChatRoomJoinedList(entireFilter: Int, joinedFilter: Int){

        //현재 locale을 보내줌.
        val locale = Util.getSystemLanguage(context).split("_")[0]
        MainScope().launch {
            chatRepository.getChatRoomJoinList(
                idol?.getId()!!,
                locale,
                joinedFilter,
                joinedLimit,
                joinedOffset,
                { response ->
                    val offset = response?.optJSONObject("meta")?.optInt("offset") ?:0
                    if(offset==0) {
                        chatRoomEntireList.clear()
                        chatRoomJoinedList.clear()
                    }

                    totalJoinChatRoomCount = response?.optJSONObject("meta")?.optInt("total_count") ?:0
                    nextJoinChatRoomResourceUrl = response?.optJSONObject("meta")?.optString("next")

                    val chatRoomJoinedArray: JSONArray? = response?.optJSONArray("objects")
                    if (chatRoomJoinedArray != null) {
                        for (i in 0 until chatRoomJoinedArray.length()) {

                            //default 채팅방의 경우는 무조건 맨처음 index에 넣어준다.
                            if(chatRoomJoinedArray.getJSONObject(i).getString("is_default") == "Y"){
                                if(chatRoomJoinedList.size>0 &&(chatRoomJoinedList[0].isDefault=="Y" && chatRoomJoinedList[0].isAnonymity=="N")
                                    && chatRoomJoinedArray.getJSONObject(i).getString("is_anonymity")=="Y"){
                                    chatRoomJoinedList.add(
                                        1 , gson.fromJson(
                                            chatRoomJoinedArray.getJSONObject(i).toString(),
                                            ChatRoomListModel::class.java
                                        )
                                    )
                                    chatRoomJoinedList[1].isJoinedRoom=true//내 참여룸인걸로 변경
                                }else {
                                    chatRoomJoinedList.add(
                                        0, gson.fromJson(
                                            chatRoomJoinedArray.getJSONObject(i).toString(),
                                            ChatRoomListModel::class.java
                                        )
                                    )
                                    chatRoomJoinedList[0].isJoinedRoom=true//내 참여룸인걸로 변경
                                }

                            } else {

                                //offset 이 0일떄는  그냥 offset +i 해도 0,1,2 순으로 쌓이는데 30부터는
                                //30이  한번더 불리는 현상이 생긴다. 그래서 0이상의 offset 들에는 +1을 추가해준다.
                                if(offset>0){
                                    chatRoomJoinedList.add(
                                        offset+i+1 , gson.fromJson(
                                            chatRoomJoinedArray.getJSONObject(i).toString(),
                                            ChatRoomListModel::class.java
                                        )
                                    )
                                    chatRoomJoinedList[offset+i+1].isJoinedRoom=true//내 참여룸인걸로 변경
                                }else{
                                    chatRoomJoinedList.add(
                                        offset+i , gson.fromJson(
                                            chatRoomJoinedArray.getJSONObject(i).toString(),
                                            ChatRoomListModel::class.java
                                        )
                                    )
                                    chatRoomJoinedList[offset+i].isJoinedRoom=true//내 참여룸인걸로 변경
                                }

                            }
                        }

                        if(nextJoinChatRoomResourceUrl != null){

                            //다음거  가지고올께 없고,  20개 미만으로 방리스트를 가지고 왓으며,  다음  전체 방 리스트를  가지고 오는 로직 싫앵
                            //10개 이상 가지고왔을때는  전체방 페이징 30개까지 포함하면 너무 많아지므로,  스킵한다.
                            if(nextJoinChatRoomResourceUrl.equals("null")){
                                getChattingRoomList(orderBy = entireFilter)
                                //chatRoomListRcyAdapter.getRoomList(chatRoomEntireList, chatRoomJoinedList,totalChatRoomCount,totalJoinChatRoomCount)
                            }else{
                                chatRoomListRcyAdapter.getRoomList(chatRoomEntireList, chatRoomJoinedList,totalChatRoomCount,totalJoinChatRoomCount,false)
                            }
                        }
                    }
                    binding.chatRoomSwipeRefresh.isRefreshing = false
                }, { throwable ->
                    chatRoomListRcyAdapter.getRoomList(chatRoomEntireList, chatRoomJoinedList,totalChatRoomCount,totalJoinChatRoomCount,true)

                    // Fragment ChattingRoomListFragment not attached to an activity. 방지
                    if(!isAdded) return@getChatRoomJoinList

                    Toast.makeText(
                        requireActivity(),
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.chatRoomSwipeRefresh.isRefreshing = false
                }
            )
        }
    }



    //채팅 룸 리스트 가져오기
    private fun getChattingRoomList(orderBy: Int){

        //현재 locale을 보내줌.
        val locale = Util.getSystemLanguage(context).split("_")[0]
        //채팅룸  리스트 받아오기
        MainScope().launch {
            chatRepository.getChatRoomList(
                idol?.getId()!!,
                locale,
                orderBy,
                entireLimit,
                entireOffset,
                { response ->
                    if(response.optInt("gcode") == 9000) {
                        Toast.makeText(
                            requireActivity(),
                            response.optString("msg") ?: getString(R.string.error_abnormal_exception),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@getChatRoomList
                    }
                    //룸리스트 받아옴으로 혹시 기존에 남아 있는 list data는 삭제 해줌.
                    val offset = response.optJSONObject("meta")?.optInt("offset") ?:0
                    var pastCount = chatRoomEntireList.size
                    if(offset==0) {
                        pastCount = 0
                        chatRoomEntireList.clear()
                    }

                    totalChatRoomCount = response.optJSONObject("meta")?.optInt("total_count") ?:0
                    nextEntireChatRoomResourceUrl = response?.optJSONObject("meta")?.optString("next")

                    val chatRoomArray: JSONArray? = response.optJSONArray("objects")
                    if (chatRoomArray != null) {
                        for (i in 0 until chatRoomArray.length()) {
                            if (chatRoomArray.getJSONObject(i).getString("is_default") == "Y") {
                                if(chatRoomEntireList.size>0 &&(chatRoomEntireList[0].isDefault=="Y" && chatRoomEntireList[0].isAnonymity=="N")
                                    && chatRoomArray.getJSONObject(i).getString("is_anonymity")=="Y"){
                                    chatRoomEntireList.add(
                                        1 , gson.fromJson(
                                            chatRoomArray.getJSONObject(i).toString(),
                                            ChatRoomListModel::class.java
                                        )
                                    )
                                }else {
                                    chatRoomEntireList.add(
                                        0, gson.fromJson(
                                            chatRoomArray.getJSONObject(i).toString(),
                                            ChatRoomListModel::class.java
                                        )
                                    )
                                }
                            } else {
                                chatRoomEntireList.add(
                                    pastCount+ i, gson.fromJson(
                                        chatRoomArray.getJSONObject(i).toString(),
                                        ChatRoomListModel::class.java
                                    )
                                )
                            }
                        }

                        //전체 채팅방에서 내가 참여한 방이있는지 확인하고  빼준다.
                        //내가 참여한 방을 먼저 가져오기떄문에 전체 채팅방에서 모두 뺄수 있음.
                        for (i in 0 until chatRoomJoinedList.size) {
                            chatRoomEntireList.removeAll {
                                it.roomId == chatRoomJoinedList[i].roomId
                            }
                        }
                    }


                    //채팅 전체 채팅방  정렬 조정 할때  맨처음 정렬에서 참여방 뺀 리스트 사이즈가 0일때
                    //다음 리스트 목록이 있다면  새롭게 리스트를 불러준다.
                    if(chatRoomEntireList.size <= 0 && !nextEntireChatRoomResourceUrl.equals("null")){
                        entireOffset += 30
                        getChattingRoomList(entireFilterValue)
                    }else {

                        chatRoomListRcyAdapter.getRoomList(
                            chatRoomEntireList,
                            chatRoomJoinedList,
                            totalChatRoomCount,
                            totalJoinChatRoomCount,
                            false
                        )
                    }

                    binding.chatRoomSwipeRefresh.isRefreshing = false
                }, { throwable ->
                    Logger.v("여기 나옴 ")
                    chatRoomListRcyAdapter.getRoomList(chatRoomEntireList, chatRoomJoinedList,totalChatRoomCount,totalJoinChatRoomCount,true)

                    // Fragment ChattingRoomListFragment not attached to an activity. 방지
                    if(!isAdded) return@getChatRoomList

                    Toast.makeText(
                        requireActivity(),
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.chatRoomSwipeRefresh.isRefreshing = false
                }
            )
        }
    }

    //팝업 다이얼로그 각 타입별로
    private fun showPopUpDialog(popUpType: Int){
        when(popUpType){

            //미달 되는  레벨의 유저일때 나오는 팝업
            POPUP_TYPE_LOW_LEVEL_CREATE_CHAT_ROOM -> {
                Util.showDefaultIdolDialogWithBtn1(
                    requireActivity(),
                    null,
                    requireActivity().getString(R.string.chat_less_level_popup, chatLessLevel)
                ) { v: View? ->
                    Util.closeIdolDialog()
                }
            }



            //최애가  다른 채팅방에 입장 할려고 할때 나오는 팝업
            POPUP_TYPE_DIFFERENT_MOST -> {
                Util.showDefaultIdolDialogWithBtn1(
                    requireActivity(),
                    null,
                    getString(R.string.chat_room_join_popup1)
                ) { view: View? ->
                    Util.closeIdolDialog()
                }
            }


            //최애가 다른 경우에는  채팅방 생성도 불가
            POPUP_TYPE_CREATE_ROOM_DIFFERENT_MOST ->{
                Util.showDefaultIdolDialogWithBtn1(
                        requireActivity(),
                        null,
                        if(mAccount?.userModel?.most?.groupId == idol?.groupId && idol?.getId() == idol?.groupId) {
                            getString(R.string.chat_group_create_fail)
                            //"최애가 개인인 경우에 그룹 채팅방은 만들 수 없습니다."
                        }
                        else{
                        "관리자여도 최애가 아닌 채팅방은 만들 수 없습니다."
                        }
                ) { view: View? ->
                    Util.closeIdolDialog()
                }

            }

            //레벨 제한으로 입장을 할수 없을때 나오는 팝업
            POPUP_TYPE_LOW_LEVEL_ENTER_CHAT_ROOM -> {
                Util.showDefaultIdolDialogWithBtn1(
                    requireActivity(),
                    null,
                    getString(R.string.chat_room_join_popup2)
                ) { view: View? ->
                    Util.closeIdolDialog()
                }
            }


            //최대 인원 수 초과로 채팅방 입장 불가할때 나오는 팝업
            POPUP_TYPE_OVER_MAX_PEOPLE_COUNT -> {
                Util.showDefaultIdolDialogWithBtn1(
                    requireActivity(),
                    null,
                    getString(R.string.chat_room_join_popup3)
                ) { view: View? ->
                    Util.closeIdolDialog()
                }
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    //채팅방 들어가기 api
    private fun enterChatRoom(roomId: Int, chatRoomListModel: ChatRoomListModel) {
        val presentClientTs = System.currentTimeMillis()
        MainScope().launch {
            chatRepository.joinChatRoom(roomId, { response ->
                Logger.v("첫 채팅방 참여할떄 -> "+response.toString())
                Logger.v("현재 시간 ->$presentClientTs")
                if (response?.optBoolean("success")!!) {
                    //채팅방 화면 으로 넘어감
                    //여기서  비교 하는  id 가 들어감. 닉네임, 유저아이디.
                    val userNickname = response.getString("nickname")
                    val userId = response.getInt("user_id")

                    val messageObject = JSONObject()
                        .put("room_id", roomId)
                        .put("user_id", userId)
                        .put("client_ts", presentClientTs)
                        .put("server_ts", presentClientTs)
                        .put("content", getString(R.string.chat_first_join))
                        .put("content_type", MessageModel.CHAT_TYPE_TEXT)
                        .put("is_readable",true)
                        .put("is_first_join_msg",true)
                        .put("account_id", mAccount?.userId ?: 0)

                    val message = IdolGson.getInstance(false)
                        .fromJson(messageObject.toString(), MessageModel::class.java)

                    //Db에링크도 저장.
                    ChatMessageList.getInstance(requireActivity()).setChatMessage(message){
                        Util.log("idoltalk::insert db(sendLinkMessage) -> $message")
                    }

                    Util.log("RoomFrag:: in enter nickName $userNickname userId $userId")
                    startActivityForResult(
                        ChattingRoomActivity.createIntent(
                            activity,
                            roomId,
                            userNickname,
                            userId,
                            chatRoomListModel.role,
                            chatRoomListModel.isAnonymity,
                            chatRoomListModel.title
                        ), REQUEST_FIRST_FINISH_CHATTING
                    )

                } else {//false 로 올때 -> 서버 메세지를  팝업으로 띄어준다.
                    Util.showDefaultIdolDialogWithBtn1(
                        requireActivity(),
                        null,
                        response.optString("msg")
                    ) {
                        // TODO: 2021/04/18 success false 이면  삭제된 방으로 인식하고,  리스트를 update 하낟.
                        entireLimit =30
                        entireOffset=0
                        getChattingRoomList(orderBy = entireFilterValue)
                        Util.closeIdolDialog()
                    }
                }
            }, { throwable ->
                Util.showDefaultIdolDialogWithBtn1(
                    requireActivity(),
                    null,
                    throwable.message
                ) {
                    Util.closeIdolDialog()
                }
            })
        }
    }

    private fun leaveChatRoom(chatRoomListModel: ChatRoomListModel, isRoomMaster: Boolean) {

        //방장 여부에 따라 팝업창의  팝업을 다르게 띄어준다.
        val subTitle = if(isRoomMaster){//내가 방장일때
            if(chatRoomListModel.curPeopleCount ==1){//현재 인원  1명일 때  삭제됨 메세지를 보여준다.
                getString(R.string.chat_room_leave_desc3)
            } else {//사람이 있는 경우는 방장 위임 팝업을 보여준다.
                getString(R.string.chat_room_leave_desc2)
            }
        }else{//내가 그냥 참여자 일떄
            getString(R.string.chat_room_leave_desc1)
        }

        Util.showDefaultIdolDialogWithBtn2(activity,
            getString(R.string.chat_room_leave),
            subTitle,

            //확인 버튼을 눌렀을때 -> 방나가기 동작을 진행한다.
            {

                Util.showProgress(requireActivity())
                //mChatRoomInfo 서버에 null이 들어가면 안되므로 !!처리.
                MainScope().launch {
                    chatRepository.leaveChatRoom(chatRoomListModel.roomId, { response ->
                        if (response.getBoolean("success")) {

                            //해당방의 노티 지워주기
                            deleteNotification(chatRoomListModel.roomId)

                            Logger.v("leave chatroom  성공함. $response")
                            //해당 채팅방을 나가니까  해당 채팅방안에 저장된 메시지들을 지워준다.
                            ChatMessageList.getInstance(requireActivity()).deleteChatRoomMessages(roomId = chatRoomListModel.roomId)

                            //벙을 나갔을때는  리스트 재배치를 진행한다. ->  참여 방 리스트 다시 0으로 리셋
                            joinedLimit=30
                            joinedOffset =0
                            entireLimit =30
                            entireOffset=0

                            getChatRoomJoinedList(entireFilter = entireFilterValue,joinedFilter = joinedFilterValue)

                        }
                    }, { throwable ->
                        Toast.makeText(activity,
                            R.string.error_abnormal_exception,
                            Toast.LENGTH_SHORT).show()
                        if (Util.is_log()) {
                            showMessage(throwable.message)
                        }
                        Util.closeProgress()
                    })
                    Util.closeIdolDialog()
                }
            }, {
                //방 나가기 취소 처리
                Util.closeIdolDialog()
            })
    }
    //해당 방에 들어오면 현재 방의 noti들을   지워준다.
    private fun deleteNotification(roomId: Int){

        //현재방의 채팅  notification 삭제
        val notificationManager:NotificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ChattingRoomActivity.ID_CHATTING_MSG + roomId)

        //statusbar array는 m 버전 이상부터지만,  isGroup 여부를 체크 하려면  n버전 이상 부터여서 그냥  n버전으로  넣음
        val statusBarNotifications: Array<StatusBarNotification> = notificationManager.activeNotifications
        val chatNotiCount:Int =statusBarNotifications.count { it.notification.group == Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW }
        if(chatNotiCount ==1 && statusBarNotifications[0].isGroup && (statusBarNotifications[0].notification.group.equals(Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW)))  {
            notificationManager.cancel(Const.NOTIFICATION_GROUP_ID_CHAT_MSG)
        }else{
            //푸시를 받고  앱을 완전히 종료 하는 경우 ->  해당  그룹 노티가 null 로와서 안지워지는 경우가 있는데 이경우는 그냥 그룹 노티 삭제
            if(chatNotiCount == 1 &&(statusBarNotifications[0].notification.group==null)){
                notificationManager.cancel(Const.NOTIFICATION_GROUP_ID_CHAT_MSG)
            }
        }
        //n 버전  미만은 애초에 그룹핑을 지원하지 않으므로, 그냥  해당 방  노티만  지워주면 된다.
    }


    override fun leaveChatRoomClicked(chatRoomListModel: ChatRoomListModel, isRoomMaster: Boolean) {
        leaveChatRoom(chatRoomListModel,isRoomMaster)
    }




    //채팅방 리스트 아이템  클릭시 이벤트
    override fun onItemClicked(roomId: Int?, isJoinedRoom: Boolean, chatRoomListModel: ChatRoomListModel) {

        // TODO: 2021/03/17 여기서  joinlist 룸 과   join 하지 않은 룸을 구별 해서  join api 를 실행힌디.
            if (roomId != null) {
                if(isJoinedRoom){
                    startActivityForResult(ChattingRoomActivity.createIntent(activity,  roomId, chatRoomListModel.nickName, chatRoomListModel.userId,chatRoomListModel.role, chatRoomListModel.isAnonymity, chatRoomListModel.title), REQUEST_FINISH_CHATTING)
                }else {

                    //구글 anlaystics -> 채팅방 입장 버튼
                    this.setUiActionFirebaseGoogleAnalyticsFragment(
                        Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "chat_room_enter"
                    )

                    enterChatRoom(roomId, chatRoomListModel)
                }
            }

    }

    // TODO: 2021/04/18 reuslt 값 들  다   명확하게 바꿔놓을것!
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //채팅방 생성후  들어갈때 requestCode
       if (requestCode == REQUEST_CREATE_CHATTING) {
                //내가 참여한 목록을 보여주기 위해서
                joinedLimit = 30
                joinedOffset = 0
                entireLimit = 30
                entireOffset = 0
                getChatRoomJoinedList(
                    entireFilter = entireFilterValue,
                    joinedFilter = joinedFilterValue
                )


        }else if(requestCode == REQUEST_FINISH_CHATTING){//일반적으로 채팅방 들어갈때 request code
            if(resultCode == RESULT_OK){//그냥 뒤로가기 했을떄는 전체 채팅방 리스트만 업데이트 해준다.
                entireLimit =30
                entireOffset=0
                getChattingRoomList(orderBy = entireFilterValue)
            }else if(resultCode ==Const.CHATTING_LIST_RESET){//채팅방에서  폭파 및 나가기 또는  비정상 종료로  튕겼을때,  채팅 리스트를  리셋해준다.
                joinedLimit=30
                joinedOffset =0
                entireLimit =30
                entireOffset=0
                getChatRoomJoinedList(entireFilter = entireFilterValue,joinedFilter = joinedFilterValue)
            }
        }else if(requestCode == REQUEST_FIRST_FINISH_CHATTING){//맨처음  채팅방을 들어갈때  request code
            //맨처음 채팅방 들어갈떄도  내가 참여한 채팅방 리스트부터 다시 보여주기 위해
            //전체 리스트 위치를 reset 한다.
               joinedLimit=30
               joinedOffset =0
               entireLimit =30
               entireOffset=0
               getChatRoomJoinedList(entireFilter = entireFilterValue,joinedFilter = joinedFilterValue)

       }
    }


    companion object{
        const val PARAM_IDOL = "idol"

        //채팅방  정렬  타입 분기용
        const val TYPE_CHAT_ROOM_LIST_FILTER_RECENT =0//최신순
        const val TYPE_CHAT_ROOM_LIST_FILTER_MANY_TALK =1//대화 많은 순

        //팝업 타입  분기용
        const val POPUP_TYPE_LOW_LEVEL_CREATE_CHAT_ROOM = 3 //채팅 룸 생성 하기에  레벨 낮음
        const val POPUP_TYPE_DIFFERENT_MOST = 4 //최애 다를떄
        const val POPUP_TYPE_LOW_LEVEL_ENTER_CHAT_ROOM = 5//레벨 제한으로  입장 불가
        const val POPUP_TYPE_OVER_MAX_PEOPLE_COUNT = 6//최대 인원수  초과
        const val POPUP_TYPE_CREATE_ROOM_DIFFERENT_MOST =7

        //request 코드
        const val REQUEST_CREATE_CHATTING = 10
        const val REQUEST_FINISH_CHATTING = 11
        const val REQUEST_FIRST_FINISH_CHATTING =12

        @JvmStatic
        fun newInstance(idol: IdolModel): ChattingRoomListFragment {
            val args = Bundle()
            args.putSerializable(PARAM_IDOL, idol)
            val fragment = ChattingRoomListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}