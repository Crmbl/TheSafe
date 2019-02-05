package com.crmbl.thesafe

import android.content.Context
import java.io.File
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import androidx.core.content.ContextCompat
import java.net.URLConnection


class CryptoUtil(_context : Context) {

    private var context : Context = _context
    private val theSafe_path : String = ".blob"

    fun encipher(input : String, key : Int) : String {
        var output = ""
        for(character : Char in input) {
            if (!character.isLetter())
                output += character
            else {
                val tmp : Char = if (character.isUpperCase()) {'A'} else {'a'}
                output += ((character + key - tmp) % 26 + tmp.toInt()).toChar()
            }
        }

        return output.replace("\\", "--")
    }

    fun decipher(input : String) : String {
        return encipher(input.replace("--", "\\"), 26 - 10)
    }

    fun saltChecker(_password : String, salt : String) : Boolean {
        try {
            val theSafeFolder = ContextCompat.getExternalFilesDirs(context, null)[1].listFiles()[0].listFiles()[0]
            if (theSafeFolder.isDirectory && theSafeFolder.isHidden && theSafeFolder.name == theSafe_path) {
                val ins : InputStream = theSafeFolder.listFiles()[0].inputStream()
                val password = PasswordDeriveBytes(_password, salt.toByteArray(), "SHA1", 2)
                val pass32 : ByteArray = password.GetBytes(32)
                val pass16 : ByteArray = password.GetBytes(16)

                val cipher = Cipher.getInstance("AES/CBC/NoPadding ")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(pass32, "SHA1PRNG"), IvParameterSpec(pass16))
                val decrypted = cipher.doFinal(ins.readBytes())

                val fileExt = decipher(theSafeFolder.listFiles()[0].name).split('.')[1]
                val outputFile = File(theSafeFolder, "/testing.$fileExt")
                outputFile.writeBytes(decrypted)

                if (URLConnection.guessContentTypeFromStream(outputFile.inputStream()) == fileExt) {
                    outputFile.delete()
                    return true
                }

                outputFile.delete()
                return false
            }

            return false
        } catch (ex: Exception) {
            return false
        }
    }
}