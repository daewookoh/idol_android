package feature.common.exodusimagepicker.util.livedata

import androidx.lifecycle.Observer

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: singleeventoberver  event쪽에서 onEventUnhandledContent 값이 null이면  그냥 지나가고,  null이 아니면  observer를  리턴한다.
 * 그렇게 함으로써  single event 진행
 *
 * @see
 * */
class SingleEventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>) {
        event.getContentIfNotHandled()?.let { value -> onEventUnhandledContent(value) }
    }
}