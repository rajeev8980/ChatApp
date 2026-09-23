package com.example.chatapp.data.model

/** Formats backend ISO timestamps ("2026-09-22T11:20:33.123+00:00") as HH:mm. */
fun shortTime(iso: String): String {
    if (iso.isBlank()) return ""
    val m = Regex("T(\\d{2}:\\d{2})").find(iso)
    return m?.groupValues?.get(1) ?: iso.take(16)
}

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** Parses backend ISO to epoch millis, or null. */
fun isoMillis(iso: String): Long? {
    return try {
        var s = iso.trim()
        // normalize: keep "yyyy-MM-ddTHH:mm:ss", fraction -> millis, tz -> +0000
        val m = Regex("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d+))?(Z|[+-]\\d{2}:?\\d{2})?").find(s)
            ?: return null
        val frac = (m.groupValues[7] + "000").take(3)
        var tz = m.groupValues[8].ifBlank { "+0000" }
        if (tz == "Z") tz = "+0000"
        tz = tz.replace(":", "")
        val norm = "${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]}T" +
            "${m.groupValues[4]}:${m.groupValues[5]}:${m.groupValues[6]}.$frac$tz"
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        fmt.parse(norm)?.time
    } catch (_: Exception) { null }
}

/** "Sep 23, 2026, 12:39 AM" style stamp for the date header. */
fun stampText(iso: String): String {
    val t = isoMillis(iso) ?: return shortTime(iso)
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = t }
    val month = MONTHS[cal.get(java.util.Calendar.MONTH)]
    val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val year = cal.get(java.util.Calendar.YEAR)
    var h = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val min = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
    val ampm = if (h < 12) "AM" else "PM"
    h = if (h % 12 == 0) 12 else h % 12
    return "$month $day, $year, $h:$min $ampm"
}

/** "Active now" / "Active 11m ago" subtitle from presence info. */
fun activeText(online: Boolean, lastSeen: String): String {
    if (online) return "Active now"
    val t = isoMillis(lastSeen) ?: return ""
    val diffMin = ((System.currentTimeMillis() - t) / 60000).coerceAtLeast(0)
    return when {
        diffMin < 1 -> "Active just now"
        diffMin < 60 -> "Active ${diffMin}m ago"
        diffMin < 60 * 24 -> "Active ${diffMin / 60}h ago"
        else -> "Active ${diffMin / (60 * 24)}d ago"
    }
}

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val online: Boolean = false,
    val lastSeen: String = ""
)

data class Chat(
    val chatId: String = "",
    val name: String = "",
    val isGroup: Boolean = false,
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val lastSenderId: String = "",
    val photoUrl: String = "",
    val createdBy: String = ""
)

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: String = "",
    val pending: Boolean = false
) {
    val isImage: Boolean get() = imageUrl.isNotBlank()
}
