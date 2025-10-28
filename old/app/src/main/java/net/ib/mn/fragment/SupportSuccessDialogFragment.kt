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
import net.ib.mn.databinding.BottomSheetSupportBinding
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.SupportListModel
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.setFirebaseUIAction

class SupportSuccessDialogFragment(
    private val diaCount: String,
    private val supportId: Int,
    private val supportListModel: SupportListModel,
    private val adName: String
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetSupportBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetSupportBinding.inflate(inflater, container, false)
        binding.apply {
            lifecycleOwner = this@SupportSuccessDialogFragment
            view = this@SupportSuccessDialogFragment
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.tvConfirm.setOnClickListener {
            setFirebaseUIAction(GaAction.SUPPORT_SUCCESS_SHARE)
            val idolName = Util.nameSplit(requireContext(), supportListModel.idol)
            if (idolName[1] != "") idolName[1] = "#${UtilK.removeWhiteSpace(idolName[1])}"


            val params = listOf(LinkStatus.SUPPORTS.status, supportId.toString())
            val url = LinkUtil.getAppLinkUrl(context = requireContext(), params = params)
            val msg = String.format(
                getString(if (BuildConfig.CELEB) R.string.share_in_progress_support_celeb else R.string.share_in_progress_support),
                UtilK.removeWhiteSpace(idolName[0]), idolName[1], idolName[0], adName, ""
            )

            UtilK.linkStart(requireContext(), url = url, msg = msg)
            dismiss()
        }

        binding.tvSubtitle.text = requireContext().getString(R.string.popup_diavote_desc, diaCount)

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
}