package net.ib.mn.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ib.mn.R
import net.ib.mn.adapter.BottomSheetFreeBoardLanguageAdapter
import net.ib.mn.databinding.BottomSheetLanguageFilterBinding
import net.ib.mn.utils.BoardLanguage

class LanguageFilterDialogFragment(
    private val selectCallback: (Pair<String?, String>) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetLanguageFilterBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetLanguageFilterBinding.inflate(inflater, container, false)
        binding.apply {
            lifecycleOwner = this@LanguageFilterDialogFragment
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { d ->
            val bottomSheet =
                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val layoutParams = sheet.layoutParams
                layoutParams.height =
                    (resources.displayMetrics.heightPixels * 0.7).toInt() // 최대 높이 설정
                sheet.layoutParams = layoutParams
                BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvLanguage.layoutManager = LinearLayoutManager(requireContext())

        binding.rvLanguage.adapter =
            BottomSheetFreeBoardLanguageAdapter(requireContext(), BoardLanguage.all()).apply {
                this.languageClickListener(object :
                    BottomSheetFreeBoardLanguageAdapter.OnClickListener {
                    override fun onItemClicked(locale: String?, langText: String) {
                        selectCallback.invoke(Pair(locale, langText))
                        dismiss()
                    }
                })
            }

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