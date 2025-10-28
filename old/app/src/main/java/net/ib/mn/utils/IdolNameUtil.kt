package net.ib.mn.utils

import android.content.Context
import android.text.TextUtils
import net.ib.mn.core.model.IdolLiteModel

fun getNameFromIdolLiteModel(context: Context, idol: IdolLiteModel): String {
    try {
        val locale = LocaleUtil.getAppLocale(context)
        val lang = locale.language.lowercase()
        val country = locale.country.lowercase()

        if (lang.startsWith("en") && !TextUtils.isEmpty(idol.nameEN)) {
            return idol.nameEN
        } else if (lang.startsWith("ko")) {
            return idol.name
        } else if (lang == "zh" && country == "tw" && !TextUtils.isEmpty(idol.nameZHTW)) {
            return idol.nameZHTW
        } else if (lang == "zh" && !TextUtils.isEmpty(idol.nameZH)) {
            return idol.nameZH
        } else if (lang.startsWith("ja") && !TextUtils.isEmpty(idol.nameJP)) {
            return idol.nameJP
        } else if (!TextUtils.isEmpty(idol.nameEN)) {
            return idol.nameEN
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return idol.nameEN
}