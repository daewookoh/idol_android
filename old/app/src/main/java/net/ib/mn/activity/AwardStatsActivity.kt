/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 기록실 어워즈 Activity 통합된 파일
 *
 * */

package net.ib.mn.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.awards.Hda2022Fragment
import net.ib.mn.awards.Hda2023Fragment
import net.ib.mn.awards.SobaAggregatedFragment
import net.ib.mn.databinding.ActivityGaonMainBinding
import net.ib.mn.fragment.*
import net.ib.mn.model.AwardStatsModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ext.applySystemBarInsets
import java.util.*

@SuppressLint("NewApi")
@AndroidEntryPoint
class AwardStatsActivity : BaseActivity(), OnPageChangeListener {


    private val mTabs = ArrayList<Button>()
    private var mCurrentTabIdx = 0
    private var mMenuAdapter: MenuFragmentPagerAdapter? = null
    private var awardStatsModel: AwardStatsModel? = null
    private var awardType: String? = null

    private lateinit var binding: ActivityGaonMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_gaon_main)
        binding.drawerLayout.applySystemBarInsets()
        awardStatsModel = intent.extras?.getSerializable(StatsActivity.PARAM_AWARD_STATS) as AwardStatsModel?

        // 테스트용 코드
//        if(BuildConfig.DEBUG) {
//            awardStatsModel?.resultTitle = "가온 어워드 팬투표 <CHART_NAME> 인기상 최종 결과111"
//        }

        awardType = awardStatsModel?.name

        supportActionBar?.title = awardStatsModel?.title

        binding.pager.setOnPageChangeListener(this)
        mMenuAdapter = MenuFragmentPagerAdapter(supportFragmentManager)

        val chartSize = awardStatsModel?.charts?.size?: 0
        for (i in 0 until chartSize) {
            awardStatsModel?.let { createTextMenuWithBundle(it, i) }
        }
        buildMenu()

        binding.pager.offscreenPageLimit = chartSize
        binding.pager.adapter = mMenuAdapter
        mMenuAdapter!!.notifyDataSetChanged()
    }

    override fun onPageScrollStateChanged(arg0: Int) {
    }

    override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {
    }

    override fun onPageSelected(position: Int) {
        onTabClicked(position, false)
    }

    private fun getKlass(awardType: String?) : Class<out BaseFragment> {
        return when(awardType) {
            Const.EVENT_GAON_2016, Const.EVENT_GAON_2017 -> GaonStatsFragment::class.java
            Const.EVENT_SOBA2020 -> SobaAggregatedFragment::class.java
            Const.EVENT_AAA2020 -> AAA2020Fragment::class.java
            Const.AWARD_2022 -> Hda2022Fragment::class.java
            else -> Hda2023Fragment::class.java
        }
    }

    private fun createTextMenuWithBundle(awardStatsModel: AwardStatsModel, position: Int) {
        val bundle = Bundle()
        bundle.putSerializable(StatsActivity.PARAM_AWARD_STATS, awardStatsModel)
        bundle.putString(StatsActivity.PARAM_AWARD_STATS_CODE, awardStatsModel.charts[position].code)
        bundle.putInt(StatsActivity.PARAM_AWARD_STATS_INDEX, position)

        createTextMenu(awardStatsModel.charts[position].name ?: "", getKlass(awardType), bundle)
    }

    private fun createTextMenu(
        name: String,
        klass: Class<out BaseFragment>,
        args: Bundle?,
    ) {
        val tab = Button(this)
        tab.setBackgroundResource(R.drawable.btn_up_two_tab)
        tab.text = name
        tab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        tab.setTextColor(ContextCompat.getColorStateList(this, R.color.selector_tab))
        val tabIdx = mTabs.size
        tab.setOnClickListener {
            if (mCurrentTabIdx != tabIdx) {
                onTabClicked(tabIdx, true)
            }
        }
        mTabs.add(tab)
        mMenuAdapter!!.add(klass, args)
    }

    private fun buildMenu() {
        //보여질 바텀 탭 사이즈가 2보다 작을 경우
        if (mTabs.size < BOTTOM_TAB_VISIBLE_MIN_SIZE) {
            binding.tabContainer.visibility = View.GONE
            return
        }
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        for (tab in mTabs) {
            val params = LinearLayout.LayoutParams(
                width / mTabs.size,
                LinearLayout.LayoutParams.MATCH_PARENT,
            )
            binding.tabContainer.addView(tab, params)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tab.setAutoSizeTextTypeUniformWithConfiguration(10, 13, 1, 1)
            }
            tab.maxLines = 2
        }
        onTabClicked(0, false)
    }

    private fun onTabClicked(position: Int, updatePager: Boolean) {
        mTabs[mCurrentTabIdx].isSelected = false
        mTabs[position].isSelected = true
        mCurrentTabIdx = position
        if (updatePager) {
            binding.pager.currentItem = position
        }
    }

    private class MenuFragmentPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val mMenus: MutableList<MenuItem>

        private class MenuItem(internal var klass: Class<out Fragment>, internal var args: Bundle?)

        init {
            mMenus = ArrayList()
        }

        fun add(klass: Class<out Fragment>, args: Bundle?) {
            mMenus.add(MenuItem(klass, args))
        }

        override fun getItem(position: Int): Fragment {
            val item = mMenus[position]
            val fragment = item.klass.newInstance()
            fragment.arguments = item.args
            return fragment
        }

        override fun getCount(): Int {
            return mMenus.size
        }

    }
    companion object {
        const val BOTTOM_TAB_VISIBLE_MIN_SIZE = 2
        fun createIntent(context: Context?, awardStatsModel: AwardStatsModel): Intent {
            val intent = Intent(context, AwardStatsActivity::class.java)
            intent.putExtra(StatsActivity.PARAM_AWARD_STATS, awardStatsModel)
            return intent
        }
    }

}
