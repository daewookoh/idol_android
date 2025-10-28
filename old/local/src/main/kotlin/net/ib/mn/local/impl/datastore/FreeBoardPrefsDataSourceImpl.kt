package net.ib.mn.local.impl.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.ib.mn.common.util.logD
import net.ib.mn.data.local.datastore.FreeBoardPrefsDataSource
import net.ib.mn.data.model.FreeBoardPrefsEntity
import net.ib.mn.local.di.QualifierNames
import javax.inject.Inject
import javax.inject.Named

class FreeBoardPrefsDataSourceImpl @Inject constructor(
    @Named(QualifierNames.FREE_BOARD_PREFS) private val dataStore: DataStore<Preferences>
) : FreeBoardPrefsDataSource {

    private object Keys {
        val SELECTED_LANGUAGE = stringPreferencesKey("selectedFreeBoardLanguage")
        val SELECTED_LANGUAGE_ID = stringPreferencesKey("selectedFreeBoardLanguageId")
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = dataStore.data.first()
            logD(
                "MIGRATION",
                "Migrated values: lang=${prefs[Keys.SELECTED_LANGUAGE]}, id=${prefs[Keys.SELECTED_LANGUAGE_ID]}"
            )
        }
    }

    override suspend fun getFreeBoardPreference(): FreeBoardPrefsEntity {
        val prefs = dataStore.data.first()
        return FreeBoardPrefsEntity(
            selectLanguage = prefs[Keys.SELECTED_LANGUAGE],
            selectLanguageId = prefs[Keys.SELECTED_LANGUAGE_ID] ?: ""
        )
    }

    override suspend fun setFreeBoardSelectLanguagePrefs(language: String, languageId: String) {
        Log.i("FreeBoardPrefs", "inDataSource $language $languageId")
        dataStore.edit { prefs ->
            prefs[Keys.SELECTED_LANGUAGE] = language
            prefs[Keys.SELECTED_LANGUAGE_ID] = languageId
        }
    }
}