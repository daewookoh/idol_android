package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class QuizReviewModel(
        @SerializedName(QUESTION) var question: String,
        @SerializedName(QUESTION_NUMBER) var question_number: Int,
        @SerializedName(QUIZ) var quiz: QuizModel,
        var isReported : Boolean = false
): Serializable, Parcelable {
    companion object {
        const val QUESTION = "question"
        const val QUESTION_NUMBER = "question_number"
        const val QUIZ = "quiz"
    }
}