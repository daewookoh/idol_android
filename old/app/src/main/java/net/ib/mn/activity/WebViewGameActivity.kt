/**
 * Copyright (C) 2025. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Daewoo Koh daewoo@myloveidol.com
 * Description: 웹뷰 기반 미니게임 액티비티
 * 
 * 주요 기능:
 * 1. 웹뷰를 통한 미니게임 실행
 * 2. 게임 시작/종료 처리 및 하트 결제/보상 시스템
 * 3. 하트 내역 조회 (MyheartHistoryFragment 연동)
 * 4. JavaScript와 Android 간 양방향 통신
 */

package net.ib.mn.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.core.data.repository.GameRepository
import net.ib.mn.core.domain.usecase.GetUserSelfUseCase
import androidx.activity.viewModels
import androidx.lifecycle.SavedStateHandle
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.domain.usecase.datastore.GetIsEnableVideoAdPrefsUseCase
import net.ib.mn.viewmodel.MyHeartInfoViewModel
import net.ib.mn.viewmodel.MyHeartInfoViewModelFactory
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.utils.GlobalVariable
import net.ib.mn.utils.RemoteConfigUtil
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsetsAndRequest
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.common.util.logD
import net.ib.mn.core.model.GameData
import net.ib.mn.utils.ErrorControl
import androidx.core.graphics.toColorInt
import net.ib.mn.BuildConfig
import net.ib.mn.common.extension.isNull
import androidx.fragment.app.FragmentTransaction
import net.ib.mn.fragment.MyheartHistoryFragment

@AndroidEntryPoint
class WebViewGameActivity : BaseActivity() {

    // ================================
    // UI 컴포넌트
    // ================================
    private lateinit var webView: WebView
    
    // ================================
    // 의존성 주입
    // ================================
    @Inject
    lateinit var gameRepository: GameRepository  // 게임 관련 API 호출용

    @Inject
    lateinit var getUserSelfUseCase: GetUserSelfUseCase  // 사용자 정보 조회용

    @Inject
    lateinit var usersRepository: UsersRepository  // 하트 내역 조회용

    @Inject
    lateinit var accountManager: IdolAccountManager  // 계정 관리용

    @Inject
    lateinit var getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase  // 비디오 광고 설정 조회용

    // MyHeartInfoViewModel (Fragment와 공유)
    private val myHeartInfoViewModel: MyHeartInfoViewModel by viewModels {
        MyHeartInfoViewModelFactory(this, SavedStateHandle(), usersRepository, accountManager, getIsEnableVideoAdPrefsUseCase)
    }
    
    
    // ================================
    // 게임 관련 상태 변수들
    // ================================
    private var rewardProcessed = false  // 보상 중복 처리 방지 플래그
    private var logHeartId: Int = 0  // 게임 결제 로그 ID
    private var games: List<GameData>? = GlobalVariable.RemoteConfig?.game?.games  // 게임 목록
    private var portalUrl: String? = GlobalVariable.RemoteConfig?.game?.portalUrl  // 포털 URL
    private var selectedGame: GameData? = null  // 현재 선택된 게임
    private var isSplashShowing = false  // 스플래시 표시 중 플래그
    

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view_game)
        
        // ================================
        // 초기 설정
        // ================================
        // RemoteConfig가 null인 경우 다시 가져오기
        RemoteConfigUtil.fetchRemoteConfigIfNull(this)

        // 상태바 설정
        configureStatusBar()
        
        // 시스템 바 영역 처리
        findViewById<View>(R.id.main).applySystemBarInsetsAndRequest()

        // ================================
        // WebView 초기화
        // ================================
        webView = findViewById(R.id.webView)

        // 게임 데이터 유효성 검사
        if (games.isNullOrEmpty() || portalUrl == null) {
            finish()
            return
        }

        // WebView 설정
        webView.webViewClient = createWebViewClient()
        configureWebViewSettings()
        
        // JavaScript 인터페이스 추가 (웹과 통신용)
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        
        // Set initial action bar title
        supportActionBar?.title = getString(R.string.menu_minigame)

        // Fragment BackStack 변경 감지하여 container visibility 관리
        supportFragmentManager.addOnBackStackChangedListener {
            val fragmentContainer = findViewById<View>(R.id.fragment_container)
            if (supportFragmentManager.backStackEntryCount == 0) {
                fragmentContainer.visibility = View.GONE
                // Fragment가 모두 닫히면 포털로 이동
                loadPage("PORTAL")
                showActionBar()
            }
        }

        loadPage("PORTAL")
    }

    // ================================
    // Activity 생명주기 관리
    // ================================
    override fun onBackPressed() {
        // 스플래시 표시 중에는 백버튼 무시
        if (isSplashShowing) {
            return
        }

        // Fragment가 표시되어 있는 경우 Fragment를 닫음
        // (container visibility는 OnBackStackChangedListener에서 자동으로 처리됨)
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return
        }

        if (selectedGame.isNull()) {
            // 현재 포털(게임리스트) 화면이면 액티비티 종료
            super.onBackPressed()
        } else {
            // 게임화면인 경우 포털로 이동 (액티비티는 종료하지 않음)
            loadPage("PORTAL")
        }
    }
    
    override fun onPause() {
        super.onPause()
        webView.onPause()
    }
    
    override fun onResume() {
        super.onResume()
        webView.onResume()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // WebView 메모리 누수 방지
        webView.apply {
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // 윈도우 포커스 시 시스템 바 영역 업데이트
            ViewCompat.requestApplyInsets(findViewById(R.id.main))
        }
    }
    
    // ================================
    // 페이지 로딩 및 게임 관리
    // ================================
    @SuppressLint("RestrictedApi")
    private fun loadPage(gameId: String){
        runOnUiThread {
            val splashImageView = findViewById<ImageView>(R.id.splashImage)
            supportActionBar?.title = getString(R.string.menu_minigame)

            if(gameId == "PORTAL"){
                // 포털 페이지 로드
                selectedGame = null
                webView.visibility = View.VISIBLE
                val finalUrl = addParamsToUrl(portalUrl!!)
                webView.loadUrl(finalUrl)
                showActionBar()
                return@runOnUiThread
            }

            // 게임 선택 및 로드
            selectedGame = games?.firstOrNull { it.gameId == gameId }

            if(selectedGame.isNull()){
                return@runOnUiThread
            }

            hideActionBar()

            // 1. 스플래시 이미지 표시 (2초간)
            isSplashShowing = true
            splashImageView.visibility = View.VISIBLE

            try {
                splashImageView.setBackgroundColor(selectedGame!!.splashBgColor.toColorInt())
            } catch (e: IllegalArgumentException) {
                // 잘못된 색상 형식 처리
                splashImageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }

            val splashImageUrl = selectedGame!!.splashImage

            com.bumptech.glide.Glide.with(this)
                .load(splashImageUrl)
                .fitCenter()
                .into(splashImageView)

            // 2. WebView를 숨기고 새 게임 URL 로드 시작 (백그라운드에서 로딩)
            webView.visibility = View.INVISIBLE
            val finalUrl = addParamsToUrl(selectedGame!!.url)
            webView.loadUrl(finalUrl)

            // 4. 2초 후 스플래시 숨기고 WebView 표시
            android.os.Handler(mainLooper).postDelayed({
                splashImageView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                isSplashShowing = false
            }, 2000)
        }
    }
    
    
    // ================================
    // UI 설정 및 유틸리티
    // ================================
    /**
     * 상태바 설정
     */
    private fun configureStatusBar() {
        // 상태바를 투명하게 설정
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        // 상태바 아이콘 색상 설정 (다크/라이트 모드에 따라)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            val isNightMode = Util.isUsingNightModeResources(this@WebViewGameActivity)
            isAppearanceLightStatusBars = !isNightMode
        }
    }
    
    /**
     * ActionBar 표시/숨김 함수 (애니메이션 비활성화)
     */
    @SuppressLint("RestrictedApi")
    private fun showActionBar() {
        supportActionBar?.setShowHideAnimationEnabled(false)
        supportActionBar?.show()
    }
    
    @SuppressLint("RestrictedApi")
    private fun hideActionBar() {
        supportActionBar?.setShowHideAnimationEnabled(false)
        supportActionBar?.hide()
    }
    
    /**
     * WebView 설정 구성
     */
    private fun configureWebViewSettings() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
    }
    
    /**
     * 커스텀 WebViewClient 생성
     */
    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                handleWebViewError(error?.description?.toString() ?: "Unknown error")
            }
            
            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                handleWebViewError("HTTP Error: ${errorResponse?.statusCode}")
            }
        }
    }
    
    /**
     * WebView 에러 처리
     */
    private fun handleWebViewError(error: String) {
        runOnUiThread {
            logD("WebViewGameActivity", "WebView Error: $error")
            // 에러 발생 시 웹뷰를 새로고침
            if (webView.url != null) {
                webView.reload()
            }
        }
    }
    
    /**
     * 네트워크 상태 확인
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnected == true
    }
    
    /**
     * URL에 유저 정보를 쿼리스트링으로 추가
     * 닉네임, 로케일, 테마, 빌드 플레이버 정보를 추가합니다.
     */
    private fun addParamsToUrl(url: String): String {
        val account = IdolAccount.getAccount(this)
        val nickname = account?.userName ?: ""

        // 로케일 설정
        val locale: String = when (val raw = resources.configuration.locales.get(0).toLanguageTag().lowercase()) {
            "ko", "ko-kr" -> "ko"
            "en", "en-us", "en-gb" -> "en"
            "ja", "jp", "ja-jp" -> "jp"
            "zh", "zh-cn" -> "zh-cn"
            "zh-tw" -> "zh-tw"
            else -> "en"
        }

        // 다크/라이트 테마 감지
        val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val theme = if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) "dark" else "light"

        val separator = if (url.contains("?")) "&" else "?"

        val params = mutableListOf<String>()
        if (nickname.isNotEmpty()) {
            params.add("nickname=${java.net.URLEncoder.encode(nickname, "UTF-8")}")
        }
        params.add("flavor=${BuildConfig.FLAVOR}")
        params.add("locale=$locale")
        params.add("theme=$theme")

        return "$url$separator${params.joinToString("&")}"
    }
    
    // ================================
    // JavaScript 인터페이스 (웹과 Android 간 통신)
    // ================================
    /**
     * WebView와 JavaScript 간의 통신을 위한 인터페이스
     * 웹에서 전송되는 메시지를 받아서 적절한 처리 함수를 호출합니다.
     */
    inner class WebAppInterface {
        
        @JavascriptInterface
        fun receiveMessage(message: String) {
            try {
                val jsonMessage = JSONObject(message)
                val type = jsonMessage.getString("type")
                
                logD("WebViewGameActivity", "Received message type: $type")
                
                when (type) {
                    "GAME_START_REQUEST" -> {
                        handleGameStartRequest()
                    }
                    "GAME_END" -> {
                        val data = jsonMessage.getJSONObject("data")
                        handleGameEnd(data)
                    }
                    "OPEN_GAME" -> {
                        val data = jsonMessage.getJSONObject("data")
                        handleOpenGame(data)
                    }
                    "OPEN_PORTAL" -> {
                        loadPage("PORTAL")
                    }
                    "GET_HEART_BALANCE" -> {
                        handleGetHeartBalance()
                    }
                    "BACK_BUTTON_PRESSED" -> {
                        handleBackButtonPressed()
                    }
                    "OPEN_MY_HEART_HISTORY" -> {
                        handleOpenMyHeartHistory()
                    }
                    else -> {
                        logD("WebViewGameActivity", "Unknown message type: $type")
                    }
                }
            } catch (e: JSONException) {
                logD("WebViewGameActivity", "Error parsing message: $message, Error: ${e.message}")
            } catch (e: Exception) {
                logD("WebViewGameActivity", "Unexpected error in receiveMessage: ${e.message}")
            }
        }
        
        // ================================
        // 게임 관련 메시지 처리
        // ================================
        private fun handleGameStartRequest() {
            // 보상 처리 플래그 리셋
            rewardProcessed = false
            
            runOnUiThread {
                showPaymentConfirmationDialog()
            }
        }
        
        private fun handleGameEnd(data: JSONObject) {
            try {
                val score = data.getInt("score")
                
                // 게임 종료 시 보상 처리
                processGameReward(score, logHeartId)
            } catch (e: JSONException) {
                logD("WebViewGameActivity", "Error parsing game end data: ${e.message}")
            }
        }

        private fun handleOpenGame(data: JSONObject) {
            try {
                val gameId = data.getString("gameId")

                if(gameId.isNull()){
                    return
                }

                loadPage(gameId)
            } catch (e: JSONException) {
                logD("WebViewGameActivity", "Error parsing open game data: ${e.message}")
            }
        }


        // ================================
        // 하트 잔액 조회
        // ================================
        private fun handleGetHeartBalance() {
            try {
                val account = IdolAccount.getAccount(this@WebViewGameActivity)
                val ts = account?.userModel?.ts ?: 0
                
                lifecycleScope.launch {
                    getUserSelfUseCase(ts).collect { result ->
                        if (result.success && result.data != null) {
                            val response = result.data!!
                            logD("WebViewGameActivity", response.toString())

                            val objectsArray = response.optJSONArray("objects")
                            var strongHeart = 0L
                            var weakHeart = 0L
                            if (objectsArray != null && objectsArray.length() > 0) {
                                val userObj = objectsArray.optJSONObject(0)
                                strongHeart = userObj?.optLong("strong_heart", 0) ?: 0
                                weakHeart = userObj?.optLong("weak_heart", 0) ?: 0
                            }
                            val totalHeart = strongHeart + weakHeart

                            logD("WebViewGameActivity", "Heart balance - Strong: $strongHeart, Weak: $weakHeart, Total: $totalHeart")
                            
                            sendHeartBalanceToWebView(totalHeart)
                        } else {
                            logD("WebViewGameActivity", "Failed to fetch user heart balance: ${result.message}")
                            // 캐시된 데이터로 폴백
                            val cachedTotalHeart = account?.heartCount ?: 0L
                            sendHeartBalanceToWebView(cachedTotalHeart)
                        }
                    }
                }
            } catch (e: Exception) {
                logD("WebViewGameActivity", "Error in handleGetHeartBalance: ${e.message}")
                // 캐시된 데이터로 폴백
                val account = IdolAccount.getAccount(this@WebViewGameActivity)
                val cachedTotalHeart = account?.heartCount ?: 0L
                sendHeartBalanceToWebView(cachedTotalHeart)
            }
        }
        
        private fun handleBackButtonPressed() {
            if(selectedGame.isNull()) {
                runOnUiThread {
                    finish()
                }
            } else {
                loadPage("PORTAL")
            }
        }
        
        private fun handleOpenMyHeartHistory() {
            logD("WebViewGameActivity", "OPEN_MY_HEART_HISTORY received")

            runOnUiThread {
                webView.visibility = View.INVISIBLE
                hideActionBar()

                // 하트 내역 데이터 로드
                myHeartInfoViewModel.getHeartData(this@WebViewGameActivity)

                val fragmentContainer = findViewById<View>(R.id.fragment_container)

                // Fragment container를 표시
                fragmentContainer.visibility = View.VISIBLE

                // MyheartHistoryFragment 생성 및 표시
                val fragment = MyheartHistoryFragment()
                val transaction = supportFragmentManager.beginTransaction()

                transaction
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
        
        // ================================
        // 게임 결제 및 보상 처리
        // ================================
        private fun showPaymentConfirmationDialog() {
            Util.showDefaultIdolDialogWithBtn2(
                this@WebViewGameActivity,
                getString(R.string.popup_game_start_title),
                getString(R.string.popup_game_start_desc, selectedGame!!.fee.toString()),
                { _ ->
                    Util.closeIdolDialog()
                    processHeartPayment()
                },
                { _ ->
                    Util.closeIdolDialog()
                    sendGameStartRejected()
                }
            )
        }
        
        private fun showInsufficientHeartsDialog() {
            runOnUiThread {
                Util.showChargeHeartDialog(this@WebViewGameActivity)
                sendActivateStartButton()
            }
        }
        
        private fun processHeartPayment(retryCount: Int = 0) {
            // 네트워크 상태 확인
            if (!isNetworkAvailable()) {
                logD("WebViewGameActivity", "No network available for heart payment")
                Toast.makeText(this@WebViewGameActivity, R.string.desc_failed_to_connect_internet, Toast.LENGTH_SHORT).show()
                sendGameStartRejected()
                return
            }
            
            logD("WebViewGameActivity", "Processing heart payment - Fee: ${selectedGame!!.fee}, Retry: $retryCount")
            
            lifecycleScope.launch {
                gameRepository.payGameFee(
                    heart = selectedGame!!.fee,
                    gameId = selectedGame!!.gameId,
                    listener = { response ->
                        if(response.optInt("gcode") == ErrorControl.ERROR_4020) {
                            showInsufficientHeartsDialog()
                            logD("WebViewGameActivity", "Heart payment failed")
                            return@payGameFee
                        }

                        logD("WebViewGameActivity", "Heart payment successful")
                        sendGameStartApproved()
                        logHeartId = response.getInt("id")
                    },
                    errorListener = { error ->
                        logD("WebViewGameActivity", "Heart payment failed: ${error.message}")
                        
                        // 재시도 로직 (최대 2회)
                        if (retryCount < 2) {
                            logD("WebViewGameActivity", "Retrying heart payment... (${retryCount + 1}/2)")
                            processHeartPayment(retryCount + 1)
                        } else {
                            logD("WebViewGameActivity", "Max retry attempts reached for heart payment")
                            sendGameStartRejected()
                            Toast.makeText(this@WebViewGameActivity, R.string.purchase_error, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
        
        private fun processGameReward(score: Int, logHeartId: Int, retryCount: Int = 0) {
            // 보상 중복 처리 방지
            if (rewardProcessed) {
                logD("WebViewGameActivity", "Reward already processed, skipping")
                return
            }

            if (score <= 0) {
                logD("WebViewGameActivity", "Invalid score: $score, skipping reward processing")
                return
            }
            
            // 네트워크 상태 확인
            if (!isNetworkAvailable()) {
                logD("WebViewGameActivity", "No network available, skipping reward processing")
                Toast.makeText(this@WebViewGameActivity, R.string.desc_failed_to_connect_internet, Toast.LENGTH_SHORT).show()
                return
            }
            
            // 보상 처리 플래그 설정
            rewardProcessed = true
            
            logD("WebViewGameActivity", "Processing game reward - Score: $score, Retry: $retryCount")
            
            lifecycleScope.launch {
                gameRepository.claimGameReward(
                    gameId = selectedGame!!.gameId,
                    score = score,
                    logHeartId = logHeartId,
                    listener = { response ->
                        // 서버에서 받은 보상 하트 수 추출
                        val rewardHeart = try {
                            response.optInt("reward_heart", 0)
                        } catch (e: Exception) {
                            logD("WebViewGameActivity", "Error parsing reward_heart: ${e.message}")
                            0
                        }
                        
                        logD("WebViewGameActivity", "Game reward processed successfully: $rewardHeart hearts")
                        showGameRewardBottomSheet(rewardHeart)
                    },
                    errorListener = { error ->
                        logD("WebViewGameActivity", "Game reward processing failed: ${error.message}")
                        
                        // 재시도 로직 (최대 3회)
                        if (retryCount < 3) {
                            logD("WebViewGameActivity", "Retrying game reward processing... (${retryCount + 1}/3)")
                            rewardProcessed = false // 재시도를 위해 플래그 리셋
                            processGameReward(score, logHeartId, retryCount + 1)
                        } else {
                            logD("WebViewGameActivity", "Max retry attempts reached, giving up")
                            Toast.makeText(this@WebViewGameActivity, R.string.purchase_error, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
        
        private fun showGameRewardBottomSheet(rewardHeart: Int) {
            runOnUiThread {
                val bottomSheet = RewardBottomSheetDialogFragment.newInstance(
                    RewardBottomSheetDialogFragment.FLAG_GAME_REWARD,
                    rewardHeart
                ) {
                    sendActivateStartButton()
                }
                bottomSheet.show(supportFragmentManager, "game_reward")
            }
        }
        
        // ================================
        // 웹으로 메시지 전송
        // ================================
        private fun sendGameStartApproved() {
            runOnUiThread {
                val script = """
                    window.postMessage(JSON.stringify({
                        type: 'GAME_START_APPROVED',
                        data: { timestamp: Date.now() }
                    }), '*');
                """.trimIndent()
                
                webView.evaluateJavascript(script, null)
            }
        }

        private fun sendGameStartRejected() {
            runOnUiThread {
                val script = """
                    window.postMessage(JSON.stringify({
                        type: 'GAME_START_REJECTED',
                        data: { 
                            timestamp: Date.now(),
                        }
                    }), '*');
                """.trimIndent()

                webView.evaluateJavascript(script, null)

                // 팝업이 닫힐 때 START 버튼 활성화 신호 전송
                sendActivateStartButton()
            }
        }
        
        private fun sendActivateStartButton() {
            runOnUiThread {
                val script = """
                    window.postMessage(JSON.stringify({
                        type: 'ACTIVATE_START_BUTTON',
                        data: { 
                            timestamp: Date.now()
                        }
                    }), '*');
                """.trimIndent()
                
                webView.evaluateJavascript(script, null)
            }
        }
        
        private fun sendHeartBalanceToWebView(totalHeart: Long) {
            runOnUiThread {
                val script = """
                    window.postMessage(JSON.stringify({
                        type: 'SET_HEART_BALANCE',
                        data: { 
                            totalHeart: $totalHeart,
                            timestamp: Date.now()
                        }
                    }), '*');
                """.trimIndent()
                
                webView.evaluateJavascript(script, null)
            }
        }
    }
}