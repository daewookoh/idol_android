package net.ib.mn.model

import com.google.gson.annotations.SerializedName

data class SearchHistoryModel(
        @SerializedName("search") val search: String?
)