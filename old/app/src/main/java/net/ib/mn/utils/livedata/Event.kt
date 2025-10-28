/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 라이브 데이터 이벤트 관련 처리  진행
 *
 * @see getContentIfNotHandled 한번  다뤄진  이벤트인지 체크해서  hasbeenhanled가 true이면  null을 return하여  observer쪽에서  let으로 받아  아무것도 안가게  진행한다.
 * */

package net.ib.mn.utils.livedata


/**
 * @see
 * */

open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    //최신 content 가져오고 싶을때
    fun peekContent(): T = content
}