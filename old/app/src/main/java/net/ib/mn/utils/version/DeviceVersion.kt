/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 버전 전용 비교 클래스.
 *
 * */
package net.ib.mn.utils.version

import android.os.Build

object DeviceVersion {
    fun isAndroid8Later() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isAndroid11Later() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    fun isAndroid10Later() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    fun isAndroid13Later() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}