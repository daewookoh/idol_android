package net.ib.mn.support

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.databinding.ActivitySupportInfoBinding
import net.ib.mn.utils.ext.applySystemBarInsets

class SupportInfoActivity : BaseActivity() {

    private lateinit var binding: ActivitySupportInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_support_info)

        // E2E 적용
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        WindowInsetsControllerCompat(window, window.decorView).apply {
            val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }

        binding.scrollView.applySystemBarInsets()

        supportActionBar?.setTitle(R.string.support)


        //광고 종류 리스트 보기
        binding.llSupportAdTypeList.setOnClickListener {
            startActivity(SupportAdPickActivity.createIntent(this,false))
        }

        //서포트 create
        binding.llSupportCreate.setOnClickListener{
            startActivity(SupportWriteActivity.createIntent(this))
        }

        if(BuildConfig.CELEB) with(binding){
            tvModifier.text = getString(R.string.actor_support_about_subtitle)
            tvTitle.text = getString(R.string.actor_support_about_title)
            tvPromotionModifier.text = getString(R.string.actor_support_about_info1)
            tvPromotion.text = getString(R.string.actor_support_about_info2)
            tvRewards.text = getString(R.string.actor_support_about_info_desc)
        }
    }

    companion object {

        @JvmStatic
        fun createIntent(context: Context?): Intent {
            return Intent(context, SupportInfoActivity::class.java)
        }
    }
}