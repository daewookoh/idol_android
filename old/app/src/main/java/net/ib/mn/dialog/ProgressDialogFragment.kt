package net.ib.mn.dialog

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import net.ib.mn.activity.BaseActivity

class ProgressDialogFragment : DialogFragment() {
    private interface OnProgressDialogCancelListener {
        fun onCancelled(tag: String?)
    }

    private var mTag = "progress"
    private var mMessage = "loading.."
    private var mCancellable = false

    fun setOptions(tag: String, message: String, cancellable: Boolean) {
        mTag = tag
        mMessage = message
        mCancellable = cancellable
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity: Activity? = activity
        val dialog = ProgressDialog(activity)
        dialog.setMessage(mMessage)
        dialog.setCancelable(mCancellable)
        if (mCancellable && activity is OnProgressDialogCancelListener) {
            dialog.setOnCancelListener {
                (activity as OnProgressDialogCancelListener)
                    .onCancelled(mTag)
            }
        }
        dialog.setCanceledOnTouchOutside(false)
        dialog.isIndeterminate = true
        isCancelable = mCancellable
        return dialog
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog!!.setDismissMessage(null)
        }
        super.onDestroyView()
    }

    companion object {
        fun show(activity: BaseActivity?, tag: String, msgResId: Int) {
            show(activity ?: return, tag, activity.getString(msgResId), false)
        }

        fun show(
            activity: BaseActivity?, tag: String, msgResId: Int,
            cancellable: Boolean
        ) {
            show(activity ?: return, tag, activity.getString(msgResId), cancellable)
        }

        @JvmOverloads
        fun show(
            activity: BaseActivity?, tag: String, message: String,
            cancellable: Boolean = false
        ) {
            // IllegalStateException 방지
            try {
                hideAll(activity)
                val manager = activity?.supportFragmentManager ?: return
                val dialog = ProgressDialogFragment()
                dialog.setOptions(tag, message, cancellable)
                dialog.show(manager, tag)
            } catch (e: Exception) {
            }
        }

        fun hide(activity: BaseActivity?, tag: String?) {
            if (activity != null) {
                // IllegalStateException 방지 -> show에는 붙어있지만 왜 hide에는 안붙어있지...;;;
                try {
                    val manager = activity.supportFragmentManager
                    val dialog = manager.findFragmentByTag(tag)
                    if (dialog != null && dialog is ProgressDialogFragment) {
                        (dialog as DialogFragment).dismiss()
                    }
                } catch (e: Exception) {
                }
            }
        }

        fun hideAll(activity: BaseActivity?) {
            val manager = activity?.supportFragmentManager
            val fragments = manager?.fragments
            if (fragments != null) {
                for (fragment in fragments) {
                    if (fragment is ProgressDialogFragment) {
                        fragment.dismiss()
                    }
                }
            }
        }
    }
}
