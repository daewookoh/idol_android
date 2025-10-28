package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

/**
 * Copyright 2022-12-7,수,16:3. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 나의 문의내역에서 사용하는 값들 모아놓은 Model
 *
 **/

data class InquiryModel (
    @SerializedName("content") var content: String? = "",
    @SerializedName("category") var category: String? = "",
    @SerializedName("created_at") val createdAt: Date? = null,
    @SerializedName("answer") val answer: String? = "",
    @SerializedName("files") val files: ArrayList<String> = ArrayList(),
    @SerializedName("file_count") val file_count : Int = 0
):Serializable