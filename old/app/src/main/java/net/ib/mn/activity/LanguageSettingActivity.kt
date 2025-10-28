package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.LocaleManagerCompat
import androidx.databinding.DataBindingUtil
import net.ib.mn.R
import net.ib.mn.adapter.LanguageSettingAdapter
import net.ib.mn.databinding.ActivityLanguageSettingBinding
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets

class LanguageSettingActivity : BaseActivity(), LanguageSettingAdapter.OnClickListener {

    private lateinit var mLanguageAdapter : LanguageSettingAdapter
    private lateinit var binding: ActivityLanguageSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_language_setting)
        binding.clContainer.applySystemBarInsets()

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.settings_language)
        mLanguageAdapter = LanguageSettingAdapter(this, Const.languages, this)
        binding.langRecycler.adapter = mLanguageAdapter
        mLanguageAdapter.notifyDataSetChanged()

    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, LanguageSettingActivity::class.java)
        }
    }

    //아이템 클릭
    override fun onItemClicked(position: Int) {
        val localeString : String
        if (position == 0) {
            Util.setPreference(this, Const.PREF_LANGUAGE, null)
        } else {
            localeString = Const.locales[position]
            Util.setPreference(this, Const.PREF_LANGUAGE, localeString)
        }

        // 언어를 바꾸면 idol 이름이 바뀐 언어로 나오게 캐시 비움
        Util.setPreference(this, Const.PREF_ALL_IDOL_UPDATE, "")
        // 최애돌 공식 채널 가져오게
        Util.setPreference(this, Const.PREF_SHOULD_CALL_OFFICIAL_CHANNEL, true)
        Util.setPreference(this, Const.PREF_OFFICIAL_CHANNEL_UPDATE, "")

        val intent = Intent(this, StartupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}