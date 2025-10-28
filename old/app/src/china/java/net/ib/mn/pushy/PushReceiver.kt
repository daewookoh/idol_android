package net.ib.mn.pushy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.ib.mn.model.PushMessageModel
import net.ib.mn.utils.MyNotificationManager
import net.ib.mn.utils.getModelFromPref

class PushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Attempt to extract the "message" property from the payload: {"message":"Hello World!"}
        if (intent.getStringExtra("message") == null) {
            return
        }

        try {
            val notificationText = intent.getStringExtra("message")

            val pushMessageModel =
                notificationText?.getModelFromPref<PushMessageModel>()
                    ?: PushMessageModel()

            MyNotificationManager.sendNotification(context, pushMessageModel)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
