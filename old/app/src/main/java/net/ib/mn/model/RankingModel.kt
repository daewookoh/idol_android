package net.ib.mn.model

import java.io.Serializable

data class RankingModel(var ranking: Int, val idol: IdolModel?) : Serializable {
    // 섹션 헤더용
    @JvmField
    var isSection: Boolean = false
    @JvmField
    var sectionMaxVote: Long = 0 // 속한 섹션의 최대 투표수.

    // CELEB
    @JvmField
    var category: String? = null
    @JvmField
    var sectionName: String? = null //속한 섹션의 이름  나눔.

    companion object {
        private const val serialVersionUID = 1L
    }
}
