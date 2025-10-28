/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.account

import net.ib.mn.core.data.model.AccountModel


/**
 * @see
 * */

interface AccountPreferencesRepository {
    fun getAccount() : AccountModel?

}
