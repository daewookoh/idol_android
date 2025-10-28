package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.io.Serializable

data class AttendanceSaveStateModel(
    @SerializedName("stamp") val stamp: JSONObject,
    @SerializedName("rewards") val rewards: List<Map<String, StampRewardModel>>,
    @SerializedName("user") val user: UserModel,
    @SerializedName("continue_days") val continueDays: Int,
    @SerializedName("msg") val msg: String? = "",
    @SerializedName("gcode") val gcode: Int,
    @SerializedName("successOfSetStamp") var successOfSetStamp: Boolean = false,
) : Serializable