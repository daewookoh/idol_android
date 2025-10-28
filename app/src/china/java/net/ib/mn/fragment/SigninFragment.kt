package net.ib.mn.fragment

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import net.ib.mn.R
import net.ib.mn.activity.AuthActivity
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util

/**
 * 사용자가 로그인 안했을시 로그인프래그먼트로 가게된다.
 */

class SigninFragment : BaseFragment(), View.OnClickListener {
    private var mFragmentManager: FragmentManager? = null


    private val clickedCheck = false

    private var mDomain: String? = null

    private var mEmailLogin: TextView? = null
    private var mRootLayout: ScrollView? = null

    // wechat
    private var api: IWXAPI? = null
    private var mWechatBtn: ImageButton? = null

    // QQ
    private var mQQBtn: ImageButton? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mWechatBtn = view.findViewById(R.id.btn_wechat)
        mQQBtn = view.findViewById(R.id.btn_qq)
        mRootLayout = view.findViewById(R.id.scv_sign_in_root)
        mEmailLogin = view.findViewById(R.id.tv_email_login)

        regToWx()

        // pushy
        // Check whether the user has granted us the READ/WRITE_EXTERNAL_STORAGE permissions
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request both READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE so that the
            // Pushy SDK will be able to persist the device token in the external storage
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                0
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signin, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mFragmentManager = requireActivity().supportFragmentManager

        mWechatBtn!!.setOnClickListener(this)
        mQQBtn!!.setOnClickListener(this)
        mEmailLogin!!.setOnClickListener(this)

        if (Build.VERSION.SDK_INT >= 23) {
            requireActivity().window.statusBarColor =
                resources.getColor(R.color.text_white_black)
            if (Util.isUsingNightModeResources(context)) {
                requireActivity().window.decorView.systemUiVisibility = 0
            } else {
                requireActivity().window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        //키보드값이 없을경우만 실행해준다.
        if (Util.getPreferenceInt(requireActivity(), Const.KEYBOARD_HEIGHT, -1) == -1) {
            val rootHeight = intArrayOf(-1)

            //키보드 높이 측정.
            mRootLayout!!.viewTreeObserver.addOnGlobalLayoutListener {
                if (rootHeight[0] == -1) {
                    rootHeight[0] = mRootLayout!!.height
                }
                val visibleFrameSize = Rect()
                mRootLayout!!.getWindowVisibleDisplayFrame(visibleFrameSize)
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
                    Util.setPreference(
                        requireActivity(),
                        Const.KEYBOARD_HEIGHT,
                        keyboardHeight
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_GET_ACCOUNTS -> {
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                }
                return
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_wechat -> if (api!!.isWXAppInstalled) {
                mDomain = Const.DOMAIN_WECHAT
                sendWeChatAuth()
            } else {
                Util.showDefaultIdolDialogWithBtn1(
                    activity, null, getString(R.string.wechat_not_installed)
                ) { Util.closeIdolDialog() }
            }

            R.id.btn_qq -> {
                mDomain = Const.DOMAIN_QQ
                sendQQAuth()
            }

            R.id.tv_email_login -> mFragmentManager!!.beginTransaction()
                .replace(R.id.fragment_container, EmailSigninFragment())
                .addToBackStack(null).commit()
        }
    }

    override fun onDetach() {
        Util.log("Signinfragment onDetach ")
        super.onDetach()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Util.log("Signinfragment onActivityResult $requestCode")
    }

    //It is recommended to dynamically monitor WeChat to start the broadcast to register to WeChat.
    var wechatReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Register the app to WeChat

            api!!.registerApp(Const.WECHAT_APP_ID)
        }
    }

    // wechat
    private fun regToWx() {
        // Get an instance of IWXAPI through the WXAPIFactory factory
        api = WXAPIFactory.createWXAPI(activity, Const.WECHAT_APP_ID, false)

        // Register the appId of the app to WeChat
        api?.registerApp(Const.WECHAT_APP_ID)

        val wechatFilter = IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP)
        if (context != null) {
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(wechatReceiver, wechatFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        if (context != null) {
            LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(wechatReceiver)
        }
    }

    private fun sendWeChatAuth() {
        val req = SendAuth.Req()
        req.scope = "snsapi_userinfo"
        req.state = "wechat_login"
        api!!.sendReq(req)
    }

    // QQ
    private fun sendQQAuth() {
        (activity as AuthActivity).sendQQAuth()
    }

    companion object {
        // android 6.0+
        private const val PERMISSION_REQUEST_GET_ACCOUNTS = 1
        private const val PERMISSION_REQUEST_GET_ACCOUNTS_CONNECTED = 2

        private const val RC_SIGN_IN = 0
    }
}
