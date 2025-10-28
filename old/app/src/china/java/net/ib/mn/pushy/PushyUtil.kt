package net.ib.mn.pushy

import android.app.Activity
import android.os.AsyncTask
import me.pushy.sdk.Pushy
import net.ib.mn.fragment.SignupFragment.OnRegistered
import net.ib.mn.utils.Util

class PushyUtil {
    companion object {
        fun registerDevice(activity: Activity?, cb: OnRegistered?) {
            doAsync {
                try{
                    // Assign a unique token to this device
                    val deviceToken = Pushy.register(activity?.applicationContext)

                    // Log it for debugging purposes
                    Util.log("Pushy device token: $deviceToken")
                    cb?.callback(deviceToken)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun listen(activity: Activity?) {
            val activity = activity ?: return
            Pushy.listen(activity)
        }

    }

    class doAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
        init {
            execute()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            handler()
            return null
        }
    }

}