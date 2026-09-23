package com.example.chatapp.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Offline outbox: messages that failed to send are queued and retried. */
data class PendingSend(
    val tempId: String = java.util.UUID.randomUUID().toString(),
    val chatId: String = "",
    val text: String = "",
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

class OutboxStore {
    private val session: SessionManager get() = SessionManager.get()
    private val gson = Gson()
    private val type = object : TypeToken<MutableList<PendingSend>>() {}.type

    @Synchronized
    fun list(): MutableList<PendingSend> {
        val raw = session.prefs("chat_outbox").getString("pending", "[]").orEmpty()
        return try {
            gson.fromJson<MutableList<PendingSend>>(raw, type) ?: mutableListOf()
        } catch (_: Exception) { mutableListOf() }
    }

    @Synchronized
    fun add(p: PendingSend) {
        val l = list()
        l.add(p)
        save(l)
    }

    @Synchronized
    fun remove(tempId: String) {
        val l = list()
        l.removeAll { it.tempId == tempId }
        save(l)
    }

    @Synchronized
    fun forChat(chatId: String): List<PendingSend> = list().filter { it.chatId == chatId }

    private fun save(l: List<PendingSend>) {
        session.prefs("chat_outbox").edit().putString("pending", gson.toJson(l)).apply()
    }
}
