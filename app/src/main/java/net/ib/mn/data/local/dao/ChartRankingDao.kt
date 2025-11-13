package net.ib.mn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.local.entity.ChartRankingEntity

/**
 * 차트 랭킹 데이터 접근 DAO
 *
 * 5개 차트 관리:
 * - PR_S_F, PR_S_M, PR_G_F, PR_G_M, GLOBALS
 */
@Dao
interface ChartRankingDao {

    /**
     * 특정 차트의 모든 랭킹 데이터 조회 (Flow - 실시간 구독)
     * rank 순으로 정렬
     */
    @Query("SELECT * FROM chart_rankings WHERE chartCode = :chartCode ORDER BY rank ASC")
    fun observeChartRankings(chartCode: String): Flow<List<ChartRankingEntity>>

    /**
     * 특정 차트의 모든 랭킹 데이터 조회 (일회성)
     * rank 순으로 정렬
     */
    @Query("SELECT * FROM chart_rankings WHERE chartCode = :chartCode ORDER BY rank ASC")
    suspend fun getChartRankings(chartCode: String): List<ChartRankingEntity>

    /**
     * 특정 차트에 특정 아이돌이 존재하는지 확인
     */
    @Query("SELECT COUNT(*) FROM chart_rankings WHERE chartCode = :chartCode AND idolId = :idolId")
    suspend fun exists(chartCode: String, idolId: Int): Int

    /**
     * 특정 차트의 데이터 개수 조회
     */
    @Query("SELECT COUNT(*) FROM chart_rankings WHERE chartCode = :chartCode")
    suspend fun getChartSize(chartCode: String): Int

    /**
     * 차트 랭킹 데이터 삽입 (충돌 시 교체)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRanking(ranking: ChartRankingEntity)

    /**
     * 차트 랭킹 데이터 일괄 삽입 (충돌 시 교체)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRankings(rankings: List<ChartRankingEntity>)

    /**
     * 특정 차트의 모든 랭킹 데이터 삭제
     */
    @Query("DELETE FROM chart_rankings WHERE chartCode = :chartCode")
    suspend fun deleteChartRankings(chartCode: String)

    /**
     * 모든 차트 랭킹 데이터 삭제
     */
    @Query("DELETE FROM chart_rankings")
    suspend fun deleteAll()

    /**
     * 특정 차트 데이터 전체 교체 (트랜잭션)
     */
    @Transaction
    suspend fun replaceChartRankings(chartCode: String, rankings: List<ChartRankingEntity>) {
        deleteChartRankings(chartCode)
        insertRankings(rankings)
    }

    /**
     * 특정 아이돌의 하트 수 업데이트 (모든 차트)
     */
    @Query("UPDATE chart_rankings SET heartCount = :newHeartCount, updatedAt = :timestamp WHERE idolId = :idolId")
    suspend fun updateIdolHeartCount(idolId: Int, newHeartCount: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * 특정 차트에서 특정 아이돌의 하트 수 업데이트
     */
    @Query("UPDATE chart_rankings SET heartCount = :newHeartCount, updatedAt = :timestamp WHERE chartCode = :chartCode AND idolId = :idolId")
    suspend fun updateChartIdolHeartCount(
        chartCode: String,
        idolId: Int,
        newHeartCount: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * 모든 차트 코드 조회 (디버깅용)
     */
    @Query("SELECT DISTINCT chartCode FROM chart_rankings")
    suspend fun getAllChartCodes(): List<String>
}
