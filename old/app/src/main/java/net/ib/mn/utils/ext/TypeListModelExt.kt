package net.ib.mn.utils.ext

import android.content.Context
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.utils.Util

fun TypeListModel.getUiColor(context: Context): String {
    return if (Util.isUsingNightModeResources(context)) {
        uiColorDarkmode
    } else {
        uiColor
    }
}

fun TypeListModel.getFontColor(context: Context): String {
    return if (Util.isUsingNightModeResources(context)) {
        fontColorDarkmode
    } else {
        fontColor
    }
}