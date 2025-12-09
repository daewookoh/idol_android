package net.ib.mn.presentation.community.schedule

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoBottomSheet
import net.ib.mn.ui.components.ExoBottomSheetType
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoDialog
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.LocaleUtil
import java.text.DateFormat
import java.util.Date

/**
 * 스케줄 작성/수정 화면
 * 표준 Scaffold + bottomBar 패턴 사용
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
    val focusManager = LocalFocusManager.current

    // 다이얼로그 상태
    var showBackConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var savedSchedule by remember { mutableStateOf<ScheduleModel?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }

    // 포커스 관리
    val urlFocusRequester = remember { FocusRequester() }
    val extraFocusRequester = remember { FocusRequester() }

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
                is Effect.ShowSuccessDialog -> {
                    savedSchedule = effect.schedule
                    showSuccessDialog = true
                }
                is Effect.ShowCategorySelector -> showCategorySheet = true
                is Effect.ShowDateTimePicker -> showDateTimePicker = true
                is Effect.ShowIdolSelector -> { /* TODO */ }
                is Effect.ShowLocationSelector -> showLocationPicker = true
                is Effect.ShowBackConfirmDialog -> showBackConfirmDialog = true
                is Effect.ShowError -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 뒤로가기 핸들러
    BackHandler {
        viewModel.sendIntent(Intent.OnBackPressed)
    }

    // Scaffold 사용 - contentWindowInsets를 0으로 설정하여 직접 제어
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // 전체 Scaffold에 imePadding 적용
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.schedule_write),
                onNavigationClick = { viewModel.sendIntent(Intent.OnBackPressed) }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorPalette.background100)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.sendIntent(Intent.OnSubmit)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(50.dp),
                    enabled = state.canSubmit && !state.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPalette.main,
                        disabledContainerColor = ColorPalette.gray200
                    ),
                    shape = RoundedCornerShape(8.dp)
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
        },
        containerColor = ColorPalette.background100,
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // 모든 inset 직접 제어
    ) { innerPadding ->
        // 스크롤 가능한 콘텐츠 - innerPadding으로 정확한 영역 계산
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // 제목 입력 (필수)
            InputRow(
                isRequired = true,
                value = state.title,
                hint = stringResource(R.string.schedule_title),
                onValueChange = { viewModel.sendIntent(Intent.OnTitleChanged(it)) },
                imeAction = ImeAction.Next,
                onImeAction = { urlFocusRequester.requestFocus() }
            )

            // 카테고리 선택 (필수)
            SelectRow(
                isRequired = true,
                label = state.category?.let { stringResource(R.string.schedule_category) },
                hint = stringResource(R.string.schedule_category),
                value = state.category?.let { stringResource(it.labelResId) },
                leadingIcon = state.category?.iconResId,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.sendIntent(Intent.OnShowCategorySelector)
                }
            )

            // 아이돌 선택
            if (!BuildConfig.CELEB) {
                SelectRow(
                    isRequired = true,
                    label = if (state.selectedIdols.isNotEmpty()) stringResource(R.string.stats_idol) else null,
                    hint = stringResource(R.string.stats_idol),
                    value = state.idolDisplayName.ifEmpty { null },
                    showArrow = state.canSelectIdol,
                    onClick = if (state.canSelectIdol) {
                        {
                            focusManager.clearFocus()
                            viewModel.sendIntent(Intent.OnShowIdolSelector)
                        }
                    } else null
                )
            }

            // 하루종일 체크박스
            CheckboxRow(
                label = stringResource(R.string.schedule_allday),
                isChecked = state.isAllDay,
                enabled = state.category?.isAllDayOnly != true,
                onCheckedChange = {
                    focusManager.clearFocus()
                    viewModel.sendIntent(Intent.OnAllDayChanged(it))
                }
            )

            // 날짜/시간 선택 (필수)
            SelectRow(
                isRequired = true,
                label = stringResource(R.string.schedule_time),
                hint = stringResource(R.string.schedule_time),
                value = formatDateTime(context, state.selectedDate, state.isAllDay),
                onClick = {
                    focusManager.clearFocus()
                    viewModel.sendIntent(Intent.OnShowDateTimePicker)
                }
            )

            // 위치 선택
            if (!BuildConfig.CHINA) {
                LocationRow(
                    label = if (state.location.isNotEmpty()) stringResource(R.string.schedule_location) else null,
                    hint = stringResource(R.string.schedule_location),
                    value = state.location.ifEmpty { null },
                    onLocationClick = {
                        focusManager.clearFocus()
                        viewModel.sendIntent(Intent.OnShowLocationSelector)
                    },
                    onClearClick = if (state.location.isNotEmpty()) {
                        { viewModel.sendIntent(Intent.OnLocationCleared) }
                    } else null
                )
            }

            // URL 입력
            InputRow(
                isRequired = false,
                value = state.url,
                hint = "URL",
                onValueChange = { viewModel.sendIntent(Intent.OnUrlChanged(it)) },
                focusRequester = urlFocusRequester,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
                onImeAction = { extraFocusRequester.requestFocus() }
            )

            // 추가 정보 입력
            MultiLineInputRow(
                hint = stringResource(R.string.schedule_info),
                value = state.extra,
                onValueChange = { viewModel.sendIntent(Intent.OnExtraChanged(it)) },
                focusRequester = extraFocusRequester,
                onImeAction = { focusManager.clearFocus() }
            )
        }
    }

    // 다이얼로그들
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

    if (showCategorySheet) {
        CategoryBottomSheet(
            onCategorySelected = { category ->
                viewModel.sendIntent(Intent.OnCategorySelected(category))
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialDate = state.selectedDate,
            isAllDay = state.isAllDay,
            onDateSelected = { date ->
                viewModel.sendIntent(Intent.OnDateSelected(date))
                showDateTimePicker = false
            },
            onDismiss = { showDateTimePicker = false }
        )
    }

    if (showLocationPicker) {
        ScheduleLocationScreen(
            initialLatitude = state.latitude.toDoubleOrNull() ?: 37.5192336,
            initialLongitude = state.longitude.toDoubleOrNull() ?: 127.1250279,
            onLocationSelected = { address, lat, lng ->
                viewModel.sendIntent(Intent.OnLocationSelected(address, lat, lng))
                showLocationPicker = false
            },
            onNavigateBack = { showLocationPicker = false }
        )
    }

    // 저장 완료 다이얼로그
    if (showSuccessDialog && savedSchedule != null) {
        ExoDialog(
            message = stringResource(R.string.schedule_save),
            onDismiss = { },
            confirmButtonText = stringResource(R.string.confirm),
            onConfirm = {
                showSuccessDialog = false
                savedSchedule?.let { onNavigateBackWithResult(it) }
            },
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    }
}

// ============================================================
// 입력 컴포넌트
// ============================================================

@Composable
private fun InputRow(
    isRequired: Boolean,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isRequired) "*" else "",
            fontSize = 14.sp,
            color = ColorPalette.main,
            modifier = Modifier.width(10.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            textStyle = TextStyle(fontSize = 14.sp, color = ColorPalette.textDefault),
            cursorBrush = SolidColor(ColorPalette.main),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction?.invoke() },
                onDone = { onImeAction?.invoke() }
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = hint, fontSize = 14.sp, color = ColorPalette.textDimmed)
                    }
                    innerTextField()
                }
            }
        )
    }
    HorizontalDivider(color = ColorPalette.gray110)
}

@Composable
private fun SelectRow(
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
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isRequired) "*" else "",
            fontSize = 14.sp,
            color = ColorPalette.main,
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = label ?: hint,
            fontSize = 14.sp,
            color = if (label != null) ColorPalette.textDefault else ColorPalette.textDimmed
        )

        Spacer(modifier = Modifier.weight(1f))

        leadingIcon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        value?.let {
            Text(text = it, fontSize = 12.sp, color = ColorPalette.textDefault)
            Spacer(modifier = Modifier.width(14.dp))
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
    HorizontalDivider(color = ColorPalette.gray110)
}

@Composable
private fun CheckboxRow(
    label: String,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (isChecked) ColorPalette.textDefault else ColorPalette.textDimmed
        )
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
    HorizontalDivider(color = ColorPalette.gray110)
}

@Composable
private fun LocationRow(
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

        onClearClick?.let {
            IconButton(onClick = it, modifier = Modifier.size(24.dp)) {
                Icon(
                    painter = painterResource(R.drawable.btn_cancel_schedule_place),
                    contentDescription = "Clear",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
            }
        }

        value?.let {
            Text(text = it, fontSize = 12.sp, color = ColorPalette.textDefault)
            Spacer(modifier = Modifier.width(14.dp))
        }

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = ColorPalette.gray300
        )
    }
    HorizontalDivider(color = ColorPalette.gray110)
}

@Composable
private fun MultiLineInputRow(
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    onImeAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 26.dp, vertical = 15.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            textStyle = TextStyle(fontSize = 14.sp, color = ColorPalette.textDefault),
            cursorBrush = SolidColor(ColorPalette.main),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = hint, fontSize = 14.sp, color = ColorPalette.textDimmed)
                    }
                    innerTextField()
                }
            }
        )
    }
    HorizontalDivider(color = ColorPalette.gray110)
}

// ============================================================
// 바텀시트 & 다이얼로그
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBottomSheet(
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
private fun DateTimePickerDialog(
    initialDate: Date,
    isAllDay: Boolean,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var selectedDateMillis by remember { mutableLongStateOf(initialDate.time) }
    val calendar = remember { java.util.Calendar.getInstance() }

    @Suppress("DEPRECATION")
    val initialHour = remember { initialDate.hours }
    @Suppress("DEPRECATION")
    val initialMinute = remember { initialDate.minutes }

    if (step == 0) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDateMillis = millis
                        if (isAllDay) onDateSelected(Date(millis)) else step = 1
                    }
                }) {
                    Text(stringResource(if (isAllDay) R.string.confirm else R.string.btn_next))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    calendar.timeInMillis = selectedDateMillis
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(java.util.Calendar.MINUTE, timePickerState.minute)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    onDateSelected(calendar.time)
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { step = 0 }) { Text(stringResource(R.string.btn_cancel)) }
            },
            title = { Text(stringResource(R.string.schedule_time), fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

// ============================================================
// 유틸리티
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
