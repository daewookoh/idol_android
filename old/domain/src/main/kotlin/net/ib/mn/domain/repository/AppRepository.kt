package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.InAppBannerType

interface AppRepository {
    fun setInAppBannerData(bannerMap: Map<String, List<InAppBanner>>): Flow<DataResource<Unit>>
    fun getInAppBannerData(bannerType: InAppBannerType): Flow<DataResource<List<InAppBanner>>>
    fun initAdData(currentDate: Long): Flow<DataResource<Unit>>
    fun updateAdCount(currentCount: Int, maxCount: Int): Flow<DataResource<Unit>>
    fun getAdData(): Flow<DataResource<Triple<Int, Int, Long>>>
    fun isEnabledVideoAd(): Flow<DataResource<Boolean>>
    fun getAdCount(): Flow<DataResource<Int>>
    fun setAdNotification(isSet: Boolean): Flow<DataResource<Unit>>
    fun isSetAdNotification(): Flow<DataResource<Boolean>>
    fun setInviteFriendBannerTooltip(): Flow<DataResource<Unit>>
    fun isShowInviteFriendBannerTooltip(): Flow<DataResource<Boolean>>
}