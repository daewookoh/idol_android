package net.ib.mn.core.utils

object Const {
    const val PREFS_ACCOUNT = "account"
    const val PREF_KEY__EMAIL = "email"
    const val PREF_KEY__TOKEN = "token"
    const val PREF_KEY__DOMAIN = "domain" // email, kakao, line
    const val PREF_LANGUAGE: String = "language"
    const val PREF_NAME: String = "com.exodus.myloveidol"
    val locales: Array<String> = arrayOf(
        "", "ko_KR", "en_US", "zh_CN", "zh_TW", "ja_JP", "in_ID", "th_TH", "vi_VN",
        "es_ES", "pt_BR", "de_DE", "fr_FR", "it_IT", "ar_001", "fa_IR", "ru_RU", "tr_TR"
    )
}