package net.ib.mn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity

/**
 * Room Database for the application
 * Holds the database and serves as the main access point for the persisted data.
 *
 * old 프로젝트: local/src/main/kotlin/net/ib/mn/local/room/IdolDatabase.kt
 * - DB name: "new_idol" (old 프로젝트와 동일)
 * - version: 8 (ChartRankingEntity 제거, SharedPreference로 대체)
 */
@Database(
    entities = [
        IdolEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class IdolDatabase : RoomDatabase() {

    /**
     * Provides access to IdolDao.
     */
    abstract fun idolDao(): IdolDao


    companion object {
        const val DATABASE_NAME = "new_idol"  // old 프로젝트와 동일
    }
}
