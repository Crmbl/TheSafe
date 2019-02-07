package com.crmbl.thesafe

import java.io.File
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec


data class CryptoUtil(
    private var _password : String,
    private var _salt : String
) {

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

    fun decrypt(inputFile : File) : ByteArray? {
        val ins: InputStream = inputFile.inputStream()
        val password = PasswordDeriveBytes(_password, _salt.toByteArray(), "SHA1", 2)
        val pass32: ByteArray = password.GetBytes(32)
        val pass16: ByteArray = password.GetBytes(16)

        val cipher = Cipher.getInstance("AES/CBC/NoPadding ")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(pass32, "SHA1PRNG"), IvParameterSpec(pass16))
        return cipher.doFinal(ins.readBytes())
    }
}