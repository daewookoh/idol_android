package net.ib.mn.common.exception

open class IgnoreException @JvmOverloads constructor(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)
