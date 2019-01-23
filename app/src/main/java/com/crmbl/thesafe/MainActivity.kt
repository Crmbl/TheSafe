package com.crmbl.thesafe

import android.os.Bundle
import java.io.File
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fingerButton = findViewById<FloatingActionButton>(R.id.fingerButton)
        fingerButton.setOnClickListener {

            var ins: InputStream = assets.open("docd.qsp")
            var origin : ByteArray = ins.readBytes()
            ins.close()

            val salt = "EsyQJ7keJK2nkVJ8".toByteArray()
            val passPhrase = "3BVEnYwzN8eNTG8G"
            var password = PasswordDeriveBytes(passPhrase, salt, "SHA1", 2)
            var pass32 : ByteArray = password.GetBytes(32)
            var pass16 : ByteArray = password.GetBytes(16)

            try {
                val cipher = Cipher.getInstance("AES/CBC/NoPadding ")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(pass32, "SHA1PRNG"), IvParameterSpec(pass16))
                val decrypted = cipher.doFinal(origin)

                val outputFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "/decrypted.gif")
                outputFile.writeBytes(decrypted)

                val originFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "/crypted.gif")
                originFile.writeBytes(origin)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
