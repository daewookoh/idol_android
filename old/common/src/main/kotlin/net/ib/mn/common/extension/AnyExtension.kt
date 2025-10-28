package net.ib.mn.common.extension

fun Any?.isNull() = this == null

fun Any?.isNotNull() = this != null

infix fun <T> T?.isNull(action: () -> Unit): T? {
    if (this.isNull()) {
        action()
    }
    return this
}

infix fun <T> T?.isNotNull(action: (T) -> Unit): T? {
    if (this.isNotNull()) {
        action(this!!)
    }
    return this
}
