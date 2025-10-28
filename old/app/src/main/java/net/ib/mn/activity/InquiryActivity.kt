package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.adapter.InquiryPagerAdapter
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.databinding.ActivityInquiryBinding
import net.ib.mn.utils.ext.applySystemBarInsets

/**
 * Copyright 2022-12-7,수,16:4. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 자주 묻는 질문, 나의 문의내역 viewPager2로 보여주는 Activity
 *
 **/

@AndroidEntryPoint
class InquiryActivity : BaseActivity() {
    private lateinit var binding: ActivityInquiryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_inquiry)
        binding.llContainer.applySystemBarInsets()

        supportActionBar?.setTitle(R.string.title_inquiry)

        val pager: ViewPager2 = binding.vpPager
        val tab: TabLayout = binding.tlTab

        pager.apply {
            adapter = InquiryPagerAdapter(this@InquiryActivity, pager, tab)
//            isUserInputEnabled = false    좌우 스크롤 막는 코드
        }

        // 네트워크 상태 업데이트
        InternetConnectivityManager.getInstance(this)
        InternetConnectivityManager.updateNetworkState(this)
        if (!InternetConnectivityManager.getInstance(this).isConnected) {
            showErrorWithClose(getString(R.string.desc_failed_to_connect_internet))
        }

        TabLayoutMediator(tab, pager) { tab , position ->
            when (position) {
                0 -> tab.text = getString(R.string.title_faq)
                1 -> tab.text = getString(R.string.title_myinquirylist)
            }
        }.attach()
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, InquiryActivity::class.java)
        }
    }
}