package net.ib.mn.data.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.bound.flowDataResource
import net.ib.mn.data.local.datastore.AppPrefsDataSource
import net.ib.mn.data.model.InAppBannerPrefsEntity
import net.ib.mn.data.model.toEntity
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.InAppBannerType
import net.ib.mn.domain.repository.AppRepository
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val appPrefsDataSource: AppPrefsDataSource
) : AppRepository {

    override fun setInAppBannerData(bannerMap: Map<String, List<InAppBanner>>): Flow<DataResource<Unit>> =
        flowDataResource {
            val convertedMap = bannerMap.mapValues { (_, list) -> list.map { it.toEntity() } }
            appPrefsDataSource.setBannerData(convertedMap)
        }

    override fun getInAppBannerData(bannerType: InAppBannerType): Flow<DataResource<List<InAppBanner>>> =
        flowDataResource {
            val prefsList = when (bannerType) {
                InAppBannerType.MENU -> appPrefsDataSource.getMenuBannerData()
                InAppBannerType.SEARCH -> appPrefsDataSource.getSearchBannerData()
            }
            prefsList
        }

    override fun initAdData(date: Long): Flow<DataResource<Unit>> = flowDataResource {
        appPrefsDataSource.initAdData(date)
    }

    override fun updateAdCount(currentCount: Int, maxCount: Int): Flow<DataResource<Unit>> = flowDataResource {
        appPrefsDataSource.updateAdCount(currentCount, maxCount)
    }

    override fun getAdData(): Flow<DataResource<Triple<Int, Int, Long>>> = flowDataResource {
        appPrefsDataSource.getAdData()
    }

    override fun isEnabledVideoAd(): Flow<DataResource<Boolean>> = flowDataResource {
        appPrefsDataSource.isEnabledVideoAd()
    }

    override fun getAdCount(): Flow<DataResource<Int>> = flowDataResource {
        appPrefsDataSource.getAdCount()
    }

    override fun setAdNotification(isSet: Boolean): Flow<DataResource<Unit>> = flowDataResource {
        appPrefsDataSource.setAdNotification(isSet)
    }

    override fun isSetAdNotification(): Flow<DataResource<Boolean>> = flowDataResource {
        appPrefsDataSource.isSetAdNotification()
    }

    override fun setInviteFriendBannerTooltip(): Flow<DataResource<Unit>> = flowDataResource {
        appPrefsDataSource.setInviteFriendBannerTooltip()
    }

    override fun isShowInviteFriendBannerTooltip(): Flow<DataResource<Boolean>> = flowDataResource {
        appPrefsDataSource.isShowInviteFriendBannerTooltip()
    }
}