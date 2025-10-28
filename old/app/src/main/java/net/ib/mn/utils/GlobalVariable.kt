package net.ib.mn.utils

import net.ib.mn.core.model.RemoteConfigData

/**
 * Create Date: 2022/09/29
 *
 * Description: global하게 사용되는  변수들 모음.
 *
 * @see
 * */
object GlobalVariable {

    @kotlin.jvm.JvmField
    var TOP_STACK_ROOM_ID = -1
    var ServerTs: Int = 0
    var RemoteConfig: RemoteConfigData? = null
}