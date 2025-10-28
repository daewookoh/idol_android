/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import android.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.UsersApi
import net.ib.mn.core.data.dto.AlterNicknameDTO
import net.ib.mn.core.data.dto.ChangePasswordDTO
import net.ib.mn.core.data.dto.DailyRewardDTO
import net.ib.mn.core.data.dto.FindPasswordDTO
import net.ib.mn.core.data.dto.IabVerifyDTO
import net.ib.mn.core.data.dto.InhouseOfferwallDTO
import net.ib.mn.core.data.dto.ProvideHeartDTO
import net.ib.mn.core.data.dto.PushFilterDTO
import net.ib.mn.core.data.dto.SetProfileImageDTO
import net.ib.mn.core.data.dto.SignInDTO
import net.ib.mn.core.data.dto.SignUpDTO
import net.ib.mn.core.data.dto.StatusDTO
import net.ib.mn.core.data.dto.UpdateMostDTO
import net.ib.mn.core.data.dto.UpdateProfileDTO
import net.ib.mn.core.data.dto.UpdatePushKeyDTO
import net.ib.mn.core.data.dto.UpdateTutorialDTO
import net.ib.mn.core.model.BaseModel
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject


class UsersRepositoryImpl @Inject constructor(
    private val usersApi: UsersApi
) : UsersRepository, BaseRepository() {
    private val EXOKEY = "dus-"
    private val DOMAIN_EMAIL = "email"
    private val ITEM_TYPE_SUBS = "subs"

    //AES(CBC) 암호화.
    private fun AES_Encode(str: String, iv: ByteArray?): ByteArray {
        try {
            //대칭키
            val key = "FF7BF8C3B7C844C956B0344B71D166A49E5A19D7DBA9408E2D1E77A8340010CC"

            val tsBytes = str.toByteArray(charset("UTF-8"))

            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(key.toByteArray())

            val newKey = SecretKeySpec(digest, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING")
            cipher.init(Cipher.ENCRYPT_MODE, newKey, IvParameterSpec(iv))

            return cipher.doFinal(tsBytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "".toByteArray()
    }

    fun md5salt(s: String): String? {
        try {
            val md = MessageDigest.getInstance("MD5")

            val key: String = EXOKEY + s
            md.update(key.toByteArray(StandardCharsets.UTF_8), 0, key.length)

            val messageDigest = md.digest()
            val number = BigInteger(1, messageDigest)
            var md5 = number.toString(16)

            while (md5.length < 32) md5 = "0$md5"

            return md5
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return null
        }
    }

    fun getSignature(time: Long): String {
        val sig: ByteArray
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val output = ByteArrayOutputStream()

        val trTime = time / 1000
        sig = AES_Encode(trTime.toString(), iv)

        try {
            output.write(iv)
            output.write(sig)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // Unexpected char 0x0a at 44 in SIGNATURE value: qqJp40ztrX1RzN3TqrQnHWlT5n4+Sgu649dtsOcR/K0=
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    override suspend fun signUp(
        domain: String?,
        email: String,
        gmail: String,
        passwd: String,
        name: String,
        referralCode: String,
        deviceKey: String,
        version: String,
        googleAccount: String,
        recaptchaToken: String?,
        time: Long,
        appId: String,
        deviceId: String,
        facebookId: Long?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        val signature = getSignature(time)

        var password = passwd
        var domain = domain
        if( domain == null || domain == DOMAIN_EMAIL) {
            password = md5salt(passwd) ?: ""
            domain = DOMAIN_EMAIL
        }

        val body = SignUpDTO(
            domain = domain,
            email = email,
            passwd = password,
            name = name,
            referralCode = referralCode.trim(),
            deviceKey = deviceKey,
            googleAccount = googleAccount,
            time = time,
            version = version,
            gmail = gmail,
            appId = appId,
            deviceId = deviceId,
            facebookId = facebookId,
        )
        try {
            val response = usersApi.signUp(signature, body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun signIn(
        domain: String?,
        email: String,
        passwd: String,
        deviceKey: String,
        gmail: String,
        deviceId: String,
        appId: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        var password = passwd

        if( domain == null || domain == DOMAIN_EMAIL) {
            password = md5salt(passwd) ?: ""
        }
        val body = SignInDTO(
            domain = domain,
            email = email,
            passwd = password,
            deviceKey = deviceKey,
            gmail = gmail,
            deviceId = deviceId,
            appId = appId
        )

        try {
            val response = usersApi.signIn(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun giveRewardHeart(
        type: String?,
        time: Long?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        val signature = getSignature(time ?: 0)
        try {
            val response = usersApi.giveRewardHeart(signature, type)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getEvent(
        version: String,
        gmail: String,
        isVM: Boolean,
        isRooted: Boolean,
        deviceId: String,
    ): Flow<JSONObject> = flow {
        try {
            val paramVM = (if (isVM && isRooted) "T" else (if (isRooted) "R" else "F"))
            val response = usersApi.getEvent(version, gmail, paramVM, deviceId)
            val jsonResponse = response.body()?.string()
            jsonResponse?.let {
                val jsonObject = JSONObject(it)
                emit(jsonObject)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        val json = JSONObject()
        json.put("error", e.message)
        emit(json)
    }

    override suspend fun findPassword(
        email: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        val body = FindPasswordDTO(
            email = email,
        )
        try {
            val response = usersApi.findPassword(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun changePassword(
        hashedPassword: String, // salt친 md5
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        val body = ChangePasswordDTO(
            newPassword = hashedPassword
        )
        try {
            val response = usersApi.changePassword(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun setProfileImage(
        image: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        val body = SetProfileImageDTO(
            image = image
        )
        try {
            val response = usersApi.setProfileImage(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun updateMost(
        userResourceUri: String,
        idolResourceUri: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response: Response<ResponseBody>
            if(idolResourceUri == null) {
                response = usersApi.deleteMost()
            } else {
                val body = UpdateMostDTO(
                    mostResourceUri = idolResourceUri
                )
                response = usersApi.updateMost(userResourceUri, body)
            }
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun validate(
        type: String,
        value: String?,
        appId: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val params = mutableMapOf(type to value)
            params["app_id"] = appId
            val response = usersApi.validate(params)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun alterNickname(
        nickname: String,
        useCoupon: Boolean,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val dto = AlterNicknameDTO(nickname, if(useCoupon) "Y" else null)
            val response = usersApi.alterNickname(dto)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun provideHeart(
        type: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = ProvideHeartDTO(type)
            val response = usersApi.provideHeart(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun isActiveTime(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.isActiveTime()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun newFriendsRecommend(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.newFriendsRecommend()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getStatus(
        userId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getStatus(userId)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun setStatus(
        statusMessage: String?,
        feedIsViewable: String?,
        friendAllow: String?,
        newFriends: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.setStatus(StatusDTO(
                statusMessage = statusMessage,
                feedIsViewable = feedIsViewable,
                friendAllow = friendAllow,
                newFriends = newFriends)
            )
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun banAutoClicker(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.banAutoClicker()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getHeartDiamondLog(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getHeartDiamondLog()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getIabKey(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getIabKey()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun dropout(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.dropout()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getRankedUser(
        idolId: Int,
        league: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getRankedUser(idolId, league)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun iabVerify(
        receipt: String,
        signature: String,
        itemType: String,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var subscription: String? = null
            if (itemType.equals(ITEM_TYPE_SUBS, ignoreCase = true)) {
                subscription = "Y"
            }
            val dto = IabVerifyDTO(receipt, signature, state, subscription)
            val response = usersApi.iabVerify(dto)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun paymentsGoogleItem(
        receipt: String,
        signature: String,
        itemType: String?,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var subscription: String? = null
            if (itemType.equals(ITEM_TYPE_SUBS, ignoreCase = true)) {
                subscription = "Y"
            }
            val dto = IabVerifyDTO(receipt, signature, state, subscription)
            val response = usersApi.paymentGoogleItem(dto)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun paymentsGoogleSubscription(
        receipt: String,
        signature: String,
        itemType: String?,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var subscription: String? = null
            if (itemType.equals(ITEM_TYPE_SUBS, ignoreCase = true)) {
                subscription = "Y"
            }
            val dto = IabVerifyDTO(receipt, signature, state, subscription)
            val response = usersApi.paymentGoogleSubscription(dto)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun paymentsGoogleSubscriptionCheck(
        receipt: String,
        signature: String,
        itemType: String,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var subscription: String? = null
            if (itemType.equals(ITEM_TYPE_SUBS, ignoreCase = true)) {
                subscription = "Y"
            }
            val dto = IabVerifyDTO(receipt, signature, state, subscription)
            val response = usersApi.paymentGoogleSubscriptionCheck(dto)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun paymentsGoogleRestore(
        receipt: String,
        signature: String,
        itemType: String?,
        state: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var subscription: String? = null
            if (itemType.equals(ITEM_TYPE_SUBS, ignoreCase = true)) {
                subscription = "Y"
            }
            val dto = IabVerifyDTO(receipt, signature, state, subscription)
            val response = usersApi.paymentGoogleRestore(dto)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun updatePushKey(
        pushKey: String?,
        deviceId: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val z = TimeZone.getDefault()
            var offset = z.rawOffset
            if (z.inDaylightTime(Date())) {
                offset += z.dstSavings
            }
            val offsetHours = offset / 1000 / 60 / 60
            val offsetMinutes = offset / 1000 / 60 % 60
            val tzStr = String.format(Locale.US, "%+03d:%02d", offsetHours, offsetMinutes)

            val body = UpdatePushKeyDTO(
                pushKey = pushKey,
                deviceId = deviceId,
                timezone = tzStr
            )
            val response = usersApi.updatePushKey(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun updatePushFilter(
        filter: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = PushFilterDTO(filter)
            val response = usersApi.updatePushFilter(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun inhouseOfferwallCheck(
    ): Flow<BaseModel<JSONObject>> = flow {
        try {
            val result = usersApi.inhouseOfferwallCheck()
            emit(BaseModel(data = JSONObject(result), success = true))
        } catch (e: Throwable) {
            throw(e)
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, success = false))
    }

    override suspend fun inhouseOfferwallCreate(
        packageName: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.inhouseOfferwallCreate(InhouseOfferwallDTO(packageName))
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getOfferwallCallback(
        userId: Int,
        adId: Int,
        clickUrl: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getOfferwallCallback(userId, adId, clickUrl)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getTopRanker(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getTopRanker()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getFriendHeartLog(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getFriendHeartLog()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun findId(
        deviceId: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.findId(deviceId)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun searchNickname(
        q: String,
        offset: Int,
        limit: Int?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.searchNickname(q, offset, limit)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun createNativeXOrder(
        skuCode: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = HashMap<String, String>()
            body["sku_code"] = skuCode
            val response = usersApi.createNativeXOrder(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getPaymentWallSignature(
        params: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getPaymentwallSignature(params)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getNativeXSignature(
        body: String,
        tradeNo: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getNativeXSignature(body, tradeNo)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getUserInfo(
        email: String,
        ts: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getUserInfo(email, ts)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getDailyRewards(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getDailyRewards()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun postDailyReward(
        key: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.postDailyReward(DailyRewardDTO(key))
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getWechatToken(
        code: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getWechatToken(code)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun updateProfile(
        resourceUri: String,
        imageUrl: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = UpdateProfileDTO(imageUrl)
            val response = usersApi.updateProfile(resourceUri, body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun updateTutorial(
        tutorialIndex: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = UpdateTutorialDTO(tutorialIndex)
            val response = usersApi.updateTutorial(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getWebToken(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = usersApi.getWebToken()
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }
}
