package net.ib.mn.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import net.ib.mn.data.local.entity.IdolEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) for Idol data.
 * Provides methods to interact with the idols table.
 */
@Dao
interface IdolDao {

    /**
     * Get all idols as a Flow.
     * Flow automatically updates when data changes.
     */
    @Query("SELECT * FROM idols ORDER BY heartCount DESC")
    fun getAllIdols(): Flow<List<IdolEntity>>

    /**
     * Get all idols synchronously (for debugging/verification).
     */
    @Query("SELECT * FROM idols ORDER BY heartCount DESC")
    suspend fun getAllIdolsSync(): List<IdolEntity>

    /**
     * Get top 3 favorite idols.
     */
    @Query("SELECT * FROM idols WHERE isTop3 = 1 ORDER BY heartCount DESC LIMIT 3")
    fun getTop3Idols(): Flow<List<IdolEntity>>

    /**
     * Get idol by ID.
     */
    @Query("SELECT * FROM idols WHERE id = :idolId")
    suspend fun getIdolById(idolId: Int): IdolEntity?

    /**
     * Get idols by ID list.
     * old 프로젝트와 동일한 방식으로 ID 리스트로 아이돌 조회
     *
     * @param idList 아이돌 ID 리스트
     * @return List<IdolEntity> ID 리스트에 해당하는 아이돌들
     */
    @Query("SELECT * FROM idols WHERE id IN (:idList)")
    suspend fun getIdolsByIds(idList: List<Int>): List<IdolEntity>

    /**
     * Insert or update an idol.
     * OnConflictStrategy.REPLACE will update if exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdol(idol: IdolEntity)

    /**
     * Insert multiple idols.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdols(idols: List<IdolEntity>)

    /**
     * Update an idol.
     */
    @Update
    suspend fun updateIdol(idol: IdolEntity)

    /**
     * Delete an idol.
     */
    @Delete
    suspend fun deleteIdol(idol: IdolEntity)

    /**
     * Delete all idols.
     */
    @Query("DELETE FROM idols")
    suspend fun deleteAllIdols()

    /**
     * Update heart count for an idol.
     */
    @Query("UPDATE idols SET heartCount = :heartCount WHERE id = :idolId")
    suspend fun updateHeartCount(idolId: Int, heartCount: Int)

    /**
     * Toggle top3 status for an idol.
     */
    @Query("UPDATE idols SET isTop3 = :isTop3 WHERE id = :idolId")
    suspend fun updateTop3Status(idolId: Int, isTop3: Boolean)
}
