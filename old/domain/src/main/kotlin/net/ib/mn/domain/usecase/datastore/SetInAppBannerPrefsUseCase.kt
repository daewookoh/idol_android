package net.ib.mn.domain.usecase.datastore

import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.repository.AppRepository
import javax.inject.Inject

class SetInAppBannerPrefsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    operator fun invoke(bannerMap: Map<String, List<InAppBanner>>) =
        appRepository.setInAppBannerData(bannerMap)
}