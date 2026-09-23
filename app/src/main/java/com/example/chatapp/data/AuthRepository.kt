package com.example.chatapp.data

import com.example.chatapp.data.model.User
import com.example.chatapp.data.remote.LoginRequest
import com.example.chatapp.data.remote.Network
import com.example.chatapp.data.remote.PresenceRequest
import com.example.chatapp.data.remote.RegisterRequest
import com.example.chatapp.data.remote.SocketManager
import com.example.chatapp.data.remote.UpdateNameRequest

class AuthRepository {
    private val session: SessionManager get() = SessionManager.get()
    private val api get() = Network.api()

    val currentUid: String? get() = session.uid.takeIf { it.isNotBlank() }
    val isLoggedIn: Boolean get() = session.hasToken()

    suspend fun login(email: String, password: String) {
        val res = api.login(LoginRequest(email.trim(), password))
        session.save(res.token, res.user.uid, res.user.displayName, res.user.email)
        SocketManager.connect(res.token)
        setOnline(true)
    }

    suspend fun register(name: String, email: String, password: String) {
        val res = api.register(RegisterRequest(name.trim(), email.trim(), password))
        session.save(res.token, res.user.uid, res.user.displayName, res.user.email)
        SocketManager.connect(res.token)
        setOnline(true)
    }

    suspend fun loadOrCreateProfile(): User? {
        return try {
            val u = api.me().toDomain()
            session.displayName = u.displayName
            u
        } catch (_: Exception) { null }
    }

    suspend fun setOnline(online: Boolean) {
        if (!session.hasToken()) return
        try { api.presence(PresenceRequest(online)) } catch (_: Exception) {}
    }

    suspend fun updateProfile(name: String) {
        val u = api.updateMe(UpdateNameRequest(name))
        session.displayName = u.displayName
    }

    fun connectSocket() {
        if (session.hasToken()) SocketManager.connect(session.token)
    }

    fun logout() {
        SocketManager.disconnect()
        session.clear()
    }
}
