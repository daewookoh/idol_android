package net.ib.mn.presentation.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoTextField
import net.ib.mn.ui.components.ExoStatusButton
import net.ib.mn.ui.components.ExoDialog
import net.ib.mn.ui.components.LoadingOverlay
import net.ib.mn.util.KeyboardUtil

/**
 * PasswordResetScreen - 비밀번호 재설정 화면.
 * old 프로젝트의 ForgotPasswdFragment와 동일한 UI 및 비즈니스 로직.
 */
@Composable
fun PasswordResetScreen(
    onNavigateBack: () -> Unit,
    viewModel: PasswordResetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    val emailFocusRequester = remember { FocusRequester() }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PasswordResetContract.Effect.NavigateBack -> {
                    onNavigateBack()
                }
                is PasswordResetContract.Effect.ShowError -> {
                    // old 프로젝트와 동일: 에러 메시지를 다이얼로그로 표시
                    showErrorDialog = effect.message
                }
                is PasswordResetContract.Effect.ShowSuccessDialog -> {
                    showSuccessDialog = true
                }
            }
        }
    }

    // 이메일 필드에 자동 포커스
    LaunchedEffect(Unit) {
        delay(300)
        emailFocusRequester.requestFocus()
    }

    PasswordResetContent(
        state = state,
        onIntent = viewModel::sendIntent,
        emailFocusRequester = emailFocusRequester,
        onNavigateBack = onNavigateBack
    )

    // 성공 다이얼로그 (old 프로젝트와 동일: "비밀번호 안내 메일이 발송되었습니다" 표시 후 뒤로가기)
    // old 프로젝트: Util.showDefaultIdolDialogWithBtn1에서 title = null로 호출
    // 확인 버튼 클릭 시: Util.closeIdolDialog() 후 requireActivity().onBackPressed()
    // old 프로젝트: setCancelable(false), setCanceledOnTouchOutside(false) 설정
    if (showSuccessDialog) {
        ExoDialog(
            message = stringResource(id = R.string.sent_find_pw_mail),
            onDismiss = {
                showSuccessDialog = false
            },
            onConfirm = {
                showSuccessDialog = false
                // old 프로젝트와 동일: 다이얼로그 확인 버튼 클릭 시 화면 pop
                onNavigateBack()
            },
            confirmButtonText = stringResource(id = R.string.confirm),
            dismissOnBackPress = false, // old 프로젝트: setCancelable(false)
            dismissOnClickOutside = false // old 프로젝트: setCanceledOnTouchOutside(false)
        )
    }

    // 에러 다이얼로그 (old 프로젝트와 동일: Util.showDefaultIdolDialogWithBtn1 사용)
    // old 프로젝트: setCancelable(false), setCanceledOnTouchOutside(false) 설정
    showErrorDialog?.let { errorMessage ->
        ExoDialog(
            message = errorMessage,
            onDismiss = {
                showErrorDialog = null
            },
            onConfirm = {
                showErrorDialog = null
            },
            confirmButtonText = stringResource(id = R.string.confirm),
            dismissOnBackPress = false, // old 프로젝트: setCancelable(false)
            dismissOnClickOutside = false // old 프로젝트: setCanceledOnTouchOutside(false)
        )
    }

    // 로딩 인디케이터
    LoadingOverlay(isLoading = state.isLoading)
}

/**
 * PasswordResetScreen의 UI 컨텐츠 (Stateless).
 */
@Composable
private fun PasswordResetContent(
    state: PasswordResetContract.State,
    onIntent: (PasswordResetContract.Intent) -> Unit,
    emailFocusRequester: FocusRequester,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hideKeyboard = {
        KeyboardUtil.hideKeyboard(context)
    }

    ExoScaffold(
        topBar = {
            net.ib.mn.ui.components.ExoAppBar(
                title = stringResource(id = R.string.title_change_passwd),
                onNavigationClick = {
                    hideKeyboard()
                    onNavigateBack()
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    hideKeyboard()
                }
        ) {
            // 스크롤 가능한 폼 영역
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp)
                    .padding(bottom = 86.dp) // 버튼 높이 40dp + 패딩 46dp
            ) {
                // 이메일 라벨 (old: TextView - hint_email, 14sp, bold, marginStart 4dp)
                Text(
                    text = stringResource(id = R.string.hint_email),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.text_default),
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(5.dp))

                // 이메일 입력 필드 (old: EditText - height 36dp, marginTop 5dp, paddingStart/End 10dp, 
                // background bg_background_button_border_round, textSize 12sp, inputType textEmailAddress)
                ExoTextField(
                    value = state.email,
                    onValueChange = { onIntent(PasswordResetContract.Intent.EmailChanged(it)) },
                    placeholder = "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .focusRequester(emailFocusRequester),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            hideKeyboard()
                            onIntent(PasswordResetContract.Intent.Submit)
                        }
                    ),
                    isValid = if (state.emailError == null && state.email.isNotEmpty()) {
                        state.isEmailValid
                    } else null,
                    errorMessage = state.emailError,
                    onFocusChanged = { focusState: androidx.compose.ui.focus.FocusState ->
                        onIntent(PasswordResetContract.Intent.EmailFocusChanged(focusState.isFocused))
                    }
                )
            }

            // 하단 고정 버튼 (키보드가 올라왔을 때 함께 올라오도록)
            ExoStatusButton(
                text = stringResource(id = R.string.confirm),
                onClick = {
                    hideKeyboard()
                    onIntent(PasswordResetContract.Intent.Submit)
                },
                enabled = state.email.isNotEmpty() &&
                         state.emailError == null &&
                         state.isEmailValid, // 이메일 검증이 완료되었을 때만 활성화
                isLoading = state.isLoading,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Light Mode",
    showSystemUi = true,
    showBackground = true,
    locale = "ko"
)
@Composable
fun PasswordResetScreenPreviewLight() {
    net.ib.mn.ui.theme.ExodusTheme(darkTheme = false) {
        PasswordResetContent(
            state = PasswordResetContract.State(),
            onIntent = {},
            emailFocusRequester = remember { FocusRequester() },
            onNavigateBack = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dark Mode",
    showSystemUi = true,
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    locale = "ko"
)
@Composable
fun PasswordResetScreenPreviewDark() {
    net.ib.mn.ui.theme.ExodusTheme(darkTheme = true) {
        PasswordResetContent(
            state = PasswordResetContract.State(
                email = "user@example.com",
                isEmailValid = true
            ),
            onIntent = {},
            emailFocusRequester = remember { FocusRequester() },
            onNavigateBack = {}
        )
    }
}
