package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class LGCodeModel(
        @SerializedName("idol_id") val idolId: Int,
        @SerializedName("lgcode") val lgCode: String
) : Serializable