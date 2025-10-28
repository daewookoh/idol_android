package net.ib.mn.dialog

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import net.ib.mn.R


class LoadingHeartDialogFragment : DialogFragment() {

    private var frameAnimation: AnimationDrawable? = null
    private lateinit var ivLoadingHeart: AppCompatImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_loading_heart, container)
    }

    override fun onStart() {
        super.onStart()

        if (dialog == null) return

        val lpWindow = WindowManager.LayoutParams()
        val window = dialog!!.window

        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER

        window?.attributes = lpWindow
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onStop() {
        super.onStop()
        if (frameAnimation?.isRunning!!) frameAnimation?.stop()
    }

    override fun onResume() {
        super.onResume()
        if (frameAnimation?.isVisible!!) frameAnimation?.start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)

        ivLoadingHeart = view.findViewById(R.id.iv_loading_heart)
        ivLoadingHeart.setBackgroundResource(R.drawable.loading_heart)

        try {
            frameAnimation = ivLoadingHeart.background as AnimationDrawable
            frameAnimation?.start()
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }
    }

//    override fun show(manager: FragmentManager?, tag: String?) {
//        try {
//            manager.beginTransaction()
//                    .replace()
//        } catch (e: Exception) {
//            super.show(manager, tag)
//        }
//    }

    companion object {
        fun getInstance(): LoadingHeartDialogFragment {
            return LoadingHeartDialogFragment()
        }
    }
}