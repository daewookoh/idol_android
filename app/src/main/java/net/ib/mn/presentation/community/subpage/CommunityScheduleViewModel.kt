package net.ib.mn.presentation.community.subpage

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.domain.repository.ScheduleRepository
import net.ib.mn.util.LocaleUtil
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CommunityScheduleViewModel @Inject constructor(
    application: Application,
    private val scheduleRepository: ScheduleRepository
) : BaseViewModel<CommunityScheduleContract.State, CommunityScheduleContract.Intent, CommunityScheduleContract.Effect>() {

    init {
        val systemLocale = LocaleUtil.getSystemLanguage(application).split("_")[0]
        val localeIndex = CommunityScheduleContract.State.SCHEDULE_LOCALES.indexOfFirst {
            it.startsWith(systemLocale)
        }.takeIf { it >= 0 } ?: 1

        setState {
            copy(
                locale = CommunityScheduleContract.State.SCHEDULE_LOCALES[localeIndex],
                localeText = CommunityScheduleContract.State.SCHEDULE_LANGUAGES[localeIndex]
            )
        }
    }

    override fun createInitialState() = CommunityScheduleContract.State()

    override fun handleIntent(intent: CommunityScheduleContract.Intent) {
        when (intent) {
            is CommunityScheduleContract.Intent.LoadInitialData -> loadInitialData(intent.idolId)
            is CommunityScheduleContract.Intent.Refresh -> refresh(intent.idolId)
            is CommunityScheduleContract.Intent.SelectDay -> selectDay(intent.day, intent.idolId)
            is CommunityScheduleContract.Intent.PreviousMonth -> previousMonth(intent.idolId)
            is CommunityScheduleContract.Intent.NextMonth -> nextMonth(intent.idolId)
            is CommunityScheduleContract.Intent.ChangeLocale -> changeLocale(intent.locale, intent.localeText, intent.idolId)
            is CommunityScheduleContract.Intent.VoteSchedule -> voteSchedule(intent.scheduleId, intent.vote, intent.idolId)
            is CommunityScheduleContract.Intent.DeleteSchedule -> deleteSchedule(intent.scheduleId, intent.idolId)
            is CommunityScheduleContract.Intent.EditSchedule -> editSchedule(intent.schedule)
            is CommunityScheduleContract.Intent.ViewScheduleDetail -> viewScheduleDetail(intent.schedule)
            is CommunityScheduleContract.Intent.ViewMonthSchedules -> viewMonthSchedules(intent.schedule)
        }
    }

    private fun viewScheduleDetail(schedule: ScheduleModel) {
        val state = uiState.value
        setEffect {
            CommunityScheduleContract.Effect.NavigateToScheduleDetail(
                schedule = schedule,
                yearMonthDay = state.yearMonthDay,
                locale = state.locale
            )
        }
    }

    private fun editSchedule(schedule: ScheduleModel) {
        setEffect { CommunityScheduleContract.Effect.NavigateToScheduleEdit(schedule) }
    }

    private fun viewMonthSchedules(schedule: ScheduleModel) {
        val state = uiState.value
        setEffect {
            CommunityScheduleContract.Effect.NavigateToMonthScheduleDetail(
                schedule = schedule,
                yearMonth = state.yearMonth,
                locale = state.locale
            )
        }
    }

    private fun loadInitialData(idolId: Int) {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            loadMonthIcons(idolId)
            loadDaySchedules(idolId)
        }
    }

    private fun refresh(idolId: Int) {
        viewModelScope.launch {
            setState { copy(isRefreshing = true, error = null) }
            loadMonthIcons(idolId)
            loadDaySchedules(idolId)
        }
    }

    private fun selectDay(day: Int, idolId: Int) {
        setState { copy(selectedDay = day) }
        viewModelScope.launch {
            loadDaySchedules(idolId)
        }
    }

    private fun previousMonth(idolId: Int) {
        val state = uiState.value
        val cal = Calendar.getInstance()
        cal.set(state.currentYear, state.currentMonth, 1)
        cal.add(Calendar.MONTH, -1)

        val nowCal = Calendar.getInstance()
        val selectedDay = if (cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
        ) {
            nowCal.get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }

        setState {
            copy(
                currentYear = cal.get(Calendar.YEAR),
                currentMonth = cal.get(Calendar.MONTH),
                selectedDay = selectedDay,
                dayIconMap = emptyMap(),
                daySchedules = emptyList()
            )
        }

        viewModelScope.launch {
            setState { copy(isLoading = true) }
            loadMonthIcons(idolId)
            loadDaySchedules(idolId)
        }
    }

    private fun nextMonth(idolId: Int) {
        val state = uiState.value
        val cal = Calendar.getInstance()
        cal.set(state.currentYear, state.currentMonth, 1)
        cal.add(Calendar.MONTH, 1)

        val nowCal = Calendar.getInstance()
        val selectedDay = if (cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
        ) {
            nowCal.get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }

        setState {
            copy(
                currentYear = cal.get(Calendar.YEAR),
                currentMonth = cal.get(Calendar.MONTH),
                selectedDay = selectedDay,
                dayIconMap = emptyMap(),
                daySchedules = emptyList()
            )
        }

        viewModelScope.launch {
            setState { copy(isLoading = true) }
            loadMonthIcons(idolId)
            loadDaySchedules(idolId)
        }
    }

    private fun changeLocale(locale: String, localeText: String, idolId: Int) {
        setState { copy(locale = locale, localeText = localeText) }
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            loadMonthIcons(idolId)
            loadDaySchedules(idolId)
        }
    }

    private fun loadMonthIcons(idolId: Int) {
        viewModelScope.launch {
            val state = uiState.value
            scheduleRepository.getMonthScheduleIcons(
                idolId = idolId,
                yearMonth = state.yearMonth,
                locale = state.locale
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        setState { copy(dayIconMap = result.data) }
                    }
                    is ApiResult.Error -> {
                        setEffect { CommunityScheduleContract.Effect.ShowError(result.message ?: "Failed to load icons") }
                    }
                }
            }
        }
    }

    private fun loadDaySchedules(idolId: Int) {
        viewModelScope.launch {
            val state = uiState.value
            scheduleRepository.getDaySchedules(
                idolId = idolId,
                yearMonthDay = state.yearMonthDay,
                locale = state.locale
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                daySchedules = result.data
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false, isRefreshing = false, error = result.message) }
                        setEffect { CommunityScheduleContract.Effect.ShowError(result.message ?: "Failed to load schedules") }
                    }
                }
            }
        }
    }

    private fun voteSchedule(scheduleId: Int, vote: String, idolId: Int) {
        viewModelScope.launch {
            scheduleRepository.voteSchedule(scheduleId, vote).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        if (result.data) {
                            loadDaySchedules(idolId)
                        }
                    }
                    is ApiResult.Error -> {
                        setEffect { CommunityScheduleContract.Effect.ShowError(result.message ?: "Vote failed") }
                    }
                }
            }
        }
    }

    private fun deleteSchedule(scheduleId: Int, idolId: Int) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(scheduleId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> setState { copy(isLoading = true) }
                    is ApiResult.Success -> {
                        setState { copy(isLoading = false) }
                        if (result.data) {
                            setEffect { CommunityScheduleContract.Effect.ShowToast("삭제되었습니다.") }
                            loadMonthIcons(idolId)
                            loadDaySchedules(idolId)
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false) }
                        setEffect { CommunityScheduleContract.Effect.ShowError(result.message ?: "Delete failed") }
                    }
                }
            }
        }
    }

    /**
     * 스케줄 업데이트 (상세화면에서 댓글 수 변경 시 동기화)
     */
    fun updateSchedule(updatedSchedule: ScheduleModel) {
        val currentSchedules = uiState.value.daySchedules
        val updatedSchedules = currentSchedules.map { schedule ->
            if (schedule.id == updatedSchedule.id) {
                schedule.copy(numComments = updatedSchedule.numComments)
            } else {
                schedule
            }
        }
        setState { copy(daySchedules = updatedSchedules) }
    }
}
