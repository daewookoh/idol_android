package net.ib.mn.domain.usecase.datastore

import net.ib.mn.domain.repository.FreeBoardRepository
import javax.inject.Inject

class GetFreeBoardPrefsUseCase @Inject constructor(
    private val freeBoardRepository: FreeBoardRepository
) {
    operator fun invoke() = freeBoardRepository.getFreeBoardPreference()
}