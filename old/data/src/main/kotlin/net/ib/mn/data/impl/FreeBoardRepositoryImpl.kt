package net.ib.mn.data.impl

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.bound.flowDataResource
import net.ib.mn.data.local.datastore.FreeBoardPrefsDataSource
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.FreeBoardPrefs
import net.ib.mn.domain.repository.FreeBoardRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FreeBoardRepositoryImpl @Inject constructor(
    private val freeBoardPrefsDataSource: FreeBoardPrefsDataSource
) : FreeBoardRepository {

    override fun getFreeBoardPreference(): Flow<DataResource<FreeBoardPrefs>> = flowDataResource {
        freeBoardPrefsDataSource.getFreeBoardPreference()
    }

    override fun setFreeBoardSelectLanguage(
        language: String,
        languageId: String
    ): Flow<DataResource<Unit>>  = flowDataResource {
        freeBoardPrefsDataSource.setFreeBoardSelectLanguagePrefs(language, languageId)
    }
}