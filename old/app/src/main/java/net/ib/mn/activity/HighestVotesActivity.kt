package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.fragment.HighestVotesFragment

/**
 * 셀럽 최다득표 탑100
 */
@AndroidEntryPoint
class HighestVotesActivity : BaseActivity() {
    private var mFragment: HighestVotesFragment? = null

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
        actionbar!!.setTitle(R.string.stats_highest_votes)

        mFragment = HighestVotesFragment()

        val manager = supportFragmentManager
        if (savedInstanceState == null) {
            manager.beginTransaction()
                .add(android.R.id.content, mFragment!!, "highestVoteFragment").commit()
        }
    }

    companion object {
        fun createIntent(context: Context?): Intent {
            return Intent(context, HighestVotesActivity::class.java)
        }
    }
}
