package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.RedirectRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.ActivityWikiBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

@AndroidEntryPoint
class WikiActivity : BaseActivity() {
    private lateinit var binding: ActivityWikiBinding
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var redirectRepository: RedirectRepository

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wiki)
        binding.clContainer.applySystemBarInsets()

		supportActionBar?.title = getString(R.string.guide_wiki_title)
		val mIdol = intent.getSerializableExtra("idol") as IdolModel
//		mIdol.setLocalizedName(this)
		var locale = Util.getSystemLanguage(this)
		locale = when (locale) {
			"ko_KR" -> "ko"
			"zh_CN" -> "zh-cn"
			"zh_TW" -> "zh-tw"
			"ja_JP" -> "ja"
			else -> "en"
		}

        lifecycleScope.launch {
            try {
                val wikiName = idolsRepository.getWikiName(mIdol.getId(), locale)
                redirectRepository.redirect(
                    "${ServerUrl.HOST}/wiki/${locale}/idol/${Util.encodingSpecialChar(wikiName)}/",
                    { response2 ->
                        if (response2.optBoolean("success")) {
                            val url = response2.getString("url")
                            binding.tvDescription.setNetworkAvailable(true)
                            binding.tvDescription.settings.javaScriptEnabled = true
                            binding.tvDescription.settings.domStorageEnabled = true
                            binding.tvDescription.loadUrl(url)
                        } else {
                            Util.showDefaultIdolDialogWithBtn1(this@WikiActivity,
                                null,
                                getString(R.string.msg_error_ok)) {
                                finish()
                            }
                        }
                    }, { _ ->
                        Util.showDefaultIdolDialogWithBtn1(this@WikiActivity,
                            null,
                            getString(R.string.msg_error_ok)) {
                            finish()
                        }
                    }
                )
            } catch(e: Exception) {
                Util.showDefaultIdolDialogWithBtn1(this@WikiActivity,
                    null,
                    getString(R.string.msg_error_ok)) {
                    finish()
                }
            }
        }

		binding.tvDescription.webViewClient = object : WebViewClient() {
			override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
				view?.loadUrl(url)
				return true
			}
		}
		if(Util.isUsingNightModeResources(this)) binding.tvDescription.setBackgroundColor(ContextCompat.getColor(this, R.color.gray300))
	}

	override fun onBackPressed() {
		if(binding.tvDescription.canGoBack()){
			binding.tvDescription.goBack()
		}else{
			super.onBackPressed()
		}
	}

	companion object {
		fun createIntent(context: Context?): Intent {
			return Intent(context, WikiActivity::class.java)
		}
		fun createIntent(context: Context, idol: IdolModel): Intent {
			val intent = Intent(context, WikiActivity::class.java)
			intent.putExtra("idol", idol as Parcelable?)

			return intent
		}
	}
}
