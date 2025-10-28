package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

data class EmoticonsetModel(
    @SerializedName("id") val id : Int,
    @SerializedName("version") val version : Int,
    var isChanged: Boolean = true
)