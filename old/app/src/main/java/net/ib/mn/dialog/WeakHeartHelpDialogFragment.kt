package net.ib.mn.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import net.ib.mn.R
import net.ib.mn.databinding.DialogWeakheartBinding

class WeakHeartHelpDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var weak_heart_guide: String? = null
    private var _binding: DialogWeakheartBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        weak_heart_guide = args!!.getString(PARAM_GUIDE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogWeakheartBinding.inflate(inflater, container, false)
        return binding.root
    }

override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (weak_heart_guide != null) {
            binding.weakHeartGuide.text = weak_heart_guide
        }
        binding.btnConfirm.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        dismiss()
    }

    companion object {
        const val PARAM_GUIDE: String = "weak_heart_guide"
        fun getInstance(guide: String?): WeakHeartHelpDialogFragment {
            val fragment = WeakHeartHelpDialogFragment()
            val args = Bundle()
            args.putString(PARAM_GUIDE, guide)
            fragment.arguments = args
            fragment.setStyle(STYLE_NO_TITLE, 0)
            return fragment
        }
    }
}
