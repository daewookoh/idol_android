/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.utils.ext

import android.content.Intent
import net.ib.mn.utils.version.DeviceVersion
import java.io.Serializable


/**
 * @see
 * */

inline fun <reified T : Serializable> Intent.getSerializableData(name: String): T? {
    return if (DeviceVersion.isAndroid13Later()) {
        getSerializableExtra(
            name,
            T::class.java,
        )
    } else {
        getSerializableExtra(name) as T?
    }
}