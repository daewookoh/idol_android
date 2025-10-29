package net.ib.mn.util

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_DEVICE_ID = "device_id"
    }
    
    fun getFcmToken(): String {
        return prefs.getString(KEY_FCM_TOKEN, "") ?: ""
    }
    
    fun getGmail(): String {
        val uuid = getDeviceUUID()
        return sha1(uuid) ?: ""
    }
    
    fun getDeviceUUID(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrEmpty()) {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            id = try {
                if (androidId != null && androidId != "9774d56d682e549c") {
                    UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
                } else {
                    UUID.randomUUID().toString()
                }
            } catch (e: Exception) {
                UUID.randomUUID().toString()
            }
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }
    
    private fun sha1(str: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            val result = digest.digest(str.toByteArray())
            result.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
