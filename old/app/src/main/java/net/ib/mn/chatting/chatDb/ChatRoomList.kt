package net.ib.mn.chatting.chatDb

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.model.ChatMembersModel
import net.ib.mn.chatting.model.ChatRoomListModel
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.utils.SingletonHolder

class ChatRoomList private constructor(context: Context) {
//    companion object : SingletonHolder<ChatRoomList, Context>(::ChatRoomList)

    companion object {
        @Volatile private var instance: ChatRoomList? = null

        @JvmStatic fun getInstance(context: Context): ChatRoomList =
            instance ?: synchronized(this) {
                instance ?: ChatRoomList(context).also {
                    instance = it
                }
            }

        fun destroyInstance() {
            instance = null
        }
    }

    private var chatRoomList = ArrayList<ChatRoomListModel>()

    val gson: Gson = IdolGson.getInstance()

    private val ctx = context

    var accountId :Int? = null

    init {
        accountId = IdolAccount.getAccount(context)?.userId
    }

    fun setChatRoom(_chatRoomList: ArrayList<ChatRoomListModel>) {
        chatRoomList.clear()
        chatRoomList.addAll(_chatRoomList)

        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatRoomListDao()
                    ?.insertChatRooms(_chatRoomList)
            }
        }

        val thread = Thread(r)
        thread.start()
    }

    //푸시용 가져오기 (앱죽는거 방지 nullable하게 처리.)
    fun getChatRoomPush(roomId: Int?): ChatRoomListModel? {
        return ChatDB.getInstance(ctx, accountId!!)?.ChatRoomListDao()?.getChatRoomPush(roomId)
    }

    suspend fun getChatRoom(roomId: Int?): ChatRoomListModel? {
        return ChatDB.getInstance(ctx, accountId!!)
            ?.ChatRoomListDao()
            ?.getChatRoom(roomId)
    }

    //메시지 전부 삭getJoinedRoom제(현재는 테스트용이므로 나중에 삭제)
    fun clearAll(){
        chatRoomList.clear()
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)
                ?.ChatRoomListDao()
                ?.deleteAll()
        }

        val thread = Thread(r)
        thread.start()
    }

    fun getAll():ArrayList<ChatRoomListModel> {
        return chatRoomList
    }

    fun getAll12():List<ChatRoomListModel>? {
        return ChatDB.getInstance(ctx, accountId!!)?.ChatRoomListDao()?.getAll()

    }

    fun getJoinedRoom(isJoinedRoom: Int?): List<ChatRoomListModel>? {
        return ChatDB.getInstance(ctx, accountId!!)?.ChatRoomListDao()?.getJoinedRoom(isJoinedRoom)
    }


    //특적방만  room list 에서 지워준다.
    fun deleteRoom(room_id :Int,callback: (()->Unit)?){
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)
                ?.ChatRoomListDao()
                ?.deleteRoom(room_id)
            callback?.invoke()
        }

        val thread = Thread(r)
        thread.start()
    }

    //특적방만  room list 에서 지워준다.
    fun deleteRoomWithIdolId(idolId :Int,callback: (()->Unit)?){
        val r = Runnable {

            //해당 idol id의 room들  조회
            val roomList:List<ChatRoomListModel>? = ChatDB.getInstance(ctx, accountId!!)?.ChatRoomListDao()?.getChatRoomWithIdolId(idolId)
             if(!roomList.isNullOrEmpty()){
                 //조회된 room id에  매칭되는 메세지들을 전부 지워준다.
                 for(chatRoom:ChatRoomListModel in roomList){
                  ChatDB.getInstance(ctx, accountId!!)?.ChatDao()?.deleteChatRoomMessages(chatRoom.roomId)
                 }
             }
            ChatDB.getInstance(ctx, accountId!!)?.ChatRoomListDao()?.deleteRoomWithIdolId(idolId)

            callback?.invoke()
        }

        val thread = Thread(r)
        thread.start()
    }

    //해당 아이디 로그인한것만 삭제
    fun deleteAccountId(callback: (() -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatRoomListDao()
                    ?.deleteAccountId()
                callback?.invoke()
            }
        }
        val thread = Thread(r)
        thread.start()
    }


}