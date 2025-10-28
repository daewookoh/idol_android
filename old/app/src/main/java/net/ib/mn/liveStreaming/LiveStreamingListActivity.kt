package net.ib.mn.liveStreaming

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.databinding.ActivityLiveStreamingListBinding
import net.ib.mn.activity.BaseActivity
import net.ib.mn.utils.ext.applySystemBarInsets

@AndroidEntryPoint
class LiveStreamingListActivity: BaseActivity() {
    private lateinit var binding: ActivityLiveStreamingListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_live_streaming_list)
        binding.container.applySystemBarInsets()

        initSet()
    }


    //초기 세팅
    private fun initSet(){
        supportActionBar?.title = this.getString(R.string.live_actionbar_title)

        // fragment 넣기
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val frag = LiveStreamingListFragment()
        fragmentTransaction.add(R.id.container, frag)
        fragmentTransaction.commit()
    }

    companion object{
        const val REQUEST_CODE_LIVE_LIST = 1002
        @JvmStatic
        fun createIntent(context: Context, flag: Int): Intent {
            val intent = Intent(context, LiveStreamingListActivity::class.java)
            intent.flags = flag
            return intent
        }
    }
}