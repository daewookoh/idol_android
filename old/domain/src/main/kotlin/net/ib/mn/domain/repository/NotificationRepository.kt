package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Notification

interface NotificationRepository {
    fun saveNotifications(notificationList: List<Notification>): Flow<DataResource<Unit>>
    fun getNotifications(): Flow<DataResource<List<Notification>>>
    fun deleteNotification(notificationId: Long): Flow<DataResource<Unit>>
    fun deleteAllNotification(): Flow<DataResource<Unit>>
    fun deleteOldNotifications(time: Long): Flow<DataResource<Unit>>
}