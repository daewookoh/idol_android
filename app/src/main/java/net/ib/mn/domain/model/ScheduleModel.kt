package net.ib.mn.domain.model

import java.util.Date

data class ScheduleModel(
    val id: Int,
    val articleId: Int,
    val title: String,
    val category: String,
    val dtstart: Date,
    val allday: Int,
    val location: String?,
    val url: String?,
    val extra: String?,
    val numComments: Int,
    val numYes: Int,
    val numNo: Int,
    val numDup: Int,
    val vote: String?,
    val isViewable: Boolean,
    val isReadonly: Boolean,
    val userId: Int,
    val userName: String?,
    val userLevel: Int,
    val createdAt: Date?,
    val updatedAt: Date?
) {
    val isVoted: Boolean
        get() = !vote.isNullOrEmpty()

    val isVotedYes: Boolean
        get() = vote == "Y"

    val isVotedNo: Boolean
        get() = vote == "N"

    val isVotedDup: Boolean
        get() = vote == "D"
}
