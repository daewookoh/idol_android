package net.ib.mn

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.kakao.sdk.common.KakaoSdk.init
import net.ib.mn.activity.MainActivity
import net.ib.mn.utils.Const
import java.util.Locale

//jira test 5
class IdolApplication() {
    var mainActivity: MainActivity? = null

    // 커뮤니티별로 하트박스 표시여부 저장
    @JvmField
    var mapHeartboxViewable = HashMap<Int, Boolean>()

    companion object {
        @Volatile
        private var singletonInstance: IdolApplication? = null

        // Locale.getDefault().getLanguage()를 하면 앱에서 로케일을 변경한 경우 변경된 로케일이 반환됨.
        // 디바이스의 로케일을 가져오려면 앱 시작시 미리 저장해 두어야 함.
        var sDefSystemLanguage: String? = null

        @JvmField
        var STRATUP_CALLED = false
        @JvmStatic
        fun getInstance(context: Context): IdolApplication {
            if (singletonInstance != null) {
                return singletonInstance!!
            }
            synchronized(IdolApplication::class.java) {
                singletonInstance = IdolApplication()
                sDefSystemLanguage = Locale.getDefault().toString()
                if (!BuildConfig.CHINA) {
                    init(context, BuildConfig.KAKAO_APP_KEY)
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    return@synchronized
                }
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channels = arrayOf(
                    Const.PUSH_CHANNEL_ID_DEFAULT,
                    Const.PUSH_CHANNEL_ID_COMMENT,
                    Const.PUSH_CHANNEL_ID_COUPON,
                    Const.PUSH_CHANNEL_ID_FRIEND,
                    Const.PUSH_CHANNEL_ID_HEART,
                    Const.PUSH_CHANNEL_ID_NOTICE,
                    Const.PUSH_CHANNEL_ID_SCHEDULE,
                    Const.PUSH_CHANNEL_ID_SUPPORT,
                    Const.PUSH_CHANNEL_ID_CHATTING_MSG,
                    Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW
                )
                val channelNames = intArrayOf(
                    R.string.push_notification,
                    R.string.lable_community_comment,
                    R.string.coupon,
                    R.string.friend,
                    R.string.hearts_from_friends,
                    R.string.title_notice,
                    R.string.schedule,
                    R.string.support,
                    R.string.chat,
                    R.string.chat
                )
                for (i in channels.indices) {

                    //채팅 메세지용  노티 채널의 경우는 -> importance 를  importace high 로 놓아서
                    //해드업 알림이  가능하도록 한다.
                    var mChannel: NotificationChannel
                    if (i == channels.size - 1) {
                        mChannel = NotificationChannel(
                            channels[i],
                            context.getString(channelNames[i]),
                            NotificationManager.IMPORTANCE_HIGH
                        )
                        mChannel.vibrationPattern = longArrayOf(0)
                        mChannel.enableVibration(false)
                        mChannel.setSound(null, null)
                    } else {
                        mChannel = NotificationChannel(
                            channels[i],
                            context.getString(channelNames[i]),
                            NotificationManager.IMPORTANCE_LOW
                        )
                    }
                    if (mChannel.id != Const.PUSH_CHANNEL_ID_CHATTING_MSG) {    //기존 채팅방 채널 제외하고 create
                        manager.createNotificationChannel(mChannel)
                    } else {    //기존에 쓰던 채팅 채널 있다면 채널 제거. (PUSH_CHANNEL_ID_CHATTING_MSG -> PUSH_CHANNEL_ID_CHATTING_MSG_RENEW 로 변경됨)
                        manager.deleteNotificationChannel(Const.PUSH_CHANNEL_ID_CHATTING_MSG)
                    }
                }
            }
            return singletonInstance!!
        }
    }
}
