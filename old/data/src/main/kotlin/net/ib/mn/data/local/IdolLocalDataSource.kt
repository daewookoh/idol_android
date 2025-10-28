package net.ib.mn.data.local

import net.ib.mn.data.model.AnniversaryEntity
import net.ib.mn.data.model.IdolEntity
import net.ib.mn.data.model.IdolFiledDataEntity

interface IdolLocalDataSource {
    suspend fun getIdolById(id: Int): IdolEntity?
    suspend fun getIdolsByIds(idList: List<Int>): List<IdolEntity>
    suspend fun getViewableIdols(): List<IdolEntity>
    suspend fun getAll(): List<IdolEntity>
    suspend fun getIdolByTypeAndCategory(type: String, category: String): List<IdolEntity>
    suspend fun getIdolByCategory(category: String): List<IdolEntity>
    suspend fun getIdolByType(type: String): List<IdolEntity>
    suspend fun getIdolsByIdList(id: List<Int>): List<IdolEntity>
    suspend fun saveIdol(idol: IdolEntity)
    suspend fun saveIdols(idols: List<IdolEntity>): Unit
    suspend fun deleteAll()
    suspend fun deleteAllAndInsert(idols: List<IdolEntity>)
    suspend fun update(idol: IdolEntity)
    suspend fun update(id: Int, heart: Long, top3: String?, top3Type: String?, top3ImageVer: String, imageUrl: String?, imageUrl2: String?, imageUrl3: String?)
    // TODO unman cdnUrl, reqImageSize -> ConfigModel 리팩토링 끝나면 삭제
    suspend fun update(cdnUrl:String, reqImageSize: String, idolFiledDataList: List<IdolFiledDataEntity>)
    suspend fun upsertWithTs(idols: List<IdolEntity>, ts: Int): Boolean
    suspend fun updateAnniversaries(cdnUrl:String, reqImageSize: String, anniversaries: List<AnniversaryEntity>)
    suspend fun upsert(idol: IdolEntity)
}