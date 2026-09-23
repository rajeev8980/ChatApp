package com.example.chatapp.data.remote

import com.example.chatapp.data.SessionManager

/** Server address. Changeable in-app (login screen) so the app works on any network. */
object ServerConfig {
    const val DEFAULT_URL = "http://10.250.138.231:8000/"

    fun base(): String {
        val saved = try { SessionManager.get().serverUrl } catch (_: Exception) { "" }
        val raw = (if (saved.isNotBlank()) saved else DEFAULT_URL).trim().trimEnd('/') + "/"
        return raw
    }

    /** Normalize user input: "192.168.1.5:8000" -> "http://192.168.1.5:8000/" */
    fun normalize(input: String): String {
        var u = input.trim()
        require(u.isNotBlank()) { "Server address is empty" }
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        require(Regex("^https?://[^\\s/]+(:\\d+)?").containsMatchIn(u)) { "Bad address" }
        return u.trimEnd('/') + "/"
    }

    fun wsUrl(token: String): String {
        val http = base().trimEnd('/')
        val ws = when {
            http.startsWith("https://") -> "wss://" + http.removePrefix("https://")
            http.startsWith("http://") -> "ws://" + http.removePrefix("http://")
            else -> http
        }
        return "$ws/ws?token=$token"
    }

    fun absolute(path: String): String {
        if (path.startsWith("http")) return path
        return base().trimEnd('/') + path
    }
}
