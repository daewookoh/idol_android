package net.ib.mn.presentation.community.subpage

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ScheduleModel
import java.util.Calendar

class CommunityScheduleContract {

    data class State(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,

        val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
        val selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),

        val dayIconMap: Map<Int, String> = emptyMap(),
        val daySchedules: List<ScheduleModel> = emptyList(),

        val locale: String = "ko",
        val localeText: String = "한국어",

        val error: String? = null
    ) : UiState {

        val yearMonth: String
            get() = "$currentYear${String.format("%02d", currentMonth + 1)}"

        val yearMonthDay: String
            get() = "$currentYear${String.format("%02d", currentMonth + 1)}${String.format("%02d", selectedDay)}"

        val daysInMonth: Int
            get() {
                val cal = Calendar.getInstance()
                cal.set(currentYear, currentMonth, 1)
                return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }

        val firstDayOfWeek: Int
            get() {
                val cal = Calendar.getInstance()
                cal.set(currentYear, currentMonth, 1)
                return cal.get(Calendar.DAY_OF_WEEK)
            }

        companion object {
            val SCHEDULE_LANGUAGES = arrayOf(
                "한국어", "ENG", "中文", "日本語", "Indonesia", "Português", "Español", "Tiếng Việt", "ไทย"
            )
            val SCHEDULE_LOCALES = arrayOf(
                "ko", "en", "zh", "ja", "in", "pt", "es", "vi", "th"
            )
        }
    }

    sealed class Intent : UiIntent {
        data class LoadInitialData(val idolId: Int) : Intent()
        data class Refresh(val idolId: Int) : Intent()
        data class SelectDay(val day: Int, val idolId: Int) : Intent()
        data class PreviousMonth(val idolId: Int) : Intent()
        data class NextMonth(val idolId: Int) : Intent()
        data class ChangeLocale(val locale: String, val localeText: String, val idolId: Int) : Intent()
        data class VoteSchedule(val scheduleId: Int, val vote: String, val idolId: Int) : Intent()
        data class DeleteSchedule(val scheduleId: Int, val idolId: Int) : Intent()
        data class EditSchedule(val schedule: ScheduleModel) : Intent()
        data class ViewScheduleDetail(val schedule: ScheduleModel) : Intent()
    }

    sealed class Effect : UiEffect {
        data class ShowError(val message: String) : Effect()
        data class ShowToast(val message: String) : Effect()
        data class NavigateToScheduleDetail(val schedule: ScheduleModel, val yearMonthDay: String, val locale: String) : Effect()
        object ShowLanguageSelector : Effect()
        data class ShowDeleteConfirm(val scheduleId: Int) : Effect()
        data class NavigateToScheduleEdit(val schedule: ScheduleModel) : Effect()
    }
}
