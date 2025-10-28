package net.ib.mn.chatting.chatDb

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteException
import com.google.gson.Gson
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.utils.Logger
import net.ib.mn.utils.SingletonHolder
import net.ib.mn.utils.Util

class ChatMessageList private constructor(context: Context){
    //companion object : SingletonHolder<ChatMessageList, Context>(::ChatMessageList)

    companion object {
        @Volatile private var instance: ChatMessageList? = null

        @JvmStatic fun getInstance(context: Context): ChatMessageList =
            instance ?: synchronized(this) {
                instance ?: ChatMessageList(context).also {
                    instance = it
                }
            }

        fun destroyInstance() {
            instance = null
        }
    }

    private var chatMessageList = ArrayList<MessageModel>()

    val gson: Gson = IdolGson.getInstance()

    private val ctx = context

    var accountId :Int? = null

    init {
        accountId = IdolAccount.getAccount(context)?.userId
    }


    fun setChatMessage( _chatMessageList: ArrayList<MessageModel>, callback: (()->Unit)? ) {
        chatMessageList.clear()
        chatMessageList.addAll(_chatMessageList)

        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatDao()
                    ?.insertChatMessage(_chatMessageList)
                callback?.invoke()
            }
        }

        val thread = Thread(r)
        thread.start()
    }

    //메시지 한개 가져오기용. 
    fun getMessage(message: MessageModel?,callback: ((MessageModel?)->Unit)?){

        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                if (message != null) {
                    val model = ChatDB.getInstance(ctx, accountId!!)
                        ?.ChatDao()
                        ?.getMyMessage(message.userId, message.clientTs, message.roomId)
                    callback?.invoke(model)
                } else {
                    callback?.invoke(null)
                }
            }
        }

        val thread = Thread(r)
        thread.start()
    }

    //메시지 저장.
    fun setChatMessage( _chatMessage: MessageModel, callback: (()->Unit)? ) {
        Logger.v("context 를 보자구"+ctx)

        val r = Runnable {
            ChatDB.getInstance(ctx,accountId!!)?.runInTransaction {
                ChatDB.getInstance(ctx,accountId!!)
                        ?.ChatDao()
                        ?.insertChatMessage(_chatMessage)
                callback?.invoke()
            }
        }

        val thread = Thread(r)
        thread.start()
    }


    //메시지 갱신.
    fun update(message: MessageModel, callback: ((Int) -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx,accountId!!)?.runInTransaction {
                try {
                    Util.log("===update message" + message.content)
                    val returnCount = ChatDB.getInstance(ctx,accountId!!)?.ChatDao()?.update(message.userId, message.clientTs, message.roomId, message.status, message.serverTs, message.isReadable, message.statusFailed)
                    callback?.invoke(returnCount!!)
                }catch (e: SQLiteException){ //익셉션 -> 프라이머리키 중복 -> 메시지가 중복됐다는뜻.
                    callback?.invoke(-1)
                    e.printStackTrace()
                }
            }
        }

        val thread = Thread(r)
        thread.start()
    }

    //메시지 갱신.
    fun updateLinkMessage(message: MessageModel, callback: (() -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx,accountId!!)?.runInTransaction {
                Util.log("===update message" + message.content)
                ChatDB.getInstance(ctx,accountId!!)?.ChatDao()?.updateLinkMessage(message.userId, message.clientTs, message.roomId, message.isLinkUrl,message.content,message.contentType)
                callback?.invoke()
            }
        }

        val thread = Thread(r)
        thread.start()
    }

    //statusFailed 갱신용...너무헷갈려서 그냥만들음.
    fun updateStatusFailed(message: MessageModel, callback: ((Int) -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx,accountId!!)?.runInTransaction {
                try{
                    Util.log("===update message" + message.content)
                    val returnCount = ChatDB.getInstance(ctx,accountId!!)?.ChatDao()?.updateStatusFailed(message.userId, message.clientTs, message.roomId, message.statusFailed, message.serverTs)
                    callback?.invoke(returnCount!!)
                }catch (e: SQLiteException){
                    callback?.invoke(-1)
                    e.printStackTrace()
                }
            }
        }

        val thread = Thread(r)
        thread.start()
    }

    //reported 갱신용.
    fun updateReported(message: MessageModel, callback: (() -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                Util.log("===update message" + message.content)
                ChatDB.getInstance(ctx, accountId!!)?.ChatDao()?.updateReported(message.userId, message.roomId, message.reported, message.serverTs, message.contentType)
                callback?.invoke()
            }
        }

        val thread = Thread(r)
        thread.start()
    }

    //방별 db가다르므로 roomId에 따라 fetch를 시켜줌.
    suspend fun fetchMessages(roomId: Int, limit: Int, offset: Int): ArrayList<MessageModel> {
        val dbMessages =
            ChatDB.getInstance(ctx, accountId!!)?.ChatDao()?.getMessages(roomId, limit, offset)!!
        Util.log("idoltalk::fetchMessages::${dbMessages.size}")
        Logger.v("Paging check::${dbMessages.size}")
        chatMessageList.clear()
        chatMessageList.addAll(dbMessages)
        chatMessageList.reverse()
        return chatMessageList
    }

    suspend fun getMessageCount(roomId: Int) : Int? = ChatDB.getInstance(ctx, accountId!!)?.ChatDao()?.getMessagesCount(roomId)

    //현재남아있는 DB에서 가장 최신의 데이터를 가져온다(해당데이터의 server_ts~현재시간 까지의 데이터가져오기위해).
    suspend fun getLatestServerts(roomId: Int) : Long? {
        //내림차순으로 server_ts를 가져옴. 만약 사이즈가 0이면 아무 데이터도 없다는뜻 null리턴해준다.
        val listServerts =
            ChatDB.getInstance(ctx, accountId!!)?.ChatDao()?.getLatestServerts(roomId, true)
        return if (listServerts?.size == 0) {
            null
        } else {
            if (listServerts?.size!! < 10) {
                listServerts[0]
            } else {
                listServerts[9]
            }
        }
    }


    //룸에 저장된  각 채팅방의  채팅 내용을 삭제한다.
    //callback 없는 버전
    fun deleteChatRoomMessages(roomId: Int){
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatDao()
                    ?.deleteChatRoomMessages(roomId)
            }
        }
        val thread = Thread(r)
        thread.start()
    }

    //메시지 삭제한거 한개만 업데이트용(deleted).
    fun updateChatRoomMessage(message: MessageModel, callback: ((MessageModel?) -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                val returnCount = ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatDao()
                    //삭제되는 메세지들은  contenttype 이 텍스트이므로  로컬 db에도  type을  텍스트 타입으로 명시해준다.
                    ?.updateChatRoomMessage(message.roomId, message.serverTs, message.userId, true, MessageModel.CHAT_TYPE_TEXT)
                Util.log("idoltalkRoom::retrunCount$returnCount")
                if(returnCount == 0 ){
                    callback?.invoke(null)
                } else {
                    callback?.invoke(message)
                }
            }
        }
        val thread = Thread(r)
        thread.start()
    }

    //메시지 진짜 한개 삭제용.
    fun deleteChatRoomMessage(message: MessageModel, callback: ((MessageModel?) -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                val returnCount = ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatDao()
                    //삭제되는 메세지들은  contenttype 이 텍스트이므로  로컬 db에도  type을  텍스트 타입으로 명시해준다.
                    ?.deleteChatRoomMessage(message.roomId, message.serverTs, message.userId)
                Util.log("idoltalkRoom:: delete retrunCount $returnCount")
                if(returnCount == 0 ){
                    callback?.invoke(null)
                } else {
                    callback?.invoke(message)
                }
            }
        }
        val thread = Thread(r)
        thread.start()
    }

    //메시지 중복 삭제용.
    fun deleteDuplicateMessage(message: MessageModel, callback: ((MessageModel?) -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                val returnCount = ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatDao()
                    ?.deleteDuplicateMessage(message.roomId, message.clientTs, message.userId, status = false)
                Util.log("idoltalkRoom:: delete retrunCount $returnCount")
                if(returnCount == 0 ){
                    callback?.invoke(null)
                } else {
                    callback?.invoke(message)
                }
            }
        }
        val thread = Thread(r)
        thread.start()
    }

    //메시지 여러개 삭제용(lastServerTs기준으로 그이후것들 삭제할떄 사용)
    fun deleteUnReadableMessages(lastServerTs:Long, callback: (() -> Unit)?){
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)?.runInTransaction {
                ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatDao()
                    ?.deleteUnReadableMessages(lastServerTs)
                callback?.invoke()
            }
        }
        val thread = Thread(r)
        thread.start()
    }

    //해당 아이디 로그인한것만 삭제
    fun deleteAccountId(callback: (() -> Unit)?) {
        val r = Runnable {
            ChatDB.getInstance(ctx,accountId!!)?.runInTransaction {
                ChatDB.getInstance(ctx,accountId!!)
                    ?.ChatDao()
                    ?.deleteAccountId()
                callback?.invoke()
            }
        }
        val thread = Thread(r)
        thread.start()
    }

    //메시지 전부 삭제(현재는 테스트용이므로 나중에 삭제)
    fun clearAll(){
        chatMessageList.clear()
        val r = Runnable {
            ChatDB.getInstance(ctx, accountId!!)
                    ?.ChatDao()
                    ?.deleteAll()
        }

        val thread = Thread(r)
        thread.start()
    }

    fun getAll():List<MessageModel>? {
        return chatMessageList
    }
}