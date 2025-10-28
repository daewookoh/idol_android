package net.ib.mn.fragment

import android.os.Bundle
import android.view.View

import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.utils.Util

/**
 * 설정-서비스 정보-개인정보 취급방침
 */

class PrivacyPolicyFragment : BaseWebViewFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lang = Util.getAgreementLanguage(requireContext())
        val url = "${ServerUrl.HOST}/static/agreement2$lang.html"

        setContentsUrl(url)
    }

    companion object {
        fun getInstance(): PrivacyPolicyFragment { return PrivacyPolicyFragment() }
    }
}