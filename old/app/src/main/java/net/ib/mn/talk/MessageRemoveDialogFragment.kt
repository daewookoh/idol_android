package net.ib.mn.talk

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.R
import net.ib.mn.databinding.DialogRemoveBinding


class MessageRemoveDialogFragment : BaseDialogFragment(), View.OnClickListener {

    private var _binding: DialogRemoveBinding? = null
    private val binding get() = _binding!!

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val r = resources
            val width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 290f, r.displayMetrics).toInt()
            dialog.window!!.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = DialogRemoveBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.removeDesc.setText(R.string.confirm_delete)

        binding.btnConfirm.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_cancel) {
            dismiss()
        } else if (v.id == R.id.btn_confirm) {
            val result = Intent()
            setResult(result)
            setResultCode(BaseDialogFragment.RESULT_OK)
            dismiss()
        }
    }

    companion object {
        fun getInstance(): MessageRemoveDialogFragment {
            val fragment = MessageRemoveDialogFragment()
            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)

            return fragment
        }
    }
}
