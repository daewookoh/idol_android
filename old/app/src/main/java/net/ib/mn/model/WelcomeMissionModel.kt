package net.ib.mn.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.ib.mn.feature.mission.MissionItemInfo

@Parcelize
data class WelcomeMissionModel(
    val amount: Int,
    val item: String,
    val key: String,
    var status: String,
    var desc: MissionItemInfo
): Parcelable
