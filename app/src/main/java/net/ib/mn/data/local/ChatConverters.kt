package net.ib.mn.data.local

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverters for Chat Database
 * Date <-> Long 변환 처리
 *
 * old 프로젝트: app/src/main/java/net/ib/mn/chatting/roomMigration/Converters.kt
 */
class ChatConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
