package net.ib.mn.billing.util

//import com.paymentwall.alipayadapter.PsAlipay
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.paymentwall.pwunifiedsdk.core.PaymentSelectionActivity
import com.paymentwall.pwunifiedsdk.core.UnifiedRequest
import com.paymentwall.pwunifiedsdk.util.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util


// PaymentWall Manager

class PWManager {
    object ALIPAY {
        const val APP_ID = "external"
        const val PAYMENT_TYPE = "1"
        const val IT_B_PAY = "30m"
        const val FOREX_BIZ = "FP"
        const val APPENV = "system=android^version=3.0.1.2"
    }

    companion object {
        @Volatile private var instance: PWManager? = null

        @JvmStatic fun getInstance(): PWManager =
                instance ?: synchronized(this) {
                    instance ?: PWManager().also {
                        instance = it
                    }
                }

        fun destroyInstance() {
            instance = null
        }
    }

    fun purchase( context:Context, usersRepository: UsersRepository, scope: CoroutineScope, userId:Int, email: String, skuCode: String, itemName:String, amount:Double, currency: String, imageUrl: String ) {
        val widget = "pw_1"

        val request = UnifiedRequest()
        if( BuildConfig.DEBUG ) {
            request.setPwProjectKey(Const.PAYMENTWALL_KEY_TEST)
            request.setPwSecretKey(Const.PAYMENTWALL_SECRET_TEST)
        } else {
            request.setPwProjectKey(Const.PAYMENTWALL_KEY)
        }
//        request.setPwSecretKey(Const.PAYMENTWALL_SECRET)

        request.amount = amount
        request.currency = currency
        request.itemName = itemName
        request.itemId = skuCode
        request.userId = "${userId}"
//        request.itemUrl = imageUrl
        request.itemResID = R.drawable.login_logo
        request.timeout = 30000
        request.signVersion = 2

        request.addPwLocal()
        request.addPwlocalParams(com.paymentwall.sdk.pwlocal.utils.Const.P.EMAIL, "${email}")
        request.addPwlocalParams(com.paymentwall.sdk.pwlocal.utils.Const.P.WIDGET, widget)
        val history = (System.currentTimeMillis() / 1000).toString()
        request.addPwlocalParams("history[registration_date]", history)

        // signature는 아래 URL 형식처럼 생성해야 함
        var params = "ag_external_id=${skuCode}&ag_name=${itemName}&ag_type=fixed&amount=${amount}&currencyCode=${currency}&email=${email}&history[registration_date]=${history}&key=${Const.PAYMENTWALL_KEY}&sign_version=2&success_url=pwlocal%3A%2F%2Fpaymentsuccessful&uid=${userId}&widget=${widget}"

        // Test mode
        if( BuildConfig.DEBUG ) {
            request.setTestMode(false)
            request.addPwlocalParams(com.paymentwall.sdk.pwlocal.utils.Const.P.EVALUATION, "1")
            params += "&evaluation=1"

            val intent = Intent(context, PaymentSelectionActivity::class.java)
            intent.putExtra(Key.REQUEST_MESSAGE, request)
            (context as Activity).startActivityForResult(intent, PaymentSelectionActivity.REQUEST_CODE)

            return
        }

        scope.launch {
            usersRepository.getPaymentWallSignature(
                "${ServerUrl.HOST}/${ServerUrl.PREFIX}/users/paymentwall_signature/?${params}",
                { response ->
                    val sig = response.optString("sig")
                    if(!sig.isNullOrEmpty()) {
                        // 생성된 시그니처는 custom param으로 넣어준다
                        request.addCustomParam("sign", sig)

//                    val alipay = PsAlipay()
//                    alipay.setAppId(ALIPAY.APP_ID)
//                    alipay.setPaymentType(ALIPAY.PAYMENT_TYPE)
//                    // extra params for international account
////                    alipay.setItbPay(ALIPAY.IT_B_PAY)
////                    alipay.setForexBiz(ALIPAY.FOREX_BIZ)
////                    alipay.setAppenv(ALIPAY.APPENV)
//                    alipay.privateKey = Const.PAYMENTWALL_SECRET
////                    alipay.setPwSign(sig)
//
//                    val alipayPs = ExternalPs("alipay", "Alipay", R.drawable.icon48_appwx_logo, alipay)
//                    request.add(alipayPs)

                        val intent = Intent(context, PaymentSelectionActivity::class.java)
                        intent.putExtra(Key.REQUEST_MESSAGE, request)
                        (context as Activity).startActivityForResult(intent, PaymentSelectionActivity.REQUEST_CODE)
                    } else {
                        Util.showIdolDialogWithBtn1(context,
                            null,
                            context!!.getString(R.string.error_abnormal_exception)
                        ) {
                            Util.closeIdolDialog()
                        }
                    }
                }, { throwable ->
                    Util.showIdolDialogWithBtn1(context,
                        null,
                        throwable.message
                    ) {
                        Util.closeIdolDialog()
                    }
                }
            )
        }
    }

}