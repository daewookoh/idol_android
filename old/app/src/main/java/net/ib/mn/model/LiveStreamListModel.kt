package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

/**
 * ProjectName: idol_app_renew
 *
 * Description:
 * 메인 라이브 탭  라이브 리스트 용  데이터 모델
 * */
data class LiveStreamListModel(
    @SerializedName("created_at") var createdAt: Date?,
    @SerializedName("desc") var desc: String?,
    @SerializedName("heart") var heart: Long?,
    @SerializedName("id") var id: Int?,
    @SerializedName("image_url") var imageUrl: String?,
    @SerializedName("is_viewable") var isViewable: String?,
    @SerializedName("level_limit") var levelLimit: Int =0,
    @SerializedName("max_views") var maxViews: Long?,//최대 동접자수
    @SerializedName("play_url") var playUrl: String?,
    @SerializedName("start_at") var startAt: Date?,
    @SerializedName("status") var status: Int?,
    @SerializedName("thumbnail_url") var thumbnailUrl: String?,
    @SerializedName("title") var title: String?,
    @SerializedName("total_views") var totalViews: Long?,//조회수
    @SerializedName("updated_at") var updatedAt: Date?,
    @SerializedName("views") var views: Long?//동접자수
) : Serializable
