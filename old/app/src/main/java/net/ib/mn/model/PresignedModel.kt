package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Copyright 2022-11-29,화,11:42. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description:
 *
 **/

data class PresignedModel (
    @SerializedName("AWSAccessKeyId") val AWSAccessKeyId: String = "",
    @SerializedName("acl") val acl : String="",
    @SerializedName("key") val key : String="",
    @SerializedName("policy") val policy: String ="",
    @SerializedName("signature") val signature: String="",
    @SerializedName("saved_filename") var savedFilename : String = "",
    @SerializedName("url") var url : String = "",
    var success : Boolean = true
): Serializable

