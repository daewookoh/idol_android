package net.ib.mn.data.local.datastore

import net.ib.mn.data.model.InAppBannerPrefsEntity

interface AppPrefsDataSource {
    suspend fun setBannerData(bannerMap: Map<String, List<InAppBannerPrefsEntity>>)
    suspend fun getMenuBannerData(): List<InAppBannerPrefsEntity>
    suspend fun getSearchBannerData(): List<InAppBannerPrefsEntity>
    suspend fun initAdData(date :Long)
    suspend fun updateAdCount(currentCount: Int, maxCount: Int)
    suspend fun getAdData(): Triple<Int, Int, Long>
    suspend fun isEnabledVideoAd(): Boolean
    suspend fun getAdCount(): Int
    suspend fun setAdNotification(isSet: Boolean)
    suspend fun isSetAdNotification(): Boolean
    suspend fun setInviteFriendBannerTooltip()
    suspend fun isShowInviteFriendBannerTooltip(): Boolean
}