package net.ib.mn.chatting

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.chatting.chatDb.ChatRoomList
import net.ib.mn.chatting.model.ChatRoomListModel
import net.ib.mn.databinding.ItemChattingRoomListBinding
import net.ib.mn.databinding.ItemHeaderChattingRoomListEntireBinding
import net.ib.mn.databinding.ItemHeaderChattingRoomListJoinBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.getFontColor


class ChattingRoomListAdapter(
    private val onClickListener: OnClickListener,
    private val context: Context,
    private val account: IdolAccount?
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var tvFilterChatRoomJoin:TextView
    lateinit var tvFilterChatRoomEntire:TextView
    private lateinit var mSheet: BottomSheetFragment

    private var roomList=ArrayList<ChatRoomListModel>()


    private var checkNotJoinRoomListCount =0
    private var checkJoinRoomListCount =0
    private var totalChatRoomCount  =0
    private var totalJoinChatRoomCount =0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_CHATTING_ROOM_FILTER_JOIN -> {
                val binding: ItemHeaderChattingRoomListJoinBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_header_chatting_room_list_join,
                    parent,
                    false)
                ChattingRoomFilterHolder(BindingProxy(binding))
            }

            TYPE_CHATTING_ROOM_FILTER_ENTIRE -> {
                val binding: ItemHeaderChattingRoomListEntireBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_header_chatting_room_list_entire,
                    parent,
                    false)
                ChattingRoomFilterHolder(BindingProxy(binding))
            }

            else -> {
                val binding: ItemChattingRoomListBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_chatting_room_list,
                    parent,
                    false)
                ChattingRoomListViewHolder(binding)
            }
        }
    }

    //클릭리스너 interface 구현
    interface OnClickListener {
        fun onItemClicked(position: Int?, isJoinedRoom: Boolean, chatRoomListModel: ChatRoomListModel)//각 아이템 클릭시
        fun leaveChatRoomClicked(chatRoomListModel: ChatRoomListModel,isRoomMaster:Boolean)
    }

    private fun checkRole(role:String?):Boolean{
        return role.equals("O")
    }

    //방 리스트 data 를 가지고 와서  adpater에 적용하고  업데이트 한다.
    fun getRoomList(
        notJoinRoomList: ArrayList<ChatRoomListModel>,
        chatRoomJoinList: ArrayList<ChatRoomListModel>,
        totalChatRoomCount: Int,
        totalJoinChatRoomCount: Int,
        needRoomDB: Boolean
    ) {

         roomList.clear()

        //서버에서 값을  가지고 왓을경우에는  아래 로직 실행
        if(!needRoomDB) {

            this.totalChatRoomCount = totalChatRoomCount
            this.totalJoinChatRoomCount = totalJoinChatRoomCount

            checkNotJoinRoomListCount = notJoinRoomList.size
            checkJoinRoomListCount = chatRoomJoinList.size


            //전체방용  룸필터 아이템 생성
            val notJoinRoomFilter = ChatRoomListModel(accountId = account?.userId ?: 0).apply {
                this.isRoomFilter = true
                this.isJoinedRoom = false
            }

            //내가 참여한 방 용 룸필터 아이템 생성
            val joinRoomFilter = ChatRoomListModel(accountId = account?.userId ?: 0).apply {
                this.isRoomFilter = true
                this.isJoinedRoom = true
            }


            //전체 방 리스트의 수가  0보다 클떄 이전에 필터로 처리했던게 없으면,
            //새로운 필터를 추가해준다.
            if (checkNotJoinRoomListCount > 0) {
                if (!notJoinRoomList[0].isRoomFilter) {
                    notJoinRoomList.add(0, notJoinRoomFilter)
                }
            }

            //내가 참여한 방 리스트 맨처음 값이  룸필터가 아니면,  룸필터를 추가해준다.
            if (checkJoinRoomListCount > 0) {
                if (!chatRoomJoinList[0].isRoomFilter && chatRoomJoinList[0].isJoinedRoom) {
                    chatRoomJoinList.add(0, joinRoomFilter)
                }
            }

            //룸리스트에  필터까지 추가된  전체방과  참여방을   추가해준다.
            this.roomList.addAll(chatRoomJoinList + notJoinRoomList)

            //모델에 현재 로그인한 accountId저장.
            roomList.forEach {
                 it.accountId = account?.userId ?: 0
            }

            //Db에링크도 저장.
            ChatRoomList.getInstance(context).setChatRoom(roomList)
            notifyDataSetChanged()

        }else{//서버에서 값을 못가지고오면 room에  저장된 방 리스트 정보를 뿌려준다.

            //룸디비에서 룸리스트를 가지고 와서 넣어준다.
            this.roomList.addAll(ChatRoomList.getInstance(context).getAll())
            //리스트 업데이트
            notifyDataSetChanged()
        }

        Util.closeProgress()
     }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if(roomList[position].isRoomFilter){
            (holder as ChattingRoomFilterHolder).bind(position)
        }else{
            (holder as ChattingRoomListViewHolder).bind(position)
        }
    }

    override fun getItemCount(): Int {
        return roomList.size
    }

    override fun getItemViewType(position: Int): Int {
        return  if(roomList[position].isRoomFilter){
            if(roomList[position].isJoinedRoom){
                TYPE_CHATTING_ROOM_FILTER_JOIN
            }else{
                TYPE_CHATTING_ROOM_FILTER_ENTIRE
            }
        }else{
            TYPE_CHATTING_ROOM
        }
    }

    //채팅방 리스트 필터 선택 뷰 처리
    fun setChatRoomFilterSelected(status: Int, isJoinedRoom: Boolean){
        when(status){
            //대화 많은 순
            ChattingRoomListFragment.TYPE_CHAT_ROOM_LIST_FILTER_MANY_TALK -> {
                if (isJoinedRoom) {
                    tvFilterChatRoomJoin.text = context.getText(R.string.chat_many_talk_at)
                } else {
                    tvFilterChatRoomEntire.text = context.getText(R.string.chat_many_talk_at)

                }
            }

            //최신순
            ChattingRoomListFragment.TYPE_CHAT_ROOM_LIST_FILTER_RECENT -> {
                if (isJoinedRoom) {
                    tvFilterChatRoomJoin.text =  context.getText(R.string.freeboard_order_newest)

                } else {
                    tvFilterChatRoomEntire.text = context.getText(R.string.freeboard_order_newest)

                }
            }
        }
    }

    //채팅룸 리스트 holder
    inner class ChattingRoomListViewHolder(val binding: ItemChattingRoomListBinding) :
        RecyclerView.ViewHolder(binding.root),View.OnCreateContextMenuListener{

        fun bind(position: Int) { with(binding) {
            //해당 룸의  레벨 제한이 0 초과일때 레벨 제한이 있음으로 보여준다.
            if(roomList[position].levelLimit > 0){
                tvChattingLevel.visibility=View.VISIBLE
                tvChattingLevel.text = "Lv."+roomList[position].levelLimit.toString()
                if(BuildConfig.CELEB) {
                    val roomLevelLimit = tvChattingLevel.background as GradientDrawable
                    roomLevelLimit.setColor(Color.parseColor(CommunityActivity.mType?.getFontColor(context)))
                }
            }else{//레벨 제한이 없는 방의 경우 -> 레벨 tv  gone 처리
                tvChattingLevel.visibility = View.GONE
            }

            //익명방 여부
            if(roomList[position].isAnonymity.equals("Y")){
                tvAnonymous.visibility = View.VISIBLE
                if(BuildConfig.CELEB) {
                    tvAnonymous.setTextColor(Color.parseColor(CommunityActivity.mType?.getFontColor(context)))
                }
            }else{
                tvAnonymous.visibility = View.GONE
            }

            //일단 캐릭터  적용이 있었는데 소개랑  겹쳐서  빠짐
            //그런데 나중에  다시 추가될수도 있으니 로직은 남기고 gone으로  처리함.
            if(roomList[position].isDefault.equals("Y")){
                imgDefaultChattingCharacter.visibility= View.GONE
            }else{
                imgDefaultChattingCharacter.visibility= View.GONE
            }

            //채팅방 title 적용
            tvChattingRoomName.text = roomList[position].title

            //채팅방 desc 적용  -> desc 는 필수가 아니어서,  없는 경우  다른 텍스트들  item 가운데 정렬을 위해  visiblity 처리를 넣음
            if(roomList[position].desc.isNullOrEmpty()){
                tvChatRoomDesc.visibility = View.GONE
            }else{
                tvChatRoomDesc.visibility = View.VISIBLE
                tvChatRoomDesc.text = roomList[position].desc
            }

            //아이템  전체 클릭됨
            itemView.setOnClickListener {
                onClickListener.onItemClicked(
                    roomList[position].roomId,
                    roomList[position].isJoinedRoom,
                        roomList[position]
                )
            }

            itemView.setOnCreateContextMenuListener(this@ChattingRoomListViewHolder)
        }}

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {

            val chatRoom = roomList[adapterPosition]

            //내가 참여한 채팅방이면  나가기 가능
            if (chatRoom.isJoinedRoom) {
                //참여한 채팅방 나가기 메뉴
                val outChattingRoom = menu?.add(Menu.NONE, 1002, 3, R.string.chat_room_leave)
                outChattingRoom?.setOnMenuItemClickListener(onEditMenu)
            }

        }

        //컨텍스트 메뉴에서 항목 클릭시 동작을 설정합니다.
        private val onEditMenu = MenuItem.OnMenuItemClickListener {
            when (it.order) {
                JOIN_CHATTING_ROOM_OUT -> {//채팅방 나감
                     onClickListener.leaveChatRoomClicked(roomList[adapterPosition],checkRole(roomList[adapterPosition].role))
                }
            }
            return@OnMenuItemClickListener true
        }
    }

    //채팅 룸  필터용 holder
    inner class ChattingRoomFilterHolder(val binding: BindingProxy) : RecyclerView.ViewHolder(binding.root){

        private val tvChattingRoomCount = binding.tvChattingRoomCount
        private val tvChattingRoomFilter = binding.tvChattingRoomFilter
        private val llChattingRoomFilter = binding.llChattingRoomFilter

        fun bind(position: Int){
            if(roomList[position].isJoinedRoom){//
                tvFilterChatRoomJoin=tvChattingRoomFilter
                tvChattingRoomCount.text = String.format(context.getString(R.string.chat_list_join),totalJoinChatRoomCount)

            }else{
                tvFilterChatRoomEntire=tvChattingRoomFilter

                //전체 채팅방 수는 (전체 채팅방수 - 햔제 침야힌 체팅방수 )
                tvChattingRoomCount.text = String.format(context.getString(R.string.chat_list_all),totalChatRoomCount-totalJoinChatRoomCount)
            }

            llChattingRoomFilter.setOnClickListener {
                mSheet = BottomSheetFragment.newInstance(
                    BottomSheetFragment.FLAG_CHAT_ROOM_LIST_FILTER,
                    roomList[position].isJoinedRoom
                )
                val tag = "filter"
                val oldFrag = (context as CommunityActivity).supportFragmentManager.findFragmentByTag(
                    tag
                )
                if (oldFrag == null) {
                    mSheet.show((context).supportFragmentManager, tag)

                }
            }
        }
    }

    inner class BindingProxy {
        val root: View
        val tvChattingRoomCount: AppCompatTextView
        val tvChattingRoomFilter: AppCompatTextView
        val llChattingRoomFilter: LinearLayoutCompat

        constructor(binding: ItemHeaderChattingRoomListEntireBinding) {
            root = binding.root
            tvChattingRoomCount = binding.tvChattingRoomCountEntire
            tvChattingRoomFilter = binding.tvFilterEntire
            llChattingRoomFilter = binding.llFilterEntire
        }

        constructor(binding: ItemHeaderChattingRoomListJoinBinding) {
            root = binding.root
            tvChattingRoomCount = binding.tvChattingRoomCountJoin
            tvChattingRoomFilter = binding.tvFilterJoin
            llChattingRoomFilter = binding.llFilterJoin
        }
    }

    companion object {
        const val TYPE_CHATTING_ROOM_FILTER_JOIN = 0
        const val TYPE_CHATTING_ROOM_FILTER_ENTIRE =1
        const val TYPE_CHATTING_ROOM = 2
        const val JOIN_CHATTING_ROOM_OUT = 3
    }
}
