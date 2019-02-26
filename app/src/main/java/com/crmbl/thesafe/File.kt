package com.crmbl.thesafe

import com.beust.klaxon.Json

data class File(
    @Json(name = "OriginName")
    var originName : String,
    @Json(name = "UpdatedName")
    var updatedName : String,
    @Json(ignored = true)
    var decrypted : ByteArray? = null,
    @Json(ignored = true)
    var type : String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File

        if (originName != other.originName) return false
        if (updatedName != other.updatedName) return false
        if (decrypted != null) {
            if (other.decrypted == null) return false
            if (!decrypted?.contentEquals(other.decrypted!!)!!) return false
        } else if (other.decrypted != null) return false

        return true
    }

    override fun hashCode(): Int {
        return decrypted?.contentHashCode() ?: 0
    }
}