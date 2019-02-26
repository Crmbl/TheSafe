package com.crmbl.thesafe.viewModels

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.crmbl.thesafe.BR


data class SettingViewModel(
    private var _rememberUsername: Boolean = false,
    private var _useFingerprint: Boolean = false,
    private var _salt: String = "",
    private var _password: String = "",
    private var _firstUse: Boolean = true
) : BaseObservable() {

    var settingRememberUsername : Boolean
    @Bindable get() = _rememberUsername
    set(value) {
        _rememberUsername = value
        notifyPropertyChanged(BR.settingRememberUsername)
    }

    var settingUseFingerprint : Boolean
    @Bindable get() = _useFingerprint
    set(value) {
        _useFingerprint = value
        notifyPropertyChanged(BR.settingUseFingerprint)
    }

    var firstUse : Boolean
    @Bindable get() = _firstUse
    set(value) {
        _firstUse = value
        notifyPropertyChanged(BR.firstUse)
    }

    var settingSalt : String
    @Bindable get() = _salt
    set(value) {
        _salt = value
        notifyPropertyChanged(BR.settingSalt)
    }

    var settingPassword : String
    @Bindable get() = _password
    set(value) {
        _password = value
        notifyPropertyChanged(BR.settingPassword)
    }
}