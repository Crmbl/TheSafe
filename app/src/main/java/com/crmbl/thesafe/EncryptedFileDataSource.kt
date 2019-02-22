package com.crmbl.thesafe

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class EncryptedFileDataSource(
    private val mCipher: Cipher,
    private val mSecretKeySpec: SecretKeySpec,
    private val mIvParameterSpec: IvParameterSpec,
    private val mTransferListener: TransferListener<in EncryptedFileDataSource>?
) : DataSource {
    private var mInputStream: StreamingCipherInputStream? = null
    private var mUri: Uri? = null
    private var mBytesRemaining: Long = 0
    private var mOpened: Boolean = false

    @Throws(EncryptedFileDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        if (mOpened) {
            return mBytesRemaining
        }
        mUri = dataSpec.uri
        try {
            setupInputStream()
            skipToPosition(dataSpec)
            computeBytesRemaining(dataSpec)
        } catch (e: IOException) {
            throw EncryptedFileDataSourceException(e)
        }

        mOpened = true
        mTransferListener?.onTransferStart(this, dataSpec)
        return mBytesRemaining
    }

    @Throws(FileNotFoundException::class)
    private fun setupInputStream() {
        val encryptedFile = File(mUri!!.path)
        val fileInputStream = FileInputStream(encryptedFile)
        /*val tmpByte = ByteArray(4)
        fileInputStream.read(tmpByte, 0, 4)*/
        mInputStream = StreamingCipherInputStream(fileInputStream, mCipher, mSecretKeySpec, mIvParameterSpec/*, tmpByte*/)
    }

    @Throws(IOException::class)
    private fun skipToPosition(dataSpec: DataSpec) {
        mInputStream!!.forceSkip(dataSpec.position)
    }

    @Throws(IOException::class)
    private fun computeBytesRemaining(dataSpec: DataSpec) {
        if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            mBytesRemaining = dataSpec.length
        } else {
            mBytesRemaining = mInputStream!!.available().toLong()
            if (mBytesRemaining == Integer.MAX_VALUE.toLong()) {
                mBytesRemaining = C.LENGTH_UNSET.toLong()
            }
        }
    }

    @Throws(EncryptedFileDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) {
            return 0
        } else if (mBytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }
        val bytesToRead = getBytesToRead(readLength)
        val bytesRead: Int
        try {
            bytesRead = mInputStream!!.read(buffer, offset, bytesToRead)
        } catch (e: IOException) {
            throw EncryptedFileDataSourceException(e)
        }

        if (bytesRead == -1) {
            if (mBytesRemaining != C.LENGTH_UNSET.toLong()) {
                throw EncryptedFileDataSourceException(EOFException())
            }
            return C.RESULT_END_OF_INPUT
        }
        if (mBytesRemaining != C.LENGTH_UNSET.toLong()) {
            mBytesRemaining -= bytesRead.toLong()
        }
        mTransferListener?.onBytesTransferred(this, bytesRead)
        return bytesRead
    }

    private fun getBytesToRead(bytesToRead: Int): Int {
        return if (mBytesRemaining == C.LENGTH_UNSET.toLong()) {
            bytesToRead
        } else Math.min(mBytesRemaining, bytesToRead.toLong()).toInt()
    }

    override fun getUri(): Uri? {
        return mUri
    }

    @Throws(EncryptedFileDataSourceException::class)
    override fun close() {
        mUri = null
        try {
            if (mInputStream != null) {
                mInputStream!!.close()
            }
        } catch (e: IOException) {
            throw EncryptedFileDataSourceException(e)
        } finally {
            mInputStream = null
            if (mOpened) {
                mOpened = false
                mTransferListener?.onTransferEnd(this)
            }
        }
    }

    class EncryptedFileDataSourceException(cause: IOException) : IOException(cause)

    class StreamingCipherInputStream(
        private val mUpstream: InputStream,
        private val mCipher: Cipher,
        private val mSecretKeySpec: SecretKeySpec,
        private val mIvParameterSpec: IvParameterSpec,
        private val tmpByte: ByteArray? = null
    ) : CipherInputStream(mUpstream, mCipher) {

        @Throws(IOException::class)
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            return super.read(b, off, len)
            //return super.read(b, 0, toInt32(tmpByte, 0))
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

        @Throws(IOException::class)
        fun forceSkip(bytesToSkip: Long): Long {
            val skipped = mUpstream.skip(bytesToSkip)
            try {
                val skip = (bytesToSkip % AES_BLOCK_SIZE).toInt()
                val blockOffset = bytesToSkip - skip
                val numberOfBlocks = blockOffset / AES_BLOCK_SIZE
                // from here to the next inline comment, i don't understand
                val ivForOffsetAsBigInteger = BigInteger(1, mIvParameterSpec.iv).add(BigInteger.valueOf(numberOfBlocks))
                val ivForOffsetByteArray = ivForOffsetAsBigInteger.toByteArray()
                val computedIvParameterSpecForOffset: IvParameterSpec
                if (ivForOffsetByteArray.size < AES_BLOCK_SIZE) {
                    val resizedIvForOffsetByteArray = ByteArray(AES_BLOCK_SIZE)
                    System.arraycopy(
                        ivForOffsetByteArray,
                        0,
                        resizedIvForOffsetByteArray,
                        AES_BLOCK_SIZE - ivForOffsetByteArray.size,
                        ivForOffsetByteArray.size
                    )
                    computedIvParameterSpecForOffset = IvParameterSpec(resizedIvForOffsetByteArray)
                } else {
                    computedIvParameterSpecForOffset = IvParameterSpec(
                        ivForOffsetByteArray,
                        ivForOffsetByteArray.size - AES_BLOCK_SIZE,
                        AES_BLOCK_SIZE
                    )
                }
                mCipher.init(Cipher.ENCRYPT_MODE, mSecretKeySpec, computedIvParameterSpecForOffset)
                val skipBuffer = ByteArray(skip)
                mCipher.update(skipBuffer, 0, skip, skipBuffer)
                Arrays.fill(skipBuffer, 0.toByte())
            } catch (e: Exception) {
                return 0
            }

            return skipped
        }

        @Throws(IOException::class)
        override fun available(): Int {
            return mUpstream.available()
        }

        companion object {
            private const val AES_BLOCK_SIZE = 16
        }
    }
}