/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 로그인, 회원가입화면 관리하는 액티비티입니다.
 *
 * */

package net.ib.mn.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import androidx.lifecycle.lifecycleScope
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.GoogleApiClient
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.rx
import com.linecorp.linesdk.LineApiResponseCode
import com.linecorp.linesdk.api.LineApiClient
import com.linecorp.linesdk.api.LineApiClientBuilder
import com.linecorp.linesdk.auth.LineLoginApi
import com.linecorp.linesdk.auth.LineLoginResult
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.domain.usecase.GetConfigSelfUseCase
import net.ib.mn.databinding.ActivityAuthBinding
import net.ib.mn.dialog.ProgressDialogFragment
import net.ib.mn.fragment.AgreementFragment
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.fragment.GoogleMoreFragment
import net.ib.mn.fragment.SigninFragment
import net.ib.mn.fragment.SignupFragment.OnRegistered
import net.ib.mn.gcm.GcmUtils
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.AppConst
import net.ib.mn.utils.AppConst.CHANNEL_ID
import net.ib.mn.utils.Const
import net.ib.mn.utils.Const.DOMAIN_FACEBOOK
import net.ib.mn.utils.Const.DOMAIN_GOOGLE
import net.ib.mn.utils.Const.DOMAIN_KAKAO
import net.ib.mn.utils.Const.DOMAIN_LINE
import net.ib.mn.utils.Const.KEY_DOMAIN
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsetsAndRequest
import net.ib.mn.utils.ext.asyncPopBackStack
import net.ib.mn.utils.modelToString
import java.io.IOException
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : BaseActivity(), OnBackStackChangedListener {

    private lateinit var binding: ActivityAuthBinding

    private var mEmail: String? = null
    private var mPasswd: String? = null
    private var mName: String? = null
    private var mProfileUrl: String? = null
    private var mAuthToken: String? = null
    private var mGoogleApiClient: GoogleApiClient? = null

    // facebook login
    lateinit var callbackManager: CallbackManager
    private var mFacebookId: Long? = null

    // line
    private lateinit var lineApiClient: LineApiClient

    @Inject
    lateinit var usersRepository: UsersRepository

    @Inject
    lateinit var getConfigSelfUseCase: GetConfigSelfUseCase

    var disposables = CompositeDisposable()

    val wrap = OnRegistered { id -> // preference에 저장
        Util.setPreference(this@AuthActivity, Const.PREF_GCM_PUSH_KEY, id)
        // 로그인
        // 다른 쓰레드로 불려 mDomain 값이 카카오 로그인을 해도 null로 가는 현상이 있어 preference로 저장
        val domain = Util.getPreference(this@AuthActivity, KEY_DOMAIN)
        trySignin(mEmail ?: return@OnRegistered, mPasswd ?: return@OnRegistered, id, domain)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        binding.authRoot.applySystemBarInsetsAndRequest()

        setContentView(binding.root)

        val manager = supportFragmentManager
        manager.addOnBackStackChangedListener(this)

        val signinFragment = SigninFragment()
        if (intent.hasExtra(Const.IS_EMAIL_SIGNUP)) {
            signinFragment.arguments = bundleOf(
                Const.IS_EMAIL_SIGNUP to "true"
            )
        }
        if (savedInstanceState == null) {
            manager.beginTransaction()
                .replace(binding.fragmentContainer.id, signinFragment, "signin").commit()
        }

        actionbarConfigure()

        // LINE
        val apiClientBuilder = LineApiClientBuilder(applicationContext, CHANNEL_ID)
        lineApiClient = apiClientBuilder.build()

        // facebook login
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().logOut()
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    // App code
                    val loggedIn = AccessToken.getCurrentAccessToken() != null
                    if (loggedIn) {
                        requestFacebookMe(loginResult)
                    }
                }

                override fun onCancel() {
                    // App code
                }

                override fun onError(exception: FacebookException) {
                    // App code
                    Toast.makeText(
                        this@AuthActivity,
                        R.string.line_login_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        checkNotificationPermission()

        lifecycleScope.launch {
            val result = getConfigSelfUseCase().first()
            if(!result.success) {
                return@launch
            }
            Logger.v("ApiLogger", result.modelToString())
            ConfigModel.getInstance(this@AuthActivity).parse(result.data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
        googleApiclientCloseLogout()
        //lineApiClient.logout();
    }

    override fun onBackStackChanged() {
        actionbarConfigure()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // facebook login
        if ((requestCode and 0xffff) == 0xface) {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            Util.log("requestCode:$requestCode   resultCode:$resultCode")
            val fragment = supportFragmentManager.findFragmentByTag("signin") as? BaseFragment
            if (fragment != null) {
                Util.log("fragment is not null!")
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    fun actionbarConfigure() {
        val isRootFragment = supportFragmentManager.backStackEntryCount == 0
        val actionbar = supportActionBar
        if (isRootFragment) {
            actionbar?.hide()
        } else {
            actionbar?.show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val manager = supportFragmentManager
        if (manager.backStackEntryCount > 0) {
            manager.asyncPopBackStack()
            return true
        }
        return false
    }

    fun trySignin(email: String, passwd: String, deviceKey: String?, domain: String) {
        ProgressDialogFragment.show(this, "signin", R.string.wait_signin)
        var gmail = Util.getGmail(this@AuthActivity)
        if(gmail.isEmpty()) {
            gmail = Util.getDeviceUUID(this@AuthActivity)
        }
        MainScope().launch {
            usersRepository.signIn(
                domain,
                email,
                passwd,
                deviceKey ?: "",
                gmail,
                Util.getDeviceUUID(this@AuthActivity),
                AppConst.APP_ID,
                { response ->
                    Util.closeProgress()

                    if (response.optBoolean("success")) {
                        if(domain == DOMAIN_KAKAO) {
                            requestKakaoUnlink()
                        }
                        afterSignin(email, passwd, domain)
                    } else {
                        ProgressDialogFragment.hideAll(this@AuthActivity)
                        if (response.optInt("gcode") == ErrorControl.ERROR_88888 && response.optInt("mcode") == 1) {
                            Util.showDefaultIdolDialogWithBtn1(
                                this@AuthActivity,
                                null,
                                response.optString("msg"),
                                R.drawable.img_maintenance
                            ) {
                                Util.closeIdolDialog()
                                finishAffinity()
                            }
                        } else {
                            val responseMsg = ErrorControl.parseError(this@AuthActivity, response)
                            if (responseMsg != null) {
                                Util.showDefaultIdolDialogWithBtn1(
                                    this@AuthActivity,
                                    null,
                                    responseMsg
                                ) { Util.closeIdolDialog() }
                            }
                        }
                    }
                },
                { throwable ->
                    Util.closeProgress()

                    ProgressDialogFragment.hideAll(this@AuthActivity)
                    Toast.makeText(
                        this@AuthActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    // LINE
    fun requestLineSignUp(data: Intent?) {
        if(data == null){
            Toast.makeText(this, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
            return
        }
        if (Const.FEATURE_AUTH2) {
            Util.setPreference(this, KEY_DOMAIN, DOMAIN_LINE)
        }

        val result = LineLoginApi.getLoginResultFromIntent(data)
        when (result.responseCode) {
            LineApiResponseCode.SUCCESS -> requestLineProfile(result)
            LineApiResponseCode.CANCEL -> Util.log("Line: Login cancelled")
            else -> runOnUiThread {
                Util.log(result.toString())
                Toast.makeText(this, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLineProfile(result: LineLoginResult) {
        ProgressDialogFragment.show(this, DOMAIN_LINE, R.string.wait_line_signin)

        val mid = result.lineProfile?.userId
        val displayName = result.lineProfile?.displayName

        mEmail = "$mid${Const.POSTFIX_LINE}"
        mName = displayName
        mPasswd = "asdfasdf"
        mProfileUrl = null

        if (Const.FEATURE_AUTH2) {
            // ID는 user_id@line.com, 암호는 access token으로 보내자
            mAuthToken = result.lineCredential?.accessToken?.tokenString
            mPasswd = mAuthToken
        }

        // 기존에 아이디를 갖고 회원가입을 했었는지 확인한다.
        Util.log("requestLineProfile  mEmail :$mEmail   mName:$mName")
        lifecycleScope.launch {
            usersRepository.validate(
                type = "email",
                value = mEmail,
                appId = AppConst.APP_ID,
                listener = { response ->
                    ProgressDialogFragment.hide(this@AuthActivity, DOMAIN_LINE)
                    if (response.optBoolean("success")) {
                        setAgreementFragment(DOMAIN_LINE, AgreementFragment.LOGIN_LINE)
                    } else {
                        if (Const.FEATURE_AUTH2) {
                            Util.setPreference(this@AuthActivity, KEY_DOMAIN, DOMAIN_LINE)
                        }
                        GcmUtils.registerDevice(this@AuthActivity, wrap)
                    }
                },
                errorListener = {
                    setAgreementFragment(DOMAIN_LINE, AgreementFragment.LOGIN_LINE)
                }
            )
        }
    }

    fun requestKakaoLogin() {
        Util.showProgress(this, true)

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.rx.loginWithKakaoTalk(this)
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext { error ->
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        Logger.v("KakaoLogin::${error.reason}")
                        Single.error(error)
                    } else {
                        Logger.v("KakaoLogin::카카오 계정 없는경우")
                        // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                        UserApiClient.rx.loginWithKakaoAccount(this)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ token ->

                    Logger.v("KakaoLogin::로그인 성공.")

                    requestKakaoMe(token.accessToken)

                }, { error ->
                    //로그인 실패.
                    Logger.v("KakaoLogin:: ${error.message}")
                }).addTo(disposables)
        } else {
            UserApiClient.rx.loginWithKakaoAccount(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ token ->
                    requestKakaoMe(token.accessToken)
                }, {
                    Logger.v("KakaoLogin:: 카카오 체크 앱 안깔려져있음.")
                })
                .addTo(disposables)
        }
    }

    private fun requestKakaoMe(accessToken: String) {
        UserApiClient.rx.me()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ user ->
                ProgressDialogFragment.show(
                    this@AuthActivity, DOMAIN_KAKAO,
                    R.string.wait_kakao_signin
                )
                val id = user.id
                mEmail = "$id${Const.POSTFIX_KAKAO}"
                //userProfile.getEmail();
                mName = user.kakaoAccount?.profile?.nickname
                mPasswd = "asdfasdf"
                mProfileUrl = user.kakaoAccount?.profile?.thumbnailImageUrl

                if (Const.FEATURE_AUTH2) {
                    // ID는 user_id@kakao.com, 암호는 access token으로 보내자
                    mAuthToken = accessToken
                    mPasswd = mAuthToken
                }

                getSignupValidate()

            }, { error ->
                Logger.v("KakaoLogin:: Kakao me error ${error.message}")
            }).addTo(disposables)
    }

    private fun getSignupValidate() {
        // 기존에 아이디를 갖고 회원가입을 했었는지 확인한다.
        Logger.v("KakaoLogin:: mEmail :$mEmail  mName:$mName   mProfileUrl:$mProfileUrl")
        lifecycleScope.launch {
            usersRepository.validate(
                type = "email",
                value = mEmail,
                appId = AppConst.APP_ID,
                listener = { response ->
                    ProgressDialogFragment.hide(this@AuthActivity, DOMAIN_KAKAO)
                    if (response.optBoolean("success")) {
                        Util.closeProgress()
                        // 161011 약관동의 처리

                        setAgreementFragment(DOMAIN_KAKAO, AgreementFragment.LOGIN_KAKAO)
                    } else {
                        if (Const.FEATURE_AUTH2) {
                            Util.setPreference(this@AuthActivity, KEY_DOMAIN, DOMAIN_KAKAO)
                        }
                        GcmUtils.registerDevice(this@AuthActivity, wrap)
                    }
                },
                errorListener = {
                    setAgreementFragment(DOMAIN_KAKAO, AgreementFragment.LOGIN_KAKAO)
                }
            )
        }
    }

    fun afterSignin(email: String, token: String, domain: String) {
        ProgressDialogFragment.show(this, "userinfo", R.string.wait_userinfo)
        val hashToken =
            if (domain == null || domain.equals(Const.DOMAIN_EMAIL, ignoreCase = true)) {
                Util.md5salt(token)
            } else {
                mAuthToken
            }

        val appsFlyerInstance = AppsFlyerLib.getInstance()

        val eventValues = mutableMapOf<String, Any>()
        eventValues["user_id"] = appsFlyerInstance.getAppsFlyerUID(applicationContext) ?: ""
        eventValues[AFInAppEventParameterName.REGISTRATION_METHOD] = domain ?: ""

        appsFlyerInstance.logEvent(
            applicationContext,
            AFInAppEventType.COMPLETE_REGISTRATION,
            eventValues
        )

        IdolAccount.createAccount(this, email, hashToken, domain)

        //현재 로그인 시간을  넣어준다.
        Util.setPreference(this, "user_login_ts", System.currentTimeMillis())

        setResult(RESULT_OK)
        val startIntent = StartupActivity.createIntent(this)
        startActivity(startIntent)
        finish()
    }

    fun showError(msg: String?) {
        Util.showDefaultIdolDialogWithBtn1(this,
            null,
            msg,
            { Util.closeIdolDialog() }
        )
        Util.log("error $msg")
    }

    /**
     * Fetching user's information name, email, profile pic
     */
    private class AuthTask(activity: AuthActivity) :
        AsyncTask<GoogleSignInAccount, Void, String>() {

        private val activityReference = WeakReference(activity)

        override fun doInBackground(vararg params: GoogleSignInAccount): String? {
            var token: String? = null
            val account = params[0]

            try {
                token = GoogleAuthUtil.getToken(
                    activityReference.get()!!,
                    account.account!!,
                    "oauth2:https://www.googleapis.com/auth/plus.me"
                )
            } catch (e: IOException) {
                // Network or server error, try later
            } catch (e: UserRecoverableAuthException) {
                // Recover (with e.getIntent())
//                    Intent recover = e.getIntent();
//                    startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);
                activityReference.get()?.runOnUiThread {
                    Toast.makeText(
                        activityReference.get(),
                        R.string.msg_error_ok,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: GoogleAuthException) {
                // The call is not ever expected to succeed
                // assuming you have already verified that
                // Google Play services is installed.
            }

            return token
        }

        override fun onPostExecute(token: String?) {
            Util.log("Access token retrieved:$token")
            activityReference.get()?.mAuthToken = token
            activityReference.get()?.mPasswd = activityReference.get()?.mAuthToken
            activityReference.get()?.validateGoogleSignup()
        }
    }

    fun getProfileInformation(account: GoogleSignInAccount, mGoogleApiClient: GoogleApiClient) {
        Util.showProgress(this)

        this.mGoogleApiClient = mGoogleApiClient

        mEmail = account.email
        mName = account.displayName
        val id = account.id
        mPasswd = "qazqazqaz"

        mProfileUrl = ""

        if (Const.FEATURE_AUTH2) {
            Util.setPreference(this, KEY_DOMAIN, DOMAIN_GOOGLE)
            mPasswd = mAuthToken

            AuthTask(this).execute(account)
        } else {
            validateGoogleSignup()
        }
    }

    private fun validateGoogleSignup() {
        Util.log("google plus mEmail :$mEmail   mName:$mName   mProfileUrl:$mProfileUrl")
        lifecycleScope.launch {
            usersRepository.validate(
                type = "email",
                value = mEmail,
                appId = AppConst.APP_ID,
                listener = { response ->
                    if (response.optBoolean("success")) { // 로그인
                        Util.closeProgress()
                        setAgreementFragment(DOMAIN_GOOGLE, AgreementFragment.LOGIN_GOOGLE)
                    } else {
                        if (Const.FEATURE_AUTH2) {
                            Util.setPreference(this@AuthActivity, KEY_DOMAIN, DOMAIN_GOOGLE)
                        }
                        if (!response.optString(KEY_DOMAIN).equals(DOMAIN_GOOGLE, ignoreCase = true)) {
                            // facebook 로그인이 아닌 다른 로그인으로 가입되어있음
                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn2(
                                this@AuthActivity,
                                null,
                                getString(R.string.confirm_change_account),
                                R.string.yes,
                                R.string.no,
                                false,
                                true,
                                {
                                    Util.showProgress(this@AuthActivity)
                                    GcmUtils.registerDevice(this@AuthActivity, wrap)
                                    Util.closeIdolDialog()
                                },
                                {
                                    if (mGoogleApiClient?.isConnected == true) {
                                        mGoogleApiClient?.clearDefaultAccountAndReconnect()
                                    }
                                    Util.closeIdolDialog()
                                })
                        } else {
                            GcmUtils.registerDevice(this@AuthActivity, wrap)
                        }
                    }
                },
                errorListener = {
                    Util.closeProgress()
                    val mGoogleMoreFragment = GoogleMoreFragment.newInstance(mEmail, mName, mPasswd, "")
                    val mFragmentManager = supportFragmentManager
                    mFragmentManager.beginTransaction()
                        .replace(binding.fragmentContainer.id, mGoogleMoreFragment).addToBackStack(null)
                        .commit()
                }
            )
        }
    }

    fun googleApiclientClose() {
        mGoogleApiClient?.let {
            if (it.isConnected) {
                it.disconnect()
            }
        }
    }

    fun googleApiclientCloseLogout() {
        mGoogleApiClient?.let {
            if (it.isConnected) {
                it.disconnect()
            }
        }
    }

    fun requestFacebookMe(loginResult: LoginResult) {
        Util.showProgress(this, true)

        val accessToken: AccessToken = loginResult.accessToken
        mAuthToken = accessToken.token
        Util.log("FACEBOOK ACCESS TOKEN=$accessToken")

        val request = GraphRequest.newMeRequest(
            loginResult.accessToken
        ) { jsonObject, response ->
            if (jsonObject == null) {
                Util.closeProgress()
                LoginManager.getInstance().logOut()
                Util.showIdolDialogWithBtn1(
                    this@AuthActivity,
                    null,
                    getString(R.string.facebook_no_email)
                ) { Util.closeIdolDialog() }
                return@newMeRequest
            }

            // Application code
            Util.log("requestFacebookMe $jsonObject")
            mFacebookId = jsonObject.optString("id").toLongOrNull()
            mName = jsonObject.optString("name")
            mEmail = jsonObject.optString("email")

            // 사용자가 email 제공을 거부한 경우
            if (mEmail == null || mEmail!!.isEmpty()) {
                Util.closeProgress()
                LoginManager.getInstance().logOut()
                Util.showIdolDialogWithBtn1(
                    this@AuthActivity,
                    null,
                    getString(R.string.facebook_no_email)
                ) { Util.closeIdolDialog() }
                return@newMeRequest
            }

            ProgressDialogFragment.show(
                this@AuthActivity,
                DOMAIN_FACEBOOK,
                R.string.lable_get_info
            )

            mPasswd = mAuthToken

            // 기존에 아이디를 갖고 회원가입을 했었는지 확인한다.
            Util.log("requestFacebookMe  mEmail :$mEmail   mName:$mName   id:$mFacebookId")
            lifecycleScope.launch {
                usersRepository.validate(
                    type = "email",
                    value = mEmail,
                    appId = AppConst.APP_ID,
                    listener = { response ->
                        ProgressDialogFragment.hide(this@AuthActivity, "facebook")
                        if (response.optBoolean("success")) {
                            Util.closeProgress()
                            setAgreementFragment(DOMAIN_FACEBOOK, AgreementFragment.LOGIN_FACEBOOK)
                        } else {
                            Util.closeProgress()
                            if (Const.FEATURE_AUTH2) {
                                Util.setPreference(this@AuthActivity, KEY_DOMAIN, DOMAIN_FACEBOOK)
                            }

                            // 이메일 가입자가 같은 이메일계정의 facebook/google 로그인시 처리
                            if (!response.optString(KEY_DOMAIN)
                                    .equals(DOMAIN_FACEBOOK, ignoreCase = true)
                            ) {
                                // facebook 로그인이 아닌 다른 로그인으로 가입되어있음
                                Util.showDefaultIdolDialogWithBtn2(
                                    this@AuthActivity,
                                    null,
                                    getString(R.string.confirm_change_account),
                                    {
                                        Util.closeIdolDialog(); Util.showProgress(this@AuthActivity); GcmUtils.registerDevice(
                                        this@AuthActivity,
                                        wrap
                                    )
                                    },
                                    { LoginManager.getInstance().logOut(); Util.closeIdolDialog() }
                                )

                            } else {
                                GcmUtils.registerDevice(this@AuthActivity, wrap)
                            }
                        }
                    },
                    errorListener = {
                        setAgreementFragment(DOMAIN_FACEBOOK, AgreementFragment.LOGIN_FACEBOOK)
                    }
                )
            }
        }
        val parameters = Bundle()
        parameters.putString("fields", "id,name,email")
        request.parameters = parameters
        request.executeAsync()
    }

    private fun setAgreementFragment(domain: String, type: Int) {
        val mFragmentManager = supportFragmentManager
        val frag = AgreementFragment().apply {
            email = mEmail
            displayName = mName
            password = mPasswd
            this.domain = domain
            loginType = type
            facebookId = mFacebookId
        }
        mFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun requestKakaoUnlink() {
        UserApiClient.rx.unlink()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Logger.v("KakaoLogin::unlink success")
            }, { error ->
                Logger.v("KakaoLogin::unlink error ${error.message}")
            }).addTo(disposables)

    }

    //알림 권한 요청.
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permissions = arrayOf(
            Manifest.permission.POST_NOTIFICATIONS
        )

        val deniedPermissions: MutableList<String?> = ArrayList()

        for (perm in permissions) {
            if (ActivityCompat.checkSelfPermission(this, perm)
                != PackageManager.PERMISSION_GRANTED
            ) deniedPermissions.add(perm)
        }

        if (deniedPermissions.size > 0) {
            val deniedPerms = deniedPermissions.toTypedArray()
            ActivityCompat.requestPermissions(this, deniedPerms, REQUEST_POST_NOTIFICATIONS)
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context?): Intent {
            return Intent(context, AuthActivity::class.java)
        }
    }

}
