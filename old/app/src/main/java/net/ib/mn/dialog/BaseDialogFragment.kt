package net.ib.mn.dialog

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.activity.BaseActivity
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
open class BaseDialogFragment : DialogFragment() {
    var firebaseAnalytics: FirebaseAnalytics? = null

    interface DialogResultHandler {
        fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?)
    }

    private var mRequestCode = 0
    private var mResultCode = 0
    private var mResult: Intent? = null
    private var mNotifyResult = true

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val r = resources
            val width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 290f, r.displayMetrics
            ).toInt()

            if (dialog.window != null) {
                dialog.window!!.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
                dialog.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
        }
    }

    fun dismiss(notifyOnDismiss: Boolean) {
        mNotifyResult = notifyOnDismiss
        dismiss()
    }

    fun setActivityRequestCode(requestCode: Int) {
        mRequestCode = requestCode
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (mNotifyResult) {
            val target = targetFragment
            if (target != null && target is DialogResultHandler) {
                val handler = target as DialogResultHandler
                handler.onDialogResult(
                    targetRequestCode, mResultCode,
                    mResult
                )
            }
            val parent: Activity? = activity
            if (parent is DialogResultHandler) {
                val handler = parent as DialogResultHandler
                handler.onDialogResult(
                    mRequestCode, mResultCode,
                    mResult
                )
            }
        }
    }

    val baseActivity: BaseActivity?
        get() = activity as BaseActivity?

    fun setResultCode(resultCode: Int) {
        mResultCode = resultCode
    }

    fun setResult(result: Intent?) {
        mResult = result
    }

    /**
     * Bug fix in dismissing when screen rotate, although retainInstance set
     * true
     */
    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog!!.setDismissMessage(null)
        }
        super.onDestroyView()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            if (manager.findFragmentByTag(tag) == null) {
                super.show(manager, tag)
            }
        } catch (e: IllegalStateException) {
            // onSaveInstanceState 함수 호출 이후, commit 함수를 호출할 때 나는 IllegalStateException 대응
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        }
    }

    protected fun setUiActionFirebaseGoogleAnalyticsDialogFragment(action: String?, label: String) {
        if (BuildConfig.CHINA) {
            return
        }
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(
                requireActivity()
            )
            firebaseAnalytics!!.setCurrentScreen(
                requireActivity(),
                javaClass.simpleName, null
            )

            val params = Bundle()
            params.putString("ui_action", action)
            firebaseAnalytics!!.logEvent(javaClass.simpleName + "_" + label, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val RESULT_CANCEL: Int = 0
        const val RESULT_OK: Int = 1
    }
}
