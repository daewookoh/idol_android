package com.exodus.myloveidol.china.wxapi

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.AuthActivity
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.fragment.SigninFragment
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import javax.inject.Inject

const val GET_TOKEN = 1

@AndroidEntryPoint
class WXEntryActivity : BaseActivity(), IWXAPIEventHandler {
    private var api: IWXAPI? = null
    private var handler: MyHandler? = null
    @Inject
    lateinit var usersRepository: UsersRepository

    private class MyHandler(wxEntryActivity: WXEntryActivity) : Handler() {
        private val wxEntryActivityWeakReference: WeakReference<WXEntryActivity>
        override fun handleMessage(msg: Message) {
            val tag = msg.what
            when (tag) {
                GET_TOKEN -> {
                    val data = msg.data
                    var json: JSONObject? = null
                    try {
                        json = JSONObject(data.getString("result"))
                        val openId: String
                        val accessToken: String
                        val refreshToken: String
                        val scope: String
                        openId = json.getString("openid")
                        accessToken = json.getString("access_token")
                        refreshToken = json.getString("refresh_token")
                        scope = json.getString("scope")
                        val intent = Intent(wxEntryActivityWeakReference.get(), SigninFragment::class.java)
                        intent.putExtra("openId", openId)
                        intent.putExtra("accessToken", accessToken)
                        intent.putExtra("refreshToken", refreshToken)
                        intent.putExtra("scope", scope)
                        wxEntryActivityWeakReference.get()!!.startActivity(intent)
                    } catch (e: JSONException) {
//                        Log.e(WXEntryActivity.TAG, e.message)
                    }
                }
            }
        }

        init {
            wxEntryActivityWeakReference = WeakReference(wxEntryActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = WXAPIFactory.createWXAPI(this, Const.WECHAT_APP_ID, false)
        handler = MyHandler(this)
        try {
            val intent = intent
            this.api?.handleIntent(intent, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        api!!.handleIntent(intent, this)
    }

    private fun showError(text: String?) {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.Theme_AppCompat_Dialog_Alert))
                .setMessage(text)
                .setPositiveButton(R.string.confirm, null)
                .show()
    }

    override fun onResp(resp: BaseResp?) {
        if( resp == null || resp.errCode != BaseResp.ErrCode.ERR_OK){
            // Signed out, show unauthenticated UI.
            Toast.makeText(this, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
            return
        }

        val authResp = resp as SendAuth.Resp
        val code = authResp.code

        lifecycleScope.launch {
            usersRepository.getWechatToken(
                code,
                { response ->
                    Util.closeProgress()
                    if (response.optBoolean("success")) {
                        val accessToken = response.optString("access_token")
                        val unionId = response.optString("unionid")

                        val i = Intent(this@WXEntryActivity, AuthActivity::class.java)
                        i.putExtra(Const.PARAM_WECHAT_ACCESS_TOKEN, accessToken)
                        i.putExtra(Const.PARAM_WECHAT_UNIONID, unionId)
                        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        startActivity(i)
                        finish()

                    } else {
                        showError(response.optString("msg"))
                    }
                }, {
                    Util.closeProgress()
                    showError(getString(R.string.line_login_failed) + "\n\n" + it.message)
                }
            )
        }
    }

    override fun onReq(p0: BaseReq?) {
    }
}