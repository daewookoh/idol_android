package net.ib.mn.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import net.ib.mn.local.model.IdolLocal
import net.ib.mn.local.room.dao.AwardsIdolDao

@Database(
    entities = [IdolLocal::class],
    version = AwardsRoomConstant.ROOM_VERSION
)

abstract class AwardsDatabase : RoomDatabase() {
    abstract fun awardsIdolDao(): AwardsIdolDao
}