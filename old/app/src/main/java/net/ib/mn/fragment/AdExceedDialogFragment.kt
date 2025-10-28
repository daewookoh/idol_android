package net.ib.mn.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.BottomSheetAdExceedBinding

class AdExceedDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetAdExceedBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetAdExceedBinding.inflate(inflater, container, false)
        binding.apply {
            lifecycleOwner = this@AdExceedDialogFragment
            view = this@AdExceedDialogFragment
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subtitle = if (BuildConfig.CELEB) {
            getString(R.string.daily_ad_exceeded_subtitle_celeb)
        } else {
            getString(R.string.daily_ad_exceeded_subtitle_idol)
        }

        binding.tvSubtitle.text = subtitle + "\n" + getString(R.string.daily_ad_exceeded_desc)

        // TODO 뒤로가기로 닫히기 막고 싶으면 사용
//        dialog?.let {
//            it.setCanceledOnTouchOutside(false)
//            it.setOnKeyListener { _, keyCode, _ ->
//                keyCode == android.view.KeyEvent.KEYCODE_BACK
//            }
//        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(
        requireContext(),
        R.style.BottomSheetDialogRewardTheme,
    ).apply {
        // TODO 차후 드래그 막을거면 사용
//        behavior.state = BottomSheetBehavior.STATE_EXPANDED
//        behavior.isDraggable = false
    }

    fun dismissDialog() {
        dismiss()
    }
}