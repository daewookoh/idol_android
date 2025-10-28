package net.ib.mn.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import net.ib.mn.local.model.IdolLocal
import net.ib.mn.local.room.IdolRoomConstant

@Dao
interface IdolDao {
    @Query("SELECT * FROM ${IdolRoomConstant.Table.IDOL} WHERE id=:id LIMIT 1")
    suspend fun getIdolById(id: Int): IdolLocal?

    @Query("SELECT * FROM ${IdolRoomConstant.Table.IDOL} WHERE id IN (:ids) ORDER BY heart DESC")
    suspend fun getIdolsByIds(ids: List<Int>): List<IdolLocal>

    @Query("SELECT * FROM ${IdolRoomConstant.Table.IDOL} WHERE isViewable='Y'")
    suspend fun getViewableIdols(): List<IdolLocal>

    @Query("SELECT * FROM ${IdolRoomConstant.Table.IDOL}")
    suspend fun getAll(): List<IdolLocal>

    @Query("SELECT * FROM ${IdolRoomConstant.Table.IDOL} WHERE type=:type AND category=:category AND isViewable='Y'")
    suspend fun getIdolByTypeAndCategory(type:String, category:String): List<IdolLocal>

    @Query("SELECT * FROM ${IdolRoomConstant.Table.IDOL} WHERE category=:category AND isViewable='Y'")
    suspend fun getByCategory(category:String): List<IdolLocal>

    @Query("SELECT * FROM ${IdolRoomConstant.Table.IDOL} WHERE type=:type AND isViewable='Y'")
    suspend fun getByType(type:String): List<IdolLocal>

    @Query("SELECT * FROM ${IdolRoomConstant.Table.IDOL} WHERE id in (:id)")
    suspend fun getId(id : List<Int>) : List<IdolLocal>

    @Insert(onConflict = REPLACE)
    suspend fun insert(idol: IdolLocal)

    @Insert(onConflict = REPLACE)
    suspend fun insert(idols: List<IdolLocal>)

    @Query("DELETE FROM ${IdolRoomConstant.Table.IDOL}")
    suspend fun deleteAll()

    @Transaction
    suspend fun deleteAllAndInsert(idols: List<IdolLocal>){
        deleteAll()
        insert(idols)
    }

    @Transaction
    @Update
    suspend fun update(idol: IdolLocal)

    @Transaction
    @Update
    suspend fun updateIdols(idols: List<IdolLocal>)

    @Transaction
    @Query("UPDATE ${IdolRoomConstant.Table.IDOL} SET heart=:heart, top3=:top3, top3Type=:top3Type, top3ImageVer=:top3ImageVer, imageUrl=:imageUrl, imageUrl2=:imageUrl2, imageUrl3=:imageUrl3 WHERE id=:id")
    suspend fun update(id: Int, heart: Long, top3: String?, top3Type: String?, top3ImageVer: String, imageUrl: String?, imageUrl2: String?, imageUrl3: String?)

    @Upsert
    suspend fun upsert(idol: IdolLocal)
}