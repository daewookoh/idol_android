package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    operator fun invoke() = notificationRepository.getNotifications()
}