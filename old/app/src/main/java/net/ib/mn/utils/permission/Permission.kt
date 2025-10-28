/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 안드로이드 버전별 권한 관련 클래스.
 *
 * */

package net.ib.mn.utils.permission

import android.Manifest
import android.app.Activity
import android.os.Build

object Permission {

    fun getStoragePermissions(activity: Activity): Array<String> {
        var permission = arrayOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = permission.plus(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity.applicationInfo.targetSdkVersion >= 30) {
            // 버전 29이상일때는 permission read external storage 만  넣어줘도됨
            permission += Manifest.permission.READ_EXTERNAL_STORAGE
        } else {
            // 29  미만일때는 WRITE_EXTERNAL_STORAGE 허용  요거는 read external 까지  같이 처리해줌.
            permission += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        return permission
    }
}