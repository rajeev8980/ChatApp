package com.example.chatapp.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface SocketEvent {
    data class Message(val chatId: String, val message: ApiMessage) : SocketEvent
    data object Refresh : SocketEvent
}

/** Single app-wide WebSocket to FastAPI /ws. Emits realtime message events. */
object SocketManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events

    @Volatile private var socket: WebSocket? = null
    @Volatile private var wantConnected = false
    @Volatile private var token: String = ""

    fun connect(token: String) {
        this.token = token
        if (token.isBlank() || wantConnected) return
        wantConnected = true
        open()
    }

    fun disconnect() {
        wantConnected = false
        try { socket?.close(1000, "logout") } catch (_: Exception) {}
        socket = null
    }

    private fun open() {
        if (!wantConnected || token.isBlank()) return
        val request = Request.Builder().url(ServerConfig.wsUrl(token)).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    if (json.optString("type") == "message") {
                        val chatId = json.optString("chatId")
                        val m = json.getJSONObject("message")
                        val rawImage = m.optString("imageUrl")
                        val msg = ApiMessage(
                            messageId = m.optString("messageId"),
                            chatId = m.optString("chatId"),
                            senderId = m.optString("senderId"),
                            senderName = m.optString("senderName"),
                            text = m.optString("text"),
                            imageUrl = if (rawImage.isNotBlank()) ServerConfig.absolute(rawImage) else "",
                            timestamp = m.optString("timestamp")
                        )
                        _events.tryEmit(SocketEvent.Message(chatId, msg))
                    } else {
                        _events.tryEmit(SocketEvent.Refresh)
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                scope.launch {
                    delay(3000)
                    if (wantConnected) open()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                if (wantConnected) scope.launch {
                    delay(3000)
                    if (wantConnected) open()
                }
            }
        })
    }
}
