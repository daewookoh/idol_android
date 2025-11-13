package net.ib.mn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import net.ib.mn.data.local.dao.ChartRankingDao
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.ChartRankingEntity
import net.ib.mn.data.local.entity.IdolEntity

/**
 * Room Database for the application - old 프로젝트와 완전히 동일한 구조
 * Holds the database and serves as the main access point for the persisted data.
 *
 * old 프로젝트: local/src/main/kotlin/net/ib/mn/local/room/IdolDatabase.kt
 * - DB name: "new_idol" (old 프로젝트와 동일)
 * - version: 7 (차트 랭킹 테이블 필드 확장)
 */
@Database(
    entities = [
        IdolEntity::class,
        ChartRankingEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class IdolDatabase : RoomDatabase() {

    /**
     * Provides access to IdolDao.
     */
    abstract fun idolDao(): IdolDao

    /**
     * Provides access to ChartRankingDao.
     */
    abstract fun chartRankingDao(): ChartRankingDao

    companion object {
        const val DATABASE_NAME = "new_idol"  // old 프로젝트와 동일
    }
}
