package com.zack.imaquickrecord2

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ima_quick_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var clientId: String
        get() = prefs.getString(KEY_CLIENT, "").orEmpty().trim()
        set(value) = prefs.edit().putString(KEY_CLIENT, value.trim()).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API, "").orEmpty().trim()
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    var knowledgeBaseId: String
        get() = prefs.getString(KEY_KB, "").orEmpty().trim()
        set(value) = prefs.edit().putString(KEY_KB, value.trim()).apply()

    val ready: Boolean
        get() = clientId.isNotEmpty() && apiKey.isNotEmpty() && knowledgeBaseId.isNotEmpty()

    private companion object {
        const val KEY_CLIENT = "client_id"
        const val KEY_API = "api_key"
        const val KEY_KB = "kb_id"
    }
}
