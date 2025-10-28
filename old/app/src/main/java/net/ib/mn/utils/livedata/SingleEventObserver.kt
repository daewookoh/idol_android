/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: singleeventoberver  event쪽에서 onEventUnhandledContent 값이 null이면  그냥 지나가고,  null이 아니면  observer를  리턴한다.
 * 그렇게 함으로써  single event 진행
 * 예를들어 토스트 같은것들은 화면이 돌아간다해도 캐싱되어있지않고 사라지는게 좋기때문에 한번 방출되고나면 더이상 방출 안하게 한다.
 *
 * */

package net.ib.mn.utils.livedata

import androidx.lifecycle.Observer


/**
 *
 *
 * @see
 * */

class SingleEventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) :
    Observer<Event<T>> {
    override fun onChanged(event: Event<T>) {
        event.getContentIfNotHandled()?.let { value -> onEventUnhandledContent(value) }
    }
}