package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Idol

interface AwardsRepository {
    fun getById(id: Int): Flow<DataResource<Idol?>>
    fun getAll(): Flow<DataResource<List<Idol>?>>
    fun insert(idolList: List<Idol>): Flow<DataResource<Unit>>
    fun deleteAll(): Flow<DataResource<Unit>>
    fun deleteAllAndInsert(idolList: List<Idol>): Flow<DataResource<Unit>>
}