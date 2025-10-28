package net.ib.mn.data.local

import net.ib.mn.data.model.NotificationEntity

interface NotificationLocalDataSource {
    suspend fun saveNotifications(notificationList: List<NotificationEntity>)
    suspend fun getNotifications(): List<NotificationEntity>
    suspend fun deleteNotification(notificationId: Long)
    suspend fun deleteAllNotifications()
    suspend fun deleteOldNotifications(time: Long)
}