package net.ib.mn.chatting.chatDb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.ib.mn.chatting.model.*
import net.ib.mn.chatting.roomMigration.Converters
import net.ib.mn.chatting.roomMigration.MigrationChatDatabase
import net.ib.mn.model.IdolModel
import java.lang.Exception

@Database(entities = [MessageModel::class, ChatMembersModel::class, ChatRoomListModel::class , ChatRoomInfoModel::class] , version = 40)
@TypeConverters(Converters::class)
abstract class ChatDB: RoomDatabase() {
    abstract fun ChatDao() : ChatDao
    abstract fun ChatMembersDao() : ChatMembersDao
    abstract fun ChatRoomListDao() : ChatRoomListDao
    abstract fun ChatRoomInfoDao() : ChatRoomInfoDao

    companion object {
        @Volatile private var instance: ChatDB? = null

        @JvmStatic fun getInstance(context: Context, accountId: Int): ChatDB? {
            if (instance == null) {
                synchronized(ChatDB::class) {
                    instance = Room.databaseBuilder(context.applicationContext,
                        ChatDB::class.java, "${accountId}_chat.db")
                        .addMigrations(
                            MigrationChatDatabase.MIGRATE_28_29,
                            MigrationChatDatabase.MIGRATE_29_30,
                            MigrationChatDatabase.MIGRATE_30_31,
                            MigrationChatDatabase.MIGRATE_31_32,
                            MigrationChatDatabase.MIGRATE_32_33,
                            MigrationChatDatabase.MIGRATE_33_34,
                            MigrationChatDatabase.MIGRATE_34_35,
                            MigrationChatDatabase.MIGRATE_35_36,
                            MigrationChatDatabase.MIGRATE_36_37)
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return instance
        }

        fun destroyInstance() {

            try {
                instance?.endTransaction()
                instance?.close()
            }catch (e : Exception){
                e.printStackTrace()
            }
            instance = null
        }
    }
}