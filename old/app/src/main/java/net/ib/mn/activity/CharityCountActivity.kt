package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.fragment.CharityCountFragment
import net.ib.mn.fragment.CharityCountFragment.Companion.newInstance
import net.ib.mn.utils.Util
import net.ib.mn.utils.VideoAdUtil
import javax.inject.Inject

@AndroidEntryPoint
class CharityCountActivity : BaseActivity() {
    private var mFragment: CharityCountFragment? = null
    private var charity = 0
    @Inject
    lateinit var videoAdUtil: VideoAdUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val actionbar = checkNotNull(supportActionBar)
        val mIntent = intent
        charity = mIntent.getIntExtra("charity", 0)
        if (charity == StatsActivity.ANGEL) {
            actionbar.setTitle(R.string.charity_angel)
        } else if (charity == StatsActivity.FAIRY) {
            actionbar.setTitle(R.string.charity_fairy)
        } else {
            actionbar.setTitle(R.string.miracle_month)
        }

        mFragment = CharityCountFragment()
        mFragment = newInstance(charity)
        val manager = supportFragmentManager
        if (savedInstanceState == null) {
            manager.beginTransaction()
                .add(android.R.id.content, mFragment!!).commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Util.handleVideoAdResult(
            this,
            false,
            true,
            requestCode,
            resultCode,
            data,
            "charitycount_videoad"
        ) { adType: String? ->
            videoAdUtil.onVideoSawCommon(
                this, true, adType, null
            )
        }
    }

    companion object {
        fun createIntent(context: Context?, charityActivity: Int): Intent {
            val intent = Intent(context, CharityCountActivity::class.java)
            intent.putExtra("charity", charityActivity)
            return intent
        }
    }
}
