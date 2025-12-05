package net.ib.mn.presentation.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.ExoBurningDayDialog
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoErrorDialog
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * BurningDayDialog - 올인데이 설정 다이얼로그
 *
 * old 프로젝트의 BurningDayPurchaseDialogFragment를 Compose로 구현
 *
 * @param onDismiss 다이얼로그 닫기
 * @param onSuccess 설정 성공 시 콜백 (burningDay 문자열 반환)
 */
@Composable
fun BurningDayDialog(
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    viewModel: BurningDayViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 선택된 날짜 인덱스
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    // 확인 다이얼로그 표시 여부
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 에러 다이얼로그 표시 여부
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 다이얼로그 표시 시 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    // 성공 시 콜백 호출 및 다이얼로그 닫기
    LaunchedEffect(uiState) {
        if (uiState is BurningDayUiState.Success) {
            val burningDay = (uiState as BurningDayUiState.Success).burningDay
            onSuccess(burningDay)
            onDismiss()
        } else if (uiState is BurningDayUiState.Error) {
            errorMessage = (uiState as BurningDayUiState.Error).message
            showErrorDialog = true
        }
    }

    // UI 상태에 따른 다이얼로그 표시
    when (val state = uiState) {
        is BurningDayUiState.Loading -> {
            // 로딩 중 다이얼로그
            ExoBurningDayDialog(
                title = stringResource(R.string.set_all_in_day),
                guideText1 = "",
                guideText2 = "",
                displayDays = emptyList(),
                selectedIndex = 0,
                onIndexChange = {},
                onConfirm = {},
                onDismiss = onDismiss,
                isLoading = true
            )
        }

        is BurningDayUiState.Ready -> {
            // 데이터 준비 완료
            ExoBurningDayDialog(
                title = stringResource(R.string.set_all_in_day),
                guideText1 = stringResource(R.string.item_guide_3_1, state.itemLevel),
                guideText2 = stringResource(R.string.item_guide_3_2, state.diamondCost),
                displayDays = state.displayDays,
                selectedIndex = selectedDayIndex,
                onIndexChange = { selectedDayIndex = it },
                onConfirm = {
                    // 확인 다이얼로그 표시
                    showConfirmDialog = true
                },
                onDismiss = onDismiss,
                isLoading = false,
                isEnabled = true
            )
        }

        is BurningDayUiState.Purchasing -> {
            // 구매 중
            ExoBurningDayDialog(
                title = stringResource(R.string.set_all_in_day),
                guideText1 = "",
                guideText2 = "",
                displayDays = emptyList(),
                selectedIndex = 0,
                onIndexChange = {},
                onConfirm = {},
                onDismiss = {},
                isLoading = true,
                isEnabled = false
            )
        }

        is BurningDayUiState.Error -> {
            // 에러는 LaunchedEffect에서 처리
        }

        is BurningDayUiState.Success -> {
            // 성공은 LaunchedEffect에서 처리
        }
    }

    // 구매 확인 다이얼로그
    if (showConfirmDialog && uiState is BurningDayUiState.Ready) {
        val readyState = uiState as BurningDayUiState.Ready
        val selectedDateYmd = readyState.daysYmd.getOrNull(selectedDayIndex)

        // 날짜 포맷팅 (한국어로 표시)
        val formattedDate = remember(selectedDateYmd) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                dateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                val date = selectedDateYmd?.let { dateFormat.parse(it) }
                val targetFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.KOREA)
                targetFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                date?.let { targetFormat.format(it) } ?: ""
            } catch (e: Exception) {
                selectedDateYmd ?: ""
            }
        }

        ExoConfirmDialog(
            title = formattedDate,
            message = stringResource(R.string.msg_confirm_burning_day),
            onConfirm = {
                showConfirmDialog = false
                selectedDateYmd?.let { viewModel.purchaseBurningDay(it) }
            },
            onDismiss = {
                showConfirmDialog = false
            },
            confirmButtonText = stringResource(R.string.register),
            dismissButtonText = stringResource(R.string.btn_cancel)
        )
    }

    // 에러 다이얼로그
    if (showErrorDialog) {
        ExoErrorDialog(
            message = errorMessage,
            onDismiss = {
                showErrorDialog = false
                onDismiss()
            }
        )
    }
}
