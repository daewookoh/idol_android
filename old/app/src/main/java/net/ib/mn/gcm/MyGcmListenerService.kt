package net.ib.mn.gcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
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
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.getAppName
import org.json.JSONException
import org.json.JSONObject
import java.util.Random

/**
 * Created by vulpes on 15. 11. 5..
 */
class MyGcmListenerService : FirebaseMessagingService() {
    private val ID_NOTICE = 999
    private val ID_COMMENT = 10
    private val ID_FRIEND = 20
    private val ID_COUPON = 30
    private val ID_SCHEDULE = 40
    private val ID_HEART = 50
    private val ID_SUPPORT = 60
    private val ID_CHATTING_MSG = 70
    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Util.log("FCM_NEW_TOKEN::$s")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data: Map<*, *> = message.data
        if (data.size > 0 && data["message"] == null) return
        val message2 = data["message"].toString()
        Util.log("FCM TEST::$message2")
        sendNotification(message2)
    }

    // 혹시나해서 synchronized 해봄
    @Synchronized
    private fun sendNotification(message: String) {
        @Suppress("NAME_SHADOWING")
        var message: String = message
        var title: String? = null
        var subTitle: String? = null
        var isChatNotification = false
        var notificationIntent: Intent? = null
        val account = IdolAccount
            .getAccount(applicationContext) ?: return
        var channel = Const.PUSH_CHANNEL_ID_DEFAULT
        var contentIntent: PendingIntent? = null
        var notificationId = 1
        try {
            val messageObj = JSONObject(message)
            subTitle = messageObj.optString("subtitle")
            title = messageObj.optString("title")
            if (TextUtils.isEmpty(title)) {
                title = getAppName(this)
            }
            message = messageObj.getString("message")
            Util.log("check_log_push_message_type -> 푸쉬 메세지를 받는다$messageObj")
            if (messageObj.getString("type") == "f") { //친구 신청
                notificationId = ID_FRIEND
                channel = Const.PUSH_CHANNEL_ID_FRIEND
                notificationIntent = PushStartActivity.createFriendIntent(applicationContext)
            } else if (messageObj.getString("type") == "c") { // 쿠폰
                notificationId = ID_COUPON
                channel = Const.PUSH_CHANNEL_ID_COUPON
                notificationIntent = PushStartActivity.createCouponIntent(applicationContext)
            } else if (messageObj.getString("type") == "s") { //스케쥴 알림
                notificationId = ID_SCHEDULE
                channel = Const.PUSH_CHANNEL_ID_SCHEDULE
                val idol_id = messageObj.getInt("idol_id")
                notificationIntent =
                    PushStartActivity.createScheduleIntent(applicationContext, idol_id)
            } else if (messageObj.getString("type") == "t") { //서포트  성공, 개설 푸시
                notificationId = ID_SUPPORT
                channel = Const.PUSH_CHANNEL_ID_SUPPORT
                val supportId = messageObj.getInt("support_id")
                val support_status = messageObj.getInt("status")
                notificationIntent = PushStartActivity.createSupportIntent(
                    applicationContext,
                    supportId,
                    support_status,
                    false
                )
            } else if (messageObj.getString("type") == "tc") { //서포트  댓글 푸시
                notificationId = ID_COMMENT
                channel = Const.PUSH_CHANNEL_ID_COMMENT
                val supportId = messageObj.getInt("support_id")
                val support_status = 1 //성공서포트
                notificationIntent = PushStartActivity.createSupportIntent(
                    applicationContext,
                    supportId,
                    support_status,
                    true
                )
            } else if (messageObj.getString("type") == "ac") { //아티클 댓글 푸시
                notificationId = ID_COMMENT
                channel = Const.PUSH_CHANNEL_ID_COMMENT
                val articleObj = messageObj.getJSONObject("article")
                articleObj.remove("created_at")
                val article = IdolGson.getInstance(true)
                    .fromJson(articleObj.toString(), ArticleModel::class.java)
                notificationIntent =
                    PushStartActivity.createCommentsIntent(applicationContext, article)
            } else if (messageObj.getString("type") == "ch") { //채팅 메세지 푸시
                isChatNotification = true //채팅 notification 임을 알림.

                //room id 에 따라서  notification id 를 나눠서 ->  같은  room의  Noti는 덮어씌어지게 만든다.
                val roomId = messageObj.getInt("room_id")
                notificationId = ID_CHATTING_MSG + roomId
                channel = Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW
                title = messageObj.getString("title")
                message = messageObj.getString("message")
                val pushLocale = messageObj.optString("locale", "")
                val idolId = messageObj.getInt("idol_id")

                //만약 현재로그인되어있는 아이디와 푸시로온 아이디가 같으면 안보여줍니다.
                val gson = IdolGson.getInstance(true)
                val messages = messageObj.getJSONArray("messages")
                for (i in 0 until messages.length()) {
                    val (_, _, _, _, _, _, _, _, _, _, userId) = gson.fromJson(
                        messages[i].toString(), MessageModel::class.java
                    )
                    Util.log(
                        "FCM TEST USERID::" + userId + "My id" + account.userId + "isBackGround" + !Util.isAppOnForeground(
                            applicationContext
                        )
                    )
                    if (userId == account.userId || Util.isAppOnForeground(
                            applicationContext
                        )
                    ) {
                        return
                    }
                }
                notificationIntent = PushStartActivity.createChattingIntent(
                    applicationContext,
                    messageObj,
                    roomId,
                    idolId,
                    pushLocale
                )
            } else if (messageObj.getString("type") == "sc") { //스케쥴 댓글 푸시
                notificationId = ID_COMMENT
                channel = Const.PUSH_CHANNEL_ID_COMMENT
                val idol_id = messageObj.getInt("idol_id")
                val scheduleId = messageObj.getInt("schedule_id")
                notificationIntent = PushStartActivity.createScheduleCommentIntent(
                    applicationContext, scheduleId, idol_id
                )
            } else if (messageObj.getString("type") == "n") { //공지푸시.
                notificationId = ID_NOTICE
                channel = Const.PUSH_CHANNEL_ID_NOTICE
                notificationIntent = PushStartActivity.createMainIntent(applicationContext)
            } else { //나머지 하트푸시 messageObj.getString("type").equals("h").
                notificationId = ID_HEART
                channel = Const.PUSH_CHANNEL_ID_HEART
                notificationIntent = if (IdolApplication.STRATUP_CALLED) {
                    Intent(
                        applicationContext, MainActivity::class.java
                    )
                } else {
                    Intent(
                        applicationContext, StartupActivity::class.java
                    )
                }
            }
            notificationIntent!!.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            notificationIntent.putExtra("push", true)
            val random = Random()
            var flags = PendingIntent.FLAG_CANCEL_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //12버전에서는 flag mutable 추가
                flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
            }
            contentIntent = PendingIntent.getActivity(
                applicationContext,
                random.nextInt(),  //1,
                notificationIntent,
                flags
            ) // 20170131 0으로 하면 자꾸 엉뚱한 글로 이동하는 현상이 일부 있어 다시 수정.
            //0);  // FLAG_CANCEL_CURRENT : 이전 다른 노티가 있으면 그게 취소되서 눌러도 앱 이동이 안됨.
        } catch (ignored: JSONException) {
        } catch (ignored: SecurityException) {
        }
        val manager = applicationContext
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        @SuppressWarnings("deprecation")
        val builder: NotificationCompat.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, channel)
        } else {
            NotificationCompat.Builder(this)
        }

        //채팅 노티일때
        if (isChatNotification) {

            //일반  채팅 notification builder
            val notiGroup = Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW
            builder.setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setSubText(subTitle)
                .setContentText(message)
                .setVibrate(null)
                .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + this.packageName + "/" + R.raw.no_sound))
                .setColor(ContextCompat.getColor(this, R.color.main))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setGroup(notiGroup)

            //채팅 notification들을  그룹핑 하는  group 용 notification builder
            val summary = NotificationCompat.Builder(this, channel)
                .setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSubText(this.getString(R.string.chat_unread_message))
                .setColor(ContextCompat.getColor(this, R.color.main))
                .setGroup(notiGroup)
                .setVibrate(null)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setGroupSummary(true)

            // 내용 길 경우 펼칠 수 있는 노티
            val bigTextStyle = NotificationCompat.BigTextStyle()
            bigTextStyle.setBigContentTitle(title)
            bigTextStyle.bigText(message)
            builder.setStyle(bigTextStyle)
            manager.notify(notificationId, builder.build())
            manager.notify(Const.NOTIFICATION_GROUP_ID_CHAT_MSG, summary.build())
        } else { //채팅 노티가 아닐때
            builder.setContentIntent(contentIntent).setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(ContextCompat.getColor(this, R.color.main))
                .setAutoCancel(true)

            // 내용 길 경우 펼칠 수 있는 노티
            val bigTextStyle = NotificationCompat.BigTextStyle()
            bigTextStyle.setBigContentTitle(title)
            bigTextStyle.bigText(message)
            builder.setStyle(bigTextStyle)

            //subtitle의  경우는 오는 경우도 있고 안오는 경우도 있기떄문에 아래처럼  있을 경우만 따로 처리
            //왜냐면 그냥 setubtext해놓고 빈값을 넣게  되면  노티의 시간이랑 title사이에  간격이 길어짐.
            //채팅 노티는 무조건 들어감으로 그냥 체크 없이 넣어줬음.
            if (!subTitle!!.isEmpty()) {
                builder.setSubText(subTitle)
            }
            manager.notify(notificationId, builder.build())
        }


        // 하트 갯수 실시간 업데이트 할 필요가 없어서 아래는 제거한다. 서버 부담이 매우 많음.
//        LocalBroadcastManager broadManager = LocalBroadcastManager
//                .getInstance(getApplicationContext());
//        broadManager.sendBroadcast(new Intent("new_heart"));
    }
}
