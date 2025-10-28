package net.ib.mn.domain.usecase.datastore

import net.ib.mn.domain.repository.AppRepository
import javax.inject.Inject

class SetFriendInviteBannerTooltipUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    operator fun invoke() = appRepository.setInviteFriendBannerTooltip()
}