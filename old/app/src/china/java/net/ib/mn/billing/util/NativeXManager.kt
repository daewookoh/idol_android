package net.ib.mn.billing.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.channel.helper.ChannelUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.NativeXActivity
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.utils.Util
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URLEncoder
import java.util.Date
import java.util.Enumeration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * NativeX Payment Manager
 *
 * see https://doc.xplorecn.com/document/payment-v3.html#_3-interface-specification
 */
class NativeXManager : CoroutineScope {
    companion object {
        @Volatile
        private var instance: NativeXManager? = null

        @JvmStatic
        val REQUEST_CODE = 9478
        @JvmStatic
        val RESULT_CLOSE = 9479
        @JvmStatic
        val APP_KEY = "a04c56787dfe7172"
//        val APP_SECRET = "NRW6l2cqYGQ0OP-sb4DJqoC8AQyS2Q7j"
        @JvmStatic
        val EXTRA_PARAMS = "params"
        @JvmStatic
        val EXTRA_TRADENO = "trade_no"
        @JvmStatic
        val EXTRA_SIGNATURE = "signature"

        @JvmStatic
        val appId = 50
        @JvmStatic
        fun getInstance(): NativeXManager =
            instance ?: synchronized(this) {
                instance ?: NativeXManager().also {
                    instance = it
                    instance?.job = Job()
                }
            }

        fun destroyInstance() {
            instance = null
        }
    }

    protected lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    suspend fun getTradeNo(context: Context, skuCode: String, usersRepository: UsersRepository, scope: CoroutineScope) = suspendCoroutine<String> { cont ->
        scope.launch {
            usersRepository.createNativeXOrder(
                skuCode,
                { response ->
                    val trade_no = response?.optString("trade_no")
                    if( trade_no != null ) {
                        cont.resume(trade_no!!)
                    } else {
                        Toast.makeText(context,
                            R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                            .show()
                        cont.resumeWithException(Exception())
                    }
                }, {
                    Toast.makeText(context,
                        R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                        .show()
                    cont.resumeWithException(Exception())
                }
            )
        }
    }

    suspend fun getSignature(context: Context, body: String, tradeNo: String, usersRepository: UsersRepository, scope: CoroutineScope) = suspendCoroutine<String> { cont ->
        scope.launch {
            usersRepository.getNativeXSignature(
                body,
                tradeNo,
                { response ->
                    val signature = response?.optString("sign")
                    if( signature != null ) {
                        cont.resume(signature!!)
                    } else {
                        Toast.makeText(context,
                            R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                            .show()
                        cont.resumeWithException(Exception())
                    }
                }, {
                    Toast.makeText(context,
                        R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                        .show()
                    cont.resumeWithException(Exception())
                }
            )
        }
    }

    fun purchase(
        context: Context,
        userId: Int,
        email: String,
        skuCode: String,
        itemName: String,
        amount: Double,
        currency: String,
        usersRepository: UsersRepository,
        scope: CoroutineScope,
    ) {

        launch {
            // 주문번호 생성
            try {
                val trade_no = getTradeNo(context, skuCode, usersRepository, scope)
                val ip = getLocalIpAddress()

                val stime = Date().time / 1000
                val pkg = ChannelUtils.getChannelInfo(context)
//                val pkgSignature = Util.md5("com.exodus.myloveidol.china${stime}${APP_SECRET}")
//                val pkg = "{\"source_id\":\"com.exodus.myloveidol.china\",\"channel_id\":\"xiaomi\",\"version_code\":\"1.0\",\"oaid\":\"xx\",\"ua\":\"xx\"," +
//                        "\"imei\":\"xx\",\"android_id\":\"xx\",\"ip\":\"${ip}\",\"os_version\":\"10.0\",\"stime\":\"${stime}\",\"sign\":\"${pkgSignature}\"}"
//                val pkg = "{}"

                val package_encoded = URLEncoder.encode(pkg, "UTF-8")
                val product_desc = itemName
                val product_desc_encoded = URLEncoder.encode(product_desc, "UTF-8")
                // calc signature
                var params = "amount=${amount}&appid=${appId}&package_data=${pkg}&product_desc=${product_desc}&timestamp=${stime}&trade_no=${trade_no}&trader_name=${skuCode}&user_ip=${ip}"
                val hash = getSignature(context, params, trade_no, usersRepository, scope)
//                val hash = Util.md5(params+APP_SECRET)

                val intent = Intent(context, NativeXActivity::class.java)
                params = "amount=${amount}&appid=${appId}&package_data=${package_encoded}&product_desc=${product_desc_encoded}&timestamp=${stime}&trade_no=${trade_no}&trader_name=${skuCode}&user_ip=${ip}"
                intent.putExtra(EXTRA_PARAMS, params)
                intent.putExtra(EXTRA_TRADENO, trade_no)
                intent.putExtra(EXTRA_SIGNATURE, hash)
                (context as BaseActivity).startActivityForResult(intent, REQUEST_CODE)
            } catch (e: Exception) {
                e.printStackTrace()
                Util.closeProgress()
            }
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress() && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return ""
    }
}