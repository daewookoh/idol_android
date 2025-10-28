package net.ib.mn.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class SubscriptionModel(
        var familyappId: Int,
        var name: String,
        var orderId: String,
        var packageName: String,
        var purchaseToken: String,
        var skuCode: String,
        var subscriptionCreatedAt: String,
        var subscriptionExpiredAt: String
) : Serializable, Parcelable