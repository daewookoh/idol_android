package net.ib.mn.presentation.login

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import net.ib.mn.util.ToastUtil
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
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.google.firebase.messaging.FirebaseMessaging
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
import net.ib.mn.ui.components.ExoDialog
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.LoadingOverlay
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
    
    // 푸시 알림 권한 요청 Launcher (Old 프로젝트의 checkNotificationPermission과 동일)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("LoginScreen", "Notification permission result: $isGranted")
        // Old 프로젝트: 권한이 허용되면 FCM 토큰을 가져와서 저장
        // GcmUtils.registerDevice()와 동일한 로직
        if (isGranted) {
            android.util.Log.d("LoginScreen", "Notification permission granted, registering FCM token")
            try {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    android.util.Log.d("LoginScreen", "FCM token received: ${token?.take(20)}...")
                    if (token != null) {
                        coroutineScope.launch {
                            viewModel.registerFcmToken(token)
                        }
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("LoginScreen", "Failed to get FCM token", e)
                }
            } catch (e: IllegalStateException) {
                android.util.Log.e("LoginScreen", "IllegalStateException while getting FCM token", e)
            }
        } else {
            android.util.Log.d("LoginScreen", "Notification permission denied")
        }
    }

    // ============================================================
    // Google 로그인 권한 요청 (Old 프로젝트의 GET_ACCOUNTS 권한 체크)
    // ============================================================

    // Google 권한 설명 다이얼로그 표시 여부
    var showGooglePermissionDialog by remember { mutableStateOf(false) }
    
    // 에러 다이얼로그 표시 여부 및 메시지
    var showErrorDialog by remember { mutableStateOf<String?>(null) }

    // Google Sign-In Launcher (old 프로젝트와 동일: resultCode 체크하지 않음)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val GOOGLE_LOGIN_TAG = "GOOGLE_LOGIN"
        android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "Google sign-in result received")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "  resultCode: ${result.resultCode}")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "  data: ${result.data}")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")

        // old 프로젝트와 동일: resultCode 체크하지 않고 data != null만 체크
        if (result.data != null) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)

                android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "Google account received")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "  email: ${account.email}")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "  displayName: ${account.displayName}")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "  idToken: ${account.idToken?.take(20)}...")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")

                // Google 로그인 성공
                // Old 프로젝트와 동일: email이 필수, 없으면 에러 처리
                val email = account.email
                if (email.isNullOrEmpty()) {
                    android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                    android.util.Log.e(GOOGLE_LOGIN_TAG, "Google account email is null or empty")
                    android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                    viewModel.handleSnsLoginError(context.getString(R.string.facebook_no_email))
                } else {
                    // Old 프로젝트: GoogleAuthUtil.getToken()으로 access token 가져오기
                    // 백그라운드 스레드에서 실행
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "Getting Google access token via GoogleAuthUtil.getToken()")
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "  email: $email")
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "  account: ${account.account}")
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                            
                            val accessToken = try {
                                GoogleAuthUtil.getToken(
                                    context,
                                    account.account!!,
                                    "oauth2:https://www.googleapis.com/auth/plus.me"
                                )
                            } catch (e: UserRecoverableAuthException) {
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "UserRecoverableAuthException")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "  message: ${e.message}")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                                // Old 프로젝트: 에러 발생 시 토스트만 표시하고 계속 진행
                                null
                            } catch (e: GoogleAuthException) {
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "GoogleAuthException")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "  message: ${e.message}")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                                null
                            } catch (e: IOException) {
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "IOException")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "  message: ${e.message}")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                                null
                            }
                            
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "Google access token retrieved")
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "  accessToken: ${accessToken?.take(20)}...")
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                            
                            // Old 프로젝트: access token이 없으면 idToken 사용 (fallback)
                            val tokenToUse = accessToken ?: account.idToken
                            
                            if (tokenToUse == null) {
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "Both access token and idToken are null")
                                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                                withContext(Dispatchers.Main) {
                                    viewModel.handleSnsLoginError(context.getString(R.string.error_abnormal_exception))
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    viewModel.handleGoogleLoginResult(
                                        email = email,
                                        displayName = account.displayName,
                                        accessToken = tokenToUse
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                            android.util.Log.e(GOOGLE_LOGIN_TAG, "Exception while getting Google access token")
                            android.util.Log.e(GOOGLE_LOGIN_TAG, "  error: ${e.message}")
                            android.util.Log.e(GOOGLE_LOGIN_TAG, "  stackTrace:", e)
                            android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                            withContext(Dispatchers.Main) {
                                viewModel.handleSnsLoginError(context.getString(R.string.error_abnormal_exception))
                            }
                        }
                    }
                }
            } catch (e: ApiException) {
                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "Google sign-in ApiException")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "  statusCode: ${e.statusCode}")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "  message: ${e.message}")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                
                // Google 로그인 취소 시에도 에러 다이얼로그 표시
                if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "Google login cancelled by user")
                    viewModel.handleSnsLoginError(context.getString(R.string.line_login_failed))
                } else {
                    viewModel.handleSnsLoginError(context.getString(R.string.line_login_failed))
                }
            }
        } else {
            // data가 null인 경우 (사용자가 취소) - 로그인 실패 다이얼로그 표시
            android.util.Log.w(GOOGLE_LOGIN_TAG, "========================================")
            android.util.Log.w(GOOGLE_LOGIN_TAG, "Google login cancelled - data is null")
            android.util.Log.w(GOOGLE_LOGIN_TAG, "========================================")
            viewModel.handleSnsLoginError(context.getString(R.string.line_login_failed))
        }
    }

    // GET_ACCOUNTS 권한 요청 Launcher (Android 6.0 이상에서만 필요)
    val googleAccountsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val GOOGLE_LOGIN_TAG = "GOOGLE_LOGIN"
        android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "GET_ACCOUNTS permission result: $isGranted")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
        // old 프로젝트: onPermissionDenied()가 빈 함수로, 권한이 없어도 구글 로그인 진행
        // 권한이 허용되든 거부되든 구글 로그인 진행
        android.util.Log.d(GOOGLE_LOGIN_TAG, "Starting Google sign-in (permission granted: $isGranted)")
        googleSignInClient.signOut() // 이전 세션 제거
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // Google 로그인 시작 함수 (권한 체크 포함)
    fun startGoogleSignIn() {
        val GOOGLE_LOGIN_TAG = "GOOGLE_LOGIN"
        
        // Android 6.0 (API 23) 이상에서만 런타임 권한 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.GET_ACCOUNTS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                // 권한이 이미 있으면 바로 Google 로그인 진행
                android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "GET_ACCOUNTS permission already granted, starting Google sign-in")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                googleSignInClient.signOut() // 이전 세션 제거
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            } else {
                // 권한이 없으면, 사용자가 이미 권한 선택 결과를 결정했는지 확인
                // shouldShowRequestPermissionRationale()가 false면:
                // 1. 권한이 허용된 경우 (이미 체크했으므로 여기서는 해당 없음)
                // 2. 사용자가 "다시 묻지 않음"을 선택한 경우
                // 이 경우 다이얼로그를 띄우지 않고 바로 권한 요청 진행
                val shouldShowRationale = if (activity != null) {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        activity!!,
                        Manifest.permission.GET_ACCOUNTS
                    )
                } else {
                    false
                }
                
                if (shouldShowRationale) {
                    // 사용자가 아직 권한을 거부하지 않았거나, 첫 요청인 경우 설명 다이얼로그 표시
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "Showing Google permission explanation dialog")
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "  shouldShowRationale: true")
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                    showGooglePermissionDialog = true
                } else {
                    // 사용자가 이미 권한 선택 결과를 결정한 경우 (다시 묻지 않음 선택)
                    // 다이얼로그 없이 바로 권한 요청 진행
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "User already made permission choice, skipping dialog")
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "  shouldShowRationale: false")
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "  Requesting permission directly")
                    android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                    googleAccountsPermissionLauncher.launch(Manifest.permission.GET_ACCOUNTS)
                }
            }
        } else {
            // Android 6.0 미만에서는 권한 요청 불필요 (매니페스트에 선언만 하면 됨)
            android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
            android.util.Log.d(GOOGLE_LOGIN_TAG, "Android version < M, permission not required, starting Google sign-in")
            android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
            googleSignInClient.signOut() // 이전 세션 제거
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    // Old 프로젝트: AuthActivity.onCreate()에서 checkNotificationPermission() 호출
    // Android 13 (TIRAMISU) 이상에서만 동작
    // 권한이 이미 허용된 경우 FCM 토큰을 즉시 가져와서 저장
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
                android.util.Log.d("LoginScreen", "Notification permission already granted, registering FCM token")
                // Old 프로젝트: 권한이 이미 허용되어 있으면 FCM 토큰을 즉시 가져와서 저장
                try {
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        android.util.Log.d("LoginScreen", "FCM token received: ${token?.take(20)}...")
                        if (token != null) {
                            coroutineScope.launch {
                                viewModel.registerFcmToken(token)
                            }
                        }
                    }.addOnFailureListener { e ->
                        android.util.Log.e("LoginScreen", "Failed to get FCM token", e)
                    }
                } catch (e: IllegalStateException) {
                    android.util.Log.e("LoginScreen", "IllegalStateException while getting FCM token", e)
                }
            }
        } else {
            android.util.Log.d("LoginScreen", "Android version < TIRAMISU, notification permission not required")
            // Android 13 미만에서는 권한이 필요 없으므로 FCM 토큰을 즉시 가져와서 저장
            try {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    android.util.Log.d("LoginScreen", "FCM token received: ${token?.take(20)}...")
                    if (token != null) {
                        coroutineScope.launch {
                            viewModel.registerFcmToken(token)
                        }
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("LoginScreen", "Failed to get FCM token", e)
                }
            } catch (e: IllegalStateException) {
                android.util.Log.e("LoginScreen", "IllegalStateException while getting FCM token", e)
            }
        }
    }

    // ============================================================
    // Activity Result Launchers
    // ============================================================

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

                        // Old 프로젝트: userId와 accessToken이 필수
                        val userId = lineProfile?.userId
                        val accessToken = lineCredential?.accessToken?.tokenString

                        if (userId.isNullOrEmpty() || accessToken.isNullOrEmpty()) {
                            android.util.Log.e("LoginScreen", "Line login failed - missing userId or accessToken")
                            viewModel.handleSnsLoginError(context.getString(R.string.line_login_failed))
                            return@rememberLauncherForActivityResult
                        }

                        viewModel.handleLineLoginResult(
                            userId = userId,
                            displayName = lineProfile?.displayName,
                            accessToken = accessToken
                        )
                    }
                    com.linecorp.linesdk.LineApiResponseCode.CANCEL -> {
                        android.util.Log.d("LoginScreen", "Line login cancelled")
                        // Line 로그인 취소는 에러로 표시하지 않음
                        viewModel.handleSnsLoginCancelled()
                    }
                    else -> {
                        android.util.Log.e("LoginScreen", "Line login failed - responseCode: ${lineResult.responseCode}")
                        viewModel.handleSnsLoginError(context.getString(R.string.line_login_failed))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginScreen", "Line sign-in failed", e)
                viewModel.handleSnsLoginError(context.getString(R.string.line_login_failed))
            }
        } else {
            // 사용자가 취소했거나 결과가 실패한 경우
            android.util.Log.d("LoginScreen", "Line login cancelled or failed - resultCode: ${result.resultCode}")
            viewModel.handleSnsLoginCancelled()
        }
    }

    // ============================================================
    // Facebook SDK 초기화 및 콜백 등록
    // ============================================================

    LaunchedEffect(Unit) {
        // Facebook SDK 완전 초기화 대기
        // IdolApplication에서 이미 초기화했지만, 완전히 초기화될 때까지 대기
        FacebookSdk.fullyInitialize()

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
                            viewModel.handleSnsLoginError(context.getString(R.string.facebook_no_email))
                            LoginManager.getInstance().logOut()
                            return@newMeRequest
                        }

                        val email = jsonObject.optString("email")
                        val name = jsonObject.optString("name")
                        val facebookIdStr = jsonObject.optString("id")
                        val facebookId = facebookIdStr.toLongOrNull()
                        val accessToken = loginResult.accessToken.token

                        if (email.isNullOrEmpty()) {
                            viewModel.handleSnsLoginError(context.getString(R.string.facebook_no_email))
                            LoginManager.getInstance().logOut()
                            return@newMeRequest
                        }

                        // Facebook 로그인 성공
                        viewModel.handleFacebookLoginResult(
                            email = email,
                            name = name,
                            facebookId = facebookId,
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
                    // Facebook 로그인 취소는 에러로 표시하지 않음
                }

                override fun onError(error: FacebookException) {
                    android.util.Log.e("LoginScreen", "Facebook login error", error)
                    viewModel.handleSnsLoginError(context.getString(R.string.line_login_failed))
                }
            })
    }

    // ============================================================
    // Effect 처리
    // ============================================================

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginContract.Effect.NavigateToStartUp -> {
                    onNavigateToMain()
                }
                is LoginContract.Effect.NavigateToMain -> {
                    onNavigateToMain()
                }
                is LoginContract.Effect.NavigateToEmailLogin -> {
                    onNavigateToEmailLogin()
                }
                is LoginContract.Effect.NavigateToSignUp -> {
                    android.util.Log.d("LoginScreen", "========================================")
                    android.util.Log.d("LoginScreen", "NavigateToSignUp effect received")
                    android.util.Log.d("LoginScreen", "  email: ${effect.email}")
                    android.util.Log.d("LoginScreen", "  password: ${effect.password.take(20)}...")
                    android.util.Log.d("LoginScreen", "  displayName: ${effect.displayName}")
                    android.util.Log.d("LoginScreen", "  domain: ${effect.domain}")
                    android.util.Log.d("LoginScreen", "  profileImageUrl: ${effect.profileImageUrl}")
                    android.util.Log.d("LoginScreen", "========================================")
                    android.util.Log.d("LoginScreen", "Calling onNavigateToSignUp...")
                    onNavigateToSignUp(
                        effect.email,
                        effect.password,
                        effect.displayName,
                        effect.domain,
                        effect.profileImageUrl
                    )
                    android.util.Log.d("LoginScreen", "onNavigateToSignUp called")
                }
                is LoginContract.Effect.StartSocialLogin -> {
                    when (effect.loginType) {
                        LoginContract.LoginType.KAKAO -> {
                            // Kakao 로그인 시작 (Context만 필요, Activity 불필요)
                            handleKakaoLogin(viewModel, context)
                        }
                        LoginContract.LoginType.GOOGLE -> {
                            // Google 로그인 시작 (권한 체크 후 진행)
                            val GOOGLE_LOGIN_TAG = "GOOGLE_LOGIN"
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "Starting Google login...")
                            android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                            startGoogleSignIn()
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
                                ToastUtil.show(context, R.string.msg_error_ok)
                            }
                        }
                        LoginContract.LoginType.EMAIL -> {
                            // Email 로그인은 별도 화면으로 이동
                        }
                    }
                }
                is LoginContract.Effect.ShowError -> {
                    // old 프로젝트와 동일: 에러를 다이얼로그로 표시
                    showErrorDialog = effect.message
                }
                is LoginContract.Effect.ShowToast -> {
                    ToastUtil.show(context, effect.message)
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
    
    // Google 권한 설명 다이얼로그 (old 프로젝트와 동일)
    if (showGooglePermissionDialog) {
        val permissionMessage = buildString {
            append(context.getString(R.string.permission_optional))
            append("\n\n")
            append(context.getString(R.string.permission_contact))
            append("\n\n")
            append(context.getString(R.string.permission_desc))
        }
        
        ExoDialog(
            message = permissionMessage,
            onDismiss = { showGooglePermissionDialog = false },
            onConfirm = {
                showGooglePermissionDialog = false
                // 확인 버튼 클릭 시 실제 권한 요청 (old 프로젝트와 동일)
                android.util.Log.d("LoginScreen", "User confirmed permission dialog, requesting GET_ACCOUNTS permission")
                googleAccountsPermissionLauncher.launch(Manifest.permission.GET_ACCOUNTS)
            },
            // old 프로젝트: setCancelable(false), setCanceledOnTouchOutside(false)
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    }
    
    // 에러 다이얼로그 (old 프로젝트의 showDefaultIdolDialogWithBtn1와 동일)
    showErrorDialog?.let { errorMessage ->
        ExoDialog(
            message = errorMessage,
            onDismiss = { showErrorDialog = null },
            onConfirm = { showErrorDialog = null }
        )
    }
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
                viewModel.handleSnsLoginError(context.getString(R.string.error_abnormal_exception))
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
                    viewModel.handleSnsLoginError(context.getString(R.string.error_abnormal_exception))
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
                viewModel.handleSnsLoginError(context.getString(R.string.error_abnormal_exception))
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
            viewModel.handleSnsLoginError(context.getString(R.string.error_abnormal_exception))
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
            viewModel.handleSnsLoginError(context.getString(R.string.error_abnormal_exception))
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
                    // 카카오 로그인 취소는 에러로 표시하지 않음
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
                viewModel.handleSnsLoginError(context.getString(R.string.line_login_failed))
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
        LoadingOverlay(isLoading = state.isLoading)
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
