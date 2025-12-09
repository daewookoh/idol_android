package net.ib.mn.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.ib.mn.data.local.dao.ChatMemberDao
import net.ib.mn.data.local.dao.ChatMessageDao
import net.ib.mn.data.local.dao.ChatRoomDao
import net.ib.mn.data.local.dao.ChatRoomInfoDao
import net.ib.mn.domain.model.ChatMemberModel
import net.ib.mn.domain.model.ChatMessageModel
import net.ib.mn.domain.model.ChatRoomInfoModel
import net.ib.mn.domain.model.ChatRoomModel

/**
 * Chat Room Database
 * 채팅 관련 데이터를 저장하는 Room Database
 *
 * old 프로젝트: app/src/main/java/net/ib/mn/chatting/chatDb/ChatDB.kt
 * - DB name: "${accountId}_chat.db" (계정별 분리)
 * - version: 1 (신규 프로젝트이므로 초기 버전)
 */
@Database(
    entities = [
        ChatMessageModel::class,
        ChatMemberModel::class,
        ChatRoomModel::class,
        ChatRoomInfoModel::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ChatConverters::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatMemberDao(): ChatMemberDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun chatRoomInfoDao(): ChatRoomInfoDao

    companion object {
        private const val DATABASE_NAME_SUFFIX = "_chat.db"

        @Volatile
        private var instances: MutableMap<Int, ChatDatabase> = mutableMapOf()

        /**
         * 계정별 ChatDatabase 인스턴스 생성
         * old 프로젝트와 동일하게 accountId별로 별도 DB 사용
         */
        @JvmStatic
        fun getInstance(context: Context, accountId: Int): ChatDatabase {
            return instances[accountId] ?: synchronized(this) {
                instances[accountId] ?: buildDatabase(context, accountId).also {
                    instances[accountId] = it
                }
            }
        }

        private fun buildDatabase(context: Context, accountId: Int): ChatDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java,
                "$accountId$DATABASE_NAME_SUFFIX"
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * 특정 계정의 ChatDatabase 인스턴스 해제
         */
        fun destroyInstance(accountId: Int) {
            instances[accountId]?.let { db ->
                try {
                    db.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                instances.remove(accountId)
            }
        }

        /**
         * 모든 ChatDatabase 인스턴스 해제
         */
        fun destroyAll() {
            instances.forEach { (_, db) ->
                try {
                    db.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            instances.clear()
        }
    }
}
