package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.FreeBoardPrefs

interface FreeBoardRepository {
    fun getFreeBoardPreference(): Flow<DataResource<FreeBoardPrefs>>
    fun setFreeBoardSelectLanguage(language: String, languageId: String): Flow<DataResource<Unit>>
}