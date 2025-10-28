package net.ib.mn.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import net.ib.mn.IdolApplication
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.PushStartActivity
import net.ib.mn.activity.StartupActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.PushMessageModel
import org.json.JSONException
import org.json.JSONObject
import java.util.Random

object MyNotificationManager {

    private const val ID_NOTICE = 999
    private const val ID_COMMENT = 10
    private const val ID_FRIEND = 20
    private const val ID_COUPON = 30
    private const val ID_SCHEDULE = 40
    private const val ID_HEART = 50
    private const val ID_SUPPORT = 60
    private const val ID_CHATTING_MSG = 70

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

    fun sendNotification(context: Context, pushMessageModel: PushMessageModel) {
        // 만약 비어 있을경우 앱 이름 넣어준다.
        if (TextUtils.isEmpty(pushMessageModel.title)) {
            pushMessageModel.title = UtilK.getAppName(context)
        }

        val appLinkPendingIntent = if (pushMessageModel.link.isNullOrEmpty()) {
            // link오면 상관없는데 혹시 안오면 원래 legacy 로직으로 동작하게함.
            getPushStartPendingIntent(context = context, pushMessageModel = pushMessageModel)
        } else {
            getAppLinkPendingIntent(context = context, pushMessageModel = pushMessageModel)
        }

        // TODO:: 링크 달리지 않은 메시지 처리.
        val pairOfChannelAndId = getChannelAndId(pushMessageModel)

        val manager = context.applicationContext
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder: NotificationCompat.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationCompat.Builder(context, pairOfChannelAndId.first)
            } else {
                NotificationCompat.Builder(context)
            }

        notificationBuilder
            .setContentIntent(appLinkPendingIntent)
            .setContentText(pushMessageModel.message)
            .setContentTitle(pushMessageModel.title)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(ContextCompat.getColor(context, R.color.main))
            .setAutoCancel(true)

        // 내용 길 경우 펼필 수 있게 세팅.
        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.setBigContentTitle(pushMessageModel.title)
        bigTextStyle.bigText(pushMessageModel.message)
        notificationBuilder.setStyle(bigTextStyle)

        val isChattingNotification =
            pushMessageModel.type == PushType.CHATTING.type

        if (!isChattingNotification) {
            if (!pushMessageModel.subtitle.isNullOrEmpty()) {
                notificationBuilder.setSubText(pushMessageModel.subtitle)
            }

            manager.notify(pairOfChannelAndId.second, notificationBuilder.build())
            return
        }

        // 채팅 푸시 같은 경우 그룹으로 나눠야됨.
        val idChattingMsgReview = Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW

        notificationBuilder
            .setSubText(pushMessageModel.subtitle)
            .setVibrate(null)
            .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.no_sound))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setGroup(idChattingMsgReview)

        val summaryNotificationBuilder =
            NotificationCompat.Builder(context, pairOfChannelAndId.first)
                .setContentTitle(pushMessageModel.title)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSubText(context.getString(R.string.chat_unread_message))
                .setColor(ContextCompat.getColor(context, R.color.main))
                .setGroup(idChattingMsgReview)
                .setVibrate(null)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setGroupSummary(true)

        manager.notify(pairOfChannelAndId.second, notificationBuilder.build())
        manager.notify(Const.NOTIFICATION_GROUP_ID_CHAT_MSG, summaryNotificationBuilder.build())
    }

    private fun getAppLinkPendingIntent(
        context: Context,
        pushMessageModel: PushMessageModel,
    ): PendingIntent? {
        val appLinkIntent = Intent(context.applicationContext, AppLinkActivity::class.java).apply {
            data = Uri.parse(pushMessageModel.link ?: "")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AppLinkActivity.APP_LINK_PUSH_STATUS, true)
            putExtra(
                AppLinkActivity.APP_LINK_PUSH_ANALYTICS_CLICK_LABEL,
                pushMessageModel.analyticsClickLabel,
            )
        }

        val random = Random()

        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // 12버전에서는 flag mutable 추가
            flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        }

        return PendingIntent.getActivity(
            context.applicationContext,
            random.nextInt(),
            appLinkIntent,
            flags,
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
                Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW to (
                    ID_CHATTING_MSG + (
                        pushMessageModel.roomId
                            ?: 0
                        )
                    )
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

    // Legacy 코드 link가 오지 않을때 대비.
    private fun getPushStartPendingIntent(
        context: Context,
        pushMessageModel: PushMessageModel,
    ): PendingIntent? {
        val account = IdolAccount
            .getAccount(context.applicationContext)

        val pushStartIntent = when (pushMessageModel.type) {
            PushType.FRIEND.type -> { // 친구.
                PushStartActivity.createFriendIntent(context.applicationContext)
            }

            PushType.COUPON.type -> { // 쿠폰.
                PushStartActivity.createCouponIntent(context.applicationContext)
            }

            PushType.SCHEDULE.type -> {
                PushStartActivity.createScheduleIntent(
                    context.applicationContext,
                    pushMessageModel.idolId ?: 0,
                )
            }

            PushType.SUPPORT.type -> {
                PushStartActivity.createSupportIntent(
                    context.applicationContext,
                    pushMessageModel.supportId ?: 0,
                    pushMessageModel.supportStatus ?: 0,
                    false,
                )
            }

            PushType.SUPPORT_COMMENT.type -> {
                PushStartActivity.createSupportIntent(
                    context.applicationContext,
                    pushMessageModel.supportId ?: 0,
                    pushMessageModel.supportStatus ?: 0,
                    true,
                )
            }

            PushType.ARTICLE_COMMENT.type -> {
                PushStartActivity.createCommentsIntent(context.applicationContext, pushMessageModel.article)
            }

            PushType.CHATTING.type -> {
                for (i in 0 until pushMessageModel.messages.size) {
                    if (pushMessageModel.messages[i].userId == account?.userId || Util.isAppOnForeground(
                            context.applicationContext,
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
                    context.applicationContext,
                    messageJsonObject,
                    pushMessageModel.roomId ?: return null,
                    pushMessageModel.idolId ?: return null,
                    pushMessageModel.locale ?: return null,
                )
            }

            PushType.SCHEDULE_COMMENT.type -> {
                PushStartActivity.createScheduleCommentIntent(
                    context.applicationContext,
                    pushMessageModel.scheduleId ?: return null,
                    pushMessageModel.idolId ?: return null,
                )
            }

            PushType.NOTICE.type -> {
                PushStartActivity.createMainIntent(context.applicationContext)
            }

            else -> { // 하트.
                if (IdolApplication.STRATUP_CALLED) {
                    Intent(
                        context.applicationContext,
                        MainActivity::class.java,
                    )
                } else {
                    Intent(
                        context.applicationContext,
                        StartupActivity::class.java,
                    )
                }
            }
        }

        val random = Random()

        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // 12버전에서는 flag mutable 추가
            flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        }

        return PendingIntent.getActivity(
            context.applicationContext,
            random.nextInt(),
            pushStartIntent,
            flags,
        )
    }
}