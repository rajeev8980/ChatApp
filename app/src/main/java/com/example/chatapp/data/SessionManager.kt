package com.example.chatapp.data

import android.content.Context
import com.example.chatapp.ChatApp

/** Token + profile cache in SharedPreferences. */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("chat_session", Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString("token", "").orEmpty()
        set(v) = prefs.edit().putString("token", v).apply()

    var uid: String
        get() = prefs.getString("uid", "").orEmpty()
        set(v) = prefs.edit().putString("uid", v).apply()

    var displayName: String
        get() = prefs.getString("name", "").orEmpty()
        set(v) = prefs.edit().putString("name", v).apply()

    var email: String
        get() = prefs.getString("email", "").orEmpty()
        set(v) = prefs.edit().putString("email", v).apply()

    var serverUrl: String
        get() = prefs.getString("server", "").orEmpty()
        set(v) = prefs.edit().putString("server", v).apply()

    fun hasToken(): Boolean = token.isNotBlank()

    fun prefs(name: String) =
        ChatApp.instance.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)

    fun save(token: String, uid: String, name: String, email: String) {
        prefs.edit()
            .putString("token", token)
            .putString("uid", uid)
            .putString("name", name)
            .putString("email", email)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        @Volatile private var instance: SessionManager? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) instance = SessionManager(context.applicationContext)
                }
            }
        }

        fun get(): SessionManager =
            instance ?: throw IllegalStateException("SessionManager not initialized")
    }
}
