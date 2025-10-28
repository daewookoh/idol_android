/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import net.ib.mn.core.model.AwardChartsModel
import java.io.Serializable

data class AwardStatsModel(
    @SerializedName("id") val id : Int,
    @SerializedName("name") val name : String,
    @SerializedName("title") val title : String,
    @SerializedName("charts") val charts : List<AwardChartsModel>,
    @SerializedName("result_title") var resultTitle : String? = null,
): Serializable