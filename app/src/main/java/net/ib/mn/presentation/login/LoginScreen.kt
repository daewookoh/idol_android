package net.ib.mn.presentation.login

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.linecorp.linesdk.Scope
import com.linecorp.linesdk.auth.LineAuthenticationParams
import com.linecorp.linesdk.auth.LineLoginApi
import kotlinx.coroutines.launch
import net.ib.mn.MainActivity
import net.ib.mn.R
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.theme.ExodusTheme
import net.ib.mn.util.Constants

/**
 * Context에서 Activity를 추출하는 확장 함수.
 * Compose에서 Context는 ContextWrapper로 감싸져 있을 수 있으므로,
 * baseContext를 재귀적으로 탐색하여 Activity를 찾음.
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Login 화면 (old 프로젝트의 SigninFragment + AuthActivity 통합).
 * 소셜 로그인(Kakao, Google, Line, Facebook) 및 Email 로그인을 제공.
 *
 * Old 프로젝트 대비 변경사항:
 * 1. Fragment -> Composable로 변경
 * 2. View Binding -> Compose State로 변경
 * 3. GoogleApiClient (deprecated) -> GoogleSignInClient로 변경
 * 4. 각 SNS SDK를 Compose-friendly하게 통합:
 *    - Google/Line: Activity Result API 사용 (rememberLauncherForActivityResult)
 *    - Kakao: Context만 사용 (Activity 불필요)
 *    - Facebook: Context에서 Activity 추출하여 사용 (findActivity() 확장 함수)
 * 5. Activity 참조가 필요한 경우 Context.findActivity() 확장 함수로 안전하게 추출
 *
 * @param onNavigateToMain 메인 화면으로 이동 콜백
 * @param onNavigateToEmailLogin Email 로그인 화면으로 이동 콜백
 * @param viewModel LoginViewModel
 */
@Composable
fun LoginScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToEmailLogin: () -> Unit,
    onNavigateToSignUp: (
        email: String,
        password: String,
        displayName: String?,
        domain: String,
        profileImageUrl: String?
    ) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()

    // ============================================================
    // SNS SDK 초기화
    // ============================================================

    // Facebook CallbackManager
    val callbackManager = remember {
        CallbackManager.Factory.create().also {
            MainActivity.callbackManager = it
        }
    }

    // Google Sign-In Options
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("444896554540-g8k5jvtnbme5fr00e2dp16a0evelif30.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // ============================================================
    // 푸시 알림 권한 요청 (Old 프로젝트의 checkNotificationPermission과 동일)
    // ============================================================
    
    // 푸시 알림 권한 요청 Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("LoginScreen", "Notification permission result: $isGranted")
    }

    // Old 프로젝트: AuthActivity.onCreate()에서 checkNotificationPermission() 호출
    // Android 13 (TIRAMISU) 이상에서만 동작
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                android.util.Log.d("LoginScreen", "Requesting notification permission")
                notificationPermissionLauncher.launch(permission)
            } else {
                android.util.Log.d("LoginScreen", "Notification permission already granted")
            }
        } else {
            android.util.Log.d("LoginScreen", "Android version < TIRAMISU, notification permission not required")
        }
    }

    // ============================================================
    // Activity Result Launchers
    // ============================================================

    // Google Sign-In Launcher (old 프로젝트와 동일: resultCode 체크하지 않음)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("LoginScreen", "Google result - resultCode: ${result.resultCode}, data: ${result.data}")

        // old 프로젝트와 동일: resultCode 체크하지 않고 data != null만 체크
        if (result.data != null) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)

                android.util.Log.d("LoginScreen", "Google account - email: ${account.email}, displayName: ${account.displayName}")

                // Google 로그인 성공
                viewModel.handleGoogleLoginResult(
                    email = account.email ?: "",
                    displayName = account.displayName,
                    idToken = account.idToken
                )
            } catch (e: ApiException) {
                android.util.Log.e("LoginScreen", "Google sign-in ApiException - statusCode: ${e.statusCode}, message: ${e.message}", e)
                viewModel.handleSnsLoginError("Google login failed: ${e.statusCode}")
                Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
            }
        } else {
            // data가 null인 경우 (사용자가 취소)
            android.util.Log.w("LoginScreen", "Google login cancelled - data is null")
            viewModel.handleSnsLoginError("Google login cancelled")
        }
    }

    // Line Sign-In Launcher
    val lineSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val lineResult = LineLoginApi.getLoginResultFromIntent(result.data)

                when (lineResult.responseCode) {
                    com.linecorp.linesdk.LineApiResponseCode.SUCCESS -> {
                        // Line 로그인 성공
                        val lineProfile = lineResult.lineProfile
                        val lineCredential = lineResult.lineCredential

                        viewModel.handleLineLoginResult(
                            userId = lineProfile?.userId ?: "",
                            displayName = lineProfile?.displayName,
                            accessToken = lineCredential?.accessToken?.tokenString ?: ""
                        )
                    }
                    com.linecorp.linesdk.LineApiResponseCode.CANCEL -> {
                        android.util.Log.d("LoginScreen", "Line login cancelled")
                        viewModel.handleSnsLoginError("Line login cancelled")
                    }
                    else -> {
                        viewModel.handleSnsLoginError("Line login failed")
                        Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginScreen", "Line sign-in failed", e)
                viewModel.handleSnsLoginError("Line login failed: ${e.message}")
                Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
            }
        } else {
            // 사용자가 취소했거나 결과가 실패한 경우
            viewModel.handleSnsLoginError("Line login cancelled or failed")
        }
    }

    // ============================================================
    // Facebook SDK 초기화 및 콜백 등록
    // ============================================================

    LaunchedEffect(Unit) {
        // Facebook SDK 초기화 (필요 시)
        if (!FacebookSdk.isInitialized()) {
            FacebookSdk.sdkInitialize(context)
        }

        // Facebook 로그아웃 (이전 세션 제거)
        LoginManager.getInstance().logOut()

        // Facebook 로그인 콜백 등록
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    android.util.Log.d("LoginScreen", "Facebook login success")

                    // Facebook Graph API로 사용자 정보 가져오기
                    val request = GraphRequest.newMeRequest(loginResult.accessToken) { jsonObject, _ ->
                        if (jsonObject == null) {
                            viewModel.handleSnsLoginError("Facebook: Cannot get user info")
                            Toast.makeText(context, R.string.facebook_no_email, Toast.LENGTH_SHORT).show()
                            LoginManager.getInstance().logOut()
                            return@newMeRequest
                        }

                        val email = jsonObject.optString("email")
                        val name = jsonObject.optString("name")
                        val accessToken = loginResult.accessToken.token

                        if (email.isNullOrEmpty()) {
                            viewModel.handleSnsLoginError("Facebook: No email provided")
                            Toast.makeText(context, R.string.facebook_no_email, Toast.LENGTH_SHORT).show()
                            LoginManager.getInstance().logOut()
                            return@newMeRequest
                        }

                        // Facebook 로그인 성공
                        viewModel.handleFacebookLoginResult(
                            email = email,
                            name = name,
                            accessToken = accessToken
                        )
                    }

                    val parameters = android.os.Bundle()
                    parameters.putString("fields", "id,name,email")
                    request.parameters = parameters
                    request.executeAsync()
                }

                override fun onCancel() {
                    android.util.Log.d("LoginScreen", "Facebook login cancelled")
                    viewModel.handleSnsLoginError("Facebook login cancelled")
                }

                override fun onError(error: FacebookException) {
                    android.util.Log.e("LoginScreen", "Facebook login error", error)
                    viewModel.handleSnsLoginError("Facebook login error: ${error.message}")
                    Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ============================================================
    // Effect 처리
    // ============================================================

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginContract.Effect.NavigateToMain -> {
                    onNavigateToMain()
                }
                is LoginContract.Effect.NavigateToEmailLogin -> {
                    onNavigateToEmailLogin()
                }
                is LoginContract.Effect.NavigateToSignUp -> {
                    onNavigateToSignUp(
                        effect.email,
                        effect.password,
                        effect.displayName,
                        effect.domain,
                        effect.profileImageUrl
                    )
                }
                is LoginContract.Effect.StartSocialLogin -> {
                    when (effect.loginType) {
                        LoginContract.LoginType.KAKAO -> {
                            // Kakao 로그인 시작 (Context만 필요, Activity 불필요)
                            handleKakaoLogin(viewModel, context)
                        }
                        LoginContract.LoginType.GOOGLE -> {
                            // Google 로그인 시작 (Activity Result API 사용)
                            android.util.Log.d("LoginScreen", "Starting Google login...")
                            googleSignInClient.signOut() // 이전 세션 제거
                            val signInIntent = googleSignInClient.signInIntent
                            android.util.Log.d("LoginScreen", "Launching Google sign-in intent")
                            googleSignInLauncher.launch(signInIntent)
                        }
                        LoginContract.LoginType.LINE -> {
                            // Line 로그인 시작 (Activity Result API 사용)
                            val loginIntent = LineLoginApi.getLoginIntent(
                                context,
                                Constants.CHANNEL_ID,
                                LineAuthenticationParams.Builder()
                                    .scopes(listOf(Scope.PROFILE))
                                    .build()
                            )
                            lineSignInLauncher.launch(loginIntent)
                        }
                        LoginContract.LoginType.FACEBOOK -> {
                            // Facebook 로그인 시작 (Activity 필요)
                            activity?.let { act ->
                                LoginManager.getInstance().logIn(
                                    act,
                                    listOf("email"),
                                    null // loggerID
                                )
                            } ?: run {
                                android.util.Log.e("LoginScreen", "Activity not found for Facebook login")
                                Toast.makeText(context, R.string.msg_error_ok, Toast.LENGTH_SHORT).show()
                            }
                        }
                        LoginContract.LoginType.EMAIL -> {
                            // Email 로그인은 별도 화면으로 이동
                        }
                    }
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

    // ============================================================
    // UI
    // ============================================================

    LoginContent(
        state = state,
        onIntent = viewModel::sendIntent
    )
}

/**
 * Kakao 로그인 처리.
 * Old 프로젝트의 requestKakaoLogin() 로직을 Compose로 변환.
 * 
 * Old 프로젝트 로직:
 * 1. 카카오톡 설치 여부 확인
 * 2. 카카오톡이 있으면 카카오톡으로 로그인 시도
 *    - 취소된 경우: 에러 처리 (로딩 상태 해제)
 *    - 그 외 에러: 자동으로 카카오 계정으로 재시도
 * 3. 카카오톡이 없으면 바로 카카오 계정으로 로그인
 * 4. 로그인 성공 시 requestKakaoMe 호출하여 사용자 정보 가져오기
 * 
 * Kakao SDK는 Context만 있으면 동작하므로 Activity가 필수는 아님.
 */
private fun handleKakaoLogin(
    viewModel: LoginViewModel,
    context: Context
) {
    val TAG = "KAKAO_LOGIN"
    
    android.util.Log.d(TAG, "========================================")
    android.util.Log.d(TAG, "handleKakaoLogin() called")
    android.util.Log.d(TAG, "========================================")
    
    // requestKakaoMe: 사용자 정보 가져오기 (Old 프로젝트의 requestKakaoMe와 동일)
    fun requestKakaoMe(accessToken: String) {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "requestKakaoMe() called")
        android.util.Log.d(TAG, "  accessToken: ${accessToken.take(20)}...")
        android.util.Log.d(TAG, "========================================")
        
        UserApiClient.instance.me { user, meError ->
            if (meError != null) {
                android.util.Log.e(TAG, "========================================")
                android.util.Log.e(TAG, "Kakao me() ERROR")
                android.util.Log.e(TAG, "  error: ${meError.message}")
                android.util.Log.e(TAG, "  error class: ${meError.javaClass.simpleName}")
                android.util.Log.e(TAG, "  stackTrace:", meError)
                android.util.Log.e(TAG, "========================================")
                viewModel.handleSnsLoginError("Kakao user info error: ${meError.message}")
                Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
            } else if (user != null) {
                android.util.Log.d(TAG, "========================================")
                android.util.Log.d(TAG, "Kakao me() SUCCESS")
                android.util.Log.d(TAG, "  userId: ${user.id}")
                android.util.Log.d(TAG, "  nickname: ${user.kakaoAccount?.profile?.nickname}")
                android.util.Log.d(TAG, "  profileImageUrl: ${user.kakaoAccount?.profile?.thumbnailImageUrl}")
                android.util.Log.d(TAG, "  kakaoAccount: ${user.kakaoAccount != null}")
                android.util.Log.d(TAG, "========================================")
                
                // Old 프로젝트: user.id가 null이면 에러 처리
                // val id = user.id
                // mEmail = "$id${Const.POSTFIX_KAKAO}"
                val userId = user.id
                if (userId == null) {
                    android.util.Log.e(TAG, "========================================")
                    android.util.Log.e(TAG, "Kakao user.id is null")
                    android.util.Log.e(TAG, "========================================")
                    viewModel.handleSnsLoginError("Kakao user ID is null")
                    Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
                    return@me
                }
                
                // Kakao 로그인 성공 (Old 프로젝트의 requestKakaoMe 로직과 동일)
                android.util.Log.d(TAG, "Calling viewModel.handleKakaoLoginResult()")
                viewModel.handleKakaoLoginResult(
                    userId = userId,
                    nickname = user.kakaoAccount?.profile?.nickname,
                    profileImageUrl = user.kakaoAccount?.profile?.thumbnailImageUrl,
                    accessToken = accessToken
                )
                android.util.Log.d(TAG, "viewModel.handleKakaoLoginResult() called")
            } else {
                android.util.Log.e(TAG, "========================================")
                android.util.Log.e(TAG, "Kakao me() ERROR: user is null")
                android.util.Log.e(TAG, "  meError: null")
                android.util.Log.e(TAG, "  user: null")
                android.util.Log.e(TAG, "========================================")
                viewModel.handleSnsLoginError("Kakao user info is null")
                Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 카카오 계정 로그인 콜백 (Old 프로젝트의 onErrorResumeNext 로직과 동일)
    val kakaoAccountCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "kakaoAccountCallback() called")
        android.util.Log.d(TAG, "  token: ${if (token != null) "present" else "null"}")
        android.util.Log.d(TAG, "  error: ${if (error != null) error.message else "null"}")
        android.util.Log.d(TAG, "========================================")
        
        if (error != null) {
            android.util.Log.e(TAG, "========================================")
            android.util.Log.e(TAG, "Kakao Account Login ERROR")
            android.util.Log.e(TAG, "  error message: ${error.message}")
            android.util.Log.e(TAG, "  error class: ${error.javaClass.simpleName}")
            android.util.Log.e(TAG, "  stackTrace:", error)
            android.util.Log.e(TAG, "========================================")
            viewModel.handleSnsLoginError("Kakao login error: ${error.message}")
            Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
        } else if (token != null) {
            android.util.Log.d(TAG, "========================================")
            android.util.Log.d(TAG, "Kakao Account Login SUCCESS")
            android.util.Log.d(TAG, "  accessToken: ${token.accessToken.take(20)}...")
            android.util.Log.d(TAG, "  refreshToken: ${token.refreshToken?.take(20)}...")
            android.util.Log.d(TAG, "  idToken: ${token.idToken?.take(20)}...")
            android.util.Log.d(TAG, "========================================")
            android.util.Log.d(TAG, "Calling requestKakaoMe()")
            requestKakaoMe(token.accessToken)
        } else {
            android.util.Log.e(TAG, "========================================")
            android.util.Log.e(TAG, "Kakao Account Login ERROR: Both token and error are null")
            android.util.Log.e(TAG, "========================================")
            viewModel.handleSnsLoginError("Kakao login failed: Unknown error")
            Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // 카카오톡 설치 여부 확인 (Old 프로젝트와 동일)
    val isKakaoTalkAvailable = UserApiClient.instance.isKakaoTalkLoginAvailable(context)
    android.util.Log.d(TAG, "========================================")
    android.util.Log.d(TAG, "Checking Kakao Talk availability")
    android.util.Log.d(TAG, "  isKakaoTalkLoginAvailable: $isKakaoTalkAvailable")
    android.util.Log.d(TAG, "========================================")
    
    if (isKakaoTalkAvailable) {
        // 카카오톡으로 로그인 시도 (Old 프로젝트의 loginWithKakaoTalk 로직과 동일)
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "Starting Kakao Talk login")
        android.util.Log.d(TAG, "========================================")
        
        UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
            android.util.Log.d(TAG, "========================================")
            android.util.Log.d(TAG, "Kakao Talk login callback received")
            android.util.Log.d(TAG, "  token: ${if (token != null) "present" else "null"}")
            android.util.Log.d(TAG, "  error: ${if (error != null) error.message else "null"}")
            android.util.Log.d(TAG, "========================================")
            
            if (error != null) {
                android.util.Log.e(TAG, "========================================")
                android.util.Log.e(TAG, "Kakao Talk Login ERROR")
                android.util.Log.e(TAG, "  error message: ${error.message}")
                android.util.Log.e(TAG, "  error class: ${error.javaClass.simpleName}")
                android.util.Log.e(TAG, "  is ClientError: ${error is ClientError}")
                if (error is ClientError) {
                    android.util.Log.e(TAG, "  error reason: ${error.reason}")
                    android.util.Log.e(TAG, "  error cause: ${error.reason.name}")
                }
                android.util.Log.e(TAG, "  stackTrace:", error)
                android.util.Log.e(TAG, "========================================")

                // Old 프로젝트: 취소된 경우 Single.error(error) 반환 (에러 처리)
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Kakao login CANCELLED by user")
                    android.util.Log.d(TAG, "  Stopping login flow")
                    android.util.Log.d(TAG, "========================================")
                    viewModel.handleSnsLoginError("Kakao login cancelled")
                    return@loginWithKakaoTalk
                }

                // Old 프로젝트: 그 외 에러는 카카오 계정으로 자동 재시도
                // "카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도"
                android.util.Log.d(TAG, "========================================")
                android.util.Log.d(TAG, "Kakao Talk login failed, trying Kakao Account login")
                android.util.Log.d(TAG, "  This is expected behavior when Kakao Talk account is not linked")
                android.util.Log.d(TAG, "========================================")
                UserApiClient.instance.loginWithKakaoAccount(context, callback = kakaoAccountCallback)
            } else if (token != null) {
                // Old 프로젝트: "로그인 성공." -> requestKakaoMe 호출
                android.util.Log.d(TAG, "========================================")
                android.util.Log.d(TAG, "Kakao Talk Login SUCCESS")
                android.util.Log.d(TAG, "  accessToken: ${token.accessToken.take(20)}...")
                android.util.Log.d(TAG, "========================================")
                android.util.Log.d(TAG, "Calling requestKakaoMe()")
                requestKakaoMe(token.accessToken)
            } else {
                android.util.Log.e(TAG, "========================================")
                android.util.Log.e(TAG, "Kakao Talk Login ERROR: Both token and error are null")
                android.util.Log.e(TAG, "========================================")
                viewModel.handleSnsLoginError("Kakao Talk login failed: Unknown error")
                Toast.makeText(context, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        // Old 프로젝트: 카카오톡이 없으면 바로 카카오 계정으로 로그인
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "Kakao Talk not available, using Kakao Account login")
        android.util.Log.d(TAG, "========================================")
        UserApiClient.instance.loginWithKakaoAccount(context, callback = kakaoAccountCallback)
    }
}

/**
 * Login 화면의 UI 컨텐츠 (Stateless).
 * 프리뷰 및 테스트를 위한 stateless composable.
 */
@Composable
private fun LoginContent(
    state: LoginContract.State,
    onIntent: (LoginContract.Intent) -> Unit
) {
    ExoScaffold {
        Box(
            modifier = Modifier.fillMaxSize()
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
                painter = painterResource(id = R.drawable.startup_logo),
                contentDescription = null,
                modifier = Modifier
                    .width(178.dp)
                    .height(142.dp),
                contentScale = ContentScale.FillBounds
            )

            Spacer(modifier = Modifier.height(22.dp))

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.img_login_logo),
                contentDescription = null,
                modifier = Modifier
                    .height(30.dp),
                contentScale = ContentScale.FillHeight
            )

            Spacer(modifier = Modifier.height(75.dp))

            // "시작하기" 텍스트
            Text(
                text = stringResource(id = R.string.login_desc),
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
                    onClick = { onIntent(LoginContract.Intent.LoginWithKakao) }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Line
                SocialLoginButton(
                    offImageRes = R.drawable.btn_login_sns_line_off,
                    onImageRes = R.drawable.btn_login_sns_line_on,
                    contentDescription = "Line Login",
                    onClick = { onIntent(LoginContract.Intent.LoginWithLine) }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Google
                SocialLoginButton(
                    offImageRes = R.drawable.btn_login_sns_google_off,
                    onImageRes = R.drawable.btn_login_sns_google_on,
                    contentDescription = "Google Login",
                    onClick = { onIntent(LoginContract.Intent.LoginWithGoogle) }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Facebook
                SocialLoginButton(
                    offImageRes = R.drawable.btn_login_sns_facebook_off,
                    onImageRes = R.drawable.btn_login_sns_facebook_on,
                    contentDescription = "Facebook Login",
                    onClick = { onIntent(LoginContract.Intent.LoginWithFacebook) }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Email 로그인 링크
            Text(
                text = stringResource(id = R.string.login_email),
                fontSize = 13.sp,
                color = colorResource(id = R.color.text_gray),
                modifier = Modifier
                    .clickable {
                        onIntent(LoginContract.Intent.NavigateToEmailLogin)
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
        } // Box
    } // ExoScaffold
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

// ============================================================
// Preview
// ============================================================

@Preview(
    name = "Light Mode",
    showSystemUi = true,
    showBackground = true,
    locale = "ko"
)
@Composable
fun LoginScreenPreviewLight() {
    ExodusTheme(darkTheme = false) {
        LoginContent(
            state = LoginContract.State(),
            onIntent = {}
        )
    }
}

@Preview(
    name = "Dark Mode",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ko"
)
@Composable
fun LoginScreenPreviewDark() {
    ExodusTheme(darkTheme = true) {
        LoginContent(
            state = LoginContract.State(),
            onIntent = {}
        )
    }
}
