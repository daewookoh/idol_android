package net.ib.mn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import net.ib.mn.databinding.FragmentTermsOfServiceBinding
import net.ib.mn.R
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.utils.Util


/**
 * 설정-서비스 정보-이용약관
 */

class TermsOfServiceFragment : Fragment() {
    private lateinit var binding: FragmentTermsOfServiceBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTermsOfServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContents()
    }

    private fun setContents() {
        val lang = Util.getAgreementLanguage(context)

        if(Util.isUsingNightModeResources(context)) binding.tvDescription.setBackgroundColor(
            ContextCompat.getColor(
                context!!,
                R.color.gray300
            )
        )
        binding.tvDescription.loadUrl("${ServerUrl.HOST}/static/agreement1$lang.html")
    }

    companion object {
        fun getInstance(): TermsOfServiceFragment { return TermsOfServiceFragment() }
    }
}