package com.crmbl.thesafe.viewModels

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.crmbl.thesafe.BR

data class LoginViewModel(
    private var _username: String = "",
    private var _password: String = "",
    private var _fingerMessage : String = "",
    private var _isUsingFingerprint: Boolean = false,
    private var _finger: Boolean = false
) : BaseObservable() {

    var username: String
    @Bindable get() = _username
    set(value) {
        _username = value
        notifyPropertyChanged(BR.username)
    }

    var password: String
    @Bindable get() = _password
    set(value) {
        _password = value
        notifyPropertyChanged(BR.password)
    }

    var fingerMessage: String
    @Bindable get() = _fingerMessage
    set(value) {
        _fingerMessage = value
        notifyPropertyChanged(BR.fingerMessage)
    }

    var isUsingFingerprint: Boolean
    @Bindable get() = _isUsingFingerprint
    set(value) {
        _isUsingFingerprint = value
        notifyPropertyChanged(BR.usingFingerprint)
    }

    var finger: Boolean
    @Bindable get() = _finger
    set(value) {
        _finger = value
        notifyPropertyChanged(BR.finger)
    }
}