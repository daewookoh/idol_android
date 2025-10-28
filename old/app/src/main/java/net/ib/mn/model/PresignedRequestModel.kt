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

@Parcelize
data class PresignedRequestModel(
    @SerializedName("bucket") var bucket: String = "",
    @SerializedName("uri_path") var uriPath: String? = "",
    @SerializedName("src_width") var srcWidth: Int = 0,
    @SerializedName("src_height") var srcHeight: Int = 0,
    @SerializedName("hash") var hash: String? = "",
    @SerializedName("file_type") var fileType: String? = "",
    @SerializedName("byte_array") var byteArray: ByteArray? = null,
    @SerializedName("mime_type") var mimeType: String? = "",
    @SerializedName("video_file") var videoFile: CommonFileModel? = null,
):Parcelable