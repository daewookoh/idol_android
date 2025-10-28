package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class QuizCategoryModel(
    @SerializedName("name") var name: String = "",
    @SerializedName("type") var type: String? = null,
) : Serializable
