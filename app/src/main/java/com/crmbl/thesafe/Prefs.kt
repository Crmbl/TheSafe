package com.crmbl.thesafe

import android.content.Context
import android.content.SharedPreferences


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class Prefs (context: Context) {
    private val PREFS_FILENAME = "com.crmbl.thesafe.prefs"
    private val PASSWORD_HASH = "password_hash"
    private val USERNAME_HASH = "username_hash"
    private val USERNAME = "username"
    private val SALT_HASH_DECRYPT = "salt_hash_decrypt"
    private val PASSWORD_HASH_DECRYPT = "password_hash_decrypt"
    private val USE_FINGERPRINT = "use_fingerprint"
    private val REMEMBER_USERNAME = "remember_username"
    private val IS_FIRST_LOGIN = "first_login"

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

    var saltDecryptHash: String
        get() = prefs.getString(SALT_HASH_DECRYPT, "")
        set(value) = prefs.edit().putString(SALT_HASH_DECRYPT, value).apply()

    var passwordDecryptHash: String
        get() = prefs.getString(PASSWORD_HASH_DECRYPT, "")
        set(value) = prefs.edit().putString(PASSWORD_HASH_DECRYPT, value).apply()

    var useFingerprint : Boolean
        get() = prefs.getBoolean(USE_FINGERPRINT, false)
        set(value) = prefs.edit().putBoolean(USE_FINGERPRINT, value).apply()

    var rememberUsername: Boolean
        get() = prefs.getBoolean(REMEMBER_USERNAME, false)
        set(value) = prefs.edit().putBoolean(REMEMBER_USERNAME, value).apply()

    var firstLogin : Boolean
        get() = prefs.getBoolean(IS_FIRST_LOGIN, true)
        set(value) = prefs.edit().putBoolean(IS_FIRST_LOGIN, value).apply()
}