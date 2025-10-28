/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.language

import android.content.Context
import android.content.SharedPreferences
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ib.mn.core.utils.Const
import javax.inject.Named


/**
 * @see
 * */

class LanguagePreferenceRepositoryImpl(
    @Named("idol_pref") private val pref: SharedPreferences,
    @ApplicationContext private val context: Context,
) : LanguagePreferenceRepository {
    override fun getSystemLanguage(): String {
        var defaultLocaleString = ""
        try {
            val locale = LocaleList.getDefault().get(0)
            val langCode = locale.language
            val countryCode = locale.country

            defaultLocaleString = if (langCode == "zh") {
                "$langCode-${countryCode.lowercase()}"
            } else {
                "$langCode"
            }
            val prefLocaleData = pref.getString(Const.PREF_LANGUAGE, "") ?: ""
            val prefLocale: String = if (prefLocaleData.isNotEmpty()) {
                if (prefLocaleData.startsWith("zh_")) {
                    prefLocaleData.replace("_", "-").lowercase()
                } else if (prefLocaleData.contains("_")) {
                    prefLocaleData.substringBefore("_")
                } else {
                    prefLocaleData
                }
            } else {
                ""
            }

            if (!prefLocale.isNullOrEmpty()) {
                defaultLocaleString = prefLocale
            }
        } catch (e: Exception) {
            e.printStackTrace()
            defaultLocaleString = Const.locales[1]
        }
        return defaultLocaleString
    }

}