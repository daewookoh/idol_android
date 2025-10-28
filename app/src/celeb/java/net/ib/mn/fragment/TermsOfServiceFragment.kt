package net.ib.mn.fragment

import android.os.Bundle
import android.view.View
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.utils.Util


/**
 * 설정-서비스 정보-이용약관
 */

class TermsOfServiceFragment : BaseWebViewFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lang = Util.getAgreementLanguage(requireContext())
        val url = "${ServerUrl.HOST}/static/agreement1$lang.html"

        setContentsUrl(url)
    }
    companion object {
        fun getInstance(): TermsOfServiceFragment {
            val frag = TermsOfServiceFragment()
            return frag
        }
    }
}