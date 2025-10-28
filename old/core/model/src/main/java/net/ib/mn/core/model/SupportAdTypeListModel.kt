/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * @see
 * */

@Serializable
data class SupportAdTypeListModel(
    @SerialName("id") val id: Int,
    @SerialName("image_url") var imageUrl: String? = null,
    @SerialName("image_url2") var imageUrl2: String? = null,
    @SerialName("icon_url") var iconUrl: String? = null,
    @SerialName("name") val name: String,
    @SerialName("period") val period: String,
    @SerialName("description") val description: String,
    @SerialName("location") val location: String,
    @SerialName("goal") val goal: Int,
    @SerialName("require") val require: Int,
    @SerialName("guide") val guide: String,
    @SerialName("is_viewable") val isViewable: String,
    var selected: Boolean = false,
    @SerialName("category") val category: String,
    @SerialName("location_image_url") val locationImageUrl: String?,
    @SerialName("location_map_url") val locationMapUrl: String?
) : java.io.Serializable