package net.ib.mn.local

interface LocalMapper<DataModel> {
    fun toData(): DataModel
}

fun <LocalModel : LocalMapper<DataModel>, DataModel> List<LocalModel>.toData(): List<DataModel> {
    return map { it.toData() }
}