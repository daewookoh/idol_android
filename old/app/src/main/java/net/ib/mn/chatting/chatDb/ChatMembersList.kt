package net.ib.mn.chatting.chatDb

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.model.ChatMembersModel
import java.util.concurrent.CopyOnWriteArrayList

class ChatMembersList private constructor(context: Context){
//    companion object : SingletonHolder<ChatMembersList, Context>(::ChatMembersList)

    companion object {
        @Volatile private var instance: ChatMembersList? = null

        @JvmStatic fun getInstance(context: Context): ChatMembersList =
            instance ?: synchronized(this) {
                instance ?: ChatMembersList(context).also {
                    instance = it
                }
            }

        fun destroyInstance() {
            instance = null
        }
    }

    private var chatMembersList = CopyOnWriteArrayList<ChatMembersModel>()

    val gson: Gson = IdolGson.getInstance()

    private val ctx = context

    var accountId :Int? = null

    init {
        accountId = IdolAccount.getAccount(context)?.userId
    }

    suspend fun setChatMembers(_chatMembersList: CopyOnWriteArrayList<ChatMembersModel>) {
        chatMembersList.clear()
        chatMembersList.addAll(_chatMembersList)

        ChatDB.getInstance(ctx, accountId!!)
            ?.ChatMembersDao()
            ?.insertChatMembers(chatMembersList)
    }

    //userId가 같은 Model가져오기.
    suspend fun getChatRoomMember(userId: Int, roomId: Int): ChatMembersModel? {
        return ChatDB.getInstance(ctx, accountId!!)?.ChatMembersDao()
            ?.getChatRoomMember(userId, roomId)
    }

    //원래 가지고 있던 멤버의 값 업데이트
    suspend fun updateChatMember(userId: Int?, roomId: Int?, chatMembersModel: ChatMembersModel){
        ChatDB.getInstance(ctx, accountId!!)
            ?.ChatMembersDao()
            ?.updateChatMember(
                userId,
                roomId,
                chatMembersModel.nickname,
                chatMembersModel.imageUrl,
                chatMembersModel.level,
                false,
                chatMembersModel.role
            ) // ADD_JOIN 받았을 때 이미 있던 멤버일 경우 deleted, role 바꿔줍니다.
    }

    //userId , roomId 같은 Model가져오기.
    suspend fun updateDeletedMember(userId: Int?, roomId: Int?) {
        ChatDB.getInstance(ctx, accountId!!)
            ?.ChatMembersDao()
            ?.updateDeletedMember(
                userId,
                roomId,
                true,
                "N"
            ) // 삭제됐으면 방장이 되면 안되므로 무조건 role N으로 바꿔줍니다.
    }

    //userId , roomId 같은 Model가져오기.
    suspend fun updateDeletedMemberList(
        userIdList: CopyOnWriteArrayList<ChatMembersModel>?,
        roomId: Int?
    ) {
        for (i in userIdList!!.indices) {
            ChatDB.getInstance(ctx, accountId!!)
                ?.ChatMembersDao()
                ?.updateDeletedMember(
                    userIdList[i].id,
                    roomId,
                    true,
                    "N"
                ) // 삭제됐으면 방장이 되면 안되므로 무조건 role N으로 바꿔줍니다.
        }
    }

    //roomId가 같은 list가져오기.
    suspend fun getChatMemberList(roomId: Int): CopyOnWriteArrayList<ChatMembersModel> {
        val dbMembers = ChatDB.getInstance(ctx, accountId!!)
            ?.ChatMembersDao()
            ?.getChatMemberList(roomId)
        val filterMembers = CopyOnWriteArrayList<ChatMembersModel>()
        filterMembers.addAll(dbMembers ?: CopyOnWriteArrayList())
        return filterMembers
    }

    //해당 아이디 로그인한것만 삭제
    fun deleteAccountId(callback: (() -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatMembersDao()
                    ?.deleteAccountId()
                callback?.invoke()
            }
        }
        val thread = Thread(r)
        thread.start()
    }

    //멤버 전부 삭제
    fun clearAll(){
        chatMembersList.clear()
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatMembersDao()
                    ?.deleteAll()
        }

        val thread = Thread(r)
        thread.start()
    }

    fun getAll():List<ChatMembersModel>? {
        return chatMembersList
    }

}