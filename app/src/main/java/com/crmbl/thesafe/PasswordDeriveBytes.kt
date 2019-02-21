package com.crmbl.thesafe

import java.io.UnsupportedEncodingException
import java.security.DigestException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class PasswordDeriveBytes(strPassword: String, rgbSalt: ByteArray, strHashName: String, iterations: Int) {

    private var hashName: String? = null
        set(hashName) {
            if (hashName == null)
                throw NullPointerException("HashName")
            if (state != 0) {
                throw SecurityException("Can't change this property at this stage")
            }
            field = hashName

            try {
                hash = MessageDigest.getInstance(hashName)
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }

        }
    private var SaltValue: ByteArray? = null
    private var iterationCount: Int = 0
        set(iterationCount) {
            if (iterationCount < 1)
                throw NullPointerException("HashName")
            if (state != 0) {
                throw SecurityException("Can't change this property at this stage")
            }
            field = iterationCount
        }

    private var hash: MessageDigest? = null
    private var state: Int = 0
    private var password: ByteArray? = null
    private var initial: ByteArray? = null
    private var output: ByteArray? = null
    private var firstBaseOutput: ByteArray? = null
    private var position: Int = 0
    private var hashnumber: Int = 0
    private var skip: Int = 0

    private var salt: ByteArray?
        get() = if (SaltValue == null) null else SaltValue
        set(salt) {
            if (state != 0) {
                throw SecurityException("Can't change this property at this stage")
            }
            if (salt != null)
                SaltValue = salt
            else
                SaltValue = null
        }

    init {
        prepare(strPassword, rgbSalt, strHashName, iterations)
    }

    private fun prepare(strPassword: String?, rgbSalt: ByteArray, strHashName: String, iterations: Int) {
        if (strPassword == null)
            throw NullPointerException("strPassword")

        var pwd: ByteArray? = null
        try {
            pwd = strPassword.toByteArray(charset("ASCII"))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        prepare(pwd, rgbSalt, strHashName, iterations)
    }

    private fun prepare(password: ByteArray?, rgbSalt: ByteArray, strHashName: String, iterations: Int) {
        if (password == null)
            throw NullPointerException("password")

        this.password = password

        state = 0
        salt = rgbSalt
        hashName = strHashName
        iterationCount = iterations

        initial = ByteArray(hash!!.digestLength)
    }

    @Throws(DigestException::class)
    fun getBytes(cb: Int): ByteArray {
        if (cb < 1) {
            throw IndexOutOfBoundsException("cb")
        }

        if (state == 0) {
            reset()
            state = 1
        }

        val result = ByteArray(cb)
        var cpos = 0
        // the initial hash (in reset) + at least one iteration
        val iter = Math.max(1, iterationCount - 1)

        // start with the PKCS5 key
        if (output == null) {
            // calculate the PKCS5 key
            output = initial

            // generate new key material
            for (i in 0 until iter - 1) {
                output = hash!!.digest(output)
            }
        }

        while (cpos < cb) {
            var output2: ByteArray?
            when {
                hashnumber == 0 -> // last iteration on output
                    output2 = hash!!.digest(output)
                hashnumber < 1000 -> {
                    val n = Integer.toString(hashnumber).toByteArray()
                    output2 = ByteArray(output!!.size + n.size)
                    for (j in n.indices) {
                        output2[j] = n[j]
                    }
                    System.arraycopy(output!!, 0, output2, n.size, output!!.size)
                    // don't update output
                    output2 = hash!!.digest(output2)
                }
                else -> throw SecurityException("too long")
            }

            val rem = output2!!.size - position
            val l = Math.min(cb - cpos, rem)
            System.arraycopy(output2, position, result, cpos, l)

            cpos += l
            position += l
            while (position >= output2.size) {
                position -= output2.size
                hashnumber++
            }
        }

        // saving first output length
        if (state == 1) {
            skip = if (cb > 20)
                        40 - result.size
                    else
                        20 - result.size
            firstBaseOutput = ByteArray(result.size)
            System.arraycopy(result, 0, firstBaseOutput!!, 0, result.size)
            state = 2
        } else if (skip > 0) {
            val secondBaseOutput = ByteArray(firstBaseOutput!!.size + result.size)
            System.arraycopy(firstBaseOutput!!, 0, secondBaseOutput, 0, firstBaseOutput!!.size)
            System.arraycopy(result, 0, secondBaseOutput, firstBaseOutput!!.size, result.size)
            System.arraycopy(secondBaseOutput, skip, result, 0, skip)

            skip = 0
        }// processing second output

        return result
    }

    @Throws(DigestException::class)
    fun reset() {
        state = 0
        position = 0
        hashnumber = 0
        skip = 0

        if (SaltValue != null) {
            hash!!.update(password, 0, password!!.size)
            hash!!.update(SaltValue, 0, SaltValue!!.size)
            hash!!.digest(initial, 0, initial!!.size)
        } else {
            initial = hash!!.digest(password)
        }
    }
}