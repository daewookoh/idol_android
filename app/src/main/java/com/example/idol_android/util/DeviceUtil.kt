package com.example.idol_android.util

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 디바이스 관련 유틸리티.
 * FCM 토큰, 디바이스 UUID, Gmail 계정 등을 제공.
 */
@Singleton
class DeviceUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREF_NAME = "device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * FCM 토큰 반환.
     * TODO: Firebase Cloud Messaging 연동 시 실제 토큰으로 교체 필요.
     */
    fun getFcmToken(): String {
        return prefs.getString(KEY_FCM_TOKEN, "") ?: ""
    }

    /**
     * FCM 토큰 저장.
     */
    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    /**
     * Gmail 계정 반환 (SHA1 해시값).
     * 디바이스 UUID의 SHA1 해시를 반환.
     */
    fun getGmail(): String {
        val uuid = getDeviceUUID()
        return sha1(uuid) ?: ""
    }

    /**
     * 디바이스 고유 UUID 반환.
     * 한번 생성되면 앱 재설치 전까지 유지됨.
     */
    fun getDeviceUUID(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)

        if (id.isNullOrEmpty()) {
            // Android ID 사용
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            id = try {
                if (androidId != null && androidId != "9774d56d682e549c") {
                    // Android ID를 UUID로 변환
                    UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
                } else {
                    // Fallback: 랜덤 UUID 생성
                    UUID.randomUUID().toString()
                }
            } catch (e: Exception) {
                UUID.randomUUID().toString()
            }

            // 저장
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }

        return id
    }

    /**
     * SHA1 해시 생성.
     */
    private fun sha1(str: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            digest.update(str.toByteArray())
            val byteData = digest.digest()
            val sb = StringBuilder()
            for (byte in byteData) {
                sb.append(((byte.toInt() and 0xff) + 0x100).toString(16).substring(1))
            }
            sb.toString()
        } catch (e: Exception) {
            null
        }
    }
}
