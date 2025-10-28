package net.ib.mn.data.model

import net.ib.mn.data.DataMapper
import net.ib.mn.domain.model.FreeBoardPrefs

data class FreeBoardPrefsEntity(
    val selectLanguage: String?,
    val selectLanguageId: String
): DataMapper<FreeBoardPrefs> {
    override fun toDomain(): FreeBoardPrefs =
        FreeBoardPrefs(
            selectLanguage,
            selectLanguageId
        )
}

fun FreeBoardPrefs.toEntity(): FreeBoardPrefsEntity =
    FreeBoardPrefsEntity(
        selectLanguage,
        selectLanguageId
    )