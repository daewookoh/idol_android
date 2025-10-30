package net.ib.mn.util

import net.ib.mn.BuildConfig

/**
 * 서버 URL 관리 클래스
 *
 * old 프로젝트의 ServerUrl.kt와 동일한 역할
 * Deep link나 설정에 따라 서버 URL을 동적으로 변경 가능
 */
object ServerUrl {
    const val PREFIX = "api/v1"

    // 실서버
    val HOST_REAL: String =
        if (Constants.IS_CELEB) "https://www.myloveactor.com" else "https://www.myloveidol.com"

    // 테스트 서버
    val HOST_TEST: String =
        if (Constants.IS_CELEB) "https://test.myloveactor.com" else "https://test.myloveidol.com"

    // BBB 테스트 서버
    val HOST_BBB_TEST: String =
        if (Constants.IS_CELEB) "https://bbb.test.myloveactor.com" else "https://bbb.test.myloveidol.com"

    const val HOST_ACTOR: String = "www.myloveactor.com"
    const val HOST_IDOL: String = "www.myloveidol.com"

    /**
     * 현재 사용 중인 서버 HOST
     * 런타임에 변경 가능 (Deep link, 설정 등)
     */
    @Volatile
    var HOST: String = HOST_REAL
        private set

    /**
     * 현재 사용 중인 API BASE_URL (HOST + /api/v1/)
     */
    val BASE_URL: String
        get() = "$HOST/$PREFIX/"

    /**
     * 서버 URL 변경
     * @param newHost 새로운 HOST URL (scheme 포함)
     */
    fun setHost(newHost: String) {
        HOST = newHost
    }

    /**
     * 테스트 서버인지 확인
     */
    fun isTestServer(): Boolean {
        return HOST == HOST_TEST || HOST == HOST_BBB_TEST
    }

    /**
     * 실서버로 변경
     */
    fun useRealServer() {
        HOST = HOST_REAL
    }

    /**
     * 테스트 서버로 변경
     */
    fun useTestServer() {
        HOST = HOST_TEST
    }

    /**
     * BBB 테스트 서버로 변경
     */
    fun useBBBTestServer() {
        HOST = HOST_BBB_TEST
    }
}
