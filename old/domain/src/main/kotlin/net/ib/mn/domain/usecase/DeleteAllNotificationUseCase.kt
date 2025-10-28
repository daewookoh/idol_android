package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.NotificationRepository
import javax.inject.Inject

class DeleteAllNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    operator fun invoke() = notificationRepository.deleteAllNotification()
}