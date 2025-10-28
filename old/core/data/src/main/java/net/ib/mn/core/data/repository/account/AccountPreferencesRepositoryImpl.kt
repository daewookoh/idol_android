/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.account

import android.content.Context
import android.content.SharedPreferences
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ib.mn.core.data.model.AccountModel
import net.ib.mn.core.utils.Const
import net.ib.mn.core.utils.Const.PREF_KEY__DOMAIN
import net.ib.mn.core.utils.Const.PREF_KEY__EMAIL
import net.ib.mn.core.utils.Const.PREF_KEY__TOKEN
import javax.inject.Inject
import javax.inject.Named


/**
 * @see
 * */

class AccountPreferencesRepositoryImpl @Inject constructor(
    @Named("account_pref") private val pref: SharedPreferences,
    @ApplicationContext private val context: Context,
) : AccountPreferencesRepository {
    override fun getAccount(): AccountModel? {

        val email = pref.getString(PREF_KEY__EMAIL, null)
        val token = pref.getString(PREF_KEY__TOKEN, null)
        val domain = pref.getString(PREF_KEY__DOMAIN, null)

        return try {
            AccountModel(
                email = email,
                token = token,
                domain = domain
            )
        } catch (e: Exception) {
            null
        }
    }
}