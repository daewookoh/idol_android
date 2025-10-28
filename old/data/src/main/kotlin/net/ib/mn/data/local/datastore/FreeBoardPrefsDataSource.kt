package net.ib.mn.data.local.datastore

import net.ib.mn.data.model.FreeBoardPrefsEntity

interface FreeBoardPrefsDataSource {
    suspend fun getFreeBoardPreference(): FreeBoardPrefsEntity
    suspend fun setFreeBoardSelectLanguagePrefs(language: String, languageId: String)
}