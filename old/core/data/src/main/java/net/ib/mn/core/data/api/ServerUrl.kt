package net.ib.mn.core.data.api

import net.ib.mn.core.data.BuildConfig

object ServerUrl {
    const val PREFIX = "api/v1"

    val HOST_REAL: String =
        if (BuildConfig.CELEB) "https://www.myloveactor.com" else "https://www.myloveidol.com" // 실서버

    @JvmStatic
    val HOST_TEST: String =
        if (BuildConfig.CELEB) "https://test.myloveactor.com" else "https://test.myloveidol.com" // 실서버

    val HOST_BBB_TEST: String =
        if (BuildConfig.CELEB) "https://bbb.test.myloveactor.com" else "https://bbb.test.myloveidol.com" // bbb 테스트 서버.
    @JvmStatic
    var HOST: String =
        HOST_REAL

    const val HOST_ACTOR: String = "www.myloveactor.com"
    const val HOST_IDOL: String = "www.myloveidol.com"

    // for java
    @JvmStatic
    fun setHost(host: String) {
        HOST = host
    }

    fun buildBasePath(): String {
        val urlEl = arrayOf(HOST, PREFIX)
        return urlEl.joinToString("/") + "/"
    }

    fun buildBasePathWithoutPrefix(): String {
        val urlEl = arrayOf(HOST)
        return urlEl.joinToString("/") + "/"
    }
}