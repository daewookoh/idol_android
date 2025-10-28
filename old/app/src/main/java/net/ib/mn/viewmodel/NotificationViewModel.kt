package net.ib.mn.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.DeleteAllNotificationUseCase
import net.ib.mn.domain.usecase.DeleteNotificationUseCase
import net.ib.mn.domain.usecase.DeleteOldNotificationsUseCase
import net.ib.mn.domain.usecase.GetNotificationsUseCase
import net.ib.mn.domain.usecase.SaveNotificationsUseCase
import net.ib.mn.model.MessageModel
import net.ib.mn.model.toDomain
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val deleteOldNotificationsUseCase: DeleteOldNotificationsUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
    private val deleteAllNotificationUseCase: DeleteAllNotificationUseCase,
    private val getAllNotificationUseCase: GetNotificationsUseCase,
    private val saveNotificationsUseCase: SaveNotificationsUseCase
) : ViewModel() {

    private val _completeDeleteOldNotification = MutableLiveData<Event<Unit>>()
    val completeDeleteOldNotification: LiveData<Event<Unit>> get() = _completeDeleteOldNotification
    private val _completeDeleteNotification = MutableLiveData<Event<Unit>>()
    val completeDeleteNotification: LiveData<Event<Unit>> get() = _completeDeleteNotification
    private val _completeDeleteAllNotifications = MutableLiveData<Event<Unit>>()
    val completeDeleteAllNotifications: LiveData<Event<Unit>> get() = _completeDeleteAllNotifications
    private val _notificationList = MutableLiveData<Event<List<MessageModel>>>()
    val notificationList: LiveData<Event<List<MessageModel>>> get() = _notificationList
    private val _saveNotifications = MutableLiveData<Event<Unit>>()
    val saveNotifications: LiveData<Event<Unit>> get() = _saveNotifications

    fun deleteOldNotification() = viewModelScope.launch(Dispatchers.IO) {
        val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        deleteOldNotificationsUseCase(oneWeekAgo).awaitOrThrow()

        _completeDeleteOldNotification.postValue(Event(Unit))
    }

    fun deleteNotification(notificationId: Long) = viewModelScope.launch(Dispatchers.IO) {
        deleteNotificationUseCase(notificationId).awaitOrThrow()

        _completeDeleteNotification.postValue(Event(Unit))
    }

    fun deleteAllNotifications() = viewModelScope.launch(Dispatchers.IO) {
        deleteAllNotificationUseCase().awaitOrThrow()

        _completeDeleteAllNotifications.postValue(Event(Unit))
    }

    fun getAllNotificationList() = viewModelScope.launch(Dispatchers.IO) {
        val list = getAllNotificationUseCase()
            .mapDataResource {
                it?.map { notification -> notification.toPresentation() }
            }
            .awaitOrThrow()
        _notificationList.postValue(Event(list ?: emptyList()))
    }

    fun saveNotifications(newNotifications: List<MessageModel>) = viewModelScope.launch(Dispatchers.IO) {
        saveNotificationsUseCase(newNotifications.map { it.toDomain() })
            .awaitOrThrow()

        _saveNotifications.postValue(Event(Unit))
    }
}