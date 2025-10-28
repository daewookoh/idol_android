package net.ib.mn.local.impl

import androidx.core.net.toUri
import androidx.room.withTransaction
import net.ib.mn.common.util.logD
import net.ib.mn.common.util.updateQueryParameter
import net.ib.mn.data.local.IdolLocalDataSource
import net.ib.mn.data.model.AnniversaryEntity
import net.ib.mn.data.model.IdolContentTypeDataEntity
import net.ib.mn.data.model.IdolEntity
import net.ib.mn.data.model.IdolFiledDataEntity
import net.ib.mn.local.model.toLocal
import net.ib.mn.local.room.IdolDatabase
import net.ib.mn.local.room.dao.IdolDao
import net.ib.mn.local.toData
import java.util.Arrays
import javax.inject.Inject

class IdolLocalDataSourceImpl @Inject constructor(
    private val idolDatabase: IdolDatabase,
) : IdolLocalDataSource {

    private val idolDao = idolDatabase.idolDao()

    override suspend fun getIdolById(id: Int): IdolEntity? = idolDao.getIdolById(id)?.toData()

    override suspend fun getIdolsByIds(idList: List<Int>): List<IdolEntity> {
        val result = idolDao.getIdolsByIds(idList).toData()
        return result
    }

    override suspend fun getViewableIdols(): List<IdolEntity> = idolDao.getViewableIdols().toData()

    override suspend fun getAll(): List<IdolEntity> = idolDao.getAll().toData()

    override suspend fun getIdolByTypeAndCategory(
        type: String,
        category: String
    ): List<IdolEntity> = idolDao.getIdolByTypeAndCategory(type, category).toData()

    override suspend fun getIdolByCategory(category: String): List<IdolEntity> =
        idolDao.getByCategory(category).toData()

    override suspend fun getIdolByType(type: String): List<IdolEntity> =
        idolDao.getByType(type).toData()

    override suspend fun getIdolsByIdList(id: List<Int>): List<IdolEntity> =
        idolDao.getId(id).toData()

    override suspend fun saveIdol(idol: IdolEntity) = idolDao.insert(idol.toLocal())

    override suspend fun saveIdols(idols: List<IdolEntity>) {
        idolDao.insert(idols.map { it.toLocal() })
    }

    override suspend fun deleteAll() = idolDao.deleteAll()

    override suspend fun deleteAllAndInsert(idols: List<IdolEntity>) {
        idolDatabase.withTransaction {
            idolDao.deleteAllAndInsert(idols.map { it.toLocal() })
        }
    }

    override suspend fun update(idol: IdolEntity) = idolDao.update(idol.toLocal())

    override suspend fun update(
        id: Int,
        heart: Long,
        top3: String?,
        top3Type: String?,
        top3ImageVer: String,
        imageUrl: String?,
        imageUrl2: String?,
        imageUrl3: String?
    ) = idolDao.update(id, heart, top3, top3Type, top3ImageVer, imageUrl, imageUrl2, imageUrl3)

    override suspend fun update(
        cdnUrl: String,
        reqImageSize: String,
        idolFiledDataList: List<IdolFiledDataEntity>
    ) {
        logD("❤️ start updateHeartAndTop3")
        idolDatabase.withTransaction {
            idolFiledDataList.forEach { field ->
                // 초기값 세팅
                var imageUrl: String? = null
                var imageUrl2: String? = null
                var imageUrl3: String? = null

                var top3Ids: Array<String?> = if (field.top3 == null) {
                    arrayOfNulls(3)
                } else {
                    field.top3!!.split(",")
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                }
                top3Ids = Arrays.copyOf(top3Ids, 3)

                for (index in 0 until 3) {
                    if (!top3Ids[index].isNullOrEmpty()) {
                        val ver = field.top3ImageVer?.split(",")?.firstOrNull() ?: ""
                        val url: String? = when (index) {
                            0 -> field.imageUrl
                            1 -> field.imageUrl2
                            2 -> field.imageUrl3
                            else -> null
                        }

                        if (!url.isNullOrEmpty()) {
                            val newUrl = url.updateQueryParameter("ver", ver)
                            when (index) {
                                0 -> imageUrl = newUrl
                                1 -> imageUrl2 = newUrl
                                2 -> imageUrl3 = newUrl
                            }
                        }
                    }
                }

                // DB 업데이트: DAO의 update 메서드 (쿼리에서 id로 조건 걸고 업데이트)
                idolDao.update(
                    field.id,
                    field.heart,
                    field.top3,
                    field.top3Type,
                    field.top3ImageVer ?: "",
                    imageUrl,
                    imageUrl2,
                    imageUrl3
                )
            }
        }
        logD("❤️ done updateHeartAndTop3")
    }

    override suspend fun upsertWithTs(idols: List<IdolEntity>, ts: Int): Boolean {
        var hasUpdates = false

        idolDatabase.withTransaction {
            // 업데이트 할 아이돌 ID만 뽑음
            val idolIds = idols.map { it.id }
            // DB에서 해당 아이디에 맞는 아이동만 가져옴
            val existing = idolDao.getIdolsByIds(idolIds)
            val existingMap = existing.associateBy { it.id }

            // 업데이트할 아이돌 리스트
            val updates = mutableListOf<IdolEntity>()
            // 디비에 추가랑 아이동 리스트
            val inserts = mutableListOf<IdolEntity>()

            // update_ts가 현재 ts보다 이전 것들만 update하거나 insert한다
            for (idol in idols) {
                // 먼저 이미 있는 아이돌인지 체크
                val selected = existingMap[idol.id]
                if (selected != null) {
                    // 이미 있으면 이것의 update_ts가 현재 ts보다 이전이라면
                    if (selected.updateTs < ts) {
//                        logd("=== update ${idol.name} old ts=${selected.updateTs} new ts=${ts}")
                        idol.updateTs = ts
                        updates.add(idol)
                        hasUpdates = true
                    } else {
                        // 아니면 이미 최신 정보로 들어간거라 아무것도 안함
                        logD("=== NOT updating ${idol.name} updateTs=${selected.updateTs} ts=${ts}")
                    }
                } else {
                    // 새로 추가된 아이돌
//                    logd("=== Add new idol ${idol.name}")
                    inserts.add(idol)
                    hasUpdates = true
                }
            }

            if (updates.isNotEmpty()) {
                idolDao.updateIdols(updates.map { it.toLocal() })
            }
            if (inserts.isNotEmpty()) {
                idolDao.insert(inserts.map { it.toLocal() })
            }
        }

        return hasUpdates
    }

    override suspend fun updateAnniversaries(
        cdnUrl: String,
        reqImageSize: String,
        anniversaries: List<AnniversaryEntity>
    ) {
        idolDatabase.withTransaction {
            val idolIds = anniversaries.map { it.idolId }

            val dbIdols: List<IdolEntity> = idolDao.getIdolsByIds(idolIds).map { it.toData() }

            if (dbIdols.isEmpty()) return@withTransaction

            val idolMap = dbIdols.associateBy { it.id }

            val idolsToUpdate = mutableListOf<IdolEntity>()

            anniversaries.forEach { model ->
                val idol = idolMap[model.idolId]
                if (idol != null) {
                    // top3와 top3Type 처리
                    val top3Ids = if (model.top3 == null) {
                        arrayOfNulls<String>(3)
                    } else {
                        model.top3!!.split(",")
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                            .copyOf(3)
                    }

                    val top3Types = if (model.top3Type == null) {
                        arrayOfNulls<String>(3)
                    } else {
                        model.top3Type!!.split(",")
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                            .copyOf(3)
                    }

                    for (index in 0 until 3) {
                        if (!top3Ids[index].isNullOrEmpty()) {
                            var url = "$cdnUrl/a/${top3Ids[index]}.1_${reqImageSize}."
                            url = if (top3Types[index].isNullOrEmpty() ||
                                top3Types[index].equals("P", ignoreCase = true)
                            ) {
                                url + "webp"
                            } else {
                                url + "mp4"
                            }
                            when (index) {
                                0 -> idol.imageUrl = url
                                1 -> idol.imageUrl2 = url
                                2 -> idol.imageUrl3 = url
                            }
                        }
                    }
                    // heart, anniversary, burningDay 등 나머지 필드 업데이트
                    idol.heart = model.heart
                    idol.top3 = model.top3
                    idol.top3Type = model.top3Type
                    idol.anniversary = model.anniversary ?: "N"
                    idol.anniversaryDays = if (model.anniversary == "D") { // 기념일
                        model.anniversaryDays
                    } else {
                        null
                    }
                    idol.burningDay = model.burningDay

                    idolsToUpdate.add(idol)
                } else {
                    // DB에 없는 idol에 대한 처리는 필요에 따라 별도로 처리
                }
            }

            if (idolsToUpdate.isNotEmpty()) {
                idolDao.updateIdols(idolsToUpdate.map { it.toLocal() })
            }
        }
    }

    override suspend fun upsert(idol: IdolEntity) = idolDao.upsert(idol.toLocal())
}