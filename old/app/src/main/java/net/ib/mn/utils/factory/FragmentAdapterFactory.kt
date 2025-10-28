/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 뷰 페이저에 들어가는 프래그먼트 캐싱을 위한 팩토리
 *
 *
 * */

package net.ib.mn.utils.factory

import androidx.fragment.app.Fragment


/**
 * @see
 * */

class FragmentAdapterFactory<out T : Fragment>(// 프래그먼트의 변경은 허용하지 않습니다.
    private val fragments: List<T>
) {

    private val fragmentCache = mutableMapOf<Int, T>()

    fun createFragment(position: Int): T {
        return fragmentCache.getOrPut(position) {
            fragments[position]
        }
    }

    fun clearFragments() {
        fragmentCache.clear()
    }
}
