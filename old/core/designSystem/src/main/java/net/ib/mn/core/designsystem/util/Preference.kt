package net.ib.mn.core.designsystem.util

import android.content.Context

object Preference {

    fun getPreferenceBool(
        context: Context,
        packagePrefName: String,
        key: String?,
        defValue: Boolean
    ): Boolean {
        val settings =
            context.getSharedPreferences(packagePrefName, Context.MODE_PRIVATE)
        var ret = defValue
        try {
            ret = settings.getBoolean(key, defValue)
        } catch (e: ClassCastException) {
            settings.edit().putBoolean(key, defValue)
        }
        return ret
    }
}