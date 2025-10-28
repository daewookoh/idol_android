package net.ib.mn.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import net.ib.mn.fragment.*

/**
 * Copyright 2022-12-7,수,16:5. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: FAQ/문의 paging adapter (viewPager2)
 *
 **/

class InquiryPagerAdapter(
    fragmentActivity : FragmentActivity,
    viewPager2: ViewPager2,
    tabLayout: TabLayout
) : FragmentStateAdapter(fragmentActivity) {
    private val NUM_PAGES = 2

    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FAQFragment.getInstance()
            else -> MyInquiryFragment.getInstance()

        }
    }
}