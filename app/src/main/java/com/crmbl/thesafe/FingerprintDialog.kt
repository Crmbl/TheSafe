package com.crmbl.thesafe

import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_fingerprint.*
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


class FingerprintDialog : DialogFragment(), FingerprintController.Callback {

    private val controller: FingerprintController by lazy {
        FingerprintController(
            FingerprintManagerCompat.from(context!!),
            this,
            titleTextView,
            subtitleTextView,
            errorTextView,
            iconFAB
        )
    }

    private var cryptoObject: FingerprintManagerCompat.CryptoObject? = null
    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.dialog_fingerprint, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller.setTitle(arguments?.getString(ARG_TITLE)!!)
        controller.setSubtitle(arguments?.getString(ARG_SUBTITLE)!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try { keyStore = KeyStore.getInstance("AndroidKeyStore") }
        catch (e: KeyStoreException) { throw RuntimeException("Failed to get an instance of KeyStore", e) }

        try { keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore") }
        catch (e: NoSuchAlgorithmException) { throw RuntimeException("Failed to get an instance of KeyGenerator", e) }
        catch (e: NoSuchProviderException) { throw RuntimeException("Failed to get an instance of KeyGenerator", e) }

        createKey(DEFAULT_KEY_NAME)

        val defaultCipher: Cipher
        try { defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                                                            + KeyProperties.BLOCK_MODE_CBC + "/"
                                                            + KeyProperties.ENCRYPTION_PADDING_PKCS7) }
        catch (e: NoSuchAlgorithmException) { throw RuntimeException("Failed to get an instance of Cipher", e) }
        catch (e: NoSuchPaddingException) { throw RuntimeException("Failed to get an instance of Cipher", e) }

        if (initCipher(defaultCipher, DEFAULT_KEY_NAME))
            cryptoObject = FingerprintManagerCompat.CryptoObject(defaultCipher)
    }

    override fun onResume() {
        super.onResume()

        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        cryptoObject?.let {
            controller.startListening(it)
        }
    }

    override fun onPause() {
        super.onPause()
        controller.stopListening()
    }

    override fun onAuthenticated() {
        //TODO implement success
        throw NotImplementedError()
    }

    override fun onError() {
        //TODO implement error
        throw NotImplementedError()
    }

    private fun initCipher(cipher: Cipher, keyName: String): Boolean {
        return try {
            keyStore?.load(null)
            val key = keyStore?.getKey(keyName, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            true
        }
        catch (e: KeyPermanentlyInvalidatedException) { false }
        catch (e: KeyStoreException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: CertificateException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: UnrecoverableKeyException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: IOException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: NoSuchAlgorithmException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: InvalidKeyException) { throw RuntimeException("Failed to init Cipher", e) }
    }

    private fun createKey(keyName: String) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint for your flow.
        // Use of keys is necessary if you need to know if the set of enrolled fingerprints has changed.
        try {
            keyStore?.load(null)
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            val builder = KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)

            keyGenerator?.init(builder.build())
            keyGenerator?.generateKey()
        }
        catch (e: NoSuchAlgorithmException) { throw RuntimeException(e) }
        catch (e: InvalidAlgorithmParameterException) { throw RuntimeException(e) }
        catch (e: CertificateException) { throw RuntimeException(e) }
        catch (e: IOException) { throw RuntimeException(e) }
    }

    companion object {
        val FRAGMENT_TAG: String = FingerprintDialog::class.java.simpleName

        private const val ARG_TITLE = "ArgTitle"
        private const val ARG_SUBTITLE = "ArgSubtitle"
        private const val DEFAULT_KEY_NAME = "default_key"

        fun newInstance(title: String, subtitle: String): FingerprintDialog {
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_SUBTITLE, subtitle)

            val fragment = FingerprintDialog()
            fragment.arguments = args

            return fragment
        }
    }
}