package com.example.idol_android.presentation.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.idol_android.R

/**
 * Login 화면 (old 프로젝트의 SigninFragment).
 * 소셜 로그인(Kakao, Google, Line, Facebook) 및 Email 로그인을 제공.
 *
 * @param onNavigateToMain 메인 화면으로 이동 콜백
 * @param onNavigateToEmailLogin Email 로그인 화면으로 이동 콜백
 * @param viewModel LoginViewModel
 */
@Composable
fun LoginScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToEmailLogin: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginContract.Effect.NavigateToMain -> {
                    onNavigateToMain()
                }
                is LoginContract.Effect.NavigateToEmailLogin -> {
                    onNavigateToEmailLogin()
                }
                is LoginContract.Effect.StartSocialLogin -> {
                    // TODO: 실제 소셜 로그인 프로세스 시작
                    // Activity Result API를 사용하여 소셜 로그인 Intent 시작
                    Toast.makeText(
                        context,
                        "${effect.loginType.name} login coming soon",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is LoginContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is LoginContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background_100))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // Main Image
            Image(
                painter = painterResource(id = R.drawable.img_login_main),
                contentDescription = "Login Main Image",
                modifier = Modifier
                    .width(178.dp)
                    .height(142.dp),
                contentScale = ContentScale.FillBounds
            )

            Spacer(modifier = Modifier.height(22.dp))

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.img_login_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(30.dp),
                contentScale = ContentScale.FillBounds
            )

            Spacer(modifier = Modifier.height(75.dp))

            // "시작하기" 텍스트
            Text(
                text = "SNS로 간편하게 시작하기",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.text_default),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SNS 로그인 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kakao
                SocialLoginButton(
                    offImageRes = R.drawable.btn_login_sns_kakao_off,
                    onImageRes = R.drawable.btn_login_sns_kakao_on,
                    contentDescription = "Kakao Login",
                    onClick = { viewModel.sendIntent(LoginContract.Intent.LoginWithKakao) }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Line
                SocialLoginButton(
                    offImageRes = R.drawable.btn_login_sns_line_off,
                    onImageRes = R.drawable.btn_login_sns_line_on,
                    contentDescription = "Line Login",
                    onClick = { viewModel.sendIntent(LoginContract.Intent.LoginWithLine) }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Google
                SocialLoginButton(
                    offImageRes = R.drawable.btn_login_sns_google_off,
                    onImageRes = R.drawable.btn_login_sns_google_on,
                    contentDescription = "Google Login",
                    onClick = { viewModel.sendIntent(LoginContract.Intent.LoginWithGoogle) }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Facebook
                SocialLoginButton(
                    offImageRes = R.drawable.btn_login_sns_facebook_off,
                    onImageRes = R.drawable.btn_login_sns_facebook_on,
                    contentDescription = "Facebook Login",
                    onClick = { viewModel.sendIntent(LoginContract.Intent.LoginWithFacebook) }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Email 로그인 링크
            Text(
                text = "이메일로 로그인",
                fontSize = 13.sp,
                color = colorResource(id = R.color.text_gray),
                modifier = Modifier
                    .clickable {
                        viewModel.sendIntent(LoginContract.Intent.NavigateToEmailLogin)
                    }
                    .padding(10.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        // 로딩 인디케이터
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 소셜 로그인 버튼 컴포저블 (pressed 상태 지원).
 *
 * XML의 selector 기능을 Compose에서 구현:
 * - 버튼을 누르고 있을 때: onImageRes (예: btn_login_sns_kakao_on.png)
 * - 일반 상태: offImageRes (예: btn_login_sns_kakao_off.png)
 *
 * @param offImageRes 일반 상태 이미지 리소스 ID
 * @param onImageRes pressed 상태 이미지 리소스 ID
 * @param contentDescription 접근성을 위한 설명
 * @param onClick 클릭 콜백
 */
@Composable
private fun SocialLoginButton(
    offImageRes: Int,
    onImageRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        interactionSource = interactionSource
    ) {
        Image(
            painter = painterResource(id = if (isPressed) onImageRes else offImageRes),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}
