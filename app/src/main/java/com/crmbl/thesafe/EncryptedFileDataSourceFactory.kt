package com.crmbl.thesafe

import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.TransferListener
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class EncryptedFileDataSourceFactory(
    private val mCipher: Cipher,
    private val mSecretKeySpec: SecretKeySpec,
    private val mIvParameterSpec: IvParameterSpec,
    private val mTransferListener: TransferListener<in DataSource>
) : DataSource.Factory {

    override fun createDataSource(): EncryptedFileDataSource {
        return EncryptedFileDataSource(mCipher, mSecretKeySpec, mIvParameterSpec, mTransferListener)
    }

}