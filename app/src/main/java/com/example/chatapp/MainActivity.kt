package com.example.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chatapp.data.AuthRepository
import com.example.chatapp.ui.navigation.Routes
import com.example.chatapp.ui.screens.ChatScreen
import com.example.chatapp.ui.screens.CreateGroupScreen
import com.example.chatapp.ui.screens.HomeScreen
import com.example.chatapp.ui.screens.LoginScreen
import com.example.chatapp.ui.screens.ProfileScreen
import com.example.chatapp.ui.screens.RegisterScreen
import com.example.chatapp.ui.screens.SplashScreen
import com.example.chatapp.ui.theme.ChatAppTheme
import kotlinx.coroutines.launch

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
