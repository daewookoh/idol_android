package net.ib.mn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity

/**
 * Room Database for the application.
 * Holds the database and serves as the main access point for the persisted data.
 */
@Database(
    entities = [IdolEntity::class],
    version = 3,
    exportSchema = false
)
abstract class IdolDatabase : RoomDatabase() {

    /**
     * Provides access to IdolDao.
     */
    abstract fun idolDao(): IdolDao

    companion object {
        const val DATABASE_NAME = "idol_database"
    }
}
