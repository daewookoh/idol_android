package net.ib.mn.fragment

import android.Manifest
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentManager
import com.facebook.FacebookSdk.isInitialized
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.linecorp.linesdk.Scope
import com.linecorp.linesdk.auth.LineAuthenticationParams
import com.linecorp.linesdk.auth.LineLoginApi
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.AuthActivity
import net.ib.mn.databinding.FragmentSigninBinding
import net.ib.mn.utils.AppConst
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.permission.PermissionHelper.PermissionListener
import net.ib.mn.utils.permission.PermissionHelper.requestPermissionIfNeeded
import net.ib.mn.utils.setFirebaseScreenViewEvent
import net.ib.mn.utils.setFirebaseUIAction
import java.util.Arrays


class SigninFragment : BaseFragment(), View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {
    private var mFragmentManager: FragmentManager? = null

    // Google client to interact with Google API
    private var mGoogleApiClient: GoogleApiClient? = null
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        binding.scvSignInRoot.viewTreeObserver?.removeOnGlobalLayoutListener(layoutListener)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (BuildConfig.CELEB) {
            binding.ivLoginMain.setImageResource(R.drawable.img_login_main_celeb)
            binding.ivAppName.setImageResource(R.drawable.img_login_logo_celeb)
        }

        // facebook login
        if (!isInitialized()) {
            sdkInitialize(requireActivity())
        }
        LoginManager.getInstance().logOut()

        // 권한 요청 처리기 등록
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                binding.btnGoogleSignup.callOnClick()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initializing google plus api client
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestEmail()
            .build()

        mGoogleApiClient = GoogleApiClient.Builder(requireActivity())
            .enableAutoManage(
                requireActivity(),  /* FragmentActivity */
                this /* OnConnectionFailedListener */
            )
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.btnGoogleSignup.setOnClickListener(this)
        binding.btnKakaoLogin.setOnClickListener(this)
        binding.btnSigninLine.setOnClickListener(this)
        binding.btnFacebook.setOnClickListener(this)
        mFragmentManager = requireActivity().supportFragmentManager
        binding.tvEmailLogin.setOnClickListener(this)

        //키보드값이 없을경우만 실행해준다.
        if (Util.getPreferenceInt(requireActivity(), Const.KEYBOARD_HEIGHT, -1) == -1) {
            val rootHeight = intArrayOf(-1)

            //키보드 높이 측정.
            layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val safeBinding = _binding ?: return

                    if (rootHeight[0] == -1) {
                        rootHeight[0] = safeBinding.scvSignInRoot.height
                    }
                    val visibleFrameSize = Rect()
                    safeBinding.scvSignInRoot.getWindowVisibleDisplayFrame(visibleFrameSize)
                    val heightExpectKeyBoard =
                        visibleFrameSize.bottom - visibleFrameSize.top
                    if (heightExpectKeyBoard < rootHeight[0] && Util.getPreferenceInt(
                            requireActivity(),
                            Const.KEYBOARD_HEIGHT,
                            -1
                        ) == -1
                    ) {
                        //키보드 높이 계산.
                        val keyboardHeight = rootHeight[0] - heightExpectKeyBoard
                        if (keyboardHeight > Const.MINIMUM_EMOTICON_KEYBOARD_HEIGHT) {
                            Util.setPreference(
                                requireActivity(),
                                Const.KEYBOARD_HEIGHT,
                                keyboardHeight
                            )
                        }
                    }
                }
            }
            binding.scvSignInRoot.viewTreeObserver.addOnGlobalLayoutListener(layoutListener!!)
        }

        if (arguments != null) {
            val isEmailSignUp = requireArguments().getString(Const.IS_EMAIL_SIGNUP)
            if (isEmailSignUp != null) {
                mFragmentManager!!.beginTransaction()
                    .replace(R.id.fragment_container, EmailSigninFragment())
                    .addToBackStack(null).commit()
            }
            arguments = null
        }
    }

    override fun onResume() {
        super.onResume()

        setFirebaseScreenViewEvent(GaAction.LOGIN, javaClass.simpleName)
        ViewCompat.requestApplyInsets(requireView())
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_google_signup -> {
                setFirebaseUIAction(GaAction.LOGIN_GOOGLE)

                // android 6.0 처리
                // 방통위 규제사항 처리
                val permissions = arrayOf(Manifest.permission.GET_ACCOUNTS)
                val msgs = arrayOf(getString(R.string.permission_contact))
                requestPermissionIfNeeded(
                    activity, null, permissions,
                    msgs, PERMISSION_REQUEST_GET_ACCOUNTS,
                    object : PermissionListener {
                        override fun requestPermission(permissions: Array<String>) {
                            requestPermissionLauncher!!.launch(Manifest.permission.GET_ACCOUNTS)
                        }

                        override fun onPermissionAllowed() {
                            signInWithGplus()
                        }

                        override fun onPermissionDenied() {
                        }
                    }, true
                )
            }

            R.id.tv_email_login -> mFragmentManager!!.beginTransaction()
                .replace(R.id.fragment_container, EmailSigninFragment())
                .addToBackStack(null).commit()

            R.id.btn_kakao_login -> {
                setFirebaseUIAction(GaAction.LOGIN_KAKAO)

                (requireActivity() as AuthActivity).requestKakaoLogin()
            }

            R.id.btn_signin_line -> {
                setFirebaseUIAction(GaAction.LOGIN_LINE)

                val loginIntent = LineLoginApi.getLoginIntent(
                    v.context,
                    AppConst.CHANNEL_ID,
                    LineAuthenticationParams.Builder()
                        .scopes(Arrays.asList(Scope.PROFILE))
                        .build()
                )
                startActivityForResult(loginIntent, Const.LINE_REQUEST_CODE)
            }

            R.id.btn_facebook -> {
                setFirebaseUIAction(GaAction.LOGIN_FACEBOOK)

                LoginManager.getInstance().logInWithReadPermissions(
                    requireActivity(),
                    mutableListOf("email")
                )
            }
        }
    }


    /**
     * Sign-in into google
     */
    private fun signInWithGplus() {
//        mGoogleApiClient.connect();
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(
            mGoogleApiClient!!
        )
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onDetach() {
        Util.log("Signinfragment onDetach ")
        super.onDetach()
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient!!.isConnected) {
                mGoogleApiClient!!.disconnect()
            }
        }
    }

    override fun onConnectionSuspended(arg0: Int) {
        Util.log("Signinfragment onConnectionSuspended ")
        mGoogleApiClient!!.connect()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Util.log("Signinfragment onActivityResult $requestCode")
        if (requestCode == RC_SIGN_IN && data != null) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result!!)
        }
        // facebook login
        if ((requestCode and 0xffff) == 0xface) {
            (requireActivity() as AuthActivity).callbackManager.onActivityResult(
                requestCode, resultCode,
                data
            )
        }

        //line
        if (requestCode == Const.LINE_REQUEST_CODE) {
            (activity as AuthActivity).requestLineSignUp(data)
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        Util.log("handleSignInResult:" + result.isSuccess)
        if (result != null && result.isSuccess) {
            // Signed in successfully, show authenticated UI.
            val acct = result.signInAccount
            (activity as AuthActivity).getProfileInformation(acct!!, mGoogleApiClient!!)
        } else {
            // Signed out, show unauthenticated UI.
            makeText(activity, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // Google +
    override fun onConnectionFailed(result: ConnectionResult) {
        Util.log("Signinfragment onConnectionFailed ")
    }

    // Google +
    override fun onConnected(arg0: Bundle?) {
        Util.log("Signinfragment onConnected ")
    }

    companion object {
        // android 6.0+
        private const val PERMISSION_REQUEST_GET_ACCOUNTS = 1
        private const val PERMISSION_REQUEST_GET_ACCOUNTS_CONNECTED = 2

        /**
         * A flag indicating that a PendingIntent is in progress and prevents us
         * from starting further intents.
         */
        private const val RC_SIGN_IN = 0
    }
}
