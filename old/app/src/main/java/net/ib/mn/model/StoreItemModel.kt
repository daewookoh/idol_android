package net.ib.mn.model

import java.io.Serializable

class StoreItemModel : Serializable {
    val amount: Int = 0
    val bonusAmount: Int = 0
    val bonusExtraAmount: Int = 0
    val description: String? = null
    val id: Int = 0
    val imageUrl: String = ""
    val isViewable: String? = null
    val name: String = ""
    var priorPrice: String = ""
    var price: String = ""
    val priceEn: Double = 0.0
    var priceCNY: Double = 0.0
    val skuCode: String = ""
    val subscription: String? = null
    var skuDetailsJson: String? = null
    var priceAmountMicros: Long = 0
    val type: String? = null
    var goods_type: String? = null
    var isFirstPriceCheck: Boolean = false
    var welcomePriorPrice: Int = 0
    var currency: String = ""
}
