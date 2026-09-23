package com.example.chatapp.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CREATE_GROUP = "create_group"
    const val PROFILE = "profile"
    const val CHAT = "chat/{chatId}/{chatName}"

    fun chat(chatId: String, chatName: String): String {
        val safe = java.net.URLEncoder.encode(chatName, "UTF-8")
        return "chat/$chatId/$safe"
    }
}
