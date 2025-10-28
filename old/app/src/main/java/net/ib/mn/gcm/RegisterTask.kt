package net.ib.mn.gcm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.fragment.SignupFragment.OnRegistered

interface IRegisterTask {
    fun doInBackground(vararg params: Void?): String?
}

open class RegisterTask(var callbackWrap: OnRegistered): IRegisterTask {
    fun execute() = CoroutineScope(Dispatchers.IO).launch {
        doInBackground()
    }

    override fun doInBackground(vararg params: Void?): String? {
        return null
    }
}
