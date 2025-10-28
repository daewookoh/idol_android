/**
 * Copyright (C) 2025. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Daewoo Koh daewoo@myloveidol.com
 * Description:
 *
 * */

package net.ib.mn.utils

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import net.ib.mn.account.IdolAccount
import net.ib.mn.core.model.RemoteConfigData
import org.json.JSONObject

/**
 * RemoteConfig 관련 유틸리티 클래스
 */
object RemoteConfigUtil {
    
    private val remoteConfig = Firebase.remoteConfig
    
    /**
     * RemoteConfig를 가져와서 GlobalVariable.RemoteConfig에 설정
     * @param context Context
     * @param account IdolAccount (null이면 기본값 사용)
     * @param onComplete 성공 여부를 받을 콜백 (Boolean: 성공 여부)
     */
    fun fetchRemoteConfig(context: Context, account: IdolAccount? = null, onComplete: ((Boolean) -> Unit)? = null) {
        val isAdminOrManager = account?.heart == Const.LEVEL_ADMIN || account?.heart == Const.LEVEL_MANAGER
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (isAdminOrManager) 6 else 600
            }
        )
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val configKey = if (isAdminOrManager) "dev" else "production"
                val configString = remoteConfig.getString(configKey)
                if (configString.isNotEmpty()) {
                    try {
                        val jsonObject = JSONObject(configString)
                        GlobalVariable.RemoteConfig = RemoteConfigData.fromJson(jsonObject)
                        onComplete?.invoke(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onComplete?.invoke(false)
                    }
                } else {
                    onComplete?.invoke(false)
                }
            } else {
                onComplete?.invoke(false)
            }
        }
    }
    
    /**
     * RemoteConfig가 null인 경우에만 가져오기
     * @param context Context
     * @param account IdolAccount (null이면 기본값 사용)
     * @param onComplete 성공 여부를 받을 콜백 (Boolean: 성공 여부)
     */
    fun fetchRemoteConfigIfNull(context: Context, account: IdolAccount? = null, onComplete: ((Boolean) -> Unit)? = null) {
        if (GlobalVariable.RemoteConfig == null) {
            fetchRemoteConfig(context, account, onComplete)
        } else {
            onComplete?.invoke(false)
        }
    }
}
