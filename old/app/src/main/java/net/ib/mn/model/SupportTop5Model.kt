package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SupportTop5Model (
        @SerializedName("diamond") var diamond : Int,
        @SerializedName("user") val user : UserModel,
        @SerializedName("rank") var rank: Int
) : Serializable