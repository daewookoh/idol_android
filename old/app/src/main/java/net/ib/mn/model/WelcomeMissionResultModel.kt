package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class WelcomeMissionResultModel(
    @SerializedName("all_clear_reward") val allClearReward: WelcomeMissionModel,
    @SerializedName("list") val list: List<WelcomeMissionModel>
): Parcelable