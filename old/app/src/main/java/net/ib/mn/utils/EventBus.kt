package net.ib.mn.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

object EventBus {
    val subjectTable = ConcurrentHashMap<String, MutableSharedFlow<Any>>()

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: Any> sendEvent(data: T, key: String = "GlobalEvent") {
        GlobalScope.launch {
            (subjectTable[key] as? MutableSharedFlow<T>)?.emit(data)
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: Any> receiveEvent(key: String = T::class.java.name): MutableSharedFlow<T> {
        return subjectTable.getOrPut(key) { MutableSharedFlow(replay = 0) } as MutableSharedFlow<T>
    }
}

/**
 * Flow에서 RxJava의 `throttleFirst` 기능을 구현
 */
fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T> = flow {
    var lastEmitTime = 0L
    collect { value ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmitTime >= windowDuration) {
            lastEmitTime = currentTime
            emit(value)
        }
    }
}