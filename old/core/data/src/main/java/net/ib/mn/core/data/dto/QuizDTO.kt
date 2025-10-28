package net.ib.mn.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuizAnswerDTO(
    @SerialName("corrects") val corrects: String? = null,
    @SerialName("incorrects") val incorrects: String? = null,
    @SerialName("session") val session: Int,
    @SerialName("idol_id") val idolId: Int? = null,
)

@Serializable
data class ContinueQuizDTO(
    @SerialName("session") val sessionId: Int,
)

@Serializable
data class ApproveQuizDTO(
    @SerialName("session") val sessionId: Int,
    @SerialName("quiz_id") val quizId: Int,
    @SerialName("answer_number") val answerNumber: Int,
    @SerialName("answer") val answer: Int,
)

@Serializable
data class ReportQuizDTO(
    @SerialName("id") val quizId: Int,
    @SerialName("content") val content: String,
)