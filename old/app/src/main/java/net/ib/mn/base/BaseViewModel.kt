package net.ib.mn.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.ib.mn.utils.livedata.Event
import org.json.JSONObject

open class BaseViewModel : ViewModel() {

    protected val _errorToast = MutableLiveData<Event<String>>()
    val errorToast: LiveData<Event<String>> = _errorToast
    protected val _errorToastWithJson = MutableLiveData<Event<JSONObject>>()
    val errorToastWithJson: LiveData<Event<JSONObject>> = _errorToastWithJson
    protected val _errorToastWithCode = MutableLiveData<Event<Int>>()
    val errorToastWithCode: LiveData<Event<Int>> = _errorToastWithCode
}