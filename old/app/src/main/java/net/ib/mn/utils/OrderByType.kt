package net.ib.mn.utils

enum class OrderByType(val orderBy: String) {
    HEART("-heart"),
    TIME("-created_at"),
    COMMENTS("-num_comments"),
    LIKES("-like_count")
}