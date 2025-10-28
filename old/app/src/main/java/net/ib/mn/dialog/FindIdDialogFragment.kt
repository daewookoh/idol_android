package net.ib.mn.dialog

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import net.ib.mn.R
import net.ib.mn.databinding.DialogFindidBinding

class FindIdDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var _binding: DialogFindidBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFindidBinding.inflate(inflater, container, false)
        return binding.root
    }

override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirm.setOnClickListener(this)
        val email = requireArguments().getString("email")
        if (TextUtils.isEmpty(email)) {
            binding.text1.setText(R.string.failed_to_find_id)
            binding.email.visibility = View.GONE
            binding.text2.visibility = View.GONE
        } else {
            binding.email.text = email
        }
    }

    override fun onClick(v: View) {
        dismiss()
    }

    companion object {
        fun getInstance(email: String?): FindIdDialogFragment {
            val fragment = FindIdDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            args.putString("email", email)
            fragment.arguments = args
            return fragment
        }
    }
}
