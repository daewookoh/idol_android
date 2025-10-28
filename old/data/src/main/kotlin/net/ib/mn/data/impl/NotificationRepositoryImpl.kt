package net.ib.mn.data.impl

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.bound.flowDataResource
import net.ib.mn.data.local.NotificationLocalDataSource
import net.ib.mn.data.model.toNotificationEntity
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Notification
import net.ib.mn.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationLocalDataSource: NotificationLocalDataSource
) : NotificationRepository {

    override fun saveNotifications(notificationList: List<Notification>): Flow<DataResource<Unit>> =
        flowDataResource {
            notificationLocalDataSource.saveNotifications(notificationList.map { it.toNotificationEntity() })
        }

    override fun getNotifications(): Flow<DataResource<List<Notification>>> = flowDataResource {
        notificationLocalDataSource.getNotifications()
    }

    override fun deleteNotification(notificationId: Long): Flow<DataResource<Unit>> =
        flowDataResource {
            notificationLocalDataSource.deleteNotification(notificationId)
        }

    override fun deleteAllNotification(): Flow<DataResource<Unit>> = flowDataResource {
        notificationLocalDataSource.deleteAllNotifications()
    }

    override fun deleteOldNotifications(time: Long): Flow<DataResource<Unit>> = flowDataResource {
        notificationLocalDataSource.deleteOldNotifications(time)
    }
}