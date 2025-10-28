/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.ib.mn.model.IdolModel

class CommunityPagerAdapter(

    fragmentActivity: FragmentActivity,
    val fragmentList: List<Fragment>,
    val idolModel: IdolModel,
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    fun getIndexOfFragments(fragment: Fragment): Int = fragmentList.indexOf(fragment)
}