package net.ib.mn.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import net.ib.mn.R
import net.ib.mn.adapter.ServiceInfoPagerAdapter
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.databinding.ActivityServiceInfoBinding
import net.ib.mn.utils.ext.applySystemBarInsets

class ServiceInfoActivity : BaseActivity() {
    private lateinit var binding: ActivityServiceInfoBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_service_info)
        binding.llContainer.applySystemBarInsets()

        supportActionBar?.setTitle(R.string.service_information)

//        val pager: ViewPager2 = binding.vpPager
//        val tab: TabLayout = binding.tlTab

        binding.vpPager.apply {
            adapter = ServiceInfoPagerAdapter(this@ServiceInfoActivity, this, binding.tlTab)
            isUserInputEnabled = false
        }

        // 네트워크 상태 업데이트
        InternetConnectivityManager.getInstance(this)
        InternetConnectivityManager.updateNetworkState(this)
        if (!InternetConnectivityManager.getInstance(this).isConnected) {
            showErrorWithClose(getString(R.string.desc_failed_to_connect_internet))
        }

        TabLayoutMediator(binding.tlTab, binding.vpPager) { tab , position ->
            when (position) {
                0 -> {
                    tab.text = ContextCompat.getString(this, R.string.agreement1)
                }
                1 -> tab.text = ContextCompat.getString(this, R.string.agreement2)
                2 -> {
                    tab.text = ContextCompat.getString(this, R.string.opensource_license)

                    tab.view.isClickable = false
                    tab.view.setOnTouchListener { view, event ->

                        val duration = event.eventTime - event.downTime
                        if (event.action == MotionEvent.ACTION_UP && duration < 200) {
                            startActivity(Intent(this@ServiceInfoActivity, OssLicensesMenuActivity::class.java))
                            OssLicensesMenuActivity.setActivityTitle(getString(R.string.opensource_license))
                        }
                        true
                    }
                }
            }
        }.attach()
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, ServiceInfoActivity::class.java)
        }
    }
}