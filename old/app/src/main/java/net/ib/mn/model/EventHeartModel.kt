/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class EventHeartModel(
    @SerializedName("daily_heart") var dailyHeart: Int = 0,
    @SerializedName("level_heart") var levelHeart: Int = 0,
    @SerializedName("burning_heart") var burningHeart: Int = 0,
    @SerializedName("vip_heart") var sorryHeart: Int = 0,
    @SerializedName("vip_message") var vipMessage: String? = "",
    @SerializedName("deok_jil_heart") var deokJilHeart: Int = 0,
    @SerializedName("burning") var burning : Boolean = false,
    @SerializedName("burningtime") var burningTime : Boolean = false,
    @SerializedName("progress") var progress: String? = "",
    @SerializedName("guide_url") var guideUrl: String? = null,
    @SerializedName("banners") var banners: ArrayList<FrontBannerModel>? = arrayListOf(),
) : Serializable