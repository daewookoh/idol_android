package net.ib.mn.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import net.ib.mn.fragment.PrivacyPolicyFragment
import net.ib.mn.fragment.TermsOfServiceFragment

class ServiceInfoPagerAdapter(
    fragmentActivity : FragmentActivity,
    viewPager2: ViewPager2,
    tabLayout: TabLayout
) : FragmentStateAdapter(fragmentActivity) {
    private val NUM_PAGES = 3

    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TermsOfServiceFragment.getInstance()
            1 -> PrivacyPolicyFragment.getInstance()
            else -> Fragment()

        }
    }
}