/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: FCM Message Service.
 *
 * */

package net.ib.mn.service

import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import net.ib.mn.IdolApplication
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.PushStartActivity
import net.ib.mn.activity.StartupActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.common.util.logI
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.PushMessageModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.getModelFromPref
import org.json.JSONException
import org.json.JSONObject
import java.util.Random

/**
 * @see getAppLinkPendingIntent 알림 클릭시 이동되는 intent를 생성해 줍니다.
 * */

class PushMessageService : FirebaseMessagingService() {

    enum class PushType(val type: String) {
        FRIEND("f"),
        COUPON("c"),
        SCHEDULE("s"),
        SCHEDULE_COMMENT("sc"),
        SUPPORT("t"),
        SUPPORT_COMMENT("tc"),
        ARTICLE_COMMENT("ac"),
        CHATTING("ch"),
        NOTICE("n"),
    }

    // 필요시 새로운 토큰이 생성 될떄 마다 서버에 보내주기.
    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Util.log("FCM_NEW_TOKEN::$s")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        logI("PushMessageService", "onMessageReceived: " + message.data.toString())
        val data = message.data
        val raw = data["message"] ?: return

        val payloadJson = unwrapIfEnvelope(raw)

        val push = runCatching {
            IdolGson.getInstance().fromJson(payloadJson, PushMessageModel::class.java)
        }.getOrElse { PushMessageModel() }

        sendNotification(push)
    }

    private fun unwrapIfEnvelope(raw: String): String {
        return runCatching {
            val el = com.google.gson.JsonParser.parseString(raw)
            if (el.isJsonObject) {
                val obj = el.asJsonObject

                if (obj.has("title") || obj.has("link") || obj.has("type")) return raw

                val dataObj = obj.getAsJsonObject("data")
                val inner = dataObj?.get("message")
                if (inner != null) {
                    return when {
                        inner.isJsonObject -> inner.asJsonObject.toString()
                        inner.isJsonPrimitive && inner.asJsonPrimitive.isString -> {
                            val s = inner.asString
                            // s 가 JSON 문자열이면 한 번 더 파싱해서 객체 문자열로 통일
                            runCatching {
                                val el2 = com.google.gson.JsonParser.parseString(s)
                                if (el2.isJsonObject) el2.asJsonObject.toString() else s
                            }.getOrElse { s }
                        }
                        else -> raw
                    }
                }
            } else if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                return el.asString
            }
            raw
        }.getOrElse { raw }
    }

    private fun sendNotification(pushMessageModel: PushMessageModel) {
        logI("PushMessageService", "sendNotification, type: " + pushMessageModel.type)
        // 만약 비어 있을경우 앱 이름 넣어준다.
        if (TextUtils.isEmpty(pushMessageModel.title)) {
            pushMessageModel.title = UtilK.getAppName(this)
        }

        val appLinkPendingIntent = if (pushMessageModel.link.isNullOrEmpty()) {
            //link오면 상관없는데 혹시 안오면 원래 legacy 로직으로 동작하게함.
            getPushStartPendingIntent(pushMessageModel = pushMessageModel)
        } else {
            getAppLinkPendingIntent(pushMessageModel = pushMessageModel)
        }

        //TODO:: 링크 달리지 않은 메시지 처리.
        val pairOfChannelAndId = getChannelAndId(pushMessageModel)

        val manager = applicationContext
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder: NotificationCompat.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationCompat.Builder(this, pairOfChannelAndId.first)
            } else {
                NotificationCompat.Builder(this)
            }

        notificationBuilder
            .setContentIntent(appLinkPendingIntent)
            .setContentText(pushMessageModel.message)
            .setContentTitle(pushMessageModel.title)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(ContextCompat.getColor(this, R.color.main))
            .setAutoCancel(true)

        //내용 길 경우 펼필 수 있게 세팅.
        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.setBigContentTitle(pushMessageModel.title)
        bigTextStyle.bigText(pushMessageModel.message)
        notificationBuilder.setStyle(bigTextStyle)

        val isChattingNotification = pushMessageModel.type == PushType.CHATTING.type

        if (!isChattingNotification) {

            if (!pushMessageModel.subtitle.isNullOrEmpty()) {
                notificationBuilder.setSubText(pushMessageModel.subtitle)
            }

            manager.notify(pairOfChannelAndId.second, notificationBuilder.build())
            return
        }

        //채팅 푸시 같은 경우 그룹으로 나눠야됨.
        val idChattingMsgReview = Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW

        notificationBuilder
            .setSubText(pushMessageModel.subtitle)
            .setVibrate(null)
            .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + this.packageName + "/" + R.raw.no_sound))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setGroup(idChattingMsgReview)

        val summaryNotificationBuilder = NotificationCompat.Builder(this, pairOfChannelAndId.first)
            .setContentTitle(pushMessageModel.title)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSubText(this.getString(R.string.chat_unread_message))
            .setColor(ContextCompat.getColor(this, R.color.main))
            .setGroup(idChattingMsgReview)
            .setVibrate(null)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setGroupSummary(true)

        manager.notify(pairOfChannelAndId.second, notificationBuilder.build())
        manager.notify(Const.NOTIFICATION_GROUP_ID_CHAT_MSG, summaryNotificationBuilder.build())
    }

    private fun getAppLinkPendingIntent(pushMessageModel: PushMessageModel): PendingIntent? {
        Log.d("@@@@", "link ${pushMessageModel.link}")
        val appLinkIntent = Intent(applicationContext, AppLinkActivity::class.java).apply {
            data = Uri.parse(pushMessageModel.link ?: "")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AppLinkActivity.APP_LINK_PUSH_STATUS, true)
            putExtra(AppLinkActivity.APP_LINK_PUSH_ANALYTICS_CLICK_LABEL, pushMessageModel.analyticsClickLabel)
        }

        val random = Random()

        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // 12버전에서는 flag mutable 추가
            flags = flags or PendingIntent.FLAG_MUTABLE
        }

        val bundle = if (Build.VERSION.SDK_INT >= 35) {
            val options = ActivityOptions.makeBasic()
            options.pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED
            options.toBundle()
        } else {
            null
        }

        return PendingIntent.getActivity(
            applicationContext,
            random.nextInt(),
            appLinkIntent,
            flags,
            bundle
        )
    }

    private fun getChannelAndId(pushMessageModel: PushMessageModel): Pair<String, Int> =
        when (pushMessageModel.type) {
            PushType.FRIEND.type -> {
                Const.PUSH_CHANNEL_ID_FRIEND to ID_FRIEND
            }

            PushType.COUPON.type -> {
                Const.PUSH_CHANNEL_ID_COUPON to ID_COUPON
            }

            PushType.SCHEDULE.type -> {
                Const.PUSH_CHANNEL_ID_SCHEDULE to ID_SCHEDULE
            }

            PushType.SUPPORT.type -> {
                Const.PUSH_CHANNEL_ID_SUPPORT to ID_SUPPORT
            }

            PushType.SUPPORT_COMMENT.type -> {
                Const.PUSH_CHANNEL_ID_COMMENT to ID_COMMENT
            }

            PushType.ARTICLE_COMMENT.type -> {
                Const.PUSH_CHANNEL_ID_COMMENT to ID_COMMENT
            }

            PushType.CHATTING.type -> {
                Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW to (ID_CHATTING_MSG + (pushMessageModel.roomId
                    ?: 0))
            }

            PushType.SCHEDULE_COMMENT.type -> {
                Const.PUSH_CHANNEL_ID_COMMENT to ID_COMMENT
            }

            PushType.NOTICE.type -> {
                Const.PUSH_CHANNEL_ID_NOTICE to ID_NOTICE
            }

            else -> {
                Const.PUSH_CHANNEL_ID_HEART to ID_HEART
            }
        }

    //Legacy 코드 link가 오지 않을때 대비.
    private fun getPushStartPendingIntent(pushMessageModel: PushMessageModel): PendingIntent? {

        val account = IdolAccount
            .getAccount(applicationContext)

        val pushStartIntent = when (pushMessageModel.type) {
            PushType.FRIEND.type -> { // 친구.
                PushStartActivity.createFriendIntent(applicationContext)
            }

            PushType.COUPON.type -> { // 쿠폰.
                PushStartActivity.createCouponIntent(applicationContext)
            }

            PushType.SCHEDULE.type -> {
                PushStartActivity.createScheduleIntent(
                    applicationContext,
                    pushMessageModel.idolId ?: 0
                )
            }

            PushType.SUPPORT.type -> {
                PushStartActivity.createSupportIntent(
                    applicationContext,
                    pushMessageModel.supportId ?: 0,
                    pushMessageModel.supportStatus ?: 0,
                    false
                )
            }

            PushType.SUPPORT_COMMENT.type -> {
                PushStartActivity.createSupportIntent(
                    applicationContext,
                    pushMessageModel.supportId ?: 0,
                    pushMessageModel.supportStatus ?: 0,
                    true
                )
            }

            PushType.ARTICLE_COMMENT.type -> {
                PushStartActivity.createCommentsIntent(applicationContext, pushMessageModel.article)
            }

            PushType.CHATTING.type -> {
                logI("PushMessageService", "getPushStartPendingIntent for CHAT")
                for (i in 0 until pushMessageModel.messages.size) {
                    if (pushMessageModel.messages[i].userId == account?.userId || Util.isAppOnForeground(
                            applicationContext
                        )
                    ) {
                        continue
                    }
                }

                val gson = IdolGson.getInstance()
                val messageJsonObject: JSONObject?
                try {
                    messageJsonObject = JSONObject(gson.toJson(pushMessageModel))
                } catch (e: JSONException) {
                    e.printStackTrace()
                    return null
                }
                PushStartActivity.createChattingIntent(
                    applicationContext,
                    messageJsonObject,
                    pushMessageModel.roomId ?: return null,
                    pushMessageModel.idolId ?: return null,
                    pushMessageModel.locale ?: return null
                )
            }

            PushType.SCHEDULE_COMMENT.type -> {
                PushStartActivity.createScheduleCommentIntent(
                    applicationContext,
                    pushMessageModel.scheduleId ?: return null,
                    pushMessageModel.idolId ?: return null
                )
            }

            PushType.NOTICE.type -> {
                PushStartActivity.createMainIntent(applicationContext)
            }

            else -> { // 하트.
                if (IdolApplication.STRATUP_CALLED) {
                    Intent(
                        applicationContext, MainActivity::class.java
                    )
                } else {
                    Intent(
                        applicationContext, StartupActivity::class.java
                    )
                }
            }
        }

        val random = Random()

        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // 12버전에서는 flag mutable 추가
            flags = flags or PendingIntent.FLAG_MUTABLE
        }

        val bundle = if (Build.VERSION.SDK_INT >= 35) {
            val options = ActivityOptions.makeBasic()
            options.pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED
            options.toBundle()
        } else {
            null
        }

        return PendingIntent.getActivity(
            applicationContext,
            random.nextInt(),
            pushStartIntent,
            flags,
            bundle
        )
    }

    companion object {
        private const val ID_NOTICE = 999
        private const val ID_COMMENT = 10
        private const val ID_FRIEND = 20
        private const val ID_COUPON = 30
        private const val ID_SCHEDULE = 40
        private const val ID_HEART = 50
        private const val ID_SUPPORT = 60
        private const val ID_CHATTING_MSG = 70
    }
}
