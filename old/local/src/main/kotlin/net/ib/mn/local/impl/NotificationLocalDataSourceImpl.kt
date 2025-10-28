package net.ib.mn.local.impl

import net.ib.mn.data.local.NotificationLocalDataSource
import net.ib.mn.data.model.NotificationEntity
import net.ib.mn.local.model.toLocal
import net.ib.mn.local.room.dao.NotificationDao
import net.ib.mn.local.toData
import javax.inject.Inject

class NotificationLocalDataSourceImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationLocalDataSource {

    override suspend fun saveNotifications(notificationList: List<NotificationEntity>) =
        notificationDao.insertNotifications(notificationList.map { it.toLocal() })

    override suspend fun getNotifications(): List<NotificationEntity> =
        notificationDao.getLocalNotifications().toData()

    override suspend fun deleteNotification(notificationId: Long) =
        notificationDao.deleteNotification(notificationId)

    override suspend fun deleteAllNotifications() =
        notificationDao.clearLocalNotifications()

    override suspend fun deleteOldNotifications(time: Long) = notificationDao.deleteOldNotifications(time)
}