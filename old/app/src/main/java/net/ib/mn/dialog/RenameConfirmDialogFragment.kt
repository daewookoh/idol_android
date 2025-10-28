package net.ib.mn.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.databinding.DialogRenameConfirmBinding
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText

class RenameConfirmDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var _binding: DialogRenameConfirmBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRenameConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirm.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_cancel) {
            dismiss()
        } else if (v.id == R.id.btn_confirm) {
            val account = getAccount(baseActivity)
            if (account!!.heartCount < 100) {
                makeText(baseActivity, R.string.not_enough_heart, Toast.LENGTH_SHORT).show()
                return
            }
            setResultCode(RESULT_OK)
            dismiss()
        }
    }

    companion object {
        val instance: RenameConfirmDialogFragment
            get() {
                val fragment = RenameConfirmDialogFragment()
                fragment.setStyle(STYLE_NO_TITLE, 0)
                return fragment
            }
    }
}
