package com.crmbl.thesafe

import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.*

class UriByteDataHelper {
    fun getUri(data: ByteArray): Uri {
        try {
            val url = URL(null, "bytes:///" + "media", BytesHandler(data))
            return Uri.parse(url.toURI().toString())
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }

    }

    class BytesHandler(data: ByteArray) : URLStreamHandler() {
        private var mData : ByteArray = data

        override fun  openConnection(u : URL) : URLConnection {
            return ByteUrlConnection(u, mData)
        }
    }

    class ByteUrlConnection(url: URL, data: ByteArray) : URLConnection(url) {
        var mData: ByteArray = data

        override fun connect() {}
        override fun getInputStream(): InputStream {
            return ByteArrayInputStream(mData)
        }
    }
}