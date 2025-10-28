package net.ib.mn.local.impl.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ib.mn.data.local.datastore.IdolPrefsDataSource
import net.ib.mn.local.di.QualifierNames
import javax.inject.Inject
import javax.inject.Named

class IdolPrefsDataSourceImpl @Inject constructor(
    @Named(QualifierNames.IDOL_PREFS) private val dataStore: DataStore<Preferences>
) : IdolPrefsDataSource {

    private object PreferencesKey {
        val IDOL_CHARTS_CODES = stringPreferencesKey("IDOL_CHART_CODES")
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getIdolChartCodePrefs(): Map<String, ArrayList<String>> {
        val prefs = dataStore.data.first()
        val jsonString = prefs[PreferencesKey.IDOL_CHARTS_CODES]

        return if (!jsonString.isNullOrEmpty()) {
            try {
                json.decodeFromString<Map<String, ArrayList<String>>>(jsonString)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    override suspend fun saveIdolChartCodePrefs(codes: Map<String, List<String>>) {
        val jsonString = json.encodeToString(codes)
        dataStore.edit { preferences ->
            preferences[PreferencesKey.IDOL_CHARTS_CODES] = jsonString
        }
    }
}