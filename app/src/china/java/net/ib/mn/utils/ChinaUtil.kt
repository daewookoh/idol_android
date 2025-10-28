package net.ib.mn.utils

import android.content.Context
import com.channel.helper.ChannelUtils
import net.ib.mn.billing.util.NativeXManager

class ChinaUtil {
    companion object {
        fun initNativeX(context: Context? = null) {
            val context = context ?: return
            ChannelUtils.initChannel(context, NativeXManager.APP_KEY)
        }
    }
}