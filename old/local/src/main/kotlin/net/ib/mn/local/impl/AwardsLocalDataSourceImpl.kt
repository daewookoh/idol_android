package net.ib.mn.local.impl

import androidx.room.withTransaction
import net.ib.mn.data.local.AwardsLocalDataSource
import net.ib.mn.data.model.IdolEntity
import net.ib.mn.local.model.toLocal
import net.ib.mn.local.room.AwardsDatabase
import net.ib.mn.local.room.dao.AwardsIdolDao
import net.ib.mn.local.toData
import javax.inject.Inject

class AwardsLocalDataSourceImpl @Inject constructor(
    private val awardsDatabase: AwardsDatabase
) : AwardsLocalDataSource {

    private val awardsIdolDao = awardsDatabase.awardsIdolDao()

    override suspend fun getById(id: Int): IdolEntity? = awardsIdolDao.getById(id)?.toData()

    override suspend fun getAll(): List<IdolEntity>? = awardsIdolDao.getAll().toData()

    override suspend fun insert(idols: List<IdolEntity>) = awardsIdolDao.insert(idols.map { it.toLocal() })

    override suspend fun deleteAll() = awardsIdolDao.deleteAll()

    override suspend fun deleteAllAndInsert(idols: List<IdolEntity>) {
        awardsDatabase.withTransaction {
            awardsIdolDao.deleteAllAndInsert(idols.map { it.toLocal() })
        }
    }
}