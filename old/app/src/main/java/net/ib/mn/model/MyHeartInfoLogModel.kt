/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 나의 정보 화면 하트, 다이아 로그 데이터 모델.
 *
 * */

package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DiamondLogModel(
    @SerializedName("diamond") val diamond: Int?,
    @SerializedName("key") val key: String?,
    @SerializedName("time") val time: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("desc") val desc: String?,
) : Parcelable

@Parcelize
data class HeartLogModel(
    @SerializedName("heart") val heart: Int?,
    @SerializedName("key") val key: String?,
    @SerializedName("time") val time: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("desc") val desc: String?,
) : Parcelable