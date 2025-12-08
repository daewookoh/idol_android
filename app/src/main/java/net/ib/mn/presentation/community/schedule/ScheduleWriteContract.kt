package net.ib.mn.presentation.community.schedule

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.domain.model.ScheduleCategory
import net.ib.mn.domain.model.ScheduleModel
import java.util.Date

/**
 * 스케줄 작성/수정 화면의 MVI Contract
 * Old 프로젝트의 ScheduleWriteActivity 기능을 Compose MVI로 구현
 */
class ScheduleWriteContract {

    data class State(
        // 기본 상태
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,

        // 수정 모드
        val isEditMode: Boolean = false,
        val editingScheduleId: Int? = null,

        // 아이돌 정보
        val idol: IdolEntity? = null,
        val selectedIdols: List<IdolEntity> = emptyList(),
        val canSelectIdol: Boolean = true,

        // 입력 필드
        val title: String = "",
        val category: ScheduleCategory? = null,
        val isAllDay: Boolean = false,
        val selectedDate: Date = Date(),
        val location: String = "",
        val latitude: String = "",
        val longitude: String = "",
        val url: String = "",
        val extra: String = "",

        // 유효성 검사
        val isTitleValid: Boolean = false,
        val isCategoryValid: Boolean = false,
        val isIdolValid: Boolean = false
    ) : UiState {
        val canSubmit: Boolean
            get() = isTitleValid && isCategoryValid && isIdolValid && !isSaving

        val idolDisplayName: String
            get() = selectedIdols.joinToString(", ") { it.name }

        companion object {
            const val MAX_TITLE_LENGTH = 30
            const val MAX_URL_LENGTH = 100
            const val MAX_EXTRA_LENGTH = 100
        }
    }

    sealed class Intent : UiIntent {
        data class Initialize(
            val idolId: Int?,
            val date: Date?,
            val editingSchedule: ScheduleModel? = null
        ) : Intent()

        data class OnTitleChanged(val title: String) : Intent()
        data class OnCategorySelected(val category: ScheduleCategory) : Intent()
        data class OnAllDayChanged(val isAllDay: Boolean) : Intent()
        data class OnDateSelected(val date: Date) : Intent()
        data class OnLocationSelected(
            val address: String,
            val latitude: String,
            val longitude: String
        ) : Intent()
        data object OnLocationCleared : Intent()
        data class OnUrlChanged(val url: String) : Intent()
        data class OnExtraChanged(val extra: String) : Intent()
        data class OnIdolsSelected(val idols: List<IdolEntity>) : Intent()

        data object OnShowCategorySelector : Intent()
        data object OnShowDateTimePicker : Intent()
        data object OnShowIdolSelector : Intent()
        data object OnShowLocationSelector : Intent()

        data object OnSubmit : Intent()
        data object OnBackPressed : Intent()
        data object OnConfirmBack : Intent()
    }

    sealed class Effect : UiEffect {
        data object NavigateBack : Effect()
        data class NavigateBackWithResult(val schedule: ScheduleModel) : Effect()
        data class ShowSuccessDialog(val schedule: ScheduleModel) : Effect()
        data object ShowCategorySelector : Effect()
        data object ShowDateTimePicker : Effect()
        data object ShowIdolSelector : Effect()
        data object ShowLocationSelector : Effect()
        data object ShowBackConfirmDialog : Effect()
        data class ShowError(val message: String) : Effect()
    }
}
