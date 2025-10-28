package net.ib.mn.data

internal interface DataMapper<DomainModel> {
    fun toDomain(): DomainModel
}

internal fun <EntityModel, DomainModel> EntityModel.toDomainModel(): DomainModel? {
    if (this == null) return null
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is DataMapper<*> -> toDomain()
        is List<*> -> map {
            val domainModel: DomainModel = it.toDomainModel() ?: throw IllegalArgumentException("List 요소 중 null이 반환됨")
            domainModel
        }
        is Unit -> this
        is Boolean -> this
        is Int -> this
        is String -> this
        is Byte -> this
        is Short -> this
        is Long -> this
        is Char -> this
        is Triple<*, *, *> -> this
        // 일반적인 상황에서는 필요없으나 캐싱 아이템들 몇몇 케이스가 data class로 쓰기엔 너무 비효율적이라 추가
        is Map<*, *> -> this
        else -> {
            throw IllegalArgumentException("DataModel은 DataMapper<>, List<DataMapper<>>, Unit중 하나여야 함")
        }
    } as DomainModel
}

internal fun <EntityModel : DataMapper<DomainModel>, DomainModel> List<EntityModel>.toDomain(): List<DomainModel> {
    return map(DataMapper<DomainModel>::toDomain)
}