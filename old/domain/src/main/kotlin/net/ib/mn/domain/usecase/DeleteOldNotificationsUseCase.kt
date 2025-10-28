package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.NotificationRepository
import javax.inject.Inject

class DeleteOldNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(ts: Long) = notificationRepository.deleteOldNotifications(ts)
}