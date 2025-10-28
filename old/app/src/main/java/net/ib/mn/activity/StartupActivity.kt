/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.activity

import android.app.ActivityManager
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.BillingClient.SkuType.SUBS
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.bumptech.glide.Glide
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.IdolApplication
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.billing.util.BillingManager
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.core.domain.usecase.GetUserSelfUseCase
import net.ib.mn.domain.usecase.DeleteAllAndSaveIdolsUseCase
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.domain.usecase.UpdateHeartAndTop3UseCase
import net.ib.mn.domain.usecase.UpsertIdolsWithTsUseCase
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.pushy.PushyUtil
import net.ib.mn.utils.*
import net.ib.mn.utils.ErrorControl.ERROR_8000
import net.ib.mn.utils.ErrorControl.ERROR_88888
import net.ib.mn.utils.Util.Companion.DEBUG
import net.ib.mn.utils.Util.Companion.PROPERTY_AD_ID
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.StartupViewModel
import okhttp3.internal.notify
import okhttp3.internal.notifyAll
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import net.ib.mn.utils.RemoteConfigUtil

/**
 * @see
 * */
@AndroidEntryPoint
class StartupActivity : BaseActivity() {

    // push test
    private var rlContainer: RelativeLayout? = null
    private var mProgress: ProgressBar? = null
    private var skuCode: String? = null
    private var check_IAB: Boolean = true
    private var mBillingManager: BillingManager? = null
    private val viewModel: StartupViewModel by viewModels()

    private var mThread: StartupThread? = null
    private var mAuthIntent: Intent? = null
    private var mIntroIntent: Intent? = null
    private var mNextIntent: Intent? = null
    private var isOpenedByDeeplink: Boolean = false
    private var mAccountLock: Any? = null
    private var tvTalkId: String? = null

    private var account: IdolAccount? = null
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var miscRepository: MiscRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    // 서버 전환시 바꿔줄 용도
    @Inject
    lateinit var deleteAllAndSaveIdolsUseCase: DeleteAllAndSaveIdolsUseCase
    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase
    @Inject
    lateinit var upsertIdolsWithTsUseCase: UpsertIdolsWithTsUseCase
    @Inject
    lateinit var updateHeartAndTop3UseCase: UpdateHeartAndTop3UseCase
    @Inject
    lateinit var favoritesRepository: FavoritesRepository
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var idolApiManager: IdolApiManager
    @Inject
    lateinit var getUserSelfUseCase: GetUserSelfUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        val darkMode = Util.getPreferenceInt(this, Const.KEY_DARKMODE, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(darkMode)

        super.onCreate(savedInstanceState)

        Logger.v("Screen", "--------start")

        // viewModel을 일찍 생성시키기 위해 위로 올림
        observeVM()

        // pushy
        if (BuildConfig.CHINA) {
            PushyUtil.registerDevice(this, null)
            ChinaUtil.initNativeX(this)
        }

        window.statusBarColor = resources.getColor(R.color.text_white_black, theme)
        if (Util.isUsingNightModeResources(this)) {
            window.decorView.systemUiVisibility = 0
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        Util.setPreference(this, Const.AWARD_RANKING, 0L)
        Util.setPreference(this, Const.IS_VIDEO_SOUND_ON, false)

        // DB 초기화
//        IdolDB.getInstance(this)

        // KakaoSDK init을 위해 IdolApplication 인스턴스를 생성
        IdolApplication.getInstance(this)

        setContentView(R.layout.activity_startup)

        Util.log("device id >> " + Util.getDeviceUUID(this))

        mProgress = findViewById(R.id.progress)
        rlContainer = findViewById(R.id.rl_container)
        rlContainer?.applySystemBarInsets()

        account = IdolAccount.getAccount(this)
        mAccountLock = Any()
        val intent = intent
        mAuthIntent = AuthActivity.createIntent(this)

        // Figure out what to do based on the intent type
        val flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK

        if (intent.type == null) {
            if (mNextIntent == null) {
                mNextIntent = MainActivity.createIntent(this, false)
            }

            val data: Uri? = intent.data
            if (data != null && data.isHierarchical) {
                val uri = intent.dataString
                if (uri != null) {
                    if (Uri.parse(uri).path == "/auth/request") {
                        mNextIntent?.apply {
                            putExtra("is_auth_request", true)
                            putExtra("uri", uri)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                    }
                }
            }
        } else if (intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (mNextIntent == null && account != null) {
                mNextIntent = if (account?.most == null) {
                    MainActivity.createIntent(this, true)
                } else {
                    WriteArticleActivity.createIntent(this, account?.most!!, sharedText ?: "")
                }
                mNextIntent?.flags = flags
                check_IAB = false
            }
        } else if (intent.type?.startsWith("image/") == true) {
            if (mNextIntent == null && account != null) {
                mNextIntent = if (account?.most == null) {
                    MainActivity.createIntent(this, true)
                } else {
                    WriteArticleActivity.createIntent(
                        this,
                        account?.most!!,
                        (intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: Uri.parse("")) as Uri
                    )
                }
                mNextIntent?.flags = flags
                check_IAB = false
            }
        } else {
            Util.log("share error >  ${intent.type}")
        }

        val animation = Util.containsPreference(this, Const.PREF_ANIMATION_MODE)
        if (!animation) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                Util.setPreference(this, Const.PREF_ANIMATION_MODE, true)
            } else {
                Util.setPreference(this, Const.PREF_ANIMATION_MODE, false)
            }
        }

        // 캐시 삭제
        cacheDelete()

        // 비디오 캐시 숨기기
        Util.log(if (Util.videoCacheHide(this)) "video cache folder ok" else "video cache folder false")

        startThread()

        val uri: Uri? = intent.data
        getReferrerMapFromUri(uri)

        var host = Util.getPreference(this, Const.PREF_SERVER_URL)
        if (!host.isNullOrEmpty()) {
            // retrofit
            ServerUrl.HOST = host

            // IdolApiManager 서버 변경 적용
            idolApiManager.reinit(
                deleteAllAndSaveIdolsUseCase, getIdolByIdUseCase, upsertIdolsWithTsUseCase, updateHeartAndTop3UseCase, favoritesRepository, idolsRepository
            )

            accountManager.reinit(getUserSelfUseCase)
        }

        // URL scheme으로 서버 변경
        if (Intent.ACTION_VIEW == intent.action) {
            val devScheme = if (BuildConfig.CELEB) "myloveactor" else "devloveidol"
            val scheme = if (BuildConfig.CELEB) "choeaedolceleb" else "choeaedol"
            if (devScheme.equals(intent.scheme, ignoreCase = true)) {

                host = uri?.getQueryParameter("host") ?: return

                if (!host.isNullOrEmpty()) {
                    if (!host.startsWith("http")) {
                        host = "http://$host"
                    }
                    // retrofit
                    ServerUrl.HOST = host

                    Util.setPreference(this, Const.PREF_SERVER_URL, ServerUrl.HOST)

                    // 변경된 호스트가 retrofit에 반영되도록 액티비티 재시작
                    val i = Intent(this, StartupActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(i)
                    finish()
                    return
                }

                val resetAuth = uri?.getQueryParameter("reset_auth")
                if (resetAuth.equals("true", ignoreCase = true)) {
                    IdolAccount.getAccount(this)?.let {
                        IdolAccount.removeAccount(this)
                    }
                }
            }
        }

        RemoteConfigUtil.fetchRemoteConfig(this, account)

        resetPreferenceMainScreen()
    }

    private fun observeVM() = with(viewModel) {
        recreateActivity.observe(this@StartupActivity, SingleEventObserver {
            recreate()
        })

        updateProgress.observe(this@StartupActivity, SingleEventObserver { progress ->
            mProgress?.progress = progress.toInt()
        })

        configLoadComplete.observe(this@StartupActivity, SingleEventObserver { success ->
            if(success) {
                goCompleteProcess()
            }
        })
    }

    override fun onPause() {
        super.onPause()
        if (mThread != null && mThread!!.isAlive) {
            mThread!!.setPause()
        }
    }

    override fun onResume() {
        super.onResume()

        setFirebaseScreenViewEvent(GaAction.SPLASH, localClassName)

        if (mThread != null && mThread!!.isAlive) {
            mThread!!.setResume()
        }
    }

    override fun onPostResume() {
        super.onPostResume()

        tvTalkId = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Const.PREF_TV_TALK_ID, "") ?: ""
        Util.log("tv id : $tvTalkId")

        // Detect VM/Rooting
        val vm = VMDetector.getInstance(this)
        vm.addPackageName("com.google.android.launcher.layouts.genymotion")
        vm.addPackageName("com.vphone.launcher")   // nox
        vm.addPackageName("com.bluestacks")    // bluestacks
        vm.addPackageName("com.bluestacks.appmart")    // bluestacks
        vm.addPackageName("com.bignox.app")    // nox
        vm.addPackageName("com.microvirt.launcher2")    // memu
        vm.addPackageName("com.microvirt.launcher")    // memu
        vm.addPackageName("com.android.emu.coreservice")   // momo

        val isVM = vm.isVM()
        val isRooted = vm.isRooted
        val isX86 = vm.isx86Port()
        val log = vm.toString()
        val hwInfo = vm.hWInfo

        if (isVM /*|| isRooted || isX86*/ && !DEBUG) {
            UtilK.postVMLog(this, lifecycleScope, miscRepository, log, Const.VM_DETECT_LOG)
            UtilK.postVMLog(this, lifecycleScope, miscRepository, hwInfo, Const.VM_DETECT_LOG)

            finish()
            return
        }
    }

    override fun onDestroy() {
        if (mThread != null && mThread!!.isAlive) {
            mThread!!.interrupt()
        }
        super.onDestroy()
    }

    // 백버튼 눌렀을 때 finish() 처리
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private inner class StartupThread : Thread() {
        private val mPauseLock: Any = Any()
        private var mPaused: Boolean = false

        override fun run() {
            try {
                var adInfo: AdvertisingIdClient.Info? = null
                try {
                    adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
                } catch (e: IOException) {
                    // Unrecoverable error connecting to Google Play services (e.g.,
                    // the old version of the service doesn't support getting AdvertisingId).
                } catch (e: GooglePlayServicesNotAvailableException) {
                    // Google Play services is not available entirely.
                } catch (e: GooglePlayServicesRepairableException) {
                    e.printStackTrace()
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }


                try {
                    val id = adInfo?.id
                    if (id != null) {
                        Util.setPreference(this@StartupActivity, PROPERTY_AD_ID, id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (account == null) {
                    account = IdolAccount.getAccount(this@StartupActivity)
                }

                // 스토어에서 저작권 문제로 앱이 삭제되어 다시 로그인 창이 먼저 나오게 처리.
                if (account == null) {
                    mNextIntent = if (intent.hasExtra(Const.IS_EMAIL_SIGNUP)) {
                        AuthActivity.createIntent(this@StartupActivity).apply {
                            putExtra(Const.IS_EMAIL_SIGNUP, "true") // 데이터를 추가
                        }
                    } else {
                        AuthActivity.createIntent(this@StartupActivity)
                    }
                    startNextActivity()
                    finish()
                } else {

                    // 220222 messages 추가로 8->9, CELEB: // 20230216 offerwall reward제거로 9->8
                    Util.setPreference(
                        this@StartupActivity,
                        Const.PREF_HEART_BOX_VIEWABLE,
                        true
                    ) // 앱을 실행할 때 true로 설정

                    viewModel.getInAppBanner(this@StartupActivity)

                    val fromIdolIntent = intent
                    val isFromIdol = fromIdolIntent.getBooleanExtra(
                        if (BuildConfig.CELEB) Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_CELEB else Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_IDOL,
                        false
                    )
                    val rewardTo =
                        if (BuildConfig.CELEB) Const.IDOL_IN_HOUSE_OFFER_WALL_ITEM_KEY else Const.CELEB_IN_HOUSE_OFFER_WALL_ITEM_KEY
                    val isBlockFirst = !Util.getPreferenceBool(
                        this@StartupActivity,
                        Const.USER_BLOCK_FIRST,
                        false
                    )
                    // 시작 화면 API를 전부 불러옵니다.
                    viewModel.getStartApi(
                        context = this@StartupActivity,
                        account = account,
                        isFromIdol = isFromIdol,
                        isUserBlockFirst = isBlockFirst,
                        to = rewardTo
                    )
                }
            } catch (e: InterruptedException) {
                return
            }
        }

        fun setPause() {
            synchronized(mPauseLock) {
                mPaused = true
            }
        }

        fun setResume() {
            synchronized(mPauseLock) {
                mPaused = false
                mPauseLock.notifyAll()
            }
        }


    }

    // 딥링크를 타고 들어갈 때, task 상 엑티비티 개수가 1이면 외부에서 링크를 탄 경우고,
// 1 초과이면, 내부에서 링크를 탄 경우라서, 아래처럼 체크하여 true/false를 반환하도록 함.
    private fun isDeepLinkClickFromIdol(): Boolean {
        val mngr = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val taskList = mngr.getRunningTasks(10)
        return try {
            taskList[0].numActivities > 1
        } catch (e: Exception) {
            false
        }
    }

    @Throws(InterruptedException::class)
    private fun checkSubscriptions() {
        // iOS 구독 사용자들에게 알림 메시지 주기
        account?.userModel?.let { userModel ->
            userModel.subscriptions?.let { subscriptions ->
                if (subscriptions.isNotEmpty()) {
                    val latch = CountDownLatch(subscriptions.size)

                    for (subscription in subscriptions) {
                        val jsonObject = JSONObject()
                        if (subscription.familyappId == 1) {
                            val pkgName =
                                if (BuildConfig.CELEB) "com.exodus.myloveactor" else "net.ib.mn"
                            if (packageName.equals(pkgName, ignoreCase = true)) {
                                try {
                                    jsonObject.put("orderId", subscription.orderId)
                                        .put("productId", subscription.skuCode)
                                        .put("packageName", subscription.packageName)
                                        .put("purchaseToken", subscription.purchaseToken)
                                    skuCode = subscription.skuCode

                                    val listener: (JSONObject) -> Unit = { response ->
                                        Util.log("checkSubscriptions() response=$response")
                                        // 데일리팩 하트 지급받는 경우 세팅
                                        account?.mDailyPackHeart =
                                            if (response.optInt("gcode") == 0) {
                                                response.optInt("heart")
                                            } else {
                                                0
                                            }
                                        account?.saveAccount(this@StartupActivity)
                                        accountManager.fetchUserInfo(this@StartupActivity)
                                        latch.countDown()
                                    }

                                    val errorListener: (Throwable) -> Unit = {
                                        latch.countDown()
                                    }

                                    lifecycleScope.launch {
                                        if (BuildConfig.ONESTORE) {
                                            usersRepository.iabVerify(
                                                receipt = jsonObject.toString(),
                                                signature = "",
                                                itemType = SUBS,
                                                state = Const.IAB_STATE_ABNORMAL,
                                                listener = listener,
                                                errorListener = errorListener
                                            )
                                        } else {
                                            usersRepository.paymentsGoogleSubscriptionCheck(
                                                receipt = jsonObject.toString(),
                                                signature = "",
                                                itemType = SUBS,
                                                state = Const.IAB_STATE_ABNORMAL,
                                                listener = listener,
                                                errorListener = errorListener
                                            )
                                        }
                                    }
                                } catch (e: JSONException) {
                                    latch.countDown()
                                    e.printStackTrace()
                                }
                            } else { // global, onestore 버전에선 구독하트 못 받는다는 알림 주기
                                if (Util.getPreferenceBool(
                                        applicationContext,
                                        Const.PREF_SHOW_WARNING_PLATFORM_CHANGE_SUBSCRIPTION,
                                        true
                                    )
                                ) {
                                    runOnUiThread {
                                        showWarningPlatformChange(
                                            latch,
                                            subscription.name,
                                            R.string.warning_subscription_only_original
                                        )
                                    }
                                } else {
                                    latch.countDown()
                                }
                            }
                        } else if (subscription.familyappId == 3) {
                            if (Util.getPreferenceBool(
                                    applicationContext,
                                    Const.PREF_SHOW_WARNING_PLATFORM_CHANGE_SUBSCRIPTION,
                                    true
                                )
                            ) {
                                runOnUiThread {
                                    showWarningPlatformChange(
                                        latch,
                                        subscription.name,
                                        R.string.warning_subscription_platform_change_android
                                    )
                                }
                            } else {
                                latch.countDown()
                            }
                        } else {
                            latch.countDown()
                        }
                    }
                    latch.await()
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    private fun checkIAB() {
        val latch = CountDownLatch(1)
        lifecycleScope.launch {
            usersRepository.getIabKey(
                { response ->
                    if (response.optBoolean("success")) {
                        val key = response.optString("key")
                        val pKey = checkKey(key)

                        if (mBillingManager == null) {
                            mBillingManager = BillingManager(
                                this@StartupActivity,
                                pKey,
                                object : BillingManager.BillingUpdatesListener {
                                    override fun onBillingClientSetupFinished() {
                                        Util.log("Billing Client connection finished")

                                        if (mBillingManager == null) {
                                            latch.countDown()
                                            return
                                        }

                                        // 소비되지 않은 구매목록을 가져와 소비 처리
                                        mBillingManager?.queryPurchases()
                                    }

                                    override fun onBillingClientSetupFailed() {
                                        Util.log("Billing Client connection FAILED")
                                        latch.countDown()
                                    }

                                    override fun onConsumeFinished(
                                        purchase: Purchase?,
                                        result: Int
                                    ) {
                                        Util.log("Consumption finished. Purchase token: ${purchase?.purchaseToken}, result: $result")
                                    }

                                    override fun onAcknowledgeFinished(
                                        purchase: Purchase?,
                                        result: Int
                                    ) {
                                    }

                                    override fun onPurchasesUpdated(
                                        billingResult: BillingResult,
                                        purchases: List<Purchase>?
                                    ) {
                                        if (mBillingManager == null) {
                                            latch.countDown()
                                            return
                                        }

                                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                                            Util.log("Consuming...")

                                            for (purchase in purchases) {
                                                if (!mBillingManager!!.isSubscription(purchase)) {
                                                    mBillingManager!!.consumeAsync(
                                                        purchase.purchaseToken,
                                                        purchase.developerPayload,
                                                        purchase
                                                    )

                                                    if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
                                                        continue
                                                    }

                                                    val listener: (JSONObject) -> Unit = { response ->
                                                        if (!response.optBoolean("success") && response.optInt(
                                                                "gcode"
                                                            ) == ERROR_88888
                                                        ) {
                                                            UtilK.handleCommonError(this@StartupActivity, response)
                                                        }
                                                    }

                                                    lifecycleScope.launch {
                                                        if (BuildConfig.ONESTORE) {
                                                            usersRepository.iabVerify(
                                                                receipt = purchase.originalJson,
                                                                signature = purchase.signature,
                                                                itemType = INAPP,
                                                                state = Const.IAB_STATE_ABNORMAL,
                                                                listener = listener,
                                                                errorListener = {}
                                                            )
                                                        } else {
                                                            usersRepository.paymentsGoogleItem(
                                                                receipt = purchase.originalJson,
                                                                signature = purchase.signature,
                                                                itemType = INAPP,
                                                                state = Const.IAB_STATE_ABNORMAL,
                                                                listener = listener,
                                                                errorListener = {}
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        mBillingManager = null
                                        latch.countDown()
                                    }
                                })
                        } else {
                            if (response.optInt("gcode") == ERROR_88888 && response.optInt("mcode") == 1) {
                                mThread?.interrupt()
                                runOnUiThread {
                                    Util.showDefaultIdolDialogWithBtn1(
                                        this@StartupActivity,
                                        null,
                                        response.optString("msg"),
                                        R.drawable.img_maintenance
                                    ) { v ->
                                        Util.closeIdolDialog()
                                        finishAffinity()
                                    }
                                }
                                return@getIabKey
                            } else if (response.optInt("gcode") == ERROR_8000) {
                                Util.showDefaultIdolDialogWithBtn1(
                                    this@StartupActivity,
                                    null,
                                    getString(R.string.subscription_account_holding),
                                    { openPlayStoreAccount() },
                                    true
                                )
                            }
                            latch.countDown()
                        }
                    }
                }, {
                    latch.countDown()
                }
            )
        }

        latch.await()
    }

    fun verifyDeveloperPayload(p: Purchase): Boolean {
        val payload = p.developerPayload
        val data = Base64.decode(payload, 0)
        val account = IdolAccount.getAccount(this)
        val newPayload = String(data)
        Util.log("newPayload:$newPayload")
        return newPayload == account?.email
    }

    fun cacheDelete() {
        // Glide 쓰기 전 회원들을 위함
        val prevChildren = cacheDir.list()
        val children = Glide.getPhotoCacheDir(this)?.list() ?: arrayOf()
        val sf = SimpleDateFormat("yyyy-MM-dd / HH:mm:ss", Locale.US)
        Util.log("cache list>>>>>${children.size}")

        prevChildren?.forEach { child ->
            val path = "$cacheDir/$child"
            val f = File(path)

            if (f.isFile) {
                if (f.lastModified() + Const.CACHE_TTL * 1000 < System.currentTimeMillis()) {
                    Util.log("cache delete>>>>>$path")
                    f.delete()
                }
            }
        }

        children.forEach { child ->
            val path = "${Glide.getPhotoCacheDir(this)}/$child"
            val f = File(path)

            if (f.isFile) {
                if (f.lastModified() + Const.CACHE_TTL * 1000 < System.currentTimeMillis()) {
                    Util.log("cache delete>>>>>$path")
                    f.delete()
                }
            }
        }
    }

    private fun startNextActivity() {
        mNextIntent?.let {
            // 이걸 해줘야 로케일 재설정이 먹힌다
            if (it.flags == 0) {
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(it)

            // 앱 종료 후 언어 변경이나 메모리부족으로 앱이 재시작되는 경우가 있음.
            mNextIntent = null // 다음번에 또 불리면 MainActivity가 또 실행되지 않게 처리
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_AUTH -> {
                if (resultCode == RESULT_OK) {
                    synchronized(mAccountLock!!) {
                        mAccountLock?.notify()
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    mThread?.interrupt()
                    finish()
                }
            }
            REQUEST_CODE_INTRO -> startThread()
            REQUEST_CODE_GUIDE -> {
                if (resultCode == RESULT_OK) {
                    startActivityForResult(mAuthIntent!!, REQUEST_CODE_AUTH)
                } else if (resultCode == RESULT_CANCELED) {
                    mThread?.interrupt()
                    finish()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startThread() {
        mThread = StartupThread()
        mThread?.start()
    }

    private fun getReferrerMapFromUri(uri: Uri?) {
        // ClassCastException 방지
        try {
            uri?.let {
                if (it.getQueryParameter(CAMPAIGN_SOURCE_PARAM) != null) {
                    // 캠페인 소스가 존재하면 처리
                } else {
                    // 일반적인 화면 뷰
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkKey(key: String): String {
        val key1 = key.substring(key.length - 7)
        val data = key.substring(0, key.length - 7)
        val pKey = Util.xor(data.toByteArray(), key1.toByteArray())
        return String(pKey)
    }

    private fun showWarningPlatformChange(latch: CountDownLatch, productName: String, msgResId: Int) {
        if (idolDialog != null && idolDialog!!.isShowing) return

        idolDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        idolDialog!!.window?.attributes = lpWindow
        idolDialog!!.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        idolDialog!!.setContentView(R.layout.dialog_not_show_again)
        val tvMessage = idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_message)
        val btnDoNotShowAgain = idolDialog!!.findViewById<AppCompatButton>(R.id.btn_do_not_show_again)
        val btnOk = idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)

        tvMessage.text = String.format(getString(msgResId), productName)

        btnDoNotShowAgain.setOnClickListener {
            Util.setPreference(applicationContext, Const.PREF_SHOW_WARNING_PLATFORM_CHANGE_SUBSCRIPTION, false)
            idolDialog!!.cancel()
            latch.countDown()
        }

        btnOk.setOnClickListener {
            idolDialog!!.cancel()
            latch.countDown()
        }

        idolDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        idolDialog!!.show()
    }

    private fun openPlayStoreAccount() {
        try {
            Util.log("skuCode::$skuCode packageName${packageName}")
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?sku=$skuCode&package=$packageName"))
            )
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun resetPreferenceMainScreen() {
        Util.setPreference(this, Const.PREF_HAS_SHOWN_MY_FAV_TOAST, false)
        Util.setPreference(this, Const.MAIN_BOTTOM_TAB_CURRENT_INDEX, 0)
    }

    private fun goCompleteProcess() = lifecycleScope.launch(Dispatchers.IO) {

        // 카테고리 설정이 없으면
        if (!Util.containsPreference(
                this@StartupActivity,
                Const.PREF_DEFAULT_CATEGORY
            )
        ) {
            account?.let {
                if (it.most == null || it.most?.type.equals("B", ignoreCase = true)) {
                    // 최애가 없거나 비밀의방이면 기본으로 'M'
                    Util.setPreference(
                        this@StartupActivity,
                        Const.PREF_DEFAULT_CATEGORY,
                        "M"
                    )
                } else {
                    val category = it.most?.category
                    Util.setPreference(
                        this@StartupActivity,
                        Const.PREF_DEFAULT_CATEGORY,
                        category
                    )
                }
            }
        } else if (!BuildConfig.CELEB) { // 저장된 카테고리와 실제 최애 카테고리가 다르면 변경
            account?.most?.let {
                if (!Util.getPreference(
                        this@StartupActivity,
                        Const.PREF_DEFAULT_CATEGORY
                    ).equals(it.category)
                ) {
                    val category =
                        if (it.category == "B") "M" else it.category // 최애가 비밀의 방이면 M 아니면 최애 Category
                    Util.setPreference(
                        this@StartupActivity,
                        Const.PREF_DEFAULT_CATEGORY,
                        category
                    )
                }
            }
        }

        IdolApplication.STRATUP_CALLED = true

        val checkGoPush = intent.getBooleanExtra("go_push_start", false)

        // 푸시 처리로 다시 넘김
        if (checkGoPush) {
            Util.log("Start::in checkGoPush")
            withContext(Dispatchers.Main) {
                setResult(RESULT_OK)
                finish()
            }

            return@launch
        }

        if (account == null || (account != null && !account!!.hasUserInfo())) {
            Handler(Looper.getMainLooper()).post {
                // 유저 정보를 받아오는데 실패하면 에러출력을 하지 말고 로그인 화면으로 보내버리자.
                mNextIntent = AuthActivity.createIntent(this@StartupActivity)
                startNextActivity()
                finish()
            }
            return@launch
        }

        val nextIntentForLink: Intent? = intent.getParcelableExtra(PARAM_NEXT_INTENT)
        if (nextIntentForLink != null) {
            Logger.v("ExceptionTest :: -------------- --")
            mNextIntent = MainActivity.createIntentFromDeepLink(
                this@StartupActivity,
                false,
                isDeepLinkClickFromIdol()
            )

            mNextIntent?.putExtra(PARAM_NEXT_INTENT, nextIntentForLink)
            mNextIntent?.putExtra(
                EXTRA_NEXT_ACTIVITY,
                intent.getSerializableExtra(EXTRA_NEXT_ACTIVITY)
            )

            withContext(Dispatchers.Main) {
                startNextActivity()
                finish()
            }
        }


        if (!BuildConfig.ONESTORE) {
            checkSubscriptions()
        }

        if (check_IAB) {
            checkIAB()
        }

        if (!isOpenedByDeeplink) {
            startNextActivity()
            finish()
        }
    }


    companion object {
        private var idolDialog: Dialog? = null
        private const val CAMPAIGN_SOURCE_PARAM = "utm_source"

        private const val REQUEST_CODE_AUTH = 10
        private const val REQUEST_CODE_GUIDE = 11
        private const val REQUEST_CODE_INTRO = 12


        fun createIntentForLink(
            context: Context,
            nextIntent: Intent?,
            nextActivity: Class<*>
        ): Intent {
            return Intent(context, StartupActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
                )
                putExtra(PARAM_NEXT_INTENT, nextIntent)
                putExtra(EXTRA_NEXT_ACTIVITY, nextActivity)
            }
        }

        fun createIntent(context: Context): Intent {
            return createIntent(context, null)
        }

        fun createIntent(context: Context, nextIntent: Intent?): Intent {
            return Intent(context, StartupActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
                )
                putExtra(PARAM_NEXT_INTENT, nextIntent)
            }
        }
    }

}