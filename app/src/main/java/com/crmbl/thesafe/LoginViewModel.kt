package com.crmbl.thesafe

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

data class LoginViewModel(
    private var _username: String,
    private var _password: String,
    private var _isUsingFingerprint: Boolean = false
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

    var isUsingFingerprint: Boolean
    @Bindable get() = _isUsingFingerprint
    set(value) {
        _isUsingFingerprint = value
        notifyPropertyChanged(BR.usingFingerprint)
    }


}