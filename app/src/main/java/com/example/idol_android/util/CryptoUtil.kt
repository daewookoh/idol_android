package com.example.idol_android.util

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * CryptoUtil - 암호화 관련 유틸리티
 */
object CryptoUtil {

    private const val EXOKEY = "dus-"

    /**
     * MD5 해시 생성
     */
    fun md5(input: String): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            md.update(input.toByteArray(StandardCharsets.UTF_8), 0, input.length)

            val messageDigest = md.digest()
            val number = BigInteger(1, messageDigest)
            var md5Hash = number.toString(16)

            while (md5Hash.length < 32) {
                md5Hash = "0$md5Hash"
            }

            md5Hash
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * MD5 Salt 해시 생성 (EXOKEY 포함)
     *
     * 이메일 로그인 시 비밀번호를 해시하여 저장하는데 사용
     */
    fun md5salt(input: String): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val key = EXOKEY + input

            md.update(key.toByteArray(StandardCharsets.UTF_8), 0, key.length)

            val messageDigest = md.digest()
            val number = BigInteger(1, messageDigest)
            var md5Hash = number.toString(16)

            while (md5Hash.length < 32) {
                md5Hash = "0$md5Hash"
            }

            md5Hash
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        }
    }
}
