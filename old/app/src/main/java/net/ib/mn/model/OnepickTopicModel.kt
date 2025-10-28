package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import net.ib.mn.addon.IdolGson
import java.io.Serializable
import java.util.*


data class OnepickTopicModel (
        @SerializedName("title") val title: String?,
        @SerializedName("subtitle") val subtitle: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("created_at") var createdAt: Date,
        @SerializedName("expired_at") var expiredAt: Date,
        @SerializedName("count") var count: Int,
        @SerializedName("status") val status: Int,
        @SerializedName("vote") var vote: String?,
        @SerializedName("hash_tag") val hashTag: String?,
        @SerializedName("resource_uri") val resourceUri: String?,
        @SerializedName("vote_type") var voteType : String?,
        @SerializedName("alarm") var alarm: Boolean = false,
) : Serializable {
    val id: Int
        get() = resourceUri?.split("/")!!.last { it.isNotEmpty() }.toInt()
    companion object {
        const val STATUS_PREPARING = 0
        const val STATUS_PROGRESS = 1
        const val STATUS_FINISHED = 2
    }
}