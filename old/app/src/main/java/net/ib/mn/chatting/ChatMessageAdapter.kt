package net.ib.mn.chatting

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.text.SpannableString
import android.text.method.ArrowKeyMovementMethod
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import net.ib.mn.utils.Toast
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.github.mikephil.charting.utils.Utils
import com.google.gson.reflect.TypeToken
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.model.ChatMembersModel
import net.ib.mn.chatting.model.ChatMsgLinkModel
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.databinding.AwardsRankingFragmentBinding
import net.ib.mn.databinding.ItemChatLinkMessageBinding
import net.ib.mn.databinding.ItemChatLinkMessageMineBinding
import net.ib.mn.databinding.ItemChatLinkMessageMineStartBinding
import net.ib.mn.databinding.ItemChatLinkMessageStartBinding
import net.ib.mn.databinding.ItemChatMessageBinding
import net.ib.mn.databinding.ItemChatMessageFirstJoinBinding
import net.ib.mn.databinding.ItemChatMessageMineBinding
import net.ib.mn.databinding.ItemChatMessageMineStartBinding
import net.ib.mn.databinding.ItemChatMessageStartBinding
import net.ib.mn.databinding.NewFragmentRankingBinding
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.*
import net.ib.mn.onepick.OnepickMatchActivity
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.modelToString
import net.ib.mn.view.ExodusImageView
import net.ib.mn.view.TalkLinkImageView
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class ChatMessageAdapter(
    private val mContext: Context,
    private val glideRequestManager: RequestManager,
    private val mItems: MutableList<MessageModel>,
    private val account: IdolAccount,
    private var members: CopyOnWriteArrayList<ChatMembersModel>,
    private var isAnonymity: String?,
    private var mUserId:Int?,
    private val displaySize : Int,
    private val showLastImageListener: ShowLastImageListener,
    private val photoClick:(MessageModel, View, Int) -> Unit,
) : RecyclerView.Adapter<ChatMessageAdapter.ViewHolder>() {
    private var onUserProfileClickListener: OnUserProfileClickListener? = null

    private val gson = IdolGson.getInstance()
    private val emoListType = object : TypeToken<ArrayList<EmoticonDetailModel>>() {}.type

    private var clickedPosition: Int = 0
    private var needScrollDown : Boolean = false    //스크롤 아래로 내리는 것이 필요한지 체크하기 위한 변수
    private var dateWidth : Int = 0

    init {


        val duplicateMessage = mItems.groupingBy { it }.eachCount().filter { it.value>2 }
        Logger.v("duplicatei ->>><>>>>>>>"+ duplicateMessage)

        //첫번째 메세지 db에  있을때 항상  맨위로 올라오도록  수정
        val firstJoinMsg = mItems.find { it.isFirstJoinMsg }
        if (firstJoinMsg != null) {
            val firstMsgIndex = mItems.indexOf(firstJoinMsg)
            if (firstMsgIndex != 0) {
                mItems.remove(firstJoinMsg)
                firstJoinMsg.serverTs = mItems[0].serverTs - 1
                mItems.add(0, firstJoinMsg)
            }
        }
    }

    fun setMembers(members:CopyOnWriteArrayList<ChatMembersModel>){
        this.members = members
        notifyDataSetChanged()
    }

    fun setOgChatMessage(ogMessages:List<MessageModel>,callback: (()->Unit)?){

        if(ogMessages.isNotEmpty()){
            for(element in ogMessages){
                Logger.v("aasdaasasasassadsad ->"+element)
               //if(this.mItems.any { it == element }){
                   mItems.remove(mItems.find { it.serverTs == element.serverTs && it.userId == element.userId } )
              // }
            }
            this.mItems.addAll(ogMessages)

            this.mItems.sortBy { it.serverTs }

            //첫번째 메세지 db에  있을때 항상  맨위로 올라오도록  수정
            val firstJoinMsg = mItems.find { it.isFirstJoinMsg }
            if (firstJoinMsg != null) {
                //만약 첫참여 메시지가 맨처음이 아닐경우 해당 server_ts를 가장첫번째 메시지의 -1 해준걸로 넣어준다.
                val firstMsgIndex = mItems.indexOf(firstJoinMsg)
                if (firstMsgIndex != 0) {
                    mItems.remove(firstJoinMsg)
                    firstJoinMsg.serverTs = mItems[0].serverTs - 1
                    mItems.add(0, firstJoinMsg)
                }
            }

            notifyDataSetChanged()
            callback?.invoke()

        }
    }

    fun setRoomStatus(isAnonymity:String){
        this.isAnonymity = isAnonymity
        notifyDataSetChanged()
    }

    fun setUserId(userId:Int){
        this.mUserId = userId
    }

    interface OnPhotoClickListener{
        fun onPhotoClick(model:MessageModel, v: View?, position: Int)
    }

    //
    interface OnUserProfileClickListener{
        fun onProfileClick(chatMemberInfo:ChatMembersModel?)
    }

    interface ShowLastImageListener{
        fun showLastImage()
    }

    //유저 피드 눌렀을때  클릭 리스너
    fun setOnUserProfileClickListener(onUserProfileClickListener: OnUserProfileClickListener) {
        this.onUserProfileClickListener = onUserProfileClickListener
    }

    fun getItem(index: Int): MessageModel? {
        return if( index < mItems.size ) mItems[index] else null
    }

    fun linkClicked(url: String) {
        if (Util.checkUrls(url).isNullOrEmpty()) return

        val intent = Intent(mContext, AppLinkActivity::class.java).apply {
            data = Uri.parse(url)
        }
        mContext.startActivity(intent)
    }

    override fun getItemCount(): Int {
        return mItems.size + 1 // 마지막에 여백용 footer 추가
    }

    //메시지 삭제,복사 ,신고 필요
    fun getClickedPosition(): Int {
        return clickedPosition
    }

    private fun setClickedPosition(position: Int) {
        this.clickedPosition = position
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.itemView.setOnLongClickListener(null)
        super.onViewRecycled(holder)
    }

    override fun getItemViewType(position: Int): Int {
        if (position == mItems.size) {
            return TYPE_CHAT_FOOTER
        }
        // 연결 끊어진 후 다시 붙을 때 NPE
//        Logger.v("이거 중요한 체크야+++++ ${position}->"+mItems)
//        Logger.v("이거 중요한 체크야 ${position}->"+mItems[position])
            //남이 보낸  메세지 인경우
        if(mItems[position].userId != this.mUserId){
            return if(position==0){
                  if(mItems[position].isLinkUrl && !mItems[position].reported  && !mItems[position].deleted){
                      TYPE_CHAT_LINK_URL_START
                  }else{
                      TYPE_CHAT_START
                  }
            }else{

                if(mItems[position].isLinkUrl && !mItems[position].reported  && !mItems[position].deleted){
                    //이전 메세지가  다른 유저의 메세지일때  or  이전메세지가  lastmessage true 일때  다시 처음 시작 메세지로
                    if(((mItems[position-1].userId != mItems[position].userId)) || checkLastMessage(position-1)){
                        TYPE_CHAT_LINK_URL_START
                    }else{
                        TYPE_CHAT_LINK_URL
                    }
                }else{
                    //이전 메세지가  다른 유저의 메세지일때  or  이전메세지가  lastmessage true 일때  다시 처음 시작 메세지로
                    if(((mItems[position-1].userId != mItems[position].userId)) || checkLastMessage(position-1)){
                        TYPE_CHAT_START
                    }else{
                        TYPE_CHAT
                    }
                }
            }
        }else{//내가 보낸  메세지 인경우
            return if(position==0){

                if(mItems[0].isFirstJoinMsg){//isjoinmessage일때

                    Logger.v("가져와 베이 ->>> "+mItems[0].content)
                  TYPE_CHAT_FIRST_JOIN
                }else{
                    if(mItems[position].isLinkUrl && !mItems[position].reported  && !mItems[position].deleted){
                        TYPE_CHAT_MINE_LINK_URL_START
                     }else{
                        TYPE_CHAT_MINE_START
                    }
                }
            }else{

                //정렬이 안되었을때 첫참여메시지 해당뷰로 보이게하기.
                if(mItems[position].isFirstJoinMsg){
                    TYPE_CHAT_FIRST_JOIN
                } else{
                    //url link 이면서  신고랑 삭제 가 안당했던  메세지  -> url 로 처리
                    if(mItems[position].isLinkUrl && !mItems[position].reported  && !mItems[position].deleted){
                        if(((mItems[position-1].userId != mItems[position].userId)) || checkLastMessage(position-1) || mItems[position-1].isFirstJoinMsg){
                            TYPE_CHAT_MINE_LINK_URL_START
                        }else{
                            TYPE_CHAT_MINE_LINK_URL
                        }
                    }else{
                        //이전 메세지가  다른 유저의 메세지일때  or  이전메세지가  lastmessage true 일때  다시 처음 시작 메세지로
                        if(((mItems[position-1].userId != mItems[position].userId)) || checkLastMessage(position-1) || mItems[position-1].isFirstJoinMsg){
                            TYPE_CHAT_MINE_START
                        }else{
                            TYPE_CHAT_MINE
                        }
                    }
                }

            }
        }
    }

    // 메시지 보내기 실패했을때 느낌표 아이콘으로 변경.
    fun sendFailedMessage(failedItemIndex: Long) {
        for ((index, chatMessage) in mItems.withIndex()) {
            if (chatMessage.clientTs == failedItemIndex){ //이건 무조건 clientTs로 비교해줘야됨.
                Util.log("Thread timer adapter:: ${chatMessage.content}")
                mItems[index].statusFailed = true
                notifyItemChanged(index)
            }
        }
    }

    //메시지 업데이트.
    fun updateChatMessage(updateItemTs: Long) {
        for ((index, chatMessage) in mItems.withIndex()) {
            if (chatMessage.serverTs != updateItemTs) continue
            Util.log("idoltalk::update content -> ${chatMessage.content}")
            mItems[index].content = mContext.resources.getString(R.string.chat_deleted_message)
            mItems[index].deleted = true
            notifyItemChanged(index)
            if (chatMessage.userId == mUserId) {
                Toast.makeText(mContext,
                    R.string.tiele_friend_delete_result,
                    Toast.LENGTH_SHORT).show()
            }
            break
        }
    }

    //메시지 삭제.
    fun deleteChatMessage(deletedItemTs: Long) {
        for ((index, chatMessage) in mItems.withIndex()) {
            if (chatMessage.serverTs != deletedItemTs) continue
            Util.log("idoltalk::deleted content -> ${chatMessage.content}")
            mItems.removeAt(index)
//            notifyItemChanged(index)
            notifyItemRangeChanged(index-2, index)
            break
        }
    }

    fun sendImageSocket(){
        needScrollDown = false
    }

    fun reportMessage(reportedItemTs: Long, callback: ((MessageModel) -> Unit)?) {
        for ((index, message) in mItems.withIndex()) {
            if (message.serverTs != reportedItemTs) continue

            mItems[index].apply {
                content = mContext.resources.getString(R.string.already_reported)
                reported = true
            }
            notifyItemChanged(index)
            callback?.invoke(message)
//            if (message.userId != mUserId) {
//                Util.showDefaultIdolDialogWithBtn1(
//                    mContext,
//                    null,
//                    mContext.resources?.getString(R.string.report_done),
//                    { Util.closeIdolDialog() },
//                    true)
//            }
            break
        }
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
        lateinit var viewHolder: ViewHolder
        val view: View

        when (viewType) {
            TYPE_CHAT_FOOTER -> {
                view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_footer, parent, false)
                viewHolder = FooterViewHolder(view)
            }
            TYPE_CHAT_LINK_URL ->{
                val binding: ItemChatLinkMessageBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_chat_link_message,
                    parent,
                    false)
                viewHolder = LinkUrlChatViewHolder(BindingProxy(binding))
            }
            TYPE_CHAT_LINK_URL_START ->{
                val binding: ItemChatLinkMessageStartBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_chat_link_message_start,
                    parent,
                    false)
                viewHolder = LinkUrlChatViewHolder(BindingProxy(binding))
            }

            TYPE_CHAT_MINE_LINK_URL->{
                val binding: ItemChatLinkMessageMineBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_chat_link_message_mine,
                    parent,
                    false)
                viewHolder = MyLinkUrlChatViewHolder(BindingProxy(binding))
            }
            TYPE_CHAT_MINE_LINK_URL_START ->{
                val binding: ItemChatLinkMessageMineStartBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_chat_link_message_mine_start,
                    parent,
                    false)
                viewHolder = MyLinkUrlChatViewHolder(BindingProxy(binding))
            }

            TYPE_CHAT_MINE_START ->{
                val binding: ItemChatMessageMineStartBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_chat_message_mine_start,
                    parent,
                    false)
                viewHolder = MyChatViewHolder(MineBindingProxy(binding))
            }

           TYPE_CHAT_MINE -> {
               val binding: ItemChatMessageMineBinding = DataBindingUtil.inflate(
                   LayoutInflater.from(parent.context),
                   R.layout.item_chat_message_mine,
                   parent,
                   false)
               viewHolder = MyChatViewHolder(MineBindingProxy(binding))
            }

            TYPE_CHAT->{
                val binding: ItemChatMessageBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_chat_message,
                    parent,
                    false)
                viewHolder = ChatViewHolder(ChatMessageBindingProxy(binding))
            }

            TYPE_CHAT_START->{
                val binding: ItemChatMessageStartBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_chat_message_start,
                    parent,
                    false)
                viewHolder = ChatViewHolder(ChatMessageBindingProxy(binding))
            }

            TYPE_CHAT_FIRST_JOIN->{
                val binding: ItemChatMessageFirstJoinBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_chat_message_first_join,
                    parent,
                    false)
                viewHolder = FirstJoinChatViewHolder(binding)
            }
        }
        return viewHolder
    }

    private fun checkSameDay(position: Int, context: Context):Boolean{
        var isSameDay = true
        if (position == 0) {
            isSameDay = false
        } else {
            val prev = mItems[position - 1]
            val sdf = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(context))
            if (sdf.format(prev.serverTs) != sdf.format(mItems[position].serverTs)) {
                isSameDay = false
            }
        }
        return isSameDay
    }


    //가장  마지막  메세지 인지 체크 하기
    //마지막인 경우는 리스트의 마지막일 경우나,
    //다음 posiition의 userid가  다를때
    //다음 포지션의 날짜 분대가 다를때
    private fun checkLastMessage(position: Int):Boolean{
        var isLastMessage = false

        if (position == mItems.size - 1) {
            isLastMessage = true
        } else if (mItems[position].userId != mItems[position + 1].userId) {
            isLastMessage = true
        } else {
            Logger.v("position check ->$position")
            if (position > 0 && position < mItems.size - 1) {
                val presentMessageTime = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(Date(mItems[position].serverTs))
                val nextMessageTime = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(Date(mItems[position + 1].serverTs))
                if (!presentMessageTime.equals(nextMessageTime)) {
                    isLastMessage = true
                }
            }
        }

        return isLastMessage
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        try {
             when (holder.itemViewType) {
                TYPE_CHAT -> {
                    holder.bind(mItems[position], position, checkSameDay(position, holder.itemView.context),checkLastMessage(position))
                }
                TYPE_CHAT_START -> {
                    holder.bind(mItems[position], position, checkSameDay(position, holder.itemView.context),checkLastMessage(position))
                }
                TYPE_CHAT_MINE_START -> {
                    holder.bind(mItems[position], position, checkSameDay(position, holder.itemView.context),checkLastMessage(position))
                }
                TYPE_CHAT_MINE -> {
                    holder.bind(mItems[position], position, checkSameDay(position, holder.itemView.context),checkLastMessage(position))
                }
                TYPE_CHAT_FIRST_JOIN->{
                    holder.bind(mItems[position], position, checkSameDay(position, holder.itemView.context),checkLastMessage(position))
                }
                TYPE_CHAT_MINE_LINK_URL->{
                    holder.bind(mItems[position], position, checkSameDay(position, holder.itemView.context),checkLastMessage(position))
                }

                TYPE_CHAT_LINK_URL ->{
                    holder.bind(mItems[position], position, checkSameDay(position, holder.itemView.context),checkLastMessage(position))
                }

                TYPE_CHAT_LINK_URL_START ->{
                    holder.bind(mItems[position], position, checkSameDay(position, holder.itemView.context),checkLastMessage(position))
                }

                TYPE_CHAT_MINE_LINK_URL_START->{
                    holder.bind(mItems[position], position, checkSameDay(position, holder.itemView.context),checkLastMessage(position))
                }
            }


            if(holder.itemViewType != TYPE_CHAT_FIRST_JOIN) {
                holder.itemView.findViewById<View>(R.id.message_chat)?.setOnClickListener {
                    Util.hideSoftKeyboard(mContext, it)
                }

                holder.itemView.findViewById<View>(R.id.message_wrapper_chat)?.setOnLongClickListener {
                    setClickedPosition(holder.adapterPosition)
                    return@setOnLongClickListener false
                }

                holder.itemView.findViewById<View>(R.id.message_chat)?.setOnLongClickListener {
                    setClickedPosition(holder.adapterPosition)
                    return@setOnLongClickListener false
                }

                holder.itemView.findViewById<View>(R.id.photo_chat)?.setOnLongClickListener {
                    setClickedPosition(holder.adapterPosition)
                    return@setOnLongClickListener false
                }

                holder.itemView.findViewById<View>(R.id.tv_nickname_chat)?.setOnLongClickListener {
                    setClickedPosition(holder.adapterPosition)
                    return@setOnLongClickListener false
                }

                holder.itemView.findViewById<View>(R.id.ll_preview_wrapper_chat)?.setOnLongClickListener {
                    setClickedPosition(holder.adapterPosition)
                    return@setOnLongClickListener false
                }
            }

        }catch (e:Exception){
            e.printStackTrace()
        }
    }


    inner class LinkUrlChatViewHolder(val binding: BindingProxy): ViewHolder(binding.root), View.OnCreateContextMenuListener {
        init {
            itemView.setOnCreateContextMenuListener(this)

        }
       override fun onCreateContextMenu(menu: ContextMenu?,
                                         v: View?,
                                         menuInfo: ContextMenu.ContextMenuInfo?) {
            binding.message.movementMethod = ArrowKeyMovementMethod.getInstance()

            val menuIds = intArrayOf(
                    ChattingRoomActivity.MENU_COPY,
                    ChattingRoomActivity.MENU_REPORT,
                    ChattingRoomActivity.MENU_DELETE)
            val menuItems = arrayOf(mContext.getString(android.R.string.copy),
                    mContext.getString(R.string.report),
                    mContext.getString(R.string.remove))

            // 운영자는 delete 메뉴가 나오게
            var showDelete = false
            if (account.heart == Const.LEVEL_ADMIN
                    || account.heart == Const.LEVEL_MANAGER) {
                showDelete = true
            }

            for (i in 0 until menuItems.size - if (showDelete) 0 else 1) {
                menu?.add(Menu.NONE, menuIds[i], i, menuItems[i])
                val item = menu?.getItem(i)
                val spanString = SpannableString(item.toString())
                spanString.setSpan(mContext.resources.getColor(R.color.gray1000,mContext.theme), 0, spanString.length, 0)
                item?.title = spanString
            }
        }


        override fun bind(item: MessageModel, position: Int, isSameDay: Boolean, isLastMessage: Boolean) {

            val gson = IdolGson.getInstance(true)
            val linkContent = gson.fromJson(item.content, ChatMsgLinkModel::class.java)
            val convertedDate = Date(item.serverTs)
            Logger.v("link dat33333 ${item.content}")
            Logger.v("link dat33333 $linkContent")

            //아이템 컨테이너  클릭시 키보드 사라지게 만듬
            itemView.setOnClickListener {
                Util.hideSoftKeyboard(mContext,it)
            }

            binding.message.text = linkContent.originalMsg//original 값 넣어줌.
            glideRequestManager
                .load(linkContent.imageUrl)
                .into(binding.previewImage)
            binding.previewDescription.text = linkContent.description
            binding.previewTitle.text = linkContent.title

            if(linkContent.imageUrl == null && linkContent.title == null){
                binding.previewWrapper.visibility= View.GONE
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.clChatMessage)
                constraintSet.connect(R.id.date_chat,ConstraintSet.START,R.id.message_wrapper_chat,ConstraintSet.END)
                constraintSet.applyTo(binding.clChatMessage)
            }else{
                binding.previewWrapper.visibility= View.VISIBLE
            }


            //프리뷰 클릭 이벤트 -> url 에 해당 하는 곳으로 넘어감
            binding.previewWrapper.setOnClickListener {
                if (!linkContent.detectUrl.isNullOrEmpty()) {
                    linkContent.detectUrl?.let { it1 ->
                        linkClicked(it1)
                    }
                }
            }
              //익명방일땐 닉네임 보이기.
            if(isAnonymity == "Y"){
                val params:ConstraintLayout.LayoutParams = binding.messageWrapper.layoutParams as ConstraintLayout.LayoutParams
                if(itemViewType == TYPE_CHAT_LINK_URL_START){//익명방의 경우도 첫 시작 채팅만  닉네임이 보이도록 한다.
                    binding.tvNickname.visibility = View.VISIBLE
                    binding.tvNickname.setTextColor(ContextCompat.getColor(mContext,R.color.gray250))
                    params.topMargin =Util.convertDpToPixel(mContext,5F).toInt()
                    params.leftMargin =Util.convertDpToPixel(mContext,2F).toInt()
                    binding.messageWrapper.layoutParams = params

                    //익명 닉네임 넣기.(이것도 첫시작만 알수없음 표시).
                    members.find { it.id==item.userId && !it.deleted }.apply {
                        if (this != null) {
                            binding.tvNickname.text = this.nickname
                        }else{
                            binding.tvNickname.text = mContext.getString(R.string.chat_leave_user)
                        }
                    }


                }else{
                    binding.tvNickname.visibility = View.GONE
                    params.topMargin =Util.convertDpToPixel(mContext,2F).toInt()
                    params.leftMargin =Util.convertDpToPixel(mContext,2F).toInt()
                    binding.messageWrapper.layoutParams = params
                }
                binding.userPhoto?.visibility = View.GONE
                binding.userLevel?.visibility = View.GONE


            }else if(isAnonymity == "N"){
                Util.log("ChatAdapter:: 익명아니에요.")
                //익명방이 아닐때 두개다 gone처리.

                // TODO: 2021/04/27 일단 이렇게  막았는데 나중에  타입별로 완전히 viewholder 를  분리하자.
                if(itemViewType== TYPE_CHAT_LINK_URL_START){
                    binding.tvNickname.visibility = View.VISIBLE
                    binding.tvNickname.setTextColor(ContextCompat.getColor(mContext,R.color.text_chat))
                    binding.userPhoto?.visibility = View.VISIBLE

                    members.find { it.id==item.userId && !it.deleted }.apply {
                        if(this != null){
                            binding.userLevel?.visibility = View.VISIBLE
                            binding.tvNickname.text = this.nickname
                            binding.userLevel?.setImageDrawable(Util.getLevelImageDrawable(mContext, this))
                            binding.userPhoto?.let {
                                glideRequestManager
                                    .load(this.imageUrl)
                                    .apply(RequestOptions.circleCropTransform())
                                    .error(Util.noProfileImage(this.id))
                                    .fallback(Util.noProfileImage(this.id))
                                    .placeholder(Util.noProfileImage(this.id))
                                    .into(binding.userPhoto)
                            }
                        }else{

                            binding.userLevel?.visibility = View.GONE
                            binding.tvNickname.text = mContext.getString(R.string.chat_leave_user)
                            binding.userPhoto?.let {
                                glideRequestManager
                                    .load(Util.noProfileImage(-1))
                                    .apply(RequestOptions.circleCropTransform())
                                    .error(Util.noProfileImage(-1))
                                    .fallback(Util.noProfileImage(-1))
                                    .placeholder(Util.noProfileImage(-1))
                                    .into(binding.userPhoto)
                            }
                        }
                    }
                }
            }

            //다른 유저  프로필  사진 눌렀을때  유저 피드로 넘어가기위해  usermodel 값을  넘긴다.
            binding.userPhoto?.setOnClickListener {
                val chatMember = members.find { it.id==item.userId } //챗 멤버리스트에서  해당 클릭된 유저의 유저 아이디에 해당하는 멤버를 찾느다.
                Logger.v("most id 체크 "+chatMember?.toString())
                onUserProfileClickListener?.onProfileClick(chatMember)
            }

            //기본 View 넣어주기.
            binding.date.text = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtil.getAppLocale(binding.root.context)).format(convertedDate)

            var messageLeftPadding =0
            if(isAnonymity=="Y"){//다른 사람의 채팅 메세지의 경우  익명방일때  왼족간격을 좀더 띄어준다.
                messageLeftPadding = Util.convertDpToPixel(mContext, 10F).toInt()
            }

            //시간 보여줌 여부.
            if(isLastMessage){
                binding.clChatMessage.setPadding(messageLeftPadding,0,0, Util.convertDpToPixel(mContext, 6F).toInt())
                binding.date.visibility = View.VISIBLE
            }else{
                binding.clChatMessage.setPadding(messageLeftPadding,0,0, 0)
                binding.date.visibility = View.INVISIBLE
            }

            val params = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            val marginValue = Util.convertDpToPixel(mContext, 10f).toInt()

            //같으날일땐 날짜는 보여주지 않는다.
            if(isSameDay) {
                params.setMargins(0, 0, 0, 0)

                binding.tvDay.visibility = View.GONE
            }else{
                params.leftToLeft = binding.clChatMessage.id
                params.rightToRight = binding.clChatMessage.id
                params.topToTop = binding.clChatMessage.id

                if (position == 0) {
                    params.setMargins(0, marginValue, 0, marginValue)
                } else {
                    params.setMargins(0, 0, 0, marginValue)
                }

                binding.tvDay.text = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context)).format(convertedDate)
                binding.tvDay.visibility = View.VISIBLE
            }

            binding.tvDay.layoutParams = params


            if(itemViewType== TYPE_CHAT_LINK_URL_START){
                binding.messageWrapper.setBackgroundResource(R.drawable.bg_usertalk_start)
            }else if(itemViewType== TYPE_CHAT_LINK_URL){
                binding.messageWrapper.setBackgroundResource(R.drawable.bg_usertalk)
            }


            val listener = View.OnClickListener { v ->
                photoClick(item, v, position)
            }
            binding.photo.setOnClickListener(listener)

            if(item.deleted){

                Logger.v("deleteted"+item.content)
                binding.message.text = mContext.resources.getString(R.string.chat_deleted_message)
                binding.message.setTextColor(ContextCompat.getColor(mContext,R.color.gray300))
            }else{
                Logger.v("reported ->"+item.reported)
                if(item.reported){
                    binding.message.text = mContext.resources.getString(R.string.already_reported)
                    binding.message.setTextColor(ContextCompat.getColor(mContext, R.color.gray300))
                }else{
                    binding.message.text = linkContent.originalMsg
                    binding.message.setTextColor(ContextCompat.getColor(mContext, R.color.gray900))
                }
            }

        }


    }


    inner class MyLinkUrlChatViewHolder(val binding: BindingProxy): ViewHolder(binding.root), View.OnCreateContextMenuListener {
        init {
            itemView.setOnCreateContextMenuListener(this)

        }
        override fun onCreateContextMenu(menu: ContextMenu?,
                                         v: View?,
                                         menuInfo: ContextMenu.ContextMenuInfo?) {
            binding.message.movementMethod = ArrowKeyMovementMethod.getInstance()

            val menuIds = mutableListOf(
                ChattingRoomActivity.MENU_COPY,
                ChattingRoomActivity.MENU_DELETE)
            val menuItems = mutableListOf(mContext.getString(android.R.string.copy),
                mContext.getString(R.string.title_remove))

            if(!mItems[adapterPosition].status && !mItems[adapterPosition].statusFailed){
                menuItems.removeAt(1)
                menuIds.removeAt(1)
            }

            if(mItems[adapterPosition].deleted){
                menu?.add(Menu.NONE, menuIds[0], 0, menuItems[0])
                val item = menu?.getItem(0)
                val spanString = SpannableString(item.toString())
                spanString.setSpan(mContext.resources.getColor(R.color.gray1000, mContext.theme), 0, spanString.length, 0)
                item?.title = spanString
            }else{
                // 자기 메시지는 지울 수 있게
                for (i in menuItems.indices) {
                    menu?.add(Menu.NONE, menuIds[i], i, menuItems[i])
                    val item = menu?.getItem(i)
                    val spanString = SpannableString(item.toString())
                    spanString.setSpan(mContext.resources.getColor(R.color.gray1000,mContext.theme), 0, spanString.length, 0)
                    item?.title = spanString
                }
            }
        }


        override fun bind(myItem: MessageModel, position: Int, isSameDay: Boolean, isLastMessage: Boolean) {

            //아이템 컨테이너  클릭시 키보드 사라지게 만듬
            itemView.setOnClickListener {
                Util.hideSoftKeyboard(mContext,it)
            }

            val gson = IdolGson.getInstance(true)
            var linkContent = ChatMsgLinkModel()
                try {
                   linkContent=gson.fromJson(myItem.content, ChatMsgLinkModel::class.java)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            val convertedDate = Date(myItem.serverTs)

            Logger.v("link data111 ${myItem.content}")
            Logger.v("link data111 $linkContent")
            binding.message.text = linkContent.originalMsg//original 값 넣어줌.
            glideRequestManager
                    .load(linkContent.imageUrl)
                    .into(binding.previewImage)
            binding.previewDescription.text = linkContent.description
            binding.previewTitle.text = linkContent.title

            //익명닉네임 넣기.
            if(isAnonymity == "Y"){
                if(itemViewType == TYPE_CHAT_MINE_LINK_URL_START){//익명방의 경우도 첫 시작 채팅만  닉네임이 보이도록 한다.
                    binding.tvNickname.visibility = View.VISIBLE
                }else{
                    binding.tvNickname.visibility = View.GONE
                }                //닉네임 넣어주기, 레벨 넣어주기.
                for (i in 0 until members.size) {
                    if (myItem.userId == members[i].id) {
                        binding.tvNickname.text = members[i].nickname
                    }
                }
            }else{
                //익명방이 아닐때 두개다 gone처리.
                binding.tvNickname.visibility = View.GONE
            }

            //프리뷰 클릭 이벤트 -> url 에 해당 하는 곳으로 넘어감
            binding.previewWrapper.setOnClickListener {
                if (!linkContent.detectUrl.isNullOrEmpty()) {
                    linkContent.detectUrl?.let { it1 ->
                        linkClicked(it1)
                    }
                }
            }
            binding.date.text = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtil.getAppLocale(binding.root.context)).format(convertedDate)
            binding.message.setOnClickListener {
                binding.message.movementMethod = LinkMovementMethod.getInstance()
            }

            //status가 true이고 statusFailed가 false이면 두개다 없애줘도됨.
            if(myItem.status && !myItem.statusFailed){
                binding.date.visibility = View.VISIBLE
                binding.progressBar?.visibility = View.GONE
                binding.sendFailedIb?.visibility = View.GONE
            }else if(!myItem.status && !myItem.statusFailed){

                binding.date.visibility = View.INVISIBLE
                binding.sendFailedIb?.visibility = View.GONE
                binding.progressBar?.visibility = View.VISIBLE

                // progress bar 색 설정
                if (Utils.getSDKInt() < 21) {
                    binding.progressBar?.indeterminateDrawable?.setColorFilter(
                        ContextCompat.getColor(mContext, R.color.main),
                        android.graphics.PorterDuff.Mode.SRC_IN)
                }

            } else if(!myItem.status && myItem.statusFailed){ // status도 false(서버로 값이 안감), statusFailed true(10초가 지난 메시지).
                binding.progressBar?.visibility = View.GONE
                binding.date.visibility = View.INVISIBLE
                binding.sendFailedIb?.visibility = View.VISIBLE
            }

            //시간 보여줌 여부.
            if(isLastMessage){
                binding.clChatMessage.setPadding(0,0,0, 0)
                binding.date.visibility = View.VISIBLE
            }else{
                binding.clChatMessage.setPadding(0,0,0, 0)
                binding.date.visibility = View.INVISIBLE
            }

            val params = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            val marginValue = Util.convertDpToPixel(mContext, 10f).toInt()

            //같으날일땐 날짜는 보여주지 않는다.
            if(isSameDay) {
                params.setMargins(0, 0, 0, 0)

                binding.tvDay.visibility = View.GONE
            }else{
                params.leftToLeft = binding.clChatMessage.id
                params.rightToRight = binding.clChatMessage.id
                params.topToTop = binding.clChatMessage.id

                if (position == 0) {
                    params.setMargins(0, marginValue, 0, marginValue)
                } else {
                    params.setMargins(0, 0, 0, marginValue)
                }

                binding.tvDay.text = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context)).format(convertedDate)
                binding.tvDay.visibility = View.VISIBLE
            }

            binding.tvDay.layoutParams = params


            //messageWrapper.setPadding(10,10,10,10)
            glideRequestManager.clear(binding.photo)
            binding.photo.setImageDrawable(null)
            if(itemViewType== TYPE_CHAT_MINE_LINK_URL_START){
                binding.messageWrapper.setBackgroundResource(R.drawable.bg_mytalk_start)
            }else if(itemViewType== TYPE_CHAT_MINE_LINK_URL){
                binding.messageWrapper.setBackgroundResource(R.drawable.bg_mytalk)
            }

            val listener = View.OnClickListener { v ->
                photoClick(myItem, v, position)
            }
            binding.photo.setOnClickListener(listener)

            if(myItem.deleted){

                Logger.v("deleteted"+myItem.content)
                binding.message.text = mContext.resources.getString(R.string.chat_deleted_message)
                binding.message.setTextColor(ContextCompat.getColor(mContext,R.color.gray300))
            }else{
                Logger.v("reported ->"+myItem.reported)
                if(myItem.reported){
                    binding.message.text = mContext.resources.getString(R.string.already_reported)
                    binding.message.setTextColor(ContextCompat.getColor(mContext, R.color.gray300))
                }else{
                    binding.message.text = linkContent.originalMsg
                    binding.message.setTextColor(ContextCompat.getColor(mContext, R.color.gray900))
                }
            }

            binding.message.setTextColor(mContext.resources.getColor(R.color.text_white_black, mContext.theme))

        }//bind 마지막


    }


    inner class FirstJoinChatViewHolder(val binding: ItemChatMessageFirstJoinBinding): ViewHolder(binding.root) {
        override fun bind(item: MessageModel, position: Int, isSameDay: Boolean, isLastMessage: Boolean) {

            //포지션 0이고  첫번째 join 메세지일때만  뷰를 보여준다.
            if(item.isFirstJoinMsg){
                var convertedDate = Date(item.serverTs)
                if(item.serverTs == 0L){
                    convertedDate = Date(item.clientTs)
                }
                binding.tvDayChat.visibility=View.VISIBLE
                binding.tvDayChat.text = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context)).format(convertedDate)
                binding.tvFirstJoinMsg.text=item.content
            }else{
                itemView.visibility=View.GONE
            }
        }

    }

    inner class ChatViewHolder(val binding: ChatMessageBindingProxy) : ViewHolder(binding.root), View.OnCreateContextMenuListener {
        init {
            binding.messageWrapper.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?,
                                         v: View?,
                                         menuInfo: ContextMenu.ContextMenuInfo?) {
            binding.message.movementMethod = ArrowKeyMovementMethod.getInstance()

            val menuIds = mutableListOf(
                    ChattingRoomActivity.MENU_COPY,
                    ChattingRoomActivity.MENU_REPORT,
                    ChattingRoomActivity.MENU_DELETE)
            val menuItems = mutableListOf(mContext.getString(android.R.string.copy),
                    mContext.getString(R.string.report),
                    mContext.getString(R.string.remove))

            // 운영자는 delete 메뉴가 나오게
            var showDelete = false
            if (account.heart == Const.LEVEL_ADMIN
                    || account.heart == Const.LEVEL_MANAGER) {
                showDelete = true
            }

            //이미지는  클립보드 복사를  막는다. -> 메뉴 리스트에서  copy를 빼줌.
            if(mItems[adapterPosition].contentType == MessageModel.CHAT_TYPE_IMAGE){
                menuItems.removeAt(0)
                menuIds.removeAt(0)
            }


            if(!mItems[adapterPosition].reported && !mItems[adapterPosition].deleted) {
                for (i in 0 until menuItems.size - if (showDelete) 0 else 1) {
                    menu?.add(Menu.NONE, menuIds[i], i, menuItems[i])
                    val item = menu?.getItem(i)
                    val spanString = SpannableString(item.toString())
                    spanString.setSpan(mContext.resources.getColor(R.color.gray1000,mContext.theme), 0, spanString.length, 0)
                    item?.title = spanString
                }
            }
            else if(mItems[adapterPosition].reported){
                menu?.add(Menu.NONE, menuIds[0], 0, menuItems[0])
                val item = menu?.getItem(0)
                val spanString = SpannableString(item.toString())
                spanString.setSpan(mContext.resources.getColor(R.color.gray1000,mContext.theme), 0, spanString.length, 0)
                item?.title = spanString
            }
            else if(mItems[adapterPosition].deleted) {

                menu?.add(Menu.NONE, menuIds[0], 0, menuItems[0])
                val item = menu?.getItem(0)
                val spanString = SpannableString(item.toString())
                spanString.setSpan(mContext.resources.getColor(R.color.gray1000, mContext.theme), 0, spanString.length, 0)
                item?.title = spanString
            }
        }

        override fun bind(item: MessageModel, position: Int, isSameDay: Boolean,isLastMessage: Boolean) {

            //아이템 컨테이너  클릭시 키보드 사라지게 만듬
            itemView.setOnClickListener {
                Util.hideSoftKeyboard(mContext,it)
            }

            val convertedDate = Date(item.serverTs)
            Util.log("ChatAdapter:: ${members.size}")
            //익명방일땐 닉네임 보이기.
            if(isAnonymity == "Y"){
                val params:ConstraintLayout.LayoutParams = binding.messageWrapper.layoutParams as ConstraintLayout.LayoutParams
                if(itemViewType == TYPE_CHAT_START){//익명방의 경우도 첫 시작 채팅만  닉네임이 보이도록 한다.
                    binding.tvNickname.visibility = View.VISIBLE
                    binding.tvNickname.setTextColor(ContextCompat.getColor(mContext,R.color.gray250))
                    params.topMargin =Util.convertDpToPixel(mContext,8F).toInt()
                    params.leftMargin =Util.convertDpToPixel(mContext,2F).toInt()
                    binding.messageWrapper.layoutParams = params

                    //익명 닉네임 넣기.(이것도 첫시작만 알수없음 표시).
                    members.find { it.id==item.userId && !it.deleted }.apply {
                        if (this != null) {
                            binding.tvNickname.text = this.nickname
                        }else{
                            binding.tvNickname.text = mContext.getString(R.string.chat_leave_user)
                        }
                    }
                }else{
                    binding.tvNickname.visibility = View.GONE
                    params.topMargin =Util.convertDpToPixel(mContext,3F).toInt()
                    params.leftMargin =Util.convertDpToPixel(mContext,2F).toInt()
                    binding.messageWrapper.layoutParams = params
                }
                binding.userPhoto?.visibility = View.GONE
                binding.userLevel?.visibility = View.GONE


            }else if(isAnonymity == "N"){
                Util.log("ChatAdapter:: 익명아니에요.")
                //익명방이 아닐때 두개다 gone처리.

                // TODO: 2021/04/27 일단 이렇게  막았는데 나중에  타입별로 완전히 viewholder 를  분리하자.
                if(itemViewType== TYPE_CHAT_START){
                    binding.tvNickname.visibility = View.VISIBLE
                    binding.userPhoto?.visibility = View.VISIBLE
                    binding.tvNickname.setTextColor(ContextCompat.getColor(mContext,R.color.text_chat))


                    members.find { it.id==item.userId && !it.deleted }.apply {
                        if(this != null){
                            binding.userLevel?.visibility = View.VISIBLE
                            binding.tvNickname.text = this.nickname
                            binding.userLevel?.setImageDrawable(Util.getLevelImageDrawable(mContext, this))
                            binding.userPhoto?.let {
                                glideRequestManager
                                    .load(this.imageUrl)
                                    .apply(RequestOptions.circleCropTransform())
                                    .error(Util.noProfileImage(this.id))
                                    .fallback(Util.noProfileImage(this.id))
                                    .placeholder(Util.noProfileImage(this.id))
                                    .into(binding.userPhoto)
                            }
                        }else{

                            binding.userLevel?.visibility = View.GONE
                            binding.tvNickname.text = mContext.getString(R.string.chat_leave_user)
                            binding.userPhoto?.let {
                                glideRequestManager
                                    .load(Util.noProfileImage(-1))
                                    .apply(RequestOptions.circleCropTransform())
                                    .error(Util.noProfileImage(-1))
                                    .fallback(Util.noProfileImage(-1))
                                    .placeholder(Util.noProfileImage(-1))
                                    .into(binding.userPhoto)
                            }
                        }
                    }
               }
            }

            //다른 유저  프로필  사진 눌렀을때  유저 피드로 넘어가기위해  usermodel 값을  넘긴다.
            binding.userPhoto?.setOnClickListener {
                val chatMember = members.find { it.id==item.userId } //챗 멤버리스트에서  해당 클릭된 유저의 유저 아이디에 해당하는 멤버를 찾느다.
                Logger.v("most id 체크 "+chatMember?.toString())
                onUserProfileClickListener?.onProfileClick(chatMember)
            }

            //기본 View 넣어주기.
            binding.date.text = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtil.getAppLocale(binding.root.context)).format(convertedDate)
            binding.previewWrapper.visibility = View.GONE

            var messageLeftPadding =0
            if(isAnonymity=="Y"){//다른 사람의 채팅 메세지의 경우  익명방일때  왼족간격을 좀더 띄어준다.
               messageLeftPadding = Util.convertDpToPixel(mContext, 10F).toInt()
            }

            //시간 보여줌 여부.
            if(isLastMessage){
                binding.clChatMessage.setPadding(messageLeftPadding,0,0, Util.convertDpToPixel(mContext, 6F).toInt())
                binding.date.visibility = View.VISIBLE
            }else{
                binding.clChatMessage.setPadding(messageLeftPadding,0,0, 0)
                binding.date.visibility = View.INVISIBLE
            }

            val params = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            val marginValue = Util.convertDpToPixel(mContext, 10f).toInt()

            //같으날일땐 날짜는 보여주지 않는다.
            if(isSameDay) {
                params.setMargins(0, 0, 0, 0)

                binding.tvDay.visibility = View.GONE
            }else{
                params.leftToLeft = binding.clChatMessage.id
                params.rightToRight = binding.clChatMessage.id
                params.topToTop = binding.clChatMessage.id

                if (position == 0) {
                    params.setMargins(0, marginValue, 0, marginValue)
                } else {
                    params.setMargins(0, 0, 0, marginValue)
                }

                binding.tvDay.text = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context)).format(convertedDate)
                binding.tvDay.visibility = View.VISIBLE
            }

            binding.tvDay.layoutParams = params

            //메시지 타입에따라 분류(이미지, 그냥텍스트, 비디오....)
            if (item.contentType == MessageModel.CHAT_TYPE_IMAGE) {

                //바깥쪽 레이아웃 패딩 제거 및 배경 투명으로 변경.
                binding.messageWrapper.background = ColorDrawable(Color.TRANSPARENT)
                binding.message.visibility = View.GONE
                binding.previewWrapper.visibility = View.GONE
                binding.photoWrapper.visibility = View.VISIBLE
                binding.messageWrapper.setPadding(0, 0, 0, 0)

                val obj = JSONObject(item.content)
                val index = obj.getString("url").lastIndexOf(".")
                val extension = obj.getString("url").substring(index + 1)
                Util.log("Adapter::extension->$extension")

                if (extension == "gif") {
                    binding.gifImage.visibility = View.VISIBLE
                } else {
                    binding.gifImage.visibility = View.GONE
                }

                binding.photo.visibility = View.VISIBLE

                val isEmoticon  = obj.optBoolean("is_emoticon",false)

                if(isEmoticon){//이모티콘일 경우에는 -> 로컬 png를 보여줌.

                    val thumbNail = obj.optString("thumbnail")
                    val emoAllInfoList = gson.fromJson<ArrayList<EmoticonDetailModel>>(
                        Util.getPreference(
                            itemView.context,
                            Const.EMOTICON_ALL_INFO
                        ), emoListType
                    )

                    //이모티콘은 사이즈를 좀더 줄여서 보여준다.
                    binding.photo.layoutParams.width = Util.convertDpToPixel(itemView.context,150f).toInt()
                    binding.photo.layoutParams.height = Util.convertDpToPixel(itemView.context,150f).toInt()

                    //이모티콘 -> filapth uri 로 변환
                    val uri:Uri = if(UtilK.getEmoticonId(obj.getString("url")).contains("_")){
                        val splitedId = UtilK.getEmoticonTransId(obj.getString("url"))
                        Uri.parse(emoAllInfoList.find { it.id == splitedId.toInt() }?.filePath+".webp")
                    } else {
                        Uri.parse(emoAllInfoList.find { it.thumbnail == thumbNail }?.filePath+".webp")
                    }

                    val file = File(uri.path)
                    if (file.exists()) { //파일이 있다면 로컬에있는거 보여줌.
                        glideRequestManager
                            .load(uri.path)
                            .transform(CenterCrop(), RoundedCorners(40))
                            .into(binding.photo)
                    } else {//아니면 webp로 보여줍니다(상대방이 가지고있는 이모티콘이 없을떄).

                        val emoticonId = UtilK.getEmoticonId(obj.getString("url"))
                        val path = UtilK.fileImageUrl(mContext, emoticonId)
                        glideRequestManager
                            .load(path)
                            .transform(CenterCrop(), RoundedCorners(40))
                            .into(binding.photo)
                    }
                }else{//이모티콘아  아니면, 일반  사진이라 생각하고,  thumbnail을 보내준다.

                    val thumbHeight = obj.optString("thumb_height")
                    val thumbWidth = obj.optString("thumb_width")

                    //date.width = 0 오는 것 방지
                    binding.date.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    dateWidth = Util.convertPixelsToDp(itemView.context, binding.date.measuredWidth.toFloat()).toInt()

                    val remainingWidth = if(isAnonymity == "Y") { // 들어갈 수 있는 이미지 가로 사이즈 최대.
                        displaySize-dateWidth-43
                    } else {
                        displaySize-dateWidth-90 // 익명방일 경우 프로필사진크기만큼 더 빼줌
                    }
                    if(thumbHeight.isNullOrEmpty() || thumbWidth.isNullOrEmpty()){    //에러로 인하여 0이 아닌 빈 값이 오는 상황이 생길 수 있으므로
                        imageShow(itemView.context, binding.photo, obj.getString("thumbnail"), 0, 0, position, remainingWidth)
                    }
                    else{
                        imageShow(itemView.context, binding.photo, obj.getString("thumbnail"), thumbHeight.toInt(), thumbWidth.toInt(), position, remainingWidth)
                    }
                }


            }else if(item.contentType == MessageModel.CHAT_TYPE_TEXT){
                glideRequestManager.clear(binding.photo)
                binding.photo.setImageDrawable(null)
                if(itemViewType== TYPE_CHAT_START){
                    binding.messageWrapper.setBackgroundResource(R.drawable.bg_usertalk_start)
                }else if(itemViewType== TYPE_CHAT){
                    binding.messageWrapper.setBackgroundResource(R.drawable.bg_usertalk)
                }
                binding.message.text = item.content
                binding.message.visibility = View.VISIBLE
                binding.photoWrapper.visibility = View.GONE
                binding.previewWrapper.visibility = View.GONE
            }

            val listener = View.OnClickListener { v ->
                photoClick(item, v, position)
            }
            binding.photo.setOnClickListener(listener)

            if(item.deleted){

                Logger.v("deleteted"+item.content)
                binding.message.text = mContext.resources.getString(R.string.chat_deleted_message)
                binding.message.setTextColor(ContextCompat.getColor(mContext,R.color.gray300))
            }else{
                Logger.v("reported ->"+item.reported)
                if(item.reported){
                    binding.message.text = mContext.resources.getString(R.string.already_reported)
                    binding.message.setTextColor(ContextCompat.getColor(mContext, R.color.gray300))
                }else{
                    binding.message.text = item.content
                    binding.message.setTextColor(ContextCompat.getColor(mContext, R.color.gray900))
                }
            }


        }
    }

    inner class MyChatViewHolder(val binding: MineBindingProxy) : ViewHolder(binding.root), View.OnCreateContextMenuListener {
        init {
            binding.messageWrapper.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?,
                                         v: View?,
                                         menuInfo: ContextMenu.ContextMenuInfo?) {
            binding.message.movementMethod = ArrowKeyMovementMethod.getInstance()

            val menuIds = mutableListOf(
                    ChattingRoomActivity.MENU_COPY,
                    ChattingRoomActivity.MENU_DELETE)
            val menuItems = mutableListOf(mContext.getString(android.R.string.copy),
                    mContext.getString(R.string.title_remove))

            //이미지는  클립보드 복사를  막는다. -> 메뉴 리스트에서  copy를 빼줌.
            if(mItems[adapterPosition].contentType == MessageModel.CHAT_TYPE_IMAGE){
                menuItems.removeAt(0)
                menuIds.removeAt(0)
            }

            if(!mItems[adapterPosition].status && !mItems[adapterPosition].statusFailed){
                menuItems.removeAt(1)
                menuIds.removeAt(1)
            }


            if(mItems[adapterPosition].deleted){
                menu?.add(Menu.NONE, menuIds[0], 0, menuItems[0])
                val item = menu?.getItem(0)
                val spanString = SpannableString(item.toString())
                spanString.setSpan(mContext.resources.getColor(R.color.gray1000, mContext.theme), 0, spanString.length, 0)
                item?.title = spanString
            }else{
                // 자기 메시지는 지울 수 있게
                for (i in menuItems.indices) {
                    menu?.add(Menu.NONE, menuIds[i], i, menuItems[i])
                    val item = menu?.getItem(i)
                    val spanString = SpannableString(item.toString())
                    spanString.setSpan(mContext.resources.getColor(R.color.gray1000,mContext.theme), 0, spanString.length, 0)
                    item?.title = spanString
                }
            }
        }

        override fun bind(myItem: MessageModel, position: Int, isSameDay: Boolean,isLastMessage: Boolean) {


            //아이템 컨테이너  클릭시 키보드 사라지게 만듬
            itemView.setOnClickListener {
                Util.hideSoftKeyboard(mContext,it)
            }

            val convertedDate = Date(myItem.serverTs)

            //익명닉네임 넣기.
            if(isAnonymity == "Y"){//익명방일때
                if(itemViewType == TYPE_CHAT_MINE_START){//익명방의 경우도 첫 시작 채팅만  닉네임이 보이도록 한다.

                    //이전 메세지의 유저 아이디가 자신의 userid와 같을때는 (첫 참여 메세지 제외) 닉네임을 안보여준다. 그외엔 닉네임 보여주기
                    if(position>0 && myItem.userId == mItems[position-1].userId && !mItems[position-1].isFirstJoinMsg){
                        binding.tvNickname.visibility = View.GONE
                    }else{
                        binding.tvNickname.visibility = View.VISIBLE
                    }

                }else{
                    binding.tvNickname.visibility = View.GONE
                }                //닉네임 넣어주기, 레벨 넣어주기.
                for (i in 0 until members.size) {
                    if (myItem.userId == members[i].id) {
                        binding.tvNickname.text = members[i].nickname
                    }
                }
            }else{
                //익명방이 아닐때 두개다 gone처리.
                binding.tvNickname.visibility = View.GONE
            }

            binding.date.text = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtil.getAppLocale(binding.root.context)).format(convertedDate)
            binding.message.setOnClickListener {
                binding.message.movementMethod = LinkMovementMethod.getInstance()
            }
            binding.previewWrapper.visibility = View.GONE

            //status가 true이고 statusFailed가 false이면 두개다 없애줘도됨.
            if(myItem.status && !myItem.statusFailed){
                binding.date.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                binding.sendFailedIb.visibility = View.GONE
            }else if(!myItem.status && !myItem.statusFailed){

                binding.date.visibility = View.INVISIBLE
                binding.sendFailedIb.visibility = View.INVISIBLE
                binding.progressBar.visibility = View.VISIBLE

                // progress bar 색 설정
                if (Utils.getSDKInt() < 21) {
                    binding.progressBar.indeterminateDrawable?.setColorFilter(
                            ContextCompat.getColor(mContext, R.color.main),
                            android.graphics.PorterDuff.Mode.SRC_IN)
                }

            } else if(!myItem.status && myItem.statusFailed){ // status도 false(서버로 값이 안감), statusFailed true(10초가 지난 메시지).
                binding.progressBar.visibility = View.GONE
                binding.date.visibility = View.INVISIBLE
                binding.sendFailedIb.visibility = View.VISIBLE
            }

            //시간 보여줌 여부.
            if(isLastMessage){
                binding.clChatMessage.setPadding(0,0,0, 0)
                binding.date.visibility = View.VISIBLE
            }else{
                binding.clChatMessage.setPadding(0,0,0, 0)
                binding.date.visibility = View.INVISIBLE
            }

            val params = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            val marginValue = Util.convertDpToPixel(mContext, 10f).toInt()

            //같으날일땐 날짜는 보여주지 않는다.
            if(isSameDay) {
                params.setMargins(0, 0, 0, 0)

                binding.tvDay.visibility = View.GONE
            }else{
                params.leftToLeft = binding.clChatMessage.id
                params.rightToRight = binding.clChatMessage.id
                params.topToTop = binding.clChatMessage.id

                if (position == 0) {
                    params.setMargins(0, marginValue, 0, marginValue)
                } else {
                    params.setMargins(0, 0, 0, marginValue)
                }

                    binding.tvDay.text = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context)).format(convertedDate)
                binding.tvDay.visibility = View.VISIBLE
            }

            binding.tvDay.layoutParams = params

            //메시지 타입에따라 분류(이미지, 그냥텍스트, 비디오....)
            if(myItem.contentType == MessageModel.CHAT_TYPE_IMAGE){

                //바깥쪽 레이아웃 패딩 제거 및 배경 투명으로 변경.
                binding.messageWrapper.background = ColorDrawable(Color.TRANSPARENT)
                binding.message.visibility = View.GONE
                binding.previewWrapper.visibility = View.GONE
                binding.photoWrapper.visibility = View.VISIBLE
                binding.messageWrapper.setPadding(0, 0, 0, 0)

                val obj = JSONObject(myItem.content)

                val index = obj.getString("url").lastIndexOf(".")
                val extension = obj.getString("url").substring(index + 1)
                Util.log("Adapter::extension->$extension")

                if (extension == "gif") {
                    binding.gifImage.visibility = View.VISIBLE
                } else {
                    binding.gifImage.visibility = View.GONE
                }

                binding.photo.visibility = View.VISIBLE

                val isEmoticon  = obj.optBoolean("is_emoticon",false)

                if(isEmoticon){//이모티콘일 경우에는 -> 로컬 png를 보여줌.

                    val thumbNail = obj.optString("thumbnail")
                    val emoAllInfoList = gson.fromJson<ArrayList<EmoticonDetailModel>>(
                        Util.getPreference(
                            itemView.context,
                            Const.EMOTICON_ALL_INFO
                        ), emoListType
                    )

                    //이모티콘은 사이즈를 좀더 줄여서 보여준다.
                    binding.photo.layoutParams?.width = Util.convertDpToPixel(itemView.context,150f).toInt()
                    binding.photo.layoutParams?.height = Util.convertDpToPixel(itemView.context,150f).toInt()

                    //이모티콘 -> filapth uri 로 변환
                    val uri = Uri.parse(emoAllInfoList.find { it.thumbnail == thumbNail }?.filePath + ".webp")

                    val file = File(uri.path)
                    if (file.exists()) { //파일이 있다면 로컬에있는거 보여줌.
                        glideRequestManager
                            .load(uri.path)
                            .transform(CenterCrop(), RoundedCorners(40))
                            .into(binding.photo)
                    } else {//아니면 webp로 보여줍니다(상대방이 가지고있는 이모티콘이 없을떄).

                        val emoticonId = UtilK.getEmoticonId(obj.getString("url"))
                        val path = UtilK.fileImageUrl(mContext, emoticonId)
                        glideRequestManager
                            .load(path)
                            .transform(CenterCrop(), RoundedCorners(40))
                            .into(binding.photo)
                    }
                }else{//이모티콘  아니면, 일반  사진이라 생각하고,  thumbnail을 보내준다.

                    val thumbHeight = obj.optString("thumb_height")
                    val thumbWidth = obj.optString("thumb_width")

                    //date.width = 0 오는 것 방지
                    binding.date.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    dateWidth = Util.convertPixelsToDp(itemView.context, binding.date.measuredWidth.toFloat()).toInt()

                    val remainingWidth = displaySize-dateWidth-43   // 들어갈 수 있는 이미지 가로 사이즈 최대
                    if(thumbHeight.isNullOrEmpty() || thumbWidth.isNullOrEmpty()){    //에러로 인하여 0이 아닌 빈 값이 오는 상황이 생길 수 있으므로
                        imageShow(itemView.context, binding.photo, obj.getString("thumbnail"), 0, 0, position, remainingWidth)
                    }
                    else{
                        imageShow(itemView.context, binding.photo, obj.getString("thumbnail"), thumbHeight.toInt(), thumbWidth.toInt(), position, remainingWidth)
                    }
                }

            }else if(myItem.contentType == MessageModel.CHAT_TYPE_TEXT){
                //messageWrapper.setPadding(10,10,10,10)
                glideRequestManager.clear(binding.photo)
                binding.photo.setImageDrawable(null)
                if(itemViewType== TYPE_CHAT_MINE_START){
                    binding.messageWrapper.setBackgroundResource(R.drawable.bg_mytalk_start)
                }else if(itemViewType== TYPE_CHAT_MINE){
                    binding.messageWrapper.setBackgroundResource(R.drawable.bg_mytalk)
                }
                binding.message.text = myItem.content
                binding.message.visibility = View.VISIBLE
                binding.photoWrapper.visibility = View.GONE
                binding.previewWrapper.visibility = View.GONE
            }

            val listener = View.OnClickListener { v ->
                photoClick(myItem, v, position)
            }
            binding.photo.setOnClickListener(listener)

            if(myItem.deleted){
                binding.message.text = mContext.resources.getString(R.string.chat_deleted_message)
                binding.message.setTextColor(ContextCompat.getColor(mContext, R.color.my_chat_deleted))
            }else{
                binding.message.text = myItem.content
                binding.message.setTextColor(ContextCompat.getColor(mContext, R.color.text_white_black))
            }
        }
    }

    //서버에서 이미지 받아온 것 처리
    private fun imageShow(context: Context, photo : AppCompatImageView , thumbNail : String, thumbHeight : Int, thumbWidth : Int, position: Int, remainingWidth : Int){
        if(thumbHeight != 0 && thumbWidth != 0) {   //서버에서 이미지 가로,세로 길이를 주면
            when {
                thumbWidth>thumbHeight -> { //가로의 길이가 더 길 경우
                    if(remainingWidth < IMAGE_MAX_SIZE){  //서버에서 줄 수 있는 이미지 최대 크기보다 남는 공간이 적을 경우
                        photo.layoutParams.width = Util.convertDpToPixel(context, (remainingWidth).toFloat()).toInt()     //핸드폰 길이 - (날짜 길이 + 기본 33dp 마진) = 이미지 가로 길이
                        val heightRatio = (remainingWidth) * thumbHeight / thumbWidth
                        photo.layoutParams.height = Util.convertDpToPixel(context, heightRatio.toFloat()).toInt()   //wrap_content로 넣으면 맨 아래로 안내려가지는 문제가 생김
                    }
                    else{   //서버에서 줄 수 있는 이미지 최대 크기보다 남는 공간이 클 경우 400 x 400 으로 고정
                        photo.layoutParams.width = Util.convertDpToPixel(context, IMAGE_MAX_SIZE.toFloat()).toInt()
                        val heightRatio = IMAGE_MAX_SIZE.toFloat() * thumbHeight / thumbWidth
                        photo.layoutParams.height = Util.convertDpToPixel(context, heightRatio).toInt()   //wrap_content로 넣으면 맨 아래로 안내려가지는 문제가 생김
                   }
                }
                thumbWidth == thumbHeight -> { //가로 세로 길이가 같을 경우
                    if(remainingWidth < IMAGE_MAX_SIZE) {  //서버에서 줄 수 있는 이미지 최대 크기보다 남는 공간이 적을 경우
                        photo.layoutParams.width = Util.convertDpToPixel(context, 200f).toInt()
                        photo.layoutParams.height = Util.convertDpToPixel(context, 200f).toInt()
                    }
                    else{
                        photo.layoutParams.width = Util.convertDpToPixel(context, IMAGE_MAX_SIZE.toFloat()).toInt()
                        photo.layoutParams.height = Util.convertDpToPixel(context, IMAGE_MAX_SIZE.toFloat()).toInt()
                    }
                }
                else -> {   //세로의 길이가 더 길 경우
                    if(thumbHeight < 150){
                        photo.layoutParams.height = Util.convertDpToPixel(context, 150f).toInt()
                        val widthRatio = 150f * thumbWidth / thumbHeight
                        photo.layoutParams.width = Util.convertDpToPixel(context, widthRatio).toInt()
                    }
                    else{
                        photo.layoutParams.height = Util.convertDpToPixel(context, thumbHeight.toFloat()).toInt()
                        var widthRatio = thumbHeight.toFloat() * thumbWidth / thumbHeight
                        if(widthRatio > remainingWidth){
                            widthRatio = (remainingWidth).toFloat()
                            photo.layoutParams.height = Util.convertDpToPixel(context, (thumbHeight * widthRatio / thumbWidth)).toInt()
                        }
                        photo.layoutParams.width = Util.convertDpToPixel(context, widthRatio).toInt()
                    }
                }
            }
            glideRequestManager
                .asBitmap()
                .transform(RoundedCorners(40))
                .load(thumbNail)
                .into(photo)

            photo.adjustViewBounds = true

            if (position == mItems.size - 1) {
                if (!needScrollDown) {
                    showLastImageListener.showLastImage()
                    needScrollDown = true
                }
            }
        }
        else {  //서버에서 이미지 가로,세로 이미지 길이를 안주면 직접 계산해서 대입. 직접 계산하면 스크롤 아래로 안내려가는 문제 있어서 리스너를 통해 스크롤 아래로 보냄
            //해줘야 이전 이미지 잔상이 안남음
            glideRequestManager.clear(photo)
            photo.setImageDrawable(null)

            glideRequestManager
                .asBitmap()
                .load(thumbNail)
                .transform(RoundedCorners(40))
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        photo.adjustViewBounds = true
                        photo.setImageBitmap(resource)

                        val imgWidth = resource.width
                        val imgHeight = resource.height

                        when {
                            imgWidth>imgHeight -> { //가로의 길이가 더 길 경우
                                if(remainingWidth < IMAGE_MAX_SIZE){  //서버에서 줄 수 있는 이미지 최대 크기보다 남는 공간이 적을 경우
                                    photo.layoutParams.width = Util.convertDpToPixel(context, (remainingWidth).toFloat()).toInt()     //핸드폰 길이 - (날짜 길이 + 기본 33dp 마진) = 이미지 가로 길이
                                    val heightRatio = (remainingWidth) * imgHeight / imgWidth
                                    photo.layoutParams.height = Util.convertDpToPixel(context, heightRatio.toFloat()).toInt()   //wrap_content로 넣으면 맨 아래로 안내려가지는 문제가 생김
                                }
                                else{   //서버에서 줄 수 있는 이미지 최대 크기보다 남는 공간이 클 경우 400 x 400 으로 고정
                                    photo.layoutParams.width = Util.convertDpToPixel(context, IMAGE_MAX_SIZE.toFloat()).toInt()
                                    val heightRatio = IMAGE_MAX_SIZE.toFloat() * imgHeight / imgWidth
                                    photo.layoutParams.height = Util.convertDpToPixel(context, heightRatio).toInt()   //wrap_content로 넣으면 맨 아래로 안내려가지는 문제가 생김
                              }
                            }
                            imgWidth == imgHeight -> { //가로 세로 길이가 같을 경우
                                if(remainingWidth < IMAGE_MAX_SIZE) {  //서버에서 줄 수 있는 이미지 최대 크기보다 남는 공간이 적을 경우
                                    photo.layoutParams.width = Util.convertDpToPixel(context, 200f).toInt()
                                    photo.layoutParams.height = Util.convertDpToPixel(context, 200f).toInt()
                                }
                                else{
                                    photo.layoutParams.width = Util.convertDpToPixel(context, IMAGE_MAX_SIZE.toFloat()).toInt()
                                    photo.layoutParams.height = Util.convertDpToPixel(context, IMAGE_MAX_SIZE.toFloat()).toInt()
                                }
                            }
                            else -> {   //세로의 길이가 더 길 경우
                                if(imgHeight < 150){
                                    photo.layoutParams.height = Util.convertDpToPixel(context, 150f).toInt()
                                    val widthRatio = 150f * imgWidth / imgHeight
                                    photo.layoutParams.width = Util.convertDpToPixel(context, widthRatio).toInt()
                                }
                                else{
                                    photo.layoutParams.height = Util.convertDpToPixel(context, imgHeight.toFloat()).toInt()
                                    var widthRatio = imgHeight.toFloat() * imgWidth / imgHeight
                                    if(widthRatio > remainingWidth){
                                        widthRatio = (remainingWidth).toFloat()
                                        photo.layoutParams.height = Util.convertDpToPixel(context, (thumbHeight * widthRatio / thumbWidth)).toInt()
                                    }
                                    photo.layoutParams.width = Util.convertDpToPixel(context, widthRatio).toInt()
                                }
                            }
                        }

                        if (position == mItems.size - 1) {
                            if (!needScrollDown) {
                                showLastImageListener.showLastImage()
                                needScrollDown = true
                            }
                        }
                    }
                })
        }
    }

    inner class FooterViewHolder(itemView: View) : ViewHolder(itemView) {
        override fun bind(item: MessageModel, position: Int, isSameDay: Boolean, isLastMessage: Boolean) {
            //footer는 아무것도 안함
        }
    }

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal abstract fun bind(item: MessageModel,
                                   position: Int,
                                   isSameDay: Boolean,
                                   isLastMessage:Boolean)
    }


    inner class BindingProxy {
        val viewBinding: ViewDataBinding
        val root: View
        val tvDay: TextView
        val userLevel: AppCompatImageView?
        val tvNickname: TextView
        val userPhoto: AppCompatImageView?
        val messageWrapper: LinearLayoutCompat
        val photoWrapper: ConstraintLayout
        val photo: ExodusImageView // ChatMessageBindingProxy에는 AppCompatImageView
        val gifImage: AppCompatImageView
        val message: TextView
        val clChatMessage: ConstraintLayout
        val date: TextView
        val previewWrapper: CardView
        val previewImage: TalkLinkImageView
        val previewInfo: LinearLayout
        val previewTitle: TextView
        val previewDescription: TextView
        val progressBar: ProgressBar?
        val sendFailedIb: AppCompatImageButton?

        constructor(binding: ItemChatLinkMessageBinding) {
            viewBinding = binding
            root = binding.root
            tvDay = binding.tvDayChat
            userLevel = binding.ivLevelChat
            tvNickname = binding.tvNicknameChat
            userPhoto = binding.userPhoto
            messageWrapper = binding.messageWrapperChat
            photoWrapper = binding.photoWrapper
            photo = binding.photoChat
            gifImage = binding.viewGif
            message = binding.messageChat
            clChatMessage = binding.clChatMessageChat
            date = binding.dateChat
            previewWrapper = binding.llPreviewWrapperChat
            previewImage = binding.ivPreviewImageChat
            previewInfo = binding.llPreviewInfoChat
            previewTitle = binding.tvPreviewTitleChat
            previewDescription = binding.tvPreviewDescriptionChat
            progressBar = null
            sendFailedIb = null
        }

        constructor(binding: ItemChatLinkMessageStartBinding) {
            viewBinding = binding
            root = binding.root
            tvDay = binding.tvDayChat
            userLevel = binding.ivLevelChat
            tvNickname = binding.tvNicknameChat
            userPhoto = binding.userPhoto
            messageWrapper = binding.messageWrapperChat
            photoWrapper = binding.photoWrapper
            photo = binding.photoChat
            gifImage = binding.viewGif
            message = binding.messageChat
            clChatMessage = binding.clChatMessageChat
            date = binding.dateChat
            previewWrapper = binding.llPreviewWrapperChat
            previewImage = binding.ivPreviewImageChat
            previewInfo = binding.llPreviewInfoChat
            previewTitle = binding.tvPreviewTitleChat
            previewDescription = binding.tvPreviewDescriptionChat
            progressBar = null
            sendFailedIb = null
        }

        constructor(binding: ItemChatLinkMessageMineBinding) {
            viewBinding = binding
            root = binding.root
            tvDay = binding.tvDayChat
            userLevel = null
            tvNickname = binding.tvNicknameChat
            userPhoto = null
            messageWrapper = binding.messageWrapperChat
            photoWrapper = binding.photoWrapper
            photo = binding.photoChat
            gifImage = binding.viewGif
            message = binding.messageChat
            clChatMessage = binding.clChatMessageChat
            date = binding.dateChat
            previewWrapper = binding.llPreviewWrapperChat
            previewImage = binding.ivPreviewImageChat
            previewInfo = binding.llPreviewInfoChat
            previewTitle = binding.tvPreviewTitleChat
            previewDescription = binding.tvPreviewDescriptionChat
            progressBar = binding.progressBarChat
            sendFailedIb = binding.sendFailedIb
        }

        constructor(binding: ItemChatLinkMessageMineStartBinding) {
            viewBinding = binding
            root = binding.root
            tvDay = binding.tvDayChat
            userLevel = null
            tvNickname = binding.tvNicknameChat
            userPhoto = null
            messageWrapper = binding.messageWrapperChat
            photoWrapper = binding.photoWrapper
            photo = binding.photoChat
            gifImage = binding.viewGif
            message = binding.messageChat
            clChatMessage = binding.clChatMessageChat
            date = binding.dateChat
            previewWrapper = binding.llPreviewWrapperChat
            previewImage = binding.ivPreviewImageChat
            previewInfo = binding.llPreviewInfoChat
            previewTitle = binding.tvPreviewTitleChat
            previewDescription = binding.tvPreviewDescriptionChat
            progressBar = binding.progressBarChat
            sendFailedIb = binding.sendFailedIb
        }
    }

    inner class ChatMessageBindingProxy {
        val viewBinding: ViewDataBinding
        val root: View
        val tvDay: TextView
        val userLevel: AppCompatImageView?
        val tvNickname: TextView
        val userPhoto: AppCompatImageView?
        val messageWrapper: LinearLayoutCompat
        val photoWrapper: ConstraintLayout
        val photo: AppCompatImageView
        val gifImage: AppCompatImageView
        val message: TextView
        val clChatMessage: ConstraintLayout
        val date: TextView
        val previewWrapper: CardView
        val previewImage: TalkLinkImageView
        val previewInfo: LinearLayout
        val previewTitle: TextView
        val previewDescription: TextView
        val progressBar: ProgressBar?
        val sendFailedIb: AppCompatImageButton?

        constructor(binding: ItemChatMessageBinding) {
            viewBinding = binding
            root = binding.root
            tvDay = binding.tvDayChat
            userLevel = binding.ivLevelChat
            tvNickname = binding.tvNicknameChat
            userPhoto = binding.userPhoto
            messageWrapper = binding.messageWrapperChat
            photoWrapper = binding.photoWrapper
            photo = binding.photoChat
            gifImage = binding.viewGif
            message = binding.messageChat
            clChatMessage = binding.clChatMessageChat
            date = binding.dateChat
            previewWrapper = binding.llPreviewWrapperChat
            previewImage = binding.ivPreviewImageChat
            previewInfo = binding.llPreviewInfoChat
            previewTitle = binding.tvPreviewTitleChat
            previewDescription = binding.tvPreviewDescriptionChat
            progressBar = null
            sendFailedIb = null
        }

        constructor(binding: ItemChatMessageStartBinding) {
            viewBinding = binding
            root = binding.root
            tvDay = binding.tvDayChat
            userLevel = binding.ivLevelChat
            tvNickname = binding.tvNicknameChat
            userPhoto = binding.userPhoto
            messageWrapper = binding.messageWrapperChat
            photoWrapper = binding.photoWrapper
            photo = binding.photoChat
            gifImage = binding.viewGif
            message = binding.messageChat
            clChatMessage = binding.clChatMessageChat
            date = binding.dateChat
            previewWrapper = binding.llPreviewWrapperChat
            previewImage = binding.ivPreviewImageChat
            previewInfo = binding.llPreviewInfoChat
            previewTitle = binding.tvPreviewTitleChat
            previewDescription = binding.tvPreviewDescriptionChat
            progressBar = null
            sendFailedIb = null
        }
    }
    inner class MineBindingProxy {
        val viewBinding: ViewDataBinding
        val root: View
        val tvDay: TextView
        val userLevel: AppCompatImageView?
        val tvNickname: TextView
        val userPhoto: AppCompatImageView?
        val messageWrapper: LinearLayoutCompat
        val photoWrapper: ConstraintLayout
        val photo: AppCompatImageView
        val gifImage: AppCompatImageView
        val message: TextView
        val clChatMessage: ConstraintLayout
        val date: TextView
        val previewWrapper: CardView
        val previewImage: TalkLinkImageView
        val previewInfo: LinearLayout
        val previewTitle: TextView
        val previewDescription: TextView
        val progressBar: ProgressBar
        val sendFailedIb: AppCompatImageButton

        constructor(binding: ItemChatMessageMineStartBinding) {
            viewBinding = binding
            root = binding.root
            tvDay = binding.tvDayChat
            userLevel = null
            tvNickname = binding.tvNicknameChat
            userPhoto = null
            messageWrapper = binding.messageWrapperChat
            photoWrapper = binding.photoWrapper
            photo = binding.photoChat
            gifImage = binding.viewGif
            message = binding.messageChat
            clChatMessage = binding.clChatMessageChat
            date = binding.dateChat
            previewWrapper = binding.llPreviewWrapperChat
            previewImage = binding.ivPreviewImageChat
            previewInfo = binding.llPreviewInfoChat
            previewTitle = binding.tvPreviewTitleChat
            previewDescription = binding.tvPreviewDescriptionChat
            progressBar = binding.progressBarChat
            sendFailedIb = binding.sendFailedIb
        }

        constructor(binding: ItemChatMessageMineBinding) {
            viewBinding = binding
            root = binding.root
            tvDay = binding.tvDayChat
            userLevel = null
            tvNickname = binding.tvNicknameChat
            userPhoto = null
            messageWrapper = binding.messageWrapperChat
            photoWrapper = binding.photoWrapper
            photo = binding.photoChat
            gifImage = binding.viewGif
            message = binding.messageChat
            clChatMessage = binding.clChatMessageChat
            date = binding.dateChat
            previewWrapper = binding.llPreviewWrapperChat
            previewImage = binding.ivPreviewImageChat
            previewInfo = binding.llPreviewInfoChat
            previewTitle = binding.tvPreviewTitleChat
            previewDescription = binding.tvPreviewDescriptionChat
            progressBar = binding.progressBarChat
            sendFailedIb = binding.sendFailedIb
        }
    }
    companion object{

        //메세지 타입별 분류
        const val TYPE_CHAT_FOOTER = -1
        const val TYPE_CHAT_MINE_START =0//내채팅 시작
        const val TYPE_CHAT_MINE = 1//내채팅 일반
        const val TYPE_CHAT_START=2//남 채팅 시작
        const val TYPE_CHAT = 3//남 채팅 일반
        const val TYPE_CHAT_FIRST_JOIN =4 //내가 채팅방 맨처음 들어왔을때를 알려주는  메세지

        const val TYPE_CHAT_LINK_URL_START =5
        const val TYPE_CHAT_LINK_URL =6

        const val TYPE_CHAT_MINE_LINK_URL_START =7
        const val TYPE_CHAT_MINE_LINK_URL =8
        const val IMAGE_MAX_SIZE = 400
    }
}
