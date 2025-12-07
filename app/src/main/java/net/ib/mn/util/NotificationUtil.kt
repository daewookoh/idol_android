package net.ib.mn.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.ib.mn.MainActivity
import net.ib.mn.R

/**
 * 알림 유틸리티 (Old 프로젝트의 PresignedUrlService 알림 로직)
 */
object NotificationUtil {

    private const val CHANNEL_ID_ARTICLE_POSTING = "myloveidol_article_posting"
    private const val NOTIFICATION_ID_ARTICLE = 1

    // 알림 클릭 시 이동할 목적지를 나타내는 Extra 키
    const val EXTRA_NAVIGATE_TO = "navigate_to"
    const val EXTRA_IDOL_ID = "idol_id"
    const val EXTRA_TAG_ID = "tag_id"

    // 네비게이션 목적지 상수
    const val NAVIGATE_TO_FREE_BOARD = "free_board"
    const val NAVIGATE_TO_COMMUNITY_FEED = "community_feed"
    const val NAVIGATE_TO_COMMUNITY_FAN_TALK = "community_fan_talk"

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID_ARTICLE_POSTING,
                context.getString(R.string.article_post),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                vibrationPattern = longArrayOf(0)
                enableVibration(true)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showArticleUploadingNotification(context: Context) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ARTICLE_POSTING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.article_uploading))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ARTICLE, builder.build())
    }

    /**
     * 게시글 업로드 완료 알림 표시
     * @param navigateTo 이동할 목적지 (NAVIGATE_TO_FREE_BOARD, NAVIGATE_TO_COMMUNITY_FEED, NAVIGATE_TO_COMMUNITY_FAN_TALK)
     * @param idolId 아이돌 ID (FEED/팬톡의 경우 필수)
     * @param tagId 태그 ID (FREE_BOARD의 경우 해당 카테고리 탭으로 이동)
     */
    fun showArticleUploadCompleteNotification(
        context: Context,
        navigateTo: String = NAVIGATE_TO_FREE_BOARD,
        idolId: Int? = null,
        tagId: Int? = null
    ) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, navigateTo)
            idolId?.let { putExtra(EXTRA_IDOL_ID, it) }
            tagId?.let { putExtra(EXTRA_TAG_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ARTICLE_POSTING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.complete_article_upload))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ARTICLE, builder.build())
    }

    fun showArticleUploadFailedNotification(context: Context, errorMessage: String? = null) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val message = errorMessage ?: context.getString(R.string.fail_article_upload)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ARTICLE_POSTING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ARTICLE, builder.build())
    }

    fun cancelArticleNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ARTICLE)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
