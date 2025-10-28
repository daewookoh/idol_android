package net.ib.mn.model

import java.io.Serializable

class QuizRankModel : Serializable {
    val power: Int = 0
    val user: UserModel? = null
    var rank: Int = 0

    companion object {
        private const val serialVersionUID = 1L
    }
}
