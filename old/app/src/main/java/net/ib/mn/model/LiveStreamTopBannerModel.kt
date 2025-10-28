package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*


/**
 * ProjectName: idol_app_renew
 *
 * Description:  라이브 스트리밍  리스트 탑배너  용 데이터 모델
 * */
data class LiveStreamTopBannerModel(
    @SerializedName("created_at") var createdAt: Date?,
    @SerializedName("id") var id: Int?,
    @SerializedName("image_url") var imageUrl: String?,
    @SerializedName("is_viewable") var viewable: String?,
    @SerializedName("live") var live: LiveTopBannerLiveModel?,
    @SerializedName("locale") var locale: String?,
    @SerializedName("updated_at") var updatedAt: Date?
 ):Serializable
