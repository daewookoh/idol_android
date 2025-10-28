package net.ib.mn.fragment

import android.os.Bundle
import android.view.View
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Util
import net.ib.mn.utils.setFirebaseScreenViewEvent


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

    override fun onResume() {
        super.onResume()
        setFirebaseScreenViewEvent(GaAction.JOIN_TERM, this::class.simpleName ?: "TermsOfServiceFragment")
    }

    companion object {
        fun getInstance(): TermsOfServiceFragment {
            val frag = TermsOfServiceFragment()
            return frag
        }
    }
}