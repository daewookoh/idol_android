package net.ib.mn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import net.ib.mn.data.local.entity.IdolEntity

/**
 * DAO (Data Access Object) for Idol data.
 * old 프로젝트의 IdolDao와 완전히 동일한 구조
 *
 * old 프로젝트: local/src/main/kotlin/net/ib/mn/local/room/dao/IdolDao.kt
 */
@Dao
interface IdolDao {
    @Query("SELECT * FROM idols WHERE id=:id LIMIT 1")
    suspend fun getIdolById(id: Int): IdolEntity?

    @Query("SELECT * FROM idols WHERE id IN (:ids) ORDER BY heart DESC")
    suspend fun getIdolsByIds(ids: List<Int>): List<IdolEntity>

    @Query("SELECT * FROM idols WHERE isViewable='Y'")
    suspend fun getViewableIdols(): List<IdolEntity>

    @Query("SELECT * FROM idols")
    suspend fun getAll(): List<IdolEntity>

    @Query("SELECT * FROM idols WHERE type=:type AND category=:category AND isViewable='Y'")
    suspend fun getIdolByTypeAndCategory(type:String, category:String): List<IdolEntity>

    @Query("SELECT * FROM idols WHERE category=:category AND isViewable='Y'")
    suspend fun getByCategory(category:String): List<IdolEntity>

    @Query("SELECT * FROM idols WHERE type=:type AND isViewable='Y'")
    suspend fun getByType(type:String): List<IdolEntity>

    @Query("SELECT * FROM idols WHERE id in (:id)")
    suspend fun getId(id : List<Int>) : List<IdolEntity>

    @Insert(onConflict = REPLACE)
    suspend fun insert(idol: IdolEntity)

    @Insert(onConflict = REPLACE)
    suspend fun insert(idols: List<IdolEntity>)

    @Query("DELETE FROM idols")
    suspend fun deleteAll()

    @Transaction
    suspend fun deleteAllAndInsert(idols: List<IdolEntity>){
        deleteAll()
        insert(idols)
    }

    @Transaction
    @Update
    suspend fun update(idol: IdolEntity)

    @Transaction
    @Update
    suspend fun updateIdols(idols: List<IdolEntity>)

    @Transaction
    @Query("UPDATE idols SET heart=:heart, top3=:top3, top3Type=:top3Type, top3ImageVer=:top3ImageVer, imageUrl=:imageUrl, imageUrl2=:imageUrl2, imageUrl3=:imageUrl3 WHERE id=:id")
    suspend fun update(id: Int, heart: Long, top3: String?, top3Type: String?, top3ImageVer: String, imageUrl: String?, imageUrl2: String?, imageUrl3: String?)

    @Upsert
    suspend fun upsert(idol: IdolEntity)

    @Upsert
    suspend fun upsertIdols(idols: List<IdolEntity>)
}
