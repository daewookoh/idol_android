package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Anniversary
import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.model.IdolFiledData

interface IdolRepository {
    fun getAllIdols(): Flow<DataResource<List<Idol>>>
    fun saveIdols(idolList: List<Idol>): Flow<DataResource<Unit>>
    fun deleteAllAndSaveIdols(idolList: List<Idol>): Flow<DataResource<Unit>>
    fun getIdolById(idolId: Int): Flow<DataResource<Idol?>>
    fun getIdolsByIds(idList: List<Int>): Flow<DataResource<List<Idol>>>
    fun upsertWithTs(idolList: List<Idol>, ts: Int): Flow<DataResource<Boolean>>
    // TODO unman cdnUrl, reqImageSize -> ConfigModel 리팩토링 끝나면 삭제
    fun updateHeartAndTop3(cdnUrl:String, reqImageSize: String, idolFiledList: List<IdolFiledData>): Flow<DataResource<Unit>>
    fun deleteAllIdol(): Flow<DataResource<Unit>>
    fun getViewableIdols(): Flow<DataResource<List<Idol>>>
    fun updateIdol(idol: Idol): Flow<DataResource<Unit>>
    fun getIdolByTypeAndCategory(type: String?, category: String?): Flow<DataResource<List<Idol>>>
    fun updateAnniversaries(cdnUrl:String, reqImageSize: String, anniversaryList: List<Anniversary>): Flow<DataResource<Unit>>
    fun upsert(idol: Idol): Flow<DataResource<Unit>>
    fun getIdolChartCodes(): Flow<DataResource<Map<String, List<String>>>>
    fun saveIdolChartCodes(codes: Map<String, List<String>>): Flow<DataResource<Unit>>
}