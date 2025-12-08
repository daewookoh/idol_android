package net.ib.mn.presentation.community.schedule

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ScheduleModel

class ScheduleDetailContract {

    data class State(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val schedules: List<ScheduleModel> = emptyList(),
        val groupedSchedules: Map<String, List<ScheduleModel>> = emptyMap(),
        val idolId: Int = 0,
        val yearMonth: String = "",
        val locale: String = "ko",
        val initialScheduleId: Int? = null,
        val error: String? = null
    ) : UiState

    sealed class Intent : UiIntent {
        data class Initialize(
            val idolId: Int,
            val yearMonth: String,
            val locale: String,
            val initialScheduleId: Int?
        ) : Intent()
        object Refresh : Intent()
        data class VoteSchedule(val scheduleId: Int, val vote: String) : Intent()
        data class DeleteSchedule(val scheduleId: Int) : Intent()
        data class EditSchedule(val schedule: ScheduleModel) : Intent()
        data class ViewComments(val schedule: ScheduleModel) : Intent()
        data class OpenMap(val schedule: ScheduleModel) : Intent()
        data class OpenUrl(val url: String) : Intent()
    }

    sealed class Effect : UiEffect {
        data class ShowError(val message: String) : Effect()
        data class ShowToast(val message: String) : Effect()
        data class NavigateToScheduleEdit(val schedule: ScheduleModel) : Effect()
        data class NavigateToComments(val schedule: ScheduleModel) : Effect()
        data class OpenMapIntent(val lat: String, val lng: String, val location: String) : Effect()
        data class OpenUrlIntent(val url: String) : Effect()
        data class ShowDeleteConfirm(val scheduleId: Int) : Effect()
        data class ScrollToSchedule(val scheduleId: Int) : Effect()
    }
}
