package net.ib.mn.domain.usecase

import net.ib.mn.domain.model.Notification
import net.ib.mn.domain.repository.NotificationRepository
import javax.inject.Inject

class SaveNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    operator fun invoke(notifications: List<Notification>) =
        notificationRepository.saveNotifications(notifications)
}