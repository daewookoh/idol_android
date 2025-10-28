/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.base

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.ib.mn.utils.livedata.Event

/**
 * @see
 * */

open class BaseAndroidViewModel(application: Application) : AndroidViewModel(application) {

    protected val _errorToast = MutableLiveData<Event<String>>()
    val errorToast: LiveData<Event<String>> = _errorToast
}