package net.ib.mn.gcm

import android.app.Activity
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import net.ib.mn.BuildConfig
import net.ib.mn.fragment.SignupFragment.OnRegistered

object GcmUtils {
    @JvmStatic
    fun registerDevice(activity: Activity?, cb: OnRegistered) {
        if (BuildConfig.CHINA) {
            return
        }
        if (BuildConfig.DEBUG && Build.FINGERPRINT.startsWith("generic")) {
            cb.callback("")
            return
        }

        object : RegisterTask(cb) {
            override fun doInBackground(vararg params: Void?): String {
                val msg = ""
                try {
                    FirebaseMessaging.getInstance().token.addOnSuccessListener(
                        activity!!
                    ) { newToken: String? -> callbackWrap.callback(newToken) }
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
                return msg
            }
        }.execute()
    }
}
