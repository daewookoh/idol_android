package net.ib.mn.data.local

import net.ib.mn.data.model.IdolEntity

interface AwardsLocalDataSource {
    suspend fun getById(id: Int): IdolEntity?
    suspend fun getAll(): List<IdolEntity>?
    suspend fun insert(idols: List<IdolEntity>)
    suspend fun deleteAll()
    suspend fun deleteAllAndInsert(idols: List<IdolEntity>)
}