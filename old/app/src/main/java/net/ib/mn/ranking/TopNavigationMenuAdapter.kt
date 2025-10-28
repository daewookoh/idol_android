/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.ranking

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter


/**
 * @see
 * */

class TopNavigationMenuAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment) {
    val mMenus: MutableList<MenuItem> = ArrayList()

    class MenuItem(
        val klass: Class<out Fragment>,
        val args: Bundle?,
    )

    fun clear() {
        mMenus.clear()
    }

    fun add(
        klass: Class<out Fragment>,
        args: Bundle?,
    ) {
        mMenus.add(MenuItem(klass, args))
    }

    // 각 메뉴 fragment 클래스를 체크해서 맞는 클래스의 index를 보여준다.
    fun indexOfMenu(klass: Class<out Fragment>): Int = mMenus.indexOfFirst { it.klass == klass }

    fun getFragmentSimpleName(position: Int): String {
        val item = mMenus[position]
        return item.klass.simpleName
    }

    override fun getItemCount(): Int = mMenus.size

    override fun createFragment(position: Int): Fragment {
        val item = mMenus[position]
        return item.klass.getConstructor().newInstance().apply {
            arguments = item.args?.apply {
                putString("fragment_tag", "fragment_$position") // 고유 태그 설정
            }
        }
    }
}