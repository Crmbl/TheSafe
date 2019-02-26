package com.crmbl.thesafe.utils

import java.math.BigInteger
import java.security.MessageDigest


class StringUtil {
    fun md5(value : String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(value.toByteArray())).toString(16).padStart(32, '0')
    }
}