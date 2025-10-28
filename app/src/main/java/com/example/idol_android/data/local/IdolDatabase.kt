package com.example.idol_android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.idol_android.data.local.dao.IdolDao
import com.example.idol_android.data.local.entity.IdolEntity

/**
 * Room Database for the application.
 * Holds the database and serves as the main access point for the persisted data.
 */
@Database(
    entities = [IdolEntity::class],
    version = 1,
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
