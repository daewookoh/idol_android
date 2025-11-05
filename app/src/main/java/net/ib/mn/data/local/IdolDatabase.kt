package net.ib.mn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity

/**
 * Room Database for the application - old 프로젝트와 완전히 동일한 구조
 * Holds the database and serves as the main access point for the persisted data.
 *
 * old 프로젝트: local/src/main/kotlin/net/ib/mn/local/room/IdolDatabase.kt
 * - DB name: "new_idol" (old 프로젝트와 동일)
 * - version: 5 (old 프로젝트와 동일)
 */
@Database(
    entities = [IdolEntity::class],
    version = 5,
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
