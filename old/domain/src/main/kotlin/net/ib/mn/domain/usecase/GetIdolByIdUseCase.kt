package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class GetIdolByIdUseCase @Inject constructor(
    private val idolRepository: IdolRepository
){
    operator fun invoke(idolId: Int) = idolRepository.getIdolById(idolId)
}