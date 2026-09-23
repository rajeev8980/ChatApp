package com.example.chatapp.data.remote

/** Server address. Emulator -> host PC is 10.0.2.2. Physical device: use your PC's LAN IP. */
object ServerConfig {
    var BASE_URL = "http://10.0.2.2:8000/"

    fun wsUrl(token: String): String {
        val http = BASE_URL.trim().trimEnd('/')
        val ws = when {
            http.startsWith("https://") -> "wss://" + http.removePrefix("https://")
            http.startsWith("http://") -> "ws://" + http.removePrefix("http://")
            else -> http
        }
        return "$ws/ws?token=$token"
    }

    fun absolute(path: String): String {
        if (path.startsWith("http")) return path
        return BASE_URL.trim().trimEnd('/') + path
    }
}
