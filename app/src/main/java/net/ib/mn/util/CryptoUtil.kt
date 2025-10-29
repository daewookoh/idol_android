package net.ib.mn.util

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object CryptoUtil {
    private const val EXOKEY = "dus-"

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
