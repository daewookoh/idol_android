package net.ib.mn.utils

import android.content.Context
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.activity.MyCouponActivity
import net.ib.mn.core.domain.usecase.GetCouponMessage
import net.ib.mn.core.model.CouponModel
import java.util.*


class MessageManager {
    private var coupons : ArrayList<CouponModel> = ArrayList()
    suspend fun getCoupons(context: Context?, getCouponMessage: GetCouponMessage, cb: (() -> Unit)?) {
        if( context == null ) {
            cb?.invoke()
            return
        }

        coupons.clear()

        try {
            getCouponMessage("C", null).collectLatest { result ->

                if (result.success) {
                    coupons.addAll(result.data ?: listOf())
                }
                cb?.invoke()

            }

        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hasNicknameCoupon() : Boolean {
        for( c in coupons ) {
            if( c.type == "C" && c.value == MyCouponActivity.COUPON_CHANGE_NICKNAME ) {
                return true
            }
        }
        return false
    }

    fun hasCoupon() : Boolean {
        return coupons.size!=0
    }

    companion object {
        val ourInstance = MessageManager()

        @JvmStatic fun shared(): MessageManager {
            return ourInstance
        }
    }

}