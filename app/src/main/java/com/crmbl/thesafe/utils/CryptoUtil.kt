package com.crmbl.thesafe.utils

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import java.io.*
import java.io.File
import javax.crypto.CipherInputStream


data class CryptoUtil(
    private var _password : String,
    private var _salt : String
) {

    private fun encipher(input : String, key : Int) : String {
        var output = ""
        for(character : Char in input) {
            output += if (!character.isLetter()) {
                character
            } else {
                val tmp : Char = if (character.isUpperCase()) {'A'} else {'a'}
                ((character + key - tmp) % 26 + tmp.toInt()).toChar()
            }
        }

        return output.replace("\\", "--")
    }

    fun decipher(input : String) : String {
        return encipher(input.replace("--", "\\"), 26 - 10)
    }

    fun decrypt(inputFile : File) : ByteArray? {
        val password =
            PasswordDeriveBytes(_password, _salt.toByteArray(Charsets.US_ASCII), "SHA1", 2)
        val pass32: ByteArray = password.getBytes(32)
        val pass16: ByteArray = password.getBytes(16)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        FileInputStream(inputFile).use { fileIn ->
            val tmpByte = ByteArray(4)
            fileIn.read(tmpByte, 0 , 4)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(pass32, "SHA1PRNG"), IvParameterSpec(pass16))

            CipherInputStream(fileIn, cipher).use { cipherIn ->
                return ByteArrayInputStream(cipherIn.readBytes(), 0, toInt32(tmpByte, 0)).use { byteStream ->
                    byteStream.readBytes()
                }
            }
        }
    }

    @Throws(Exception::class)
    fun toInt32(bytes:ByteArray, index:Int):Int  {
        if (bytes.size != 4)
            throw Exception("The length of the byte array must be 4 bytes long.")

        return ((0xff and bytes[index].toInt()) shl 32 or (
                (0xff and bytes[index + 1].toInt()) shl 40) or (
                (0xff and bytes[index + 2].toInt()) shl 48) or (
                (0xff and bytes[index + 3].toInt()) shl 56))
    }
}