package net.ib.mn.data.impl

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.bound.flowDataResource
import net.ib.mn.data.local.AwardsLocalDataSource
import net.ib.mn.data.model.toIdolEntity
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.repository.AwardsRepository
import javax.inject.Inject

class AwardsRepositoryImpl @Inject constructor(
    private val awardsLocalDataSource: AwardsLocalDataSource
) : AwardsRepository {
    override fun getById(id: Int): Flow<DataResource<Idol?>> = flowDataResource {
        awardsLocalDataSource.getById(id)
    }

    override fun getAll(): Flow<DataResource<List<Idol>?>> = flowDataResource {
        awardsLocalDataSource.getAll()
    }

    override fun insert(idolList: List<Idol>): Flow<DataResource<Unit>> = flowDataResource {
        awardsLocalDataSource.insert(idolList.map { it.toIdolEntity() })
    }

    override fun deleteAll(): Flow<DataResource<Unit>> = flowDataResource {
        awardsLocalDataSource.deleteAll()
    }

    override fun deleteAllAndInsert(idolList: List<Idol>): Flow<DataResource<Unit>>  = flowDataResource {
        awardsLocalDataSource.deleteAllAndInsert(idolList.map { it.toIdolEntity() })
    }
}