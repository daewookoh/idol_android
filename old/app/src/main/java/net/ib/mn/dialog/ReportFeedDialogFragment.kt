package net.ib.mn.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.media3.common.util.UnstableApi
import net.ib.mn.R
import net.ib.mn.activity.FeedActivity
import net.ib.mn.databinding.DialogReportBinding
import net.ib.mn.model.UserModel

@UnstableApi
class ReportFeedDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var mUser: UserModel? = null

    private var spannedMsg: Spanned? = null
    private val reportDialog: Dialog? = null

    var _binding: DialogReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReportBinding.inflate(inflater, container, false)
        return binding.root
    }

override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textReportTitle.setText(R.string.report)
        binding.btnConfirm.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)

        if (spannedMsg != null) binding.textReportMsg.setText(spannedMsg)
    }

    override fun onActivityCreated(arg0: Bundle?) {
        super.onActivityCreated(arg0)
        mUser = requireArguments().getSerializable(PARAM_USER) as UserModel?
    }

    /**
     * 신고시 하트 소진 경고 문구 설정
     * @param spanned
     */
    fun setMessage(spanned: Spanned?) {
        spannedMsg = spanned
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_cancel) {
            dismiss()
        } else if (v.id == R.id.btn_confirm) {
            setResultCode(RESULT_OK)
            dismiss()
        }
    }

    companion object {
        fun getInstance(user: UserModel?): ReportFeedDialogFragment {
            val fragment = ReportFeedDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            args.putSerializable(PARAM_USER, user)
            fragment.arguments = args
            return fragment
        }

        const val PARAM_USER: String = FeedActivity.REPORT_USER
    }
}
