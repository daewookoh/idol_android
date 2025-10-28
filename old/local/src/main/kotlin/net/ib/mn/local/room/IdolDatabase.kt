package net.ib.mn.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.ib.mn.local.model.IdolLocal
import net.ib.mn.local.model.NotificationLocal
import net.ib.mn.local.room.dao.IdolDao
import net.ib.mn.local.room.dao.NotificationDao

@Database(
    entities = [IdolLocal::class, NotificationLocal::class],
    version = IdolRoomConstant.ROOM_VERSION
)

@TypeConverters(Converters::class)
abstract class IdolDatabase : RoomDatabase() {
    abstract fun idolDao(): IdolDao
    abstract fun notificationDao(): NotificationDao
}