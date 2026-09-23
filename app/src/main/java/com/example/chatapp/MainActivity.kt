package com.example.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chatapp.data.AuthRepository
import com.example.chatapp.data.remote.SocketEvent
import com.example.chatapp.data.remote.SocketManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.chatapp.ui.navigation.Routes
import com.example.chatapp.ui.screens.ChatScreen
import com.example.chatapp.ui.screens.CreateGroupScreen
import com.example.chatapp.ui.screens.HomeScreen
import com.example.chatapp.ui.screens.LoginScreen
import com.example.chatapp.ui.screens.ProfileScreen
import com.example.chatapp.ui.screens.RegisterScreen
import com.example.chatapp.ui.screens.SplashScreen
import com.example.chatapp.ui.theme.ChatAppTheme

data class BannerData(
    val id: Long,
    val chatId: String,
    val title: String,
    val body: String
)

class MainActivity : ComponentActivity() {
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authRepo.setOnline(true)
            }
        }
        setContent {
            ChatAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    var banner by remember { mutableStateOf<BannerData?>(null) }
                    val backStack by nav.currentBackStackEntryAsState()

                    // in-app notification banner for chats other than the open one
                    LaunchedEffect(Unit) {
                        SocketManager.events.collect { event ->
                            if (event !is SocketEvent.Message) return@collect
                            val openChat = backStack?.arguments?.getString("chatId")
                            if (openChat == event.chatId) return@collect
                            val text = event.message.text.ifBlank { "\uD83D\uDCF7 Photo" }
                            val data = BannerData(
                                id = System.currentTimeMillis(),
                                chatId = event.chatId,
                                title = event.message.senderName.ifBlank { "New message" },
                                body = text
                            )
                            banner = data
                            delay(4000)
                            if (banner?.id == data.id) banner = null
                        }
                    }

                    Box(Modifier.fillMaxSize()) {
                        NavHost(navController = nav, startDestination = Routes.SPLASH) {
                        composable(Routes.SPLASH) {
                            SplashScreen(onDone = { loggedIn ->
                                nav.navigate(if (loggedIn) Routes.HOME else Routes.LOGIN) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            })
                        }
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                onLoginSuccess = {
                                    nav.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                },
                                onGoRegister = { nav.navigate(Routes.REGISTER) }
                            )
                        }
                        composable(Routes.REGISTER) {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    nav.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                },
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable(Routes.HOME) {
                            HomeScreen(
                                onOpenChat = { id, name ->
                                    nav.navigate(Routes.chat(id, name))
                                },
                                onCreateGroup = { nav.navigate(Routes.CREATE_GROUP) },
                                onProfile = { nav.navigate(Routes.PROFILE) },
                                onLoggedOut = {
                                    nav.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.CREATE_GROUP) {
                            CreateGroupScreen(
                                onGroupCreated = { id, name ->
                                    nav.popBackStack()
                                    nav.navigate(Routes.chat(id, name))
                                },
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable(Routes.PROFILE) {
                            ProfileScreen(onBack = { nav.popBackStack() })
                        }
                        composable(
                            route = Routes.CHAT,
                            arguments = listOf(
                                navArgument("chatId") { type = NavType.StringType },
                                navArgument("chatName") { type = NavType.StringType }
                            )
                        ) { backStack ->
                            val chatId = backStack.arguments?.getString("chatId").orEmpty()
                            val encoded = backStack.arguments?.getString("chatName").orEmpty()
                            val chatName = try {
                                java.net.URLDecoder.decode(encoded, "UTF-8")
                            } catch (_: Exception) { encoded }
                            ChatScreen(chatId = chatId, chatName = chatName, onBack = {
                                nav.popBackStack()
                            })
                        }
                        }
                        // banner overlay on top
                        AnimatedVisibility(
                            visible = banner != null,
                            enter = slideInVertically { -it },
                            exit = slideOutVertically { -it },
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            banner?.let { b ->
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(top = 48.dp, start = 12.dp, end = 12.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            banner = null
                                            nav.navigate(Routes.chat(b.chatId, b.title))
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        b.title,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        b.body,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            try { authRepo.setOnline(false) } catch (_: Exception) {}
        }
    }
}
