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
        val password = PasswordDeriveBytes(_password, _salt.toByteArray(Charsets.US_ASCII), "SHA1", 2)
        val pass32: ByteArray = password.GetBytes(32)
        val pass16: ByteArray = password.GetBytes(16)

        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(pass32, "SHA1PRNG"), IvParameterSpec(pass16))

        var test1 = ByteArray(4)
        var read = ins.read(test1, 0 , 4)
        var test2 = ByteArray(toInt32(test1, 0))
        return cipher.doFinal(inputFile.readBytes(), 0, test2.size)

//        var test2 = ByteArray(0xff and test1[0].toInt() shl 32 or
//                (0xff and test1[1].toInt() shl 40) or
//                (0xff and test1[2].toInt() shl 48) or
//                (0xff and test1[3].toInt() shl 56))
//        return cipher.doFinal(test2, 0, test2.size)
//
        val input : ByteArray = ins.readBytes()
        val inputLength = toInt32(input, 0)
        //return cipher.doFinal(input, 0, inputLength)
        return cipher.doFinal(input, 0, inputLength)
    }

    @Throws(Exception::class)
     fun toInt32(bytes:ByteArray, index:Int):Int  {
//        if (bytes.size != 4)
//            throw Exception("The length of the byte array must be at least 4 bytes long.")

        return ((0xff and bytes[index].toInt()) shl 32 or (
                (0xff and bytes[index + 1].toInt()) shl 40) or (
                (0xff and bytes[index + 2].toInt()) shl 48) or (
                (0xff and bytes[index + 3].toInt()) shl 56))
    }
}