package net.ib.mn.presentation.community.schedule

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.domain.repository.ScheduleRepository
import java.text.DateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ScheduleDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository
) : BaseViewModel<ScheduleDetailContract.State, ScheduleDetailContract.Intent, ScheduleDetailContract.Effect>() {

    override fun createInitialState() = ScheduleDetailContract.State()

    override fun handleIntent(intent: ScheduleDetailContract.Intent) {
        when (intent) {
            is ScheduleDetailContract.Intent.Initialize -> initialize(
                intent.idolId,
                intent.yearMonth,
                intent.locale,
                intent.initialScheduleId
            )
            is ScheduleDetailContract.Intent.Refresh -> refresh()
            is ScheduleDetailContract.Intent.VoteSchedule -> voteSchedule(intent.scheduleId, intent.vote)
            is ScheduleDetailContract.Intent.DeleteSchedule -> deleteSchedule(intent.scheduleId)
            is ScheduleDetailContract.Intent.EditSchedule -> editSchedule(intent.schedule)
            is ScheduleDetailContract.Intent.ViewComments -> viewComments(intent.schedule)
            is ScheduleDetailContract.Intent.OpenMap -> openMap(intent.schedule)
            is ScheduleDetailContract.Intent.OpenUrl -> openUrl(intent.url)
        }
    }

    private fun initialize(idolId: Int, yearMonth: String, locale: String, initialScheduleId: Int?) {
        setState {
            copy(
                idolId = idolId,
                yearMonth = yearMonth,
                locale = locale,
                initialScheduleId = initialScheduleId
            )
        }
        loadSchedules(idolId, yearMonth, locale, initialScheduleId)
    }

    private fun refresh() {
        setState { copy(isRefreshing = true) }
        val state = uiState.value
        loadSchedules(state.idolId, state.yearMonth, state.locale, null)
    }

    private fun loadSchedules(idolId: Int, yearMonth: String, locale: String, initialScheduleId: Int?) {
        viewModelScope.launch {
            scheduleRepository.getMonthSchedules(
                idolId = idolId,
                yearMonth = yearMonth,
                locale = locale
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        if (!uiState.value.isRefreshing) {
                            setState { copy(isLoading = true) }
                        }
                    }
                    is ApiResult.Success -> {
                        val schedules = result.data.sortedBy { it.dtstart }
                        val grouped = groupSchedulesByDate(schedules, locale)
                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                schedules = schedules,
                                groupedSchedules = grouped
                            )
                        }
                        // 초기 스케줄 ID가 있으면 해당 위치로 스크롤
                        initialScheduleId?.let { scheduleId ->
                            setEffect { ScheduleDetailContract.Effect.ScrollToSchedule(scheduleId) }
                        }
                    }
                    is ApiResult.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = result.message
                            )
                        }
                        setEffect {
                            ScheduleDetailContract.Effect.ShowError(result.message ?: context.getString(R.string.failed_to_load))
                        }
                    }
                }
            }
        }
    }

    private fun groupSchedulesByDate(schedules: List<ScheduleModel>, locale: String): Map<String, List<ScheduleModel>> {
        val appLocale = when (locale) {
            "ko" -> Locale.KOREAN
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            "in" -> Locale("in", "ID") // 인도네시아어
            "pt" -> Locale("pt", "BR") // 포르투갈어
            "es" -> Locale("es", "ES") // 스페인어
            "vi" -> Locale("vi", "VN") // 베트남어
            "th" -> Locale("th", "TH") // 태국어
            else -> Locale.ENGLISH
        }
        val dateFormat = DateFormat.getDateInstance(DateFormat.FULL, appLocale)

        return schedules.groupBy { schedule ->
            dateFormat.format(schedule.dtstart)
        }
    }

    private fun voteSchedule(scheduleId: Int, vote: String) {
        viewModelScope.launch {
            scheduleRepository.voteSchedule(scheduleId, vote).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        if (result.data) {
                            val state = uiState.value
                            loadSchedules(state.idolId, state.yearMonth, state.locale, null)
                        }
                    }
                    is ApiResult.Error -> {
                        setEffect {
                            ScheduleDetailContract.Effect.ShowError(result.message ?: context.getString(R.string.msg_error_ok))
                        }
                    }
                }
            }
        }
    }

    private fun deleteSchedule(scheduleId: Int) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(scheduleId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> setState { copy(isLoading = true) }
                    is ApiResult.Success -> {
                        setState { copy(isLoading = false) }
                        if (result.data) {
                            setEffect { ScheduleDetailContract.Effect.ShowToast(context.getString(R.string.tiele_friend_delete_result)) }
                            val state = uiState.value
                            loadSchedules(state.idolId, state.yearMonth, state.locale, null)
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false) }
                        setEffect {
                            ScheduleDetailContract.Effect.ShowError(result.message ?: context.getString(R.string.msg_error_ok))
                        }
                    }
                }
            }
        }
    }

    private fun editSchedule(schedule: ScheduleModel) {
        setEffect { ScheduleDetailContract.Effect.NavigateToScheduleEdit(schedule) }
    }

    private fun viewComments(schedule: ScheduleModel) {
        setEffect { ScheduleDetailContract.Effect.NavigateToComments(schedule) }
    }

    private fun openMap(schedule: ScheduleModel) {
        schedule.location?.let { location ->
            // ScheduleModel에 lat, lng 정보가 없으므로 location만 전달
            setEffect { ScheduleDetailContract.Effect.OpenMapIntent("", "", location) }
        }
    }

    private fun openUrl(url: String) {
        setEffect { ScheduleDetailContract.Effect.OpenUrlIntent(url) }
    }

    /**
     * 스케줄 업데이트 (댓글 작성 후 동기화)
     */
    fun updateSchedule(updatedSchedule: ScheduleModel) {
        val currentSchedules = uiState.value.schedules
        val updatedSchedules = currentSchedules.map { schedule ->
            if (schedule.id == updatedSchedule.id) {
                schedule.copy(numComments = updatedSchedule.numComments)
            } else {
                schedule
            }
        }
        val grouped = groupSchedulesByDate(updatedSchedules, uiState.value.locale)
        setState { copy(schedules = updatedSchedules, groupedSchedules = grouped) }
    }
}
