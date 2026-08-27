package com.RIKAPLAY.zhirpem_app

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpUtils {

    fun generateQrCodeBitmap(content: String, size: Int = 500): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun generateSecretKey(): String {
        val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        return (1..32)
            .map { i -> kotlin.random.Random.nextInt(0, charPool.length) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun decodeBase32(base32: String): ByteArray {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleanBase32 = base32.uppercase().replace(" ", "").replace("-", "")
        var buffer = 0
        var bitsLeft = 0
        val result = mutableListOf<Byte>()

        for (char in cleanBase32) {
            val value = base32Chars.indexOf(char)
            if (value == -1) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                result.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return result.toByteArray()
    }

    fun generateTotp(secret: String, timeInterval: Long = 30): String {
        val secretBytes = decodeBase32(secret)
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val counter = currentTimeSeconds / timeInterval

        val counterBytes = ByteArray(8)
        var tempCounter = counter
        for (i in 7 downTo 0) {
            counterBytes[i] = (tempCounter and 0xFF).toByte()
            tempCounter = tempCounter shr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        val keySpec = SecretKeySpec(secretBytes, "HmacSHA1")
        mac.init(keySpec)
        val hash = mac.doFinal(counterBytes)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = (
            ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        )

        val otp = binary % 10.0.pow(6.0).toInt()
        return otp.toString().padStart(6, '0')
    }

    fun verifyTotp(secret: String, code: String, window: Int = 1): Boolean {
        for (i in -window..window) {
            val currentTimeSeconds = (System.currentTimeMillis() / 1000) + (i * 30)
            val secretBytes = decodeBase32(secret)
            val counter = currentTimeSeconds / 30

            val counterBytes = ByteArray(8)
            var tempCounter = counter
            for (j in 7 downTo 0) {
                counterBytes[j] = (tempCounter and 0xFF).toByte()
                tempCounter = tempCounter shr 8
            }

            val mac = Mac.getInstance("HmacSHA1")
            val keySpec = SecretKeySpec(secretBytes, "HmacSHA1")
            mac.init(keySpec)
            val hash = mac.doFinal(counterBytes)

            val offset = hash[hash.size - 1].toInt() and 0x0F
            val binary = (
                ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
            )

            val otp = binary % 10.0.pow(6.0).toInt()
            if (otp.toString().padStart(6, '0') == code) return true
        }
        return false
    }

    fun getQrCodeUri(secret: String, account: String, issuer: String): String {
        return "otpauth://totp/$issuer:$account?secret=$secret&issuer=$issuer"
    }
}
