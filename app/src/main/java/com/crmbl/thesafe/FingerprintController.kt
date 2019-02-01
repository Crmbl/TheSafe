package com.crmbl.thesafe

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal

class FingerprintController(
    private val fingerprintManager: FingerprintManagerCompat,
    private val callback: Callback,
    private val title: TextView,
    private val subtitle: TextView,
    private val errorText: TextView,
    private val icon: ImageView) : FingerprintManagerCompat.AuthenticationCallback() {

    private val context: Context
        get() = errorText.context

    private var cancellationSignal: CancellationSignal? = null
    private var selfCancelled = false

    private val isFingerprintAuthAvailable: Boolean
        get() = fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()

    private val resetErrorTextRunnable: Runnable = Runnable {
        errorText.setTextColor(ContextCompat.getColor(context, R.color.colorHint))
        errorText.text = "Touch sensor"
        icon.setImageResource(R.drawable.ic_fingerprint_white_24dp)
    }

    init {
        errorText.post(resetErrorTextRunnable)
    }

    interface Callback {
        fun onAuthenticated()
        fun onError()
    }

    fun startListening(cryptoObject: FingerprintManagerCompat.CryptoObject) {
        if (!isFingerprintAuthAvailable) return

        cancellationSignal = CancellationSignal()
        selfCancelled = false
        fingerprintManager.authenticate(cryptoObject, 0, cancellationSignal, this, null)
    }

    fun stopListening() {
        cancellationSignal?.let {
            selfCancelled = true
            it.cancel()
            cancellationSignal = null
        }
    }

    private fun showError(text: CharSequence?) {
        icon.setImageResource(R.drawable.ic_fingerprint_white_24dp)
        errorText.text = text
        errorText.setTextColor(ContextCompat.getColor(errorText.context, R.color.colorError))
        errorText.removeCallbacks(resetErrorTextRunnable)
        errorText.postDelayed(resetErrorTextRunnable, ERROR_TIMEOUT_MILLIS)
    }

    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
        if (!selfCancelled) {
            //TODO in english pls !
            showError(errString)
            icon.postDelayed({
                callback.onError()
            }, ERROR_TIMEOUT_MILLIS)
        }
    }

    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
        errorText.removeCallbacks(resetErrorTextRunnable)
        icon.setImageResource(R.drawable.ic_fingerprint_white_24dp)
        errorText.setTextColor(ContextCompat.getColor(errorText.context, R.color.colorAccent))
        //errorText.text = errorText.context.getString(R.string.fingerprint_recognized)
        errorText.text = "Success"
        icon.postDelayed({
            callback.onAuthenticated()
        }, SUCCESS_DELAY_MILLIS)
    }

    override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
        //TODO change for english helpmessage ! not fr :(
        showError(helpString)
    }

    override fun onAuthenticationFailed() {
        //showError(errorText.context.getString(R.string.fingerprint_not_recognized))
        showError("Not recognized")
    }

    fun setTitle(title: String) {
        this.title.text = title
    }

    fun setSubtitle(subtitle: String) {
        this.subtitle.text = subtitle
    }

    companion object {
        private const val ERROR_TIMEOUT_MILLIS = 1600L
        private const val SUCCESS_DELAY_MILLIS = 1300L
    }
}
