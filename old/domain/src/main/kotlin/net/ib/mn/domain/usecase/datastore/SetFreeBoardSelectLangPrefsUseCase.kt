package net.ib.mn.domain.usecase.datastore

import net.ib.mn.domain.repository.FreeBoardRepository
import javax.inject.Inject

class SetFreeBoardSelectLangPrefsUseCase @Inject constructor(
    private val freeBoardRepository: FreeBoardRepository
) {
    operator fun invoke(selectLanguage: String?, languageId: String = "") =
        freeBoardRepository.setFreeBoardSelectLanguage(selectLanguage ?: "", languageId)
}