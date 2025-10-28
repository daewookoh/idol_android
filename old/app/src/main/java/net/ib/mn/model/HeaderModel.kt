package net.ib.mn.model

import com.google.gson.annotations.SerializedName

data class HeaderModel(
        @SerializedName("Connection") val connection: String,
        @SerializedName("Date") val date: String,
        @SerializedName("Server") val Server: String

)