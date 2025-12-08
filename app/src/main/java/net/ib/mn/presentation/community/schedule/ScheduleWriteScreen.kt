package net.ib.mn.presentation.community.schedule

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.domain.model.ScheduleCategory
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.presentation.community.schedule.ScheduleWriteContract.Effect
import net.ib.mn.presentation.community.schedule.ScheduleWriteContract.Intent
import net.ib.mn.presentation.community.schedule.ScheduleWriteContract.State
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoBottomSheet
import net.ib.mn.ui.components.ExoBottomSheetType
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.LocaleUtil
import java.text.DateFormat
import java.util.Date

/**
 * 스케줄 작성/수정 화면
 * Old 프로젝트의 ScheduleWriteActivity를 Compose로 구현
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleWriteScreen(
    idolId: Int? = null,
    initialDate: Date? = null,
    editingSchedule: ScheduleModel? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateBackWithResult: (ScheduleModel) -> Unit = {},
    viewModel: ScheduleWriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 다이얼로그 상태
    var showBackConfirmDialog by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }

    // 초기화
    LaunchedEffect(Unit) {
        viewModel.sendIntent(Intent.Initialize(idolId, initialDate, editingSchedule))
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is Effect.NavigateBack -> onNavigateBack()
                is Effect.NavigateBackWithResult -> onNavigateBackWithResult(effect.schedule)
                is Effect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is Effect.ShowCategorySelector -> showCategorySheet = true
                is Effect.ShowDateTimePicker -> showDateTimePicker = true
                is Effect.ShowIdolSelector -> {
                    // TODO: 아이돌 선택 화면 구현
                }
                is Effect.ShowLocationSelector -> {
                    showLocationPicker = true
                }
                is Effect.ShowBackConfirmDialog -> showBackConfirmDialog = true
                is Effect.ShowError -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 뒤로가기 핸들러
    BackHandler {
        viewModel.sendIntent(Intent.OnBackPressed)
    }

    Scaffold(
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.schedule_write),
                onNavigationClick = { viewModel.sendIntent(Intent.OnBackPressed) }
            )
        },
        containerColor = ColorPalette.background100
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 스크롤 가능한 컨텐츠
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp) // 하단 버튼 공간 확보
            ) {
                // 제목 입력 (필수)
                ScheduleInputRow(
                    isRequired = true,
                    value = state.title,
                    hint = stringResource(R.string.schedule_title),
                    onValueChange = { viewModel.sendIntent(Intent.OnTitleChanged(it)) }
                )

                // 카테고리 선택 (필수)
                ScheduleSelectRow(
                    isRequired = true,
                    label = if (state.category != null) stringResource(R.string.schedule_category) else null,
                    hint = stringResource(R.string.schedule_category),
                    value = state.category?.let { stringResource(it.labelResId) },
                    leadingIcon = state.category?.iconResId,
                    onClick = { viewModel.sendIntent(Intent.OnShowCategorySelector) }
                )

                // 아이돌 선택 (필수, CELEB이 아닌 경우만)
                if (!BuildConfig.CELEB) {
                    ScheduleSelectRow(
                        isRequired = true,
                        label = if (state.selectedIdols.isNotEmpty()) {
                            if (BuildConfig.CELEB) stringResource(R.string.actor) else stringResource(R.string.stats_idol)
                        } else null,
                        hint = if (BuildConfig.CELEB) stringResource(R.string.actor) else stringResource(R.string.stats_idol),
                        value = state.idolDisplayName.ifEmpty { null },
                        showArrow = state.canSelectIdol,
                        onClick = if (state.canSelectIdol) {
                            { viewModel.sendIntent(Intent.OnShowIdolSelector) }
                        } else null
                    )
                }

                // 하루종일 체크박스
                ScheduleCheckboxRow(
                    label = stringResource(R.string.schedule_allday),
                    isChecked = state.isAllDay,
                    enabled = state.category?.isAllDayOnly != true,
                    onCheckedChange = { viewModel.sendIntent(Intent.OnAllDayChanged(it)) }
                )

                // 날짜/시간 선택 (필수)
                ScheduleSelectRow(
                    isRequired = true,
                    label = stringResource(R.string.schedule_time),
                    hint = stringResource(R.string.schedule_time),
                    value = formatDateTime(context, state.selectedDate, state.isAllDay),
                    onClick = { viewModel.sendIntent(Intent.OnShowDateTimePicker) }
                )

                // 위치 선택 (선택사항, 중국 버전 제외)
                if (!BuildConfig.CHINA) {
                    ScheduleLocationRow(
                        label = if (state.location.isNotEmpty()) stringResource(R.string.schedule_location) else null,
                        hint = stringResource(R.string.schedule_location),
                        value = state.location.ifEmpty { null },
                        onLocationClick = { viewModel.sendIntent(Intent.OnShowLocationSelector) },
                        onClearClick = if (state.location.isNotEmpty()) {
                            { viewModel.sendIntent(Intent.OnLocationCleared) }
                        } else null
                    )
                }

                // URL 입력 (선택사항)
                ScheduleInputRow(
                    isRequired = false,
                    value = state.url,
                    hint = "URL",
                    onValueChange = { viewModel.sendIntent(Intent.OnUrlChanged(it)) }
                )

                // 추가 정보 입력 (선택사항)
                ScheduleMultiLineInputRow(
                    hint = stringResource(R.string.schedule_info),
                    value = state.extra,
                    onValueChange = { viewModel.sendIntent(Intent.OnExtraChanged(it)) }
                )
            }

            // 플로팅 저장 버튼 (키보드와 함께 올라옴)
            Button(
                onClick = { viewModel.sendIntent(Intent.OnSubmit) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .padding(10.dp)
                    .height(50.dp),
                enabled = state.canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorPalette.main,
                    disabledContainerColor = ColorPalette.gray200
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.lable_save),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // 뒤로가기 확인 다이얼로그
    if (showBackConfirmDialog) {
        ExoConfirmDialog(
            title = "",
            message = stringResource(R.string.schedule_write_stop),
            confirmButtonText = stringResource(R.string.confirm),
            dismissButtonText = stringResource(R.string.btn_cancel),
            onConfirm = {
                showBackConfirmDialog = false
                viewModel.sendIntent(Intent.OnConfirmBack)
            },
            onDismiss = { showBackConfirmDialog = false }
        )
    }

    // 카테고리 선택 바텀시트
    if (showCategorySheet) {
        CategorySelectorBottomSheet(
            onCategorySelected = { category ->
                viewModel.sendIntent(Intent.OnCategorySelected(category))
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    // 날짜/시간 선택 다이얼로그
    if (showDateTimePicker) {
        ScheduleDateTimePickerDialog(
            initialDate = state.selectedDate,
            isAllDay = state.isAllDay,
            onDateSelected = { date ->
                viewModel.sendIntent(Intent.OnDateSelected(date))
                showDateTimePicker = false
            },
            onDismiss = { showDateTimePicker = false }
        )
    }

    // 위치 선택 화면
    if (showLocationPicker) {
        ScheduleLocationScreen(
            initialLatitude = state.latitude.toDoubleOrNull() ?: 37.5192336,
            initialLongitude = state.longitude.toDoubleOrNull() ?: 127.1250279,
            onLocationSelected = { address, latitude, longitude ->
                viewModel.sendIntent(Intent.OnLocationSelected(address, latitude, longitude))
                showLocationPicker = false
            },
            onNavigateBack = { showLocationPicker = false }
        )
    }
}

// ============================================================
// Input Row Components
// ============================================================

@Composable
private fun ScheduleInputRow(
    isRequired: Boolean,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(ColorPalette.background100)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRequired) {
            Text(
                text = "*",
                fontSize = 14.sp,
                color = ColorPalette.main,
                modifier = Modifier.width(8.5.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(8.5.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = ColorPalette.textDefault
            ),
            cursorBrush = SolidColor(ColorPalette.textDefault),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            fontSize = 14.sp,
                            color = ColorPalette.textDimmed
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
    HorizontalDivider(thickness = 1.dp, color = ColorPalette.gray110)
}

@Composable
private fun ScheduleSelectRow(
    isRequired: Boolean,
    label: String?,
    hint: String,
    value: String?,
    leadingIcon: Int? = null,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(ColorPalette.background100)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRequired) {
            Text(
                text = "*",
                fontSize = 14.sp,
                color = ColorPalette.main,
                modifier = Modifier.width(8.5.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(8.5.dp))
        }

        Text(
            text = label ?: hint,
            fontSize = 14.sp,
            color = if (label != null) ColorPalette.textDefault else ColorPalette.textDimmed
        )

        Spacer(modifier = Modifier.weight(1f))

        leadingIcon?.let { iconRes ->
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        value?.let { text ->
            Text(
                text = text,
                fontSize = 12.sp,
                color = ColorPalette.textDefault,
                modifier = Modifier.padding(end = 14.dp)
            )
        }

        if (showArrow && onClick != null) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = ColorPalette.gray300
            )
        }
    }
    HorizontalDivider(thickness = 1.dp, color = ColorPalette.gray110)
}

@Composable
private fun ScheduleCheckboxRow(
    label: String,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(ColorPalette.background100)
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isChecked) label else "",
            fontSize = 14.sp,
            color = if (isChecked) ColorPalette.textDefault else ColorPalette.textDimmed
        )

        if (!isChecked) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = ColorPalette.textDimmed
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Checkbox(
            checked = isChecked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            colors = CheckboxDefaults.colors(
                checkedColor = ColorPalette.main,
                uncheckedColor = ColorPalette.gray200
            )
        )
    }
    HorizontalDivider(thickness = 1.dp, color = ColorPalette.gray110)
}

@Composable
private fun ScheduleLocationRow(
    label: String?,
    hint: String,
    value: String?,
    onLocationClick: () -> Unit,
    onClearClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(ColorPalette.background100)
            .clickable { onLocationClick() }
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label ?: hint,
            fontSize = 14.sp,
            color = if (label != null) ColorPalette.textDefault else ColorPalette.textDimmed
        )

        Spacer(modifier = Modifier.weight(1f))

        onClearClick?.let { clearAction ->
            IconButton(
                onClick = clearAction,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.btn_cancel_schedule_place),
                    contentDescription = "Clear",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
            }
        }

        value?.let { text ->
            Text(
                text = text,
                fontSize = 12.sp,
                color = ColorPalette.textDefault,
                modifier = Modifier.padding(end = 14.dp)
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = ColorPalette.gray300
        )
    }
    HorizontalDivider(thickness = 1.dp, color = ColorPalette.gray110)
}

@Composable
private fun ScheduleMultiLineInputRow(
    hint: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(ColorPalette.background100)
            .padding(horizontal = 26.dp, vertical = 15.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = ColorPalette.textDefault
            ),
            cursorBrush = SolidColor(ColorPalette.textDefault),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            fontSize = 14.sp,
                            color = ColorPalette.textDimmed
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
    HorizontalDivider(thickness = 1.dp, color = ColorPalette.gray110)
}

// ============================================================
// Bottom Sheet & Dialog Components
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelectorBottomSheet(
    onCategorySelected: (ScheduleCategory) -> Unit,
    onDismiss: () -> Unit
) {
    ExoBottomSheet(
        onDismissRequest = onDismiss,
        type = ExoBottomSheetType.LIST,
        containerColor = ColorPalette.background200,
        title = stringResource(R.string.schedule_category)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            ScheduleCategory.entries.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategorySelected(category) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(category.iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(category.labelResId),
                        fontSize = 15.sp,
                        color = ColorPalette.textDefault
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDateTimePickerDialog(
    initialDate: Date,
    isAllDay: Boolean,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    // 단계: 0 = 날짜 선택, 1 = 시간 선택
    var step by remember { mutableIntStateOf(0) }
    var selectedDateMillis by remember { mutableLongStateOf(initialDate.time) }

    val calendar = remember { java.util.Calendar.getInstance() }

    @Suppress("DEPRECATION")
    val initialHour = remember { initialDate.hours }
    @Suppress("DEPRECATION")
    val initialMinute = remember { initialDate.minutes }

    if (step == 0) {
        // 날짜 선택
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )

        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateMillis = millis
                            if (isAllDay) {
                                // 종일인 경우 바로 완료
                                onDateSelected(Date(millis))
                            } else {
                                // 시간 선택으로 이동
                                step = 1
                            }
                        }
                    }
                ) {
                    Text(stringResource(if (isAllDay) R.string.confirm else R.string.btn_next))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        // 시간 선택
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        calendar.timeInMillis = selectedDateMillis
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        calendar.set(java.util.Calendar.MINUTE, timePickerState.minute)
                        calendar.set(java.util.Calendar.SECOND, 0)
                        onDateSelected(calendar.time)
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { step = 0 }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.schedule_time),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

// ============================================================
// Utility Functions
// ============================================================

private fun formatDateTime(context: android.content.Context, date: Date, isAllDay: Boolean): String {
    val locale = LocaleUtil.getAppLocale(context)
    val format = if (isAllDay) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
    } else {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
    }
    return format.format(date)
}
