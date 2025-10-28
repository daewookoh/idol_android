package net.ib.mn.common.exception

class LogException(override val cause: Throwable) : Exception(cause)
