package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName
import net.ib.mn.data.model.TypeListModel

/**
 * /configs/typelist/ API Response
 *
 * old 프로젝트와 동일한 구조
 */
data class TypeListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("objects")
    val objects: List<TypeListModel>
)
