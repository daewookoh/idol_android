package net.ib.mn.model

import java.util.Date

class HallTopModel {
    val createdAt: Date? = null
    val idol: IdolModel? = null
    val heart: Long = 0
    val rank: Int = 0 // 현재 순위
    val difference: Int = 0 // 차이
    val status: String? = null // increase, same, new, decrease
    val imageUrl: String? = null
    private val resource_uri: String? = null

    val id: String
        get() {
            var id = ""
            try {
                val splitUri =
                    resource_uri!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                id = splitUri[splitUri.size - 1]
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                return id
            }
        }
}
