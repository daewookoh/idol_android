package net.ib.mn.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import net.ib.mn.local.model.IdolLocal
import net.ib.mn.local.room.AwardsRoomConstant

@Dao
interface AwardsIdolDao {
    @Query("SELECT * FROM ${AwardsRoomConstant.Table.IDOL} WHERE id=:id LIMIT 1")
    suspend fun getById(id: Int): IdolLocal?

    @Query("SELECT * FROM ${AwardsRoomConstant.Table.IDOL}")
    suspend fun getAll(): List<IdolLocal>

    @Insert(onConflict = REPLACE)
    suspend fun insert(idols: List<IdolLocal>)

    @Query("DELETE FROM ${AwardsRoomConstant.Table.IDOL}")
    suspend fun deleteAll()

    @Transaction
    suspend fun deleteAllAndInsert(idols: List<IdolLocal>){
        deleteAll()
        insert(idols)
    }
}