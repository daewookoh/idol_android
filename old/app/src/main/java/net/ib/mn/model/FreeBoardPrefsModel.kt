package net.ib.mn.model

import net.ib.mn.domain.model.FreeBoardPrefs

data class FreeBoardPrefsModel(
    var selectLanguage: String?,
    var selectLanguageId: String
)

fun FreeBoardPrefs.toPresentation(): FreeBoardPrefsModel =
    FreeBoardPrefsModel(
        selectLanguage = selectLanguage,
        selectLanguageId = selectLanguageId
    )