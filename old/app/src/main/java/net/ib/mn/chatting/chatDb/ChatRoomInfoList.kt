package net.ib.mn.chatting.chatDb

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.model.ChatMembersModel
import net.ib.mn.chatting.model.ChatRoomInfoModel
import net.ib.mn.utils.SingletonHolder
import kotlin.collections.ArrayList

class ChatRoomInfoList private constructor(context: Context){
//    companion object : SingletonHolder<ChatRoomInfoList, Context>(::ChatRoomInfoList)

    companion object {
        @Volatile private var instance: ChatRoomInfoList? = null

        @JvmStatic fun getInstance(context: Context): ChatRoomInfoList =
            instance ?: synchronized(this) {
                instance ?: ChatRoomInfoList(context).also {
                    instance = it
                }
            }

        fun destroyInstance() {
            instance = null
        }
    }

    private var chatRoomInfoList = ArrayList<ChatRoomInfoModel>()

    val gson: Gson = IdolGson.getInstance()

    private val ctx = context

    var accountId :Int? = null

    init {
        accountId = IdolAccount.getAccount(context)?.userId
    }

    suspend fun setChatRoomInfo(_chatRoomInfoModel: ChatRoomInfoModel?) {
        ChatDB.getInstance(ctx, accountId!!)
            ?.ChatRoomInfoDao()
            ?.insertChatRoomInfo(_chatRoomInfoModel ?: return)
    }

    suspend fun getChatRoomInfo(roomId: Int): ChatRoomInfoModel? {
        return ChatDB.getInstance(ctx, accountId!!)
            ?.ChatRoomInfoDao()
            ?.getChatRoomInfo(roomId)
    }

    fun deleteRoomInfo(roomId: Int ,callback: (() -> Unit)?){
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatRoomInfoDao()
                    ?.deleteRoomInfo(roomId)
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
                    ?.ChatRoomInfoDao()
                    ?.deleteAccountId()
                callback?.invoke()
            }
        }
        val thread = Thread(r)
        thread.start()
    }

}