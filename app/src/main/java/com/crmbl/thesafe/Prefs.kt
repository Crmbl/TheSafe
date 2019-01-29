package com.crmbl.thesafe

import android.content.Context
import android.content.SharedPreferences

class Prefs (context: Context) {
    private val PREFS_FILENAME = "com.crmbl.thesafe.prefs"
    private val PASSWORD_HASH = "password"
    private val USERNAME_HASH = "username_hash"
    private val USERNAME = "username"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var usernameHash: String
        get() = prefs.getString(USERNAME_HASH, "8381be6dd3f59795fae00f72c3800231")
        set(value) = prefs.edit().putString(USERNAME_HASH, value).apply()

    var passwordHash: String
        get() = prefs.getString(PASSWORD_HASH, "8c9db394975f874c2730e9900ab0b41b")
        set(value) = prefs.edit().putString(PASSWORD_HASH, value).apply()

    var username: String
        get() = prefs.getString(USERNAME, "")
        set(value) = prefs.edit().putString(USERNAME, value).apply()
}