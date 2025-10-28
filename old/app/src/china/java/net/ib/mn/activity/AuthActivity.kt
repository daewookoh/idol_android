package net.ib.mn.activity

import android.Manifest
import android.R
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.CallbackManager.Factory.create
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk.isInitialized
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.GoogleApiClient
import com.linecorp.linesdk.LineApiResponseCode
import com.linecorp.linesdk.api.LineApiClient
import com.linecorp.linesdk.api.LineApiClientBuilder
import com.linecorp.linesdk.auth.LineLoginApi
import com.linecorp.linesdk.auth.LineLoginResult
import com.tencent.connect.UserInfo
import com.tencent.connect.common.Constants
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.account.IdolAccount.Companion.createAccount
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivityAuthBinding
import net.ib.mn.dialog.ProgressDialogFragment
import net.ib.mn.fragment.AgreementFragment
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.fragment.GoogleMoreFragment.Companion.newInstance
import net.ib.mn.fragment.KakaoMoreFragment.Companion.newInstance
import net.ib.mn.fragment.SigninFragment
import net.ib.mn.fragment.SignupFragment.OnRegistered
import net.ib.mn.gcm.GcmUtils.registerDevice
import net.ib.mn.pushy.PushyUtil
import net.ib.mn.utils.AppConst
import net.ib.mn.utils.AppConst.CHANNEL_ID
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsetsAndRequest
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * 로그인화면(카카오,라인,구글,페이스북)
 */

@AndroidEntryPoint
class AuthActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener, IUiListener {
    private lateinit var binding: ActivityAuthBinding

    private var mEmail: String? = null
    private var mPasswd: String? = null
    private var mName: String? = null
    private var mProfileUrl: String? = null
    private var mAuthToken: String? = null
    private var mGoogleApiClient: GoogleApiClient? = null

    // facebook login
    var callbackManager: CallbackManager? = null

    // QQ
    private var mTencent: Tencent? = null

    @Inject
    lateinit var usersRepository: UsersRepository

    var wrap: OnRegistered = OnRegistered { id -> // preference에 저장
        Util.setPreference(this@AuthActivity, Const.PREF_GCM_PUSH_KEY, id)
        // 로그인
        // 다른 쓰레드로 불려 mDomain 값이 카카오 로그인을 해도 null로 가는 현상이 있어 preference로 저장
        val domain = Util.getPreference(this@AuthActivity, Const.KEY_DOMAIN)
        trySignin(mEmail, mPasswd, id, domain)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        binding.authRoot.applySystemBarInsetsAndRequest()

        setContentView(binding.root)

        val manager = supportFragmentManager
        manager.addOnBackStackChangedListener(this)

        val agreementFragment = AgreementFragment()

        if (savedInstanceState == null) {
            // 중국은 약관/개인정보방침이 앱 시작시 먼저 나와야 한다고 함.
            manager.beginTransaction()
                .replace(binding.fragmentContainer.id, agreementFragment, "agreement").commit()
        }

        actionbarConfigure()

        // LINE
        val apiClientBuilder = LineApiClientBuilder(applicationContext, CHANNEL_ID)
        lineApiClient = apiClientBuilder.build()

        // facebook login
        if (!isInitialized()) {
            sdkInitialize(this)
        }
        callbackManager = create()
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
                    makeText(
                        this@AuthActivity,
                        net.ib.mn.R.string.line_login_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        val intent = intent
        if (intent != null && intent.hasExtra(Const.PARAM_WECHAT_ACCESS_TOKEN)) {
            handleWeChatSignin(intent)
        }

        // QQ
        mTencent = Tencent.createInstance(Const.QQ_APP_ID, this)

        checkNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent != null && intent.hasExtra(Const.PARAM_WECHAT_ACCESS_TOKEN)) {
            handleWeChatSignin(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        googleApiclientCloseLogout()
        //lineApiClient.logout();
    }

    override fun onBackStackChanged() {
        actionbarConfigure()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // facebook login
        if ((requestCode and 0xffff) == 0xface) {
            callbackManager!!.onActivityResult(requestCode, resultCode, data)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            Util.log("requestCode:$requestCode   resultCode:$resultCode")
            val fragment = supportFragmentManager.findFragmentByTag("signin") as BaseFragment?
            if (fragment != null) {
                Util.log("fragment is not null!")
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }

        if (requestCode == Constants.REQUEST_LOGIN ||
            requestCode == Constants.REQUEST_APPBAR
        ) {
            Tencent.onActivityResultData(requestCode, resultCode, data, this)
        }
    }

    fun actionbarConfigure() {
        //로그인 화면

        val sigininFragment = supportFragmentManager
            .backStackEntryCount == 1

        //동의 화면
        val agreementFragment = supportFragmentManager
            .backStackEntryCount == 0

        val actionbar = supportActionBar

        if (sigininFragment) {
            actionbar!!.hide()
        } else {
            actionbar!!.show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val manager = supportFragmentManager
        if (manager.backStackEntryCount > 0) {
            manager.popBackStack()
            return true
        }
        return false
    }

    fun trySignin(
        email: String?, passwd: String?,
        deviceKey: String?, domain: String?
    ) {
        ProgressDialogFragment.show(this, "signin", net.ib.mn.R.string.wait_signin)

        var gmail = Util.getGmail(this@AuthActivity)
        if(gmail.isEmpty()) {
            gmail = Util.getDeviceUUID(this@AuthActivity)
        }
        MainScope().launch {
            usersRepository.signIn(
                domain = domain,
                email = email ?: "",
                passwd = passwd ?: "",
                deviceKey = deviceKey ?: "",
                gmail = gmail,
                deviceId = Util.getDeviceUUID(this@AuthActivity),
                appId = AppConst.APP_ID,
                listener = { response ->
                    Util.closeProgress()

                    if (response.optBoolean("success")) {
                        afterSignin(email, passwd, domain)
                    } else {
                        ProgressDialogFragment.hideAll(this@AuthActivity)
                        if (response.optInt("gcode") == ErrorControl.ERROR_88888 && response.optInt("mcode") == 1) {
                            Util.showDefaultIdolDialogWithBtn1(
                                this@AuthActivity,
                                null,
                                response.optString("msg"), net.ib.mn.R.drawable.img_maintenance
                            ) { v: View? ->
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
                                ) { v: View? -> Util.closeIdolDialog() }
                            }
                        }
                    }                },
                errorListener = { error ->
                    Util.closeProgress()

                    ProgressDialogFragment.hideAll(this@AuthActivity)
                    makeText(
                        this@AuthActivity,
                        net.ib.mn.R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    // WeChat
    fun handleWeChatSignin(data: Intent) {
        mAuthToken = data.getStringExtra(Const.PARAM_WECHAT_ACCESS_TOKEN)
        val unionId = data.getStringExtra(Const.PARAM_WECHAT_UNIONID)
        mEmail =
            Util.sha1(unionId ?: "") + Const.POSTFIX_WECHAT // 기본적으로 email 형태에 대소문자 구별을 안하므로 sha1을 구해서 넘기자
        mPasswd = mAuthToken

        // 기존에 아이디를 갖고 회원가입을 했었는지 확인한다.
        Util.log("request WeChat Profile  mEmail :$mEmail")
        lifecycleScope.launch {
            usersRepository.validate(
                type = "email",
                value = mEmail,
                appId = AppConst.APP_ID,
                listener = { response ->
                    ProgressDialogFragment.hide(this@AuthActivity, "wechat")
                    if (response.optBoolean("success") == false) {
                        Util.setPreference(this@AuthActivity, Const.KEY_DOMAIN, Const.DOMAIN_WECHAT)
                        // TOOD: 중국 push
                        registerDevice(this@AuthActivity, wrap)
                        PushyUtil.registerDevice(this@AuthActivity, wrap)
                    } else {
                        Util.log("here 2")
                        val mFragmentManager = supportFragmentManager

                        //                            AgreementFragment frag = new AgreementFragment();
//                            frag.email = mEmail;
//                            frag.password = mPasswd;
//                            frag.domain = DOMAIN_WECHAT;
//                            frag.loginType = AgreementFragment.LOGIN_WECHAT;
                        val frag = newInstance(mEmail, mName, mPasswd, Const.DOMAIN_WECHAT, "", null)

                        mFragmentManager.beginTransaction()
                            .replace(binding.fragmentContainer.id, frag)
                            .addToBackStack(null).commit()
                    }
                },
                errorListener = {
                    val mFragmentManager = supportFragmentManager
                    val frag = newInstance(mEmail, mName, mPasswd, Const.DOMAIN_WECHAT, "", null)

                    mFragmentManager.beginTransaction()
                        .replace(binding.fragmentContainer.id, frag)
                        .addToBackStack(null).commit()
                }
            )
        }
    }

    // LINE
    fun requestLineSignUp(data: Intent?) {
        if (Const.FEATURE_AUTH2) {
            Util.setPreference(this@AuthActivity, Const.KEY_DOMAIN, Const.DOMAIN_LINE)
        }

        val result = LineLoginApi.getLoginResultFromIntent(data)
        when (result.responseCode) {
            LineApiResponseCode.SUCCESS -> requestLineProfile(result)
            LineApiResponseCode.CANCEL -> Util.log("Line: Login cancelled")
            else -> runOnUiThread {
                Util.log(result.toString())
                makeText(
                    this@AuthActivity,
                    net.ib.mn.R.string.line_login_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun requestLineProfile(result: LineLoginResult) {
        ProgressDialogFragment.show(
            this@AuthActivity,
            Const.DOMAIN_LINE,
            net.ib.mn.R.string.wait_line_signin
        )

        // LineApiResponse verifyResponse = lineApiClient.getProfile();
        val mid = result.lineProfile!!.userId
        val displayName = result.lineProfile!!.displayName

        mEmail = mid + Const.POSTFIX_LINE
        mName = displayName
        mPasswd = "asdfasdf"
        mProfileUrl = null

        if (Const.FEATURE_AUTH2) {
            // ID는 user_id@line.com, 암호는 access token으로 보내자
            mAuthToken = result.lineCredential!!.accessToken.tokenString
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
                    ProgressDialogFragment.hide(this@AuthActivity, Const.DOMAIN_LINE)
                    if (response.optBoolean("success")) {
                        setAgreementFragment(Const.DOMAIN_LINE, AgreementFragment.LOGIN_LINE)
                    } else {
                        if (Const.FEATURE_AUTH2) {
                            Util.setPreference(
                                this@AuthActivity,
                                Const.KEY_DOMAIN,
                                Const.DOMAIN_LINE
                            )
                        }
                        registerDevice(this@AuthActivity, wrap)
                        PushyUtil.registerDevice(this@AuthActivity, wrap)
                    }
                },
                errorListener = {
                    setAgreementFragment(Const.DOMAIN_LINE, AgreementFragment.LOGIN_LINE)
                }
            )
        }
    }

    fun afterSignin(email: String?, token: String?, domain: String?) {
        ProgressDialogFragment.show(this, "userinfo", net.ib.mn.R.string.wait_userinfo)
        val hashToken = if (domain == null || domain.equals(
                Const.DOMAIN_EMAIL,
                ignoreCase = true
            )
        ) {
            Util.md5salt(token ?: "")
        } else {
            mAuthToken
        }
        createAccount(this@AuthActivity, email, hashToken, domain)

        //현재 로그인 시간을  넣어준다.
        Util.setPreference(this@AuthActivity, "user_login_ts", System.currentTimeMillis())

        setResult(RESULT_OK)
        val startIntent = StartupActivity.createIntent(this)
        startActivity(startIntent)
        finish()
    }

    fun showError(msg: String) {
        Util.showDefaultIdolDialogWithBtn1(
            this,
            null,
            msg
        ) { v: View? -> Util.closeIdolDialog() }

        Util.log("error $msg")
    }

    // QQ
    fun sendQQAuth() {
        if (mTencent!!.isSessionValid) {
            mTencent!!.logout(this)
        }
        mTencent!!.login(this, "get_user_info", this, true)
    }

    override fun onComplete(o: Any) {
        val json = o as JSONObject

        handleQQSignin(json)
    }

    override fun onError(uiError: UiError) {
        makeText(
            this,
            getString(net.ib.mn.R.string.line_login_failed) + ":" + uiError.errorDetail,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCancel() {
        showQQError()
    }

    private fun handleQQSignin(json: JSONObject) {
        Util.showProgress(this)

        mAuthToken = json.optString(Constants.PARAM_ACCESS_TOKEN)
        val openid = json.optString(Constants.PARAM_OPEN_ID)
        val expires = json.optString(Constants.PARAM_EXPIRES_IN)
        mEmail = openid + Const.POSTFIX_QQ
        mPasswd = mAuthToken

        mTencent!!.setAccessToken(mAuthToken, expires)
        mTencent!!.openId = openid

        val userInfo = UserInfo(this, mTencent!!.qqToken)
        userInfo.getUserInfo(object : IUiListener {
            override fun onComplete(o: Any) {
                val response = o as JSONObject
                mName = response.optString("nickname")

                proceedQQLogin()
            }

            override fun onError(uiError: UiError) {
                proceedQQLogin()
            }

            override fun onCancel() {
                proceedQQLogin()
            }
        })
    }

    private fun proceedQQLogin() {
        // 기존에 아이디를 갖고 회원가입을 했었는지 확인한다.
        Util.log("request QQ Profile  mEmail :$mEmail")

        val runnable = Runnable {
            Util.closeProgress()
            val mFragmentManager = supportFragmentManager

            //            AgreementFragment frag = new AgreementFragment();
//            frag.email = mEmail;
//            frag.displayName = mName;
//            frag.password = mPasswd;
//            frag.domain = DOMAIN_QQ;
//            frag.loginType = AgreementFragment.LOGIN_QQ;
            val frag = newInstance(mEmail, mName, mPasswd, Const.DOMAIN_QQ, "", null)
            mFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, frag)
                .addToBackStack(null).commit()
        }

        lifecycleScope.launch {
            usersRepository.validate(
                type = "email",
                value = mEmail,
                appId = AppConst.APP_ID,
                listener = { response ->
                    ProgressDialogFragment.hide(this@AuthActivity, "wechat")
                    if (response.optBoolean("success") == false) {
                        Util.setPreference(this@AuthActivity, Const.KEY_DOMAIN, Const.DOMAIN_QQ)
                        // TOOD: 중국 push
                        registerDevice(this@AuthActivity, wrap)
                        PushyUtil.registerDevice(this@AuthActivity, wrap)
                    } else {
                        Util.log("here 2")
                        runnable.run()
                    }
                },
                errorListener = {
                    runnable.run()
                }
            )
        }
    }

    private fun showQQError() {
        makeText(this, net.ib.mn.R.string.line_login_failed, Toast.LENGTH_SHORT).show()
    }

    /**
     * Fetching user's information name, email, profile pic
     */
    private class AuthTask(context: AuthActivity?) :
        AsyncTask<GoogleSignInAccount?, Void?, String?>() {
        private val activityReference = WeakReference(context)

        override fun doInBackground(vararg params: GoogleSignInAccount?): String? {
            var token: String? = null
            val account = params[0]

            try {
                token = GoogleAuthUtil.getToken(
                    activityReference.get()!!,
                    account?.account!!,
                    "oauth2:https://www.googleapis.com/auth/plus.me"
                )
            } catch (transientEx: IOException) {
                // Network or server error, try later
            } catch (e: UserRecoverableAuthException) {
                // Recover (with e.getIntent())
//                    Intent recover = e.getIntent();
//                    startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);
                activityReference.get()!!.runOnUiThread {
                    if (activityReference.get() != null) makeText(
                        activityReference.get(),
                        net.ib.mn.R.string.msg_error_ok,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (authEx: GoogleAuthException) {
                // The call is not ever expected to succeed
                // assuming you have already verified that
                // Google Play services is installed.
            }

            return token
        }

        override fun onPostExecute(token: String?) {
            Util.log("Access token retrieved:$token")
            activityReference.get()!!.mAuthToken = token
            activityReference.get()!!.mPasswd = activityReference.get()!!.mAuthToken
            activityReference.get()!!.validateGoogleSignup()
        }
    }

    fun getProfileInformation(account: GoogleSignInAccount, mGoogleApiClient: GoogleApiClient?) {
        Util.showProgress(this)

        this.mGoogleApiClient = mGoogleApiClient

        mEmail = account.email
        mName = account.displayName //currentPerson.getDisplayName();
        val id = account.id // 숫자로 구성된 고유값
        mPasswd = "qazqazqaz"

        mProfileUrl = "" // 실제로 안씀 //temp_url.substring(0,lastIndexOf);

        if (Const.FEATURE_AUTH2) {
            Util.setPreference(this@AuthActivity, Const.KEY_DOMAIN, Const.DOMAIN_GOOGLE)
            // ID는 email, 암호는 access token으로 보내자
            mPasswd = mAuthToken

            val task = AuthTask(this)
            task.execute(account)
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
                        setAgreementFragment(Const.DOMAIN_GOOGLE, AgreementFragment.LOGIN_GOOGLE)

                        //                    GoogleMoreFragment mGoogleMoreFragment = GoogleMoreFragment.newInstance(mEmail, mName, mPasswd, mProfileUrl,mGoogleApiClient);
//                    FragmentManager mFragmentManager = getSupportFragmentManager();
//                    mFragmentManager.beginTransaction()
//                            .replace(android.R.id.content, mGoogleMoreFragment)
//                            .addToBackStack(null).commit();
                    } else {
                        if (Const.FEATURE_AUTH2) {
                            Util.setPreference(this@AuthActivity, Const.KEY_DOMAIN, Const.DOMAIN_GOOGLE)
                        }

                        // 이메일 가입자가 같은 이메일계정의 facebook/google 로그인시 처리
                        if (!response.optString(Const.KEY_DOMAIN)
                                .equals(Const.DOMAIN_GOOGLE, ignoreCase = true)
                        ) {
                            // facebook 로그인이 아닌 다른 로그인으로 가입되어있음
                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn2(
                                this@AuthActivity,
                                null,
                                getString(net.ib.mn.R.string.confirm_change_account),
                                net.ib.mn.R.string.yes,
                                net.ib.mn.R.string.no,
                                false,
                                true,
                                { v: View? ->
                                    Util.showProgress(this@AuthActivity)
                                    registerDevice(this@AuthActivity, wrap)
                                    PushyUtil.registerDevice(this@AuthActivity, wrap)
                                    Util.closeIdolDialog()
                                },
                                { v: View? ->
                                    if (mGoogleApiClient!!.isConnected) {
                                        mGoogleApiClient!!.clearDefaultAccountAndReconnect()
                                    }
                                    Util.closeIdolDialog()
                                })
                        } else {
                            registerDevice(this@AuthActivity, wrap)
                            PushyUtil.registerDevice(this@AuthActivity, wrap)
                        }
                    }
                },
                errorListener = {
                    Util.closeProgress()

                    val mGoogleMoreFragment = newInstance(mEmail, mName, mPasswd, "")
                    val mFragmentManager = supportFragmentManager
                    mFragmentManager.beginTransaction()
                        .replace(binding.fragmentContainer.id, mGoogleMoreFragment)
                        .addToBackStack(null).commit()
                }
            )
        }
    }

    fun googleApiclientClose() {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient!!.isConnected) {
                mGoogleApiClient!!.disconnect()
            }
        }
    }

    fun googleApiclientCloseLogout() {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient!!.isConnected) {
//                Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                mGoogleApiClient!!.disconnect()
                //                mGoogleApiClient.connect();
            }
        }
    }

    fun requestFacebookMe(loginResult: LoginResult) {
        Util.showProgress(this, true)

        val accessToken = loginResult.accessToken
        mAuthToken = accessToken.token
        Util.log("FACEBOOK ACCESS TOKEN=$accessToken")

        val request = GraphRequest.newMeRequest(
            loginResult.accessToken
        ) { `object`: JSONObject?, response: GraphResponse? ->
            if (`object` == null) {
                Util.closeProgress()
                LoginManager.getInstance().logOut()
                Util.showIdolDialogWithBtn1(
                    this@AuthActivity,
                    null,
                    getString(net.ib.mn.R.string.facebook_no_email)
                ) { v: View? -> Util.closeIdolDialog() }
                return@newMeRequest
            }
            // Application code
            Util.log("requestFacebookMe $`object`")
            val id = `object`.optString("id")
            mName = `object`.optString("name")
            mEmail = `object`.optString("email")

            // 사용자가 email 제공을 거부한 경우
            if (mEmail == null || mEmail!!.length == 0) {
                Util.closeProgress()
                LoginManager.getInstance().logOut()
                Util.showIdolDialogWithBtn1(
                    this@AuthActivity,
                    null,
                    getString(net.ib.mn.R.string.facebook_no_email)
                ) { v: View? -> Util.closeIdolDialog() }
                return@newMeRequest
            }

            ProgressDialogFragment.show(
                this@AuthActivity, Const.DOMAIN_FACEBOOK,
                net.ib.mn.R.string.lable_get_info
            )

            mPasswd = mAuthToken

            // 기존에 아이디를 갖고 회원가입을 했었는지 확인한다.
            Util.log("requestFacebookMe  mEmail :$mEmail   mName:$mName   id:$id")
            lifecycleScope.launch {
                usersRepository.validate(
                    type = "email",
                    value = mEmail,
                    appId = AppConst.APP_ID,
                    listener = { response ->
                        ProgressDialogFragment.hide(this@AuthActivity, "facebook")
                        if (response.optBoolean("success")) {
                            Util.closeProgress()
                            setAgreementFragment(
                                Const.DOMAIN_FACEBOOK,
                                AgreementFragment.LOGIN_FACEBOOK
                            )
                        } else {
                            Util.closeProgress()
                            if (Const.FEATURE_AUTH2) {
                                Util.setPreference(
                                    this@AuthActivity,
                                    Const.KEY_DOMAIN,
                                    Const.DOMAIN_FACEBOOK
                                )
                            }

                            // 이메일 가입자가 같은 이메일계정의 facebook/google 로그인시 처리
                            if (!response.optString(Const.KEY_DOMAIN)
                                    .equals(Const.DOMAIN_FACEBOOK, ignoreCase = true)
                            ) {
                                // facebook 로그인이 아닌 다른 로그인으로 가입되어있음
                                Util.showDefaultIdolDialogWithBtn2(
                                    this@AuthActivity,
                                    null,
                                    getString(net.ib.mn.R.string.confirm_change_account),
                                    { v: View? ->
                                        Util.closeIdolDialog()
                                        Util.showProgress(this@AuthActivity)
                                        registerDevice(this@AuthActivity, wrap)
                                        PushyUtil.registerDevice(this@AuthActivity, wrap)
                                    },
                                    { v: View? ->
                                        LoginManager.getInstance().logOut()
                                        Util.closeIdolDialog()
                                    })
                            } else {
                                registerDevice(this@AuthActivity, wrap)
                                PushyUtil.registerDevice(this@AuthActivity, wrap)
                            }
                        }
                    },
                    errorListener = {
                        setAgreementFragment(
                            Const.DOMAIN_FACEBOOK,
                            AgreementFragment.LOGIN_FACEBOOK
                        )
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
        val frag = AgreementFragment()
        frag.email = mEmail
        frag.displayName = mName
        frag.password = mPasswd
        frag.domain = domain
        frag.loginType = type
        mFragmentManager.beginTransaction()
            .replace(R.id.content, frag)
            .addToBackStack(null).commit()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        val deniedPermissions: MutableList<String> = ArrayList()

        for (perm in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    perm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                deniedPermissions.add(perm)
            }
        }

        if (!deniedPermissions.isEmpty()) {
            val deniedPerms = deniedPermissions.toTypedArray<String>()
            ActivityCompat.requestPermissions(this, deniedPerms, REQUEST_POST_NOTIFICATIONS)
        }
    }

    companion object {
        //line
        private var lineApiClient: LineApiClient? = null

        @JvmStatic
        fun createIntent(context: Context?): Intent {
            return Intent(context, AuthActivity::class.java)
        }
    }
}
