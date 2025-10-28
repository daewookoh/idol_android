package net.ib.mn.fragment

import android.os.Bundle
import android.view.View
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Util
import net.ib.mn.utils.setFirebaseScreenViewEvent


/**
 * 회원가입 - 개인정보 취급방침
 */

class SimplePrivacyPolicyFragment : BaseWebViewFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as BaseActivity?)?.supportActionBar?.title = getString(R.string.personal_agreement)

        val lang = Util.getAgreementLanguage(requireContext())
        val url = "${ServerUrl.HOST}/static/agreement3$lang.html"

        setContentsUrl(url)
    }

    override fun onResume() {
        super.onResume()
        setFirebaseScreenViewEvent(GaAction.JOIN_PRIVACY, this::class.simpleName ?: "SimplePrivacyPolicyFragment")
    }

    companion object {
        fun getInstance(): SimplePrivacyPolicyFragment {
            val frag = SimplePrivacyPolicyFragment()
            return frag
        }
    }
}