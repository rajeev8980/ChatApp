package com.example.chatapp

import android.app.Application
import com.example.chatapp.data.SessionManager

class ChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        SessionManager.init(this)
        // Reconnect realtime socket if a session exists
        try {
            val s = SessionManager.get()
            if (s.hasToken()) {
                com.example.chatapp.data.remote.SocketManager.connect(s.token)
            }
        } catch (_: Exception) {}
    }

    companion object {
        lateinit var instance: ChatApp
            private set
    }
}
