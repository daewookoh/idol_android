package net.ib.mn.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import net.ib.mn.R
import net.ib.mn.databinding.DialogMapBinding

class MapDialog : BaseDialogFragment() {

    private lateinit var binding: DialogMapBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_map, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(requireContext()).load(arguments?.getString(PARAM_IMAGE_URL) ?: return)
            .into(binding.ivMap)

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val PARAM_IMAGE_URL = "imageUrl"

        fun getInstance(
            imageUrl: String
        ): MapDialog {

            val args = Bundle()
            val dialog = MapDialog()

            args.putString(PARAM_IMAGE_URL, imageUrl)

            dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
            dialog.arguments = args

            return dialog
        }
    }
}