package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import net.ib.mn.R
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.databinding.ActivityAgreementBinding
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets

class AgreementActivity : BaseActivity() {
    private lateinit var mType: String
    private lateinit var binding: ActivityAgreementBinding

    companion object {
        const val PARAM_AGREEMENT_TYPE = "paramAgreementType"
        const val TYPE_TERMS_OF_SERVICE = "paramTermsOfService"
        const val TYPE_PRIVACY_POLICY = "paramPrivacyPolicy"

        @JvmStatic
        fun createIntent(context: Context, type: String): Intent {
            return Intent(context, AgreementActivity::class.java)
                    .putExtra(PARAM_AGREEMENT_TYPE, type)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_agreement)
        binding.llContainer.applySystemBarInsets()
        this.supportActionBar?.setTitle(R.string.title_agreement)

        mType = intent.extras?.getString(PARAM_AGREEMENT_TYPE)!!
        setContents()

        // 네트워크 상태 업데이트
        InternetConnectivityManager.getInstance(this)
        InternetConnectivityManager.updateNetworkState(this)
        if (!InternetConnectivityManager.getInstance(this).isConnected) {
            showErrorWithClose(getString(R.string.desc_failed_to_connect_internet))
        }
    }

    private fun setContents() {
        val lang = Util.getAgreementLanguage(this)
        if(Util.isUsingNightModeResources(this)) binding.tvDescription.setBackgroundColor(ContextCompat.getColor(this, R.color.gray300))

        when (mType) {
            TYPE_TERMS_OF_SERVICE -> {
                this.supportActionBar?.setTitle(R.string.title_agreement)
                binding.tvTitle.setText(R.string.agreement1)
                binding.tvDescription.loadUrl("${ServerUrl.HOST}/static/agreement1${lang}.html")
            }
            TYPE_PRIVACY_POLICY -> {
                this.supportActionBar?.setTitle(R.string.personal_agreement)
                binding.tvTitle.setText(R.string.personal_agreement)
                binding.tvDescription.loadUrl("${ServerUrl.HOST}/static/agreement2${lang}.html")
            }
        }
    }

}