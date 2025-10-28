package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.fragment.Top1CountFragment

// 셀럽만 사용
@AndroidEntryPoint
class Top1CountActivity : BaseActivity() {
    private var mFragment: Top1CountFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }

        val contentView = findViewById<android.view.View>(android.R.id.content)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.stats_1st_place)

        mFragment = Top1CountFragment()

        val manager = supportFragmentManager
        if (savedInstanceState == null) {
            manager.beginTransaction()
                .add(android.R.id.content, mFragment!!, TAG_TOP1_FRAGMENT).commit()
        }
    }

    companion object {
        const val TAG_TOP1_FRAGMENT: String = "top1Fragment"

        fun createIntent(context: Context?): Intent {
            return Intent(context, Top1CountActivity::class.java)
        }
    }
}
