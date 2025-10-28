/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ib.mn.core.model.BaseModel
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


interface UsersRepository {
    /**
     * String domain, String email, String passwd,
     * 			String name, String recommender, String deviceKey,
     * 			String google_account, String recaptchaToken, Long time,
     */
    suspend fun signUp(
        domain: String?,
        email: String,
        gmail: String,
        passwd: String,
        name: String,
        referralCode: String,
        deviceKey: String,
        version: String,
        googleAccount: String,
        recaptchaToken: String? = null,
        time: Long,
        appId: String,
        deviceId: String,
        facebookId: Long? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun signIn(
        domain: String?,
        email: String,
        passwd: String,
        deviceKey: String,
        gmail: String,
        deviceId: String,
        appId: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun giveRewardHeart(
        type: String?,
        time: Long?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getEvent(
        version: String,
        gmail: String,
        isVM: Boolean = false,
        isRooted: Boolean = false,
        deviceId: String,
    ): Flow<JSONObject>

    suspend fun findPassword(
        email: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun changePassword(
        hashedPassword: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun setProfileImage(
        image: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun updateMost(
        userResourceUri: String,
        idolResourceUri: String? = null, // 최애 아이돌의 resource uri
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun validate(
        type: String,
        value: String?,
        appId: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun alterNickname(
        nickname: String,
        useCoupon: Boolean,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun provideHeart(
        type: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun isActiveTime(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun newFriendsRecommend(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getStatus(
        userId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun setStatus(
        statusMessage: String? = null,
        feedIsViewable: String? = null,
        friendAllow: String? = null,
        newFriends: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun banAutoClicker(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getHeartDiamondLog(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getIabKey(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun dropout(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getRankedUser(
        idolId: Int,
        league: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun iabVerify(
        receipt: String,
        signature: String,
        itemType: String,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun paymentsGoogleItem(
        receipt: String,
        signature: String,
        itemType: String?,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun paymentsGoogleSubscription(
        receipt: String,
        signature: String,
        itemType: String?,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun paymentsGoogleSubscriptionCheck(
        receipt: String,
        signature: String,
        itemType: String,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun paymentsGoogleRestore(
        receipt: String,
        signature: String,
        itemType: String?,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun updatePushKey(
        pushKey: String? = null,
        deviceId: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun updatePushFilter(
        filter: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun inhouseOfferwallCheck(
    ): Flow<BaseModel<JSONObject>>

    suspend fun inhouseOfferwallCreate(
        packageName: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getOfferwallCallback(
        userId: Int,
        adId: Int,
        clickUrl: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    // 탑300
    suspend fun getTopRanker(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getFriendHeartLog(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun findId(
        deviceId: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun searchNickname(
        q: String,
        offset: Int,
        limit: Int? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun createNativeXOrder(
        skuCode: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getPaymentWallSignature(
        params: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getNativeXSignature(
        body: String,
        tradeNo: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getUserInfo(
        email: String,
        ts: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getDailyRewards(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun postDailyReward(
        key: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getWechatToken(
        code: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun updateProfile(
        resourceUri: String,
        imageUrl: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun updateTutorial(
        tutorialIndex: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getWebToken(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
