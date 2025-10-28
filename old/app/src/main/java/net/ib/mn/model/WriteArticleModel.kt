/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import net.ib.mn.core.data.dto.CreateArticleDTO
import net.ib.mn.core.data.dto.FileData
import net.ib.mn.core.data.dto.InsertArticleDTO


@Parcelize
data class WriteArticleModel(
    @SerializedName("title") var title: String? = "",
    @SerializedName("content") var content: String? = "",
    @SerializedName("idol_id") var idolId: Int = 0,
    @SerializedName("link_title") var linkTitle: String? = "",
    @SerializedName("link_desc") var linkDesc: String? = "",
    @SerializedName("link_url") var linkUrl: String? = "",
    @SerializedName("show") var show: String? = "",
    @SerializedName("tag_id") var tagId: Int? = 0,
    val idolModel: IdolModel? = null,
    @SerializedName("files") var files: List<FileData> = emptyList(),
): Parcelable {
    fun toInsertArticleDTO(): InsertArticleDTO {
        return InsertArticleDTO(
            title = title ?: "",
            content = content ?: "",
            idolId = idolId.toString(),
            linkTitle = linkTitle ?: "",
            linkDesc = linkDesc ?: "",
            linkUrl = linkUrl ?: "",
            showScope = show ?: "",
            files = files
        )
    }

    fun toWriteArticleDTO(): CreateArticleDTO {
        return CreateArticleDTO(
            title = title ?: "",
            content = content ?: "",
            idolId = idolId.toString(),
            linkTitle = linkTitle ?: "",
            linkDesc = linkDesc ?: "",
            linkUrl = linkUrl ?: "",
            show = show ?: "",
            tagId = tagId.toString(),
            files = files
        )
    }
}