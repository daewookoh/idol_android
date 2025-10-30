package net.ib.mn.presentation.signup

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.title_agreement),
                onNavigationClick = onNavigateBack
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
            Button(
                onClick = { onIntent(SignUpContract.Intent.ProceedToSignUpForm) },
                enabled = state.canProceedStep1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.canProceedStep1) colorResource(id = R.color.main) else colorResource(id = R.color.gray200),
                    contentColor = Color.White,
                    disabledContainerColor = colorResource(id = R.color.gray200),
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_next),
                    fontSize = 14.sp
                )
            }
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

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.title_signup),
                onNavigationClick = onNavigateBack
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // 이메일 입력 (SNS 로그인의 경우 비활성화)
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
                enabled = state.preFilledEmail == null,
                isError = state.emailError != null,
                errorMessage = state.emailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
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

            // 비밀번호 입력 (SNS 로그인의 경우 비활성화)
            if (state.preFilledPassword == null) {
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
                    isError = state.passwordError != null,
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
                    isError = state.passwordConfirmError != null,
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
                isError = state.nicknameError != null,
                errorMessage = state.nicknameError,
                imeAction = ImeAction.Next,
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
                imeAction = ImeAction.Done,
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

            Spacer(modifier = Modifier.height(32.dp))

            // 가입하기 버튼
            Button(
                onClick = { onIntent(SignUpContract.Intent.SignUp) },
                enabled = state.canProceedStep2 && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.canProceedStep2) colorResource(id = R.color.main) else colorResource(id = R.color.gray200),
                    contentColor = Color.White,
                    disabledContainerColor = colorResource(id = R.color.gray200),
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.btn_signup_finish),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
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
