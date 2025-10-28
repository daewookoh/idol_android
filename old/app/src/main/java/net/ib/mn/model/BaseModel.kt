package net.ib.mn.model

data class BaseModel<T>(
    val data: T? = null,
    val message: String? = null
)
