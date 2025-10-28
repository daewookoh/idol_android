package net.ib.mn.data.impl

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.bound.flowDataResource
import net.ib.mn.data.local.IdolLocalDataSource
import net.ib.mn.data.local.datastore.IdolPrefsDataSource
import net.ib.mn.data.model.toEntity
import net.ib.mn.data.model.toIdolEntity
import net.ib.mn.data.model.toIdolFiledDataEntity
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Anniversary
import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.model.IdolFiledData
import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdolRepositoryImpl @Inject constructor(
    private val idolLocalDataSource: IdolLocalDataSource,
    private val idolPrefsDataSource: IdolPrefsDataSource
) : IdolRepository {

    override fun getAllIdols(): Flow<DataResource<List<Idol>>> = flowDataResource {
        idolLocalDataSource.getAll()
    }

    override fun saveIdols(idolList: List<Idol>): Flow<DataResource<Unit>> = flowDataResource {
        idolLocalDataSource.saveIdols(idolList.map { it.toIdolEntity() })
    }

    override fun deleteAllAndSaveIdols(idolList: List<Idol>): Flow<DataResource<Unit>> =
        flowDataResource {
            idolLocalDataSource.deleteAllAndInsert(idolList.map { it.toIdolEntity() })
        }

    override fun getIdolById(idolId: Int): Flow<DataResource<Idol?>> = flowDataResource {
        idolLocalDataSource.getIdolById(idolId)
    }

    override fun getIdolsByIds(idList: List<Int>): Flow<DataResource<List<Idol>>> =
        flowDataResource {
            idolLocalDataSource.getIdolsByIds(idList)
        }

    override fun upsertWithTs(idolList: List<Idol>, ts: Int): Flow<DataResource<Boolean>> =
        flowDataResource {
            idolLocalDataSource.upsertWithTs(idolList.map { it.toIdolEntity() }, ts)
        }

    override fun updateHeartAndTop3(
        cdnUrl: String,
        reqImageSize: String,
        idolFiledList: List<IdolFiledData>
    ): Flow<DataResource<Unit>> = flowDataResource {
        idolLocalDataSource.update(
            cdnUrl,
            reqImageSize,
            idolFiledList.map { it.toIdolFiledDataEntity() })
    }

    override fun deleteAllIdol(): Flow<DataResource<Unit>> = flowDataResource {
        idolLocalDataSource.deleteAll()
    }

    override fun getViewableIdols(): Flow<DataResource<List<Idol>>> = flowDataResource {
        idolLocalDataSource.getViewableIdols()
    }

    override fun updateIdol(idol: Idol): Flow<DataResource<Unit>> = flowDataResource {
        idolLocalDataSource.update(idol.toIdolEntity())
    }

    override fun getIdolByTypeAndCategory(
        type: String?,
        category: String?
    ): Flow<DataResource<List<Idol>>> = flowDataResource {
        if (category != null && type != null) {
            idolLocalDataSource.getIdolByTypeAndCategory(type, category)
        } else if (category == null && type == null) {
            idolLocalDataSource.getViewableIdols()
        } else if (type == null) {
            idolLocalDataSource.getIdolByCategory(category!!)
        } else {
            idolLocalDataSource.getIdolByType(type)
        }
    }

    override fun updateAnniversaries(
        cdnUrl: String,
        reqImageSize: String,
        anniversaryList: List<Anniversary>
    ): Flow<DataResource<Unit>> = flowDataResource {
        idolLocalDataSource.updateAnniversaries(
            cdnUrl,
            reqImageSize,
            anniversaryList.map { it.toEntity() })
    }

    override fun upsert(idol: Idol): Flow<DataResource<Unit>> = flowDataResource {
        idolLocalDataSource.upsert(idol.toIdolEntity())
    }

    override fun getIdolChartCodes(): Flow<DataResource<Map<String, List<String>>>> = flowDataResource {
        idolPrefsDataSource.getIdolChartCodePrefs()
    }

    override fun saveIdolChartCodes(codes: Map<String, List<String>>): Flow<DataResource<Unit>> = flowDataResource {
        idolPrefsDataSource.saveIdolChartCodePrefs(codes)
    }
}