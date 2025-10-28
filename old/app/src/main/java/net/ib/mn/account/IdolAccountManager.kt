package net.ib.mn.account

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.domain.usecase.GetUserSelfUseCase
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.safeActivity
import org.json.JSONException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdolAccountManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private var getUserSelfUseCase: GetUserSelfUseCase,
){
    fun reinit(getUserSelfUseCase: GetUserSelfUseCase) {
        this.getUserSelfUseCase = getUserSelfUseCase
    }

    fun fetchUserInfo(
        context: Context?,
        success: () -> Unit = {},
        failure: (String?) -> Unit = {},
    ) {
        val account = IdolAccount.getAccount(context)
        val ts: Int = account?.userModel?.ts ?: 0

        MainScope().launch {
            val result = getUserSelfUseCase(ts).first()
            // 304 Not Modified 처리
            if (result.code == 304) {
                // 변경된 것이 없으므로 성공 처리
                success()
                return@launch
            }
            if(!result.success || result.data == null) {
                Util.log("userSelf onErrorResponse${result.message}")
                failure(result.message)
                return@launch
            }

            val response = result.data ?: return@launch
            try {
                // 점검중 처리
                if (response.optInt("mcode", 0) == 1) {
                    context?.safeActivity?.let { activity ->
                        val msg = response.optString("msg")
                        Util.showDefaultIdolDialogWithBtn1(activity,
                            null,
                            msg,
                            R.drawable.img_maintenance
                        ) {
                                Util.closeIdolDialog()
                                activity.finishAffinity()
                        }
                    }

                    failure(Const.MSG_UNDER_MAINTENANCE)
                    return@launch
                }
                if (response.optInt("statusCode", 0) != 304) {
                    account?.setUserInfo(context, response)
                }

                if (context is BaseActivity) {
                    if (context.isAlive) {
                        success()
                    } else {
                        Util.log("skip fetchUserInfo response")
                    }
                } else {
                    success()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                failure(e.toString())
            }
        }
    }
}