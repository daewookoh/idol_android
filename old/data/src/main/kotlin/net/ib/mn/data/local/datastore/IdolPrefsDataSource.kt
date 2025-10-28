package net.ib.mn.data.local.datastore

interface IdolPrefsDataSource {
    suspend fun getIdolChartCodePrefs(): Map<String, ArrayList<String>>
    suspend fun saveIdolChartCodePrefs(codes: Map<String, List<String>>)
}