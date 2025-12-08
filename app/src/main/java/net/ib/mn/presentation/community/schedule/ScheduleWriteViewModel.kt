package net.ib.mn.presentation.community.schedule

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ScheduleCategory
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.domain.repository.ScheduleRepository
import net.ib.mn.domain.repository.ScheduleWriteRequest
import net.ib.mn.presentation.community.schedule.ScheduleWriteContract.Effect
import net.ib.mn.presentation.community.schedule.ScheduleWriteContract.Intent
import net.ib.mn.presentation.community.schedule.ScheduleWriteContract.State
import net.ib.mn.util.BadWordFilter
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

private const val TAG = "ScheduleWriteViewModel"

@HiltViewModel
class ScheduleWriteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val idolRepository: IdolRepository,
    private val scheduleRepository: ScheduleRepository,
    private val badWordFilter: BadWordFilter
) : BaseViewModel<State, Intent, Effect>() {

    override fun createInitialState(): State = State()

    override fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.Initialize -> initialize(intent)
            is Intent.OnTitleChanged -> onTitleChanged(intent.title)
            is Intent.OnCategorySelected -> onCategorySelected(intent.category)
            is Intent.OnAllDayChanged -> onAllDayChanged(intent.isAllDay)
            is Intent.OnDateSelected -> onDateSelected(intent.date)
            is Intent.OnLocationSelected -> onLocationSelected(intent.address, intent.latitude, intent.longitude)
            is Intent.OnLocationCleared -> onLocationCleared()
            is Intent.OnUrlChanged -> onUrlChanged(intent.url)
            is Intent.OnExtraChanged -> onExtraChanged(intent.extra)
            is Intent.OnIdolsSelected -> onIdolsSelected(intent.idols)
            is Intent.OnShowCategorySelector -> setEffect { Effect.ShowCategorySelector }
            is Intent.OnShowDateTimePicker -> setEffect { Effect.ShowDateTimePicker }
            is Intent.OnShowIdolSelector -> setEffect { Effect.ShowIdolSelector }
            is Intent.OnShowLocationSelector -> setEffect { Effect.ShowLocationSelector }
            is Intent.OnSubmit -> onSubmit()
            is Intent.OnBackPressed -> onBackPressed()
            is Intent.OnConfirmBack -> setEffect { Effect.NavigateBack }
        }
    }

    private fun initialize(intent: Intent.Initialize) {
        viewModelScope.launch {
            setState { copy(isLoading = true) }

            try {
                val idol = intent.idolId?.let { idolRepository.getIdolById(it) }
                val editingSchedule = intent.editingSchedule
                val isEditMode = editingSchedule != null

                // 아이돌 선택 가능 여부 (솔로/CELEB인 경우 선택 불가)
                val canSelectIdol = !BuildConfig.CELEB && idol?.type != "S"

                // 초기 아이돌 목록 설정
                val selectedIdols = if (idol != null) listOf(idol) else emptyList()

                // 초기 날짜 설정
                val initialDate = intent.date ?: Date()

                if (isEditMode && editingSchedule != null) {
                    // 수정 모드
                    val category = ScheduleCategory.fromCode(editingSchedule.category)
                    setState {
                        copy(
                            isLoading = false,
                            isEditMode = true,
                            editingScheduleId = editingSchedule.id,
                            idol = idol,
                            selectedIdols = selectedIdols,
                            canSelectIdol = canSelectIdol,
                            title = editingSchedule.title,
                            category = category,
                            isAllDay = editingSchedule.allday == 1,
                            selectedDate = editingSchedule.dtstart,
                            location = editingSchedule.location ?: "",
                            url = editingSchedule.url ?: "",
                            extra = editingSchedule.extra ?: "",
                            isTitleValid = editingSchedule.title.isNotEmpty(),
                            isCategoryValid = category != null,
                            isIdolValid = selectedIdols.isNotEmpty()
                        )
                    }
                } else {
                    // 신규 작성 모드
                    setState {
                        copy(
                            isLoading = false,
                            isEditMode = false,
                            idol = idol,
                            selectedIdols = selectedIdols,
                            canSelectIdol = canSelectIdol,
                            selectedDate = initialDate,
                            isIdolValid = selectedIdols.isNotEmpty()
                        )
                    }
                }
            } catch (e: Exception) {
                logE(TAG, "initialize error", e)
                setState { copy(isLoading = false) }
                setEffect { Effect.ShowError("초기화에 실패했습니다.") }
            }
        }
    }

    private fun onTitleChanged(title: String) {
        val trimmedTitle = title.take(State.MAX_TITLE_LENGTH)
        setState {
            copy(
                title = trimmedTitle,
                isTitleValid = trimmedTitle.isNotBlank()
            )
        }
    }

    private fun onCategorySelected(category: ScheduleCategory) {
        setState {
            copy(
                category = category,
                isCategoryValid = true,
                // 기념일 카테고리인 경우 종일로 설정
                isAllDay = if (category.isAllDayOnly) true else isAllDay
            )
        }
    }

    private fun onAllDayChanged(isAllDay: Boolean) {
        // 기념일 카테고리인 경우 종일 해제 불가
        if (currentState.category?.isAllDayOnly == true) return

        setState { copy(isAllDay = isAllDay) }
    }

    private fun onDateSelected(date: Date) {
        setState { copy(selectedDate = date) }
    }

    private fun onLocationSelected(address: String, latitude: String, longitude: String) {
        setState {
            copy(
                location = address,
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    private fun onLocationCleared() {
        setState {
            copy(
                location = "",
                latitude = "",
                longitude = ""
            )
        }
    }

    private fun onUrlChanged(url: String) {
        val trimmedUrl = url.take(State.MAX_URL_LENGTH)
        setState { copy(url = trimmedUrl) }
    }

    private fun onExtraChanged(extra: String) {
        val trimmedExtra = extra.take(State.MAX_EXTRA_LENGTH)
        setState { copy(extra = trimmedExtra) }
    }

    private fun onIdolsSelected(idols: List<IdolEntity>) {
        setState {
            copy(
                selectedIdols = idols,
                isIdolValid = idols.isNotEmpty()
            )
        }
    }

    private fun onSubmit() {
        if (!currentState.canSubmit) return

        viewModelScope.launch {
            setState { copy(isSaving = true) }

            try {
                val state = currentState
                val idol = state.idol ?: run {
                    setEffect { Effect.ShowError("아이돌 정보가 없습니다.") }
                    setState { copy(isSaving = false) }
                    return@launch
                }

                // 제목 욕설 필터링
                val filteredTitle = badWordFilter.filter(state.title)

                // 추가 정보 욕설 필터링
                val filteredExtra = if (state.extra.isNotEmpty()) {
                    badWordFilter.filter(state.extra)
                } else ""

                // 날짜 포맷팅
                val dtstart = formatDateForApi(state.selectedDate, state.isAllDay)

                // 아이돌 ID 목록
                val idolIds = state.selectedIdols.joinToString(",") { it.id.toString() }

                // 메인 아이돌 ID (CELEB인 경우 idol.id, 아니면 groupId)
                val mainIdolId = if (BuildConfig.CELEB) idol.id else idol.groupId

                val request = ScheduleWriteRequest(
                    idolId = mainIdolId,
                    idolIds = idolIds,
                    title = filteredTitle,
                    category = state.category?.code ?: "",
                    location = state.location.ifEmpty { null },
                    lat = state.latitude.ifEmpty { null },
                    lng = state.longitude.ifEmpty { null },
                    url = state.url.ifEmpty { null },
                    dtstart = dtstart,
                    duration = 60,
                    allday = if (state.isAllDay) 1 else 0,
                    extra = filteredExtra.ifEmpty { null },
                    locale = LocaleUtil.getScheduleLocale(context)
                )

                val flow = if (state.isEditMode && state.editingScheduleId != null) {
                    scheduleRepository.editSchedule(state.editingScheduleId, request)
                } else {
                    scheduleRepository.writeSchedule(request)
                }

                flow.collectLatest { result ->
                    when (result) {
                        is ApiResult.Loading -> {
                            // 이미 isSaving = true 상태
                        }
                        is ApiResult.Success -> {
                            logD(TAG, "Schedule saved successfully: ${result.data.id}")
                            setState { copy(isSaving = false) }
                            setEffect { Effect.ShowToast("스케줄이 저장되었습니다.") }
                            setEffect { Effect.NavigateBackWithResult(result.data) }
                        }
                        is ApiResult.Error -> {
                            logE(TAG, "Schedule save failed: ${result.error.message}")
                            setState { copy(isSaving = false) }
                            setEffect { Effect.ShowError(result.error.message ?: "저장에 실패했습니다.") }
                        }
                    }
                }
            } catch (e: Exception) {
                logE(TAG, "onSubmit error", e)
                setState { copy(isSaving = false) }
                setEffect { Effect.ShowError("저장 중 오류가 발생했습니다.") }
            }
        }
    }

    private fun onBackPressed() {
        // 변경사항이 있는 경우 확인 다이얼로그 표시
        val state = currentState
        val hasChanges = state.title.isNotEmpty() ||
                state.category != null ||
                state.url.isNotEmpty() ||
                state.extra.isNotEmpty() ||
                state.location.isNotEmpty()

        if (hasChanges) {
            setEffect { Effect.ShowBackConfirmDialog }
        } else {
            setEffect { Effect.NavigateBack }
        }
    }

    /**
     * API용 날짜 포맷
     * 종일인 경우: yyyy-MM-dd
     * 시간 있는 경우: yyyy-MM-dd'T'HH:mm:00 (UTC 변환)
     */
    private fun formatDateForApi(date: Date, isAllDay: Boolean): String {
        return if (isAllDay) {
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date)
        } else {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00", Locale.ENGLISH)
            utcFormat.timeZone = TimeZone.getTimeZone("UTC")
            utcFormat.format(date)
        }
    }
}
