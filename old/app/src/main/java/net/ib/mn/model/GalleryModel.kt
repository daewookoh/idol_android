package net.ib.mn.model

import java.util.Date

class GalleryModel {
    val refDate: String? = null
    val createdAt: Date? = null
    val heart: Long = 0
    val idol: IdolModel? = null
    val imageUrl: String? = null
    val imageUrl2: String? = null
    val imageUrl3: String? = null
    val imageVer: Int = 0
    var bannerUrl: String? = null
    val rank: Int = 0 // 현재 순위
    val difference: Int = 0 // 차이
    val status: String? = null // increase, same, new, decrease
    var expanded: Boolean = false
}
