package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class QuizModel(
    val answer: Int = 0,
    val choice1: String = "",
    val choice2: String = "",
    val choice3: String = "",
    val choice4: String = "",
    val content: String? = null,
    val description: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("umjjal_url")
    val umjjalUrl: String? = null,
    val user: UserModel? = null,
    val id: Int = 0,
    val rewarded: String? = null,

    @SerializedName("idol_name")
    val idolName: String? = null,
    val c_answer: Long = 0,

    @SerializedName("is_viewable")
    val isViewable: String? = null,

    @SerializedName("total_count")
    val totalCount: Int = 0
) : Serializable, Parcelable {
    companion object {
        private const val serialVersionUID = 1L
    }
}