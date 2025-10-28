package net.ib.mn.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.graphics.drawable.toDrawable
import net.ib.mn.R
import net.ib.mn.utils.Util

@AndroidEntryPoint
class VoteNotifyToastFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 다이얼로그 테두리 제거
        setStyle(STYLE_NO_FRAME, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.toast_vote_notify, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)

            val params = attributes
            params.y =  Util.convertDpToPixel(requireContext(), 40f).toInt()
            attributes = params

            setWindowAnimations(android.R.style.Animation_Dialog)
        }



        // 자동 dismiss
        Handler(Looper.getMainLooper()).postDelayed({
            dismissAllowingStateLoss()
        }, 2000)
    }
}