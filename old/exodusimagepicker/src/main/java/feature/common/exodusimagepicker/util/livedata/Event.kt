package feature.common.exodusimagepicker.util.livedata

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 라이브 데이터 이벤트 관련 처리  진행
 *
 *
 * @see getContentIfNotHandled 한번  다뤄진  이벤트인지 체크해서  hasbeenhanled가 true이면  null을 return하여  observer쪽에서  let으로 받아  아무것도 안가게  진행한다.
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

    // 최신 content 가져오고 싶을때
    fun peekContent(): T = content
}