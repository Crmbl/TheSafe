package com.crmbl.thesafe

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.util.Base64
import android.util.Log
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import kotlin.experimental.and


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val userNameTxt = findViewById<EditText>(R.id.userNameTxt)
//        val passwordTxt = findViewById<EditText>(R.id.passwordTxt)
//        val resetBtn = findViewById<Button>(R.id.resetBtn)
//        val submitBtn = findViewById<Button>(R.id.submitBtn)
//
        val fingerButton = findViewById<FloatingActionButton>(R.id.fingerButton)
        fingerButton.setOnClickListener {

            var ins: InputStream = assets.open("docd.qsp")
            var content : ByteArray = ins.readBytes()
            ins.close()

            var byteArray : Array<UByte> = Array(content.size) { i: Int -> content[i].toUByte() }
            val key = "3BVEnYwzN8eNTG8G".toByteArray()
            val salt = "EsyQJ7keJK2nkVJ8".toByteArray()
            try {
                val iv = IvParameterSpec(salt)
                val keySpec = SecretKeySpec(key, "SHA1PRNG")
                val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
                cipher.init(Cipher.DECRYPT_MODE, keySpec, iv)
                val original = cipher.doFinal(Base64.decode(byteArray as ByteArray, byteArray.size))

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

//    @Throws(Exception::class)
//    private fun encrypt(raw: ByteArray, clear: ByteArray): ByteArray {
//        val skeySpec = SecretKeySpec(raw, "AES/CBC")
//        val cipher = Cipher.getInstance("AES/CBC")
//        cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
//        return cipher.doFinal(clear)
//    }
//
//    @Throws(Exception::class)
//    private fun decrypt(raw: ByteArray, encrypted: ByteArray): ByteArray {
//        val skeySpec = SecretKeySpec(raw, "AES/CBC")
//        val cipher = Cipher.getInstance("AES/CBC")
//        cipher.init(Cipher.DECRYPT_MODE, skeySpec)
//        return cipher.doFinal(encrypted)
//    }
}
