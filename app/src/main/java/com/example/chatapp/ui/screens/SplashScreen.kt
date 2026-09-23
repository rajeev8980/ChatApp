package com.example.chatapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.chatapp.data.SessionManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: (Boolean) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        delay(800)
        val loggedIn = try {
            SessionManager.get().hasToken()
        } catch (_: Exception) {
            SessionManager.init(context)
            SessionManager.get().hasToken()
        }
        onDone(loggedIn)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
