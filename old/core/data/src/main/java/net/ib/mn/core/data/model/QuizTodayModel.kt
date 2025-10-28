/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.core.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class QuizTodayModel(
    @SerialName("maxcount") val maxCount: Int = 0,
    @SerialName("trycount") val tryCount: Int = 0,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("review_limit") val reviewLimit: Int = 0,
    @SerialName("score") val score: Int = 0,
    @SerialName("power") val power: Int = 0,
    @SerialName("status") val status: String? = null,
    @SerialName("quiz_maxitems") val quizMaxItems: Int = 100,
    @SerialName("quiz_minitems") val quizMinItems: Int = 10,
    @SerialName("quiz_point") val quizPoint: Int = 0,
    @SerialName("motd") val motd: String? = null,
    @SerialName("daily_posted_quizzes") val dailyPostedQuizzes: Int = 0,
    @SerialName("daily_quiz_post_limit") val dailyQuizPostLimit: Int = 0,
    @SerialName("incomplete_session") val incompleteSession: IncompleteSessionModel? = null,
    @SerialName("inactive_begin") val inactiveBegin: String? = null,
    @SerialName("inactive_end") val inactiveEnd: String? = null,
    @SerialName("tooltip") val tooltip: String? = null,
    @SerialName("plus_count") val plusCount: Int? = null,
    @SerialName("plus_limit") val plusLimit: Int? = null,
    @SerialName("gcode") val gcode: Int = 0,
    @SerialName("success") val success: Boolean = false,
    val msg: String? = null,
):Parcelable

@Serializable
@Parcelize
data class IncompleteSessionModel(
    @SerialName("num_of") val numOf: Int,
    @SerialName("auto_reward") val autoReward: Boolean,
    @SerialName("heart") val heart: Int,
):Parcelable