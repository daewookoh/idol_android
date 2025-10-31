package net.ib.mn.presentation.signup

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.ib.mn.R
import net.ib.mn.navigation.Screen
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoTextField
import net.ib.mn.ui.components.ExoCheckBox
import net.ib.mn.ui.components.ExoCheckBoxWithDetail
import net.ib.mn.ui.components.ExoSimpleDialog
import net.ib.mn.ui.components.ExoStatusButton
import net.ib.mn.ui.theme.ExodusTheme
import net.ib.mn.util.AgreementUtil

/**
 * 회원가입 페이지 스크린
 *
 * old 프로젝트의 AgreementFragment + SignupFragment를 Compose로 구현
 *
 * @param email SNS 로그인에서 전달받은 이메일 (옵션)
 * @param password SNS 로그인에서 전달받은 비밀번호 (옵션)
 * @param displayName SNS 로그인에서 전달받은 표시 이름 (옵션)
 * @param domain 로그인 도메인 (google, kakao, line, facebook 등)
 * @param onSignUpComplete 회원가입 완료 콜백
 * @param onNavigateBack 뒤로 가기 콜백
 */
@Composable
fun SignUpPagesScreen(
    navController: NavHostController,
    email: String? = null,
    password: String? = null,
    displayName: String? = null,
    domain: String? = null,
    onSignUpComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // SNS 로그인에서 전달받은 파라미터를 SavedStateHandle에 저장
    // Navigation Compose의 navArgument는 자동으로 SavedStateHandle에 저장되지 않으므로 수동으로 저장
    LaunchedEffect(email, password, displayName, domain) {
        if (email != null || password != null || displayName != null || domain != null) {
            viewModel.handleIntent(
                SignUpContract.Intent.InitializeFromNavigation(
                    email = email,
                    password = password,
                    displayName = displayName,
                    domain = domain
                )
            )
        }
    }

    // Side Effects 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SignUpContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SignUpContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SignUpContract.Effect.NavigateToTermsOfService -> {
                    // 이용약관 웹뷰로 이동
                    val url = AgreementUtil.getTermsOfServiceUrl(context)
                    val title = context.getString(R.string.agreement1)
                    navController.navigate(Screen.WebView.createRoute(url, title))
                }
                is SignUpContract.Effect.NavigateToPrivacyPolicy -> {
                    // 개인정보 처리방침 웹뷰로 이동
                    val url = AgreementUtil.getPrivacyPolicyUrl(context)
                    val title = context.getString(R.string.personal_agreement)
                    navController.navigate(Screen.WebView.createRoute(url, title))
                }
                is SignUpContract.Effect.NavigateToNextStep -> {
                    // 다음 단계로 이동 (State로 처리됨)
                }
                is SignUpContract.Effect.NavigateToPreviousStep -> {
                    // 이전 단계로 이동 (State로 처리됨)
                }
                is SignUpContract.Effect.NavigateToMain -> {
                    onSignUpComplete()
                }
            }
        }
    }

    // 백버튼 처리
    BackHandler {
        if (state.currentStep > 0) {
            viewModel.handleIntent(SignUpContract.Intent.GoBack)
        } else {
            onNavigateBack()
        }
    }

    // Step에 따라 다른 화면 표시
    when (state.currentStep) {
        0 -> AgreementPage(
            state = state,
            onIntent = viewModel::handleIntent,
            onNavigateBack = onNavigateBack
        )
        1 -> SignUpFormPage(
            state = state,
            onIntent = viewModel::handleIntent,
            onNavigateBack = {
                viewModel.handleIntent(SignUpContract.Intent.GoBack)
            }
        )
    }

    // 다이얼로그 표시
    state.dialogMessage?.let { message ->
        ExoSimpleDialog(
            message = message,
            onDismiss = { viewModel.handleIntent(SignUpContract.Intent.DismissDialog) }
        )
    }
}

/**
 * Step 1: 약관동의 페이지
 */
@Composable
internal fun AgreementPage(
    state: SignUpContract.State,
    onIntent: (SignUpContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // 키보드를 내리는 공통 함수
    val hideKeyboard = {
        focusManager.clearFocus()

        // Activity의 현재 포커스된 뷰에서 키보드 숨기기
        val activity = context as? android.app.Activity
        activity?.let {
            val imm = it.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager

            // 현재 포커스된 뷰가 있으면 그것을 사용
            val currentFocus = it.currentFocus
            if (currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            } else {
                // 포커스된 뷰가 없으면 DecorView를 사용
                val decorView = it.window.decorView
                imm.hideSoftInputFromWindow(decorView.windowToken, 0)
            }
        }
    }

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.title_agreement),
                onNavigationClick = {
                    hideKeyboard()
                    onNavigateBack()
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null, // 클릭 시 ripple 효과 제거
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    hideKeyboard()
                }
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // 전체 동의
            ExoCheckBox(
                checked = state.agreeAll,
                text = stringResource(R.string.agree_all),
                isMain = true,
                onCheckedChange = { onIntent(SignUpContract.Intent.UpdateAgreeAll(it)) }
            )

            // 구분선
            HorizontalDivider(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 8.dp)
                    .height(1.dp),
                color = colorResource(id = R.color.gray110)
            )

            // 이용약관 동의 (필수)
            val required = " (${stringResource(R.string.required)})"
            val termsText = buildAnnotatedString {
                append(stringResource(R.string.agreement1_agree))
                withStyle(style = SpanStyle(color = colorResource(id = R.color.main))) {
                    append(required)
                }
            }
            ExoCheckBoxWithDetail(
                checked = state.agreeTerms,
                text = termsText,
                onCheckedChange = { onIntent(SignUpContract.Intent.UpdateAgreeTerms(it)) },
                onDetailClick = { onIntent(SignUpContract.Intent.ShowTermsOfService) }
            )

            // 개인정보 처리방침 동의 (필수)
            val privacyText = buildAnnotatedString {
                append(stringResource(R.string.agreement2_agree))
                withStyle(style = SpanStyle(color = colorResource(id = R.color.main))) {
                    append(required)
                }
            }
            ExoCheckBoxWithDetail(
                checked = state.agreePrivacy,
                text = privacyText,
                onCheckedChange = { onIntent(SignUpContract.Intent.UpdateAgreePrivacy(it)) },
                onDetailClick = { onIntent(SignUpContract.Intent.ShowPrivacyPolicy) }
            )

            // 만 14세 이상 (필수)
            val ageText = buildAnnotatedString {
                append(stringResource(R.string.signup_age_limit))
                withStyle(style = SpanStyle(color = colorResource(id = R.color.main))) {
                    append(required)
                }
            }
            ExoCheckBox(
                checked = state.agreeAge,
                text = ageText,
                onCheckedChange = { onIntent(SignUpContract.Intent.UpdateAgreeAge(it)) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 다음 버튼
            ExoStatusButton(
                text = stringResource(R.string.btn_next),
                onClick = {
                    hideKeyboard()
                    onIntent(SignUpContract.Intent.ProceedToSignUpForm)
                },
                enabled = state.canProceedStep1
            )
        }
    }
}

/**
 * Step 2: 회원가입 폼 페이지
 */
@Composable
private fun SignUpFormPage(
    state: SignUpContract.State,
    onIntent: (SignUpContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // 키보드를 내리는 공통 함수
    val hideKeyboard = {
        focusManager.clearFocus()

        // Activity의 현재 포커스된 뷰에서 키보드 숨기기
        val activity = context as? android.app.Activity
        activity?.let {
            val imm = it.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager

            // 현재 포커스된 뷰가 있으면 그것을 사용
            val currentFocus = it.currentFocus
            if (currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            } else {
                // 포커스된 뷰가 없으면 DecorView를 사용
                val decorView = it.window.decorView
                imm.hideSoftInputFromWindow(decorView.windowToken, 0)
            }
        }
    }

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.title_signup),
                onNavigationClick = {
                    hideKeyboard()
                    onNavigateBack()
                }
            )
        }
    ) {
        // old 프로젝트: ViewCompat.setOnApplyWindowInsetsListener로 키보드 높이 감지
        // Compose: WindowInsets.ime를 사용하여 키보드가 올라오면 버튼이 함께 올라오도록 함
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null, // 클릭 시 ripple 효과 제거
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    hideKeyboard()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp)
                    .padding(bottom = 86.dp) // 버튼 높이 40dp + 패딩 46dp
            ) {
            // 이메일 입력 (SNS 로그인의 경우 숨김 - domain이 "email"이 아니면 SNS 로그인)
            if (state.domain == "email") {
                Text(
                    text = stringResource(R.string.id),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.text_default),
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(5.dp))

                ExoTextField(
                    value = state.email,
                    onValueChange = { onIntent(SignUpContract.Intent.UpdateEmail(it)) },
                    placeholder = stringResource(R.string.hint_email),
                    isValid = if (state.emailError != null) false else if (state.isEmailValid) true else null,
                    errorMessage = state.emailError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    onValueChangeDebounced = {
                        if (it.isNotEmpty()) {
                            onIntent(SignUpContract.Intent.ValidateEmail)
                        }
                    },
                    debounceMillis = 1000,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = stringResource(R.string.signup_desc1),
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.main)
                )

                Spacer(modifier = Modifier.height(11.dp))
            }

            // 비밀번호 입력 (SNS 로그인의 경우 숨김 - domain이 "email"이 아니면 SNS 로그인)
            if (state.domain == "email") {
                Text(
                    text = stringResource(R.string.hint_passwd),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.text_default),
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(5.dp))

                ExoTextField(
                    value = state.password,
                    onValueChange = { onIntent(SignUpContract.Intent.UpdatePassword(it)) },
                    isValid = if (state.passwordError != null) false else if (state.isPasswordValid) true else null,
                    errorMessage = state.passwordError,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    imeAction = ImeAction.Next,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = stringResource(R.string.pwd_combination),
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.main)
                )

                Spacer(modifier = Modifier.height(11.dp))

                // 비밀번호 확인
                Text(
                    text = stringResource(R.string.hint_passwd_confirm),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.text_default),
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(5.dp))

                ExoTextField(
                    value = state.passwordConfirm,
                    onValueChange = { onIntent(SignUpContract.Intent.UpdatePasswordConfirm(it)) },
                    isValid = if (state.passwordConfirmError != null) false else if (state.isPasswordConfirmValid) true else null,
                    errorMessage = state.passwordConfirmError,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    imeAction = ImeAction.Next,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )

                Spacer(modifier = Modifier.height(11.dp))
            }

            // 닉네임 입력
            Text(
                text = stringResource(R.string.hint_name),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.text_default),
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(5.dp))

            ExoTextField(
                value = state.nickname,
                onValueChange = { onIntent(SignUpContract.Intent.UpdateNickname(it)) },
                isValid = if (state.nicknameError != null) false else if (state.isNicknameValid) true else null,
                errorMessage = state.nicknameError,
                imeAction = ImeAction.Next,
                onValueChangeDebounced = {
                    if (it.isNotEmpty() && it.length >= 2) {
                        onIntent(SignUpContract.Intent.ValidateNickname)
                    }
                },
                debounceMillis = 1500,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )

            Spacer(modifier = Modifier.height(11.dp))

            // 추천인 코드 (옵션)
            Text(
                text = stringResource(R.string.recommender),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.text_default),
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(5.dp))

            ExoTextField(
                value = state.recommenderCode,
                onValueChange = { onIntent(SignUpContract.Intent.UpdateRecommenderCode(it)) },
                placeholder = stringResource(R.string.hint_recommender_1),
                isValid = if (state.recommenderError != null) false else if (state.isRecommenderValid) true else null,
                errorMessage = state.recommenderError,
                imeAction = ImeAction.Done,
                onValueChangeDebounced = {
                    if (it.isNotEmpty()) {
                        onIntent(SignUpContract.Intent.ValidateRecommenderCode)
                    }
                },
                debounceMillis = 1500,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = stringResource(R.string.signup_desc2_noreward),
                fontSize = 12.sp,
                color = colorResource(id = R.color.main)
            )
            } // 스크롤 가능한 폼 영역 끝

            // 하단 고정 버튼
            ExoStatusButton(
                text = stringResource(R.string.btn_signup_finish),
                onClick = {
                    hideKeyboard()
                    onIntent(SignUpContract.Intent.SignUp)
                },
                enabled = state.canProceedStep2,
                isLoading = state.isLoading,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } // Box 끝
    }
}

@Preview(
    name = "Agreement Page - Light Mode",
    showSystemUi = true,
    showBackground = true,
    locale = "ko"
)
@Composable
fun AgreementPagePreviewLight() {
    ExodusTheme(darkTheme = false) {
        AgreementPage(
            state = SignUpContract.State(),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Agreement Page - Dark Mode",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ko"
)
@Composable
fun AgreementPagePreviewDark() {
    ExodusTheme(darkTheme = true) {
        AgreementPage(
            state = SignUpContract.State(),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "SignUp Form Page - Light Mode",
    showSystemUi = true,
    showBackground = true,
    locale = "ko"
)
@Composable
fun SignUpFormPagePreviewLight() {
    ExodusTheme(darkTheme = false) {
        SignUpFormPage(
            state = SignUpContract.State(
                currentStep = 1,
                email = "",
                password = "",
                passwordConfirm = "",
                nickname = "",
                recommenderCode = ""
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "SignUp Form Page - Dark Mode",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ko"
)
@Composable
fun SignUpFormPagePreviewDark() {
    ExodusTheme(darkTheme = true) {
        SignUpFormPage(
            state = SignUpContract.State(
                currentStep = 1,
                email = "test@example.com",
                password = "password123",
                passwordConfirm = "password123",
                nickname = "테스트",
                recommenderCode = "ABC123",
                isEmailValid = true,
                isPasswordValid = true,
                isPasswordConfirmValid = true,
                isNicknameValid = true,
                canProceedStep2 = true
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
