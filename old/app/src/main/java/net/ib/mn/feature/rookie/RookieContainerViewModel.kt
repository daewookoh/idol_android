/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.feature.rookie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


/**
 * @see
 * */

class RookieContainerViewModel : ViewModel() {

    private val _firstPlaceRankerName = MutableLiveData<String>()
    val firstPlaceRankerName: LiveData<String> get() = _firstPlaceRankerName

    fun setFirstPlaceRankerName(name: String) {
        _firstPlaceRankerName.postValue(name)
    }
}