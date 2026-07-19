package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.AeroStrangeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChatViewModel = viewModel()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val activeCall by viewModel.activeCall.collectAsState()

            // Observe dynamic configurations from settings Flow
            val themeSetting = settings["theme"] ?: "dark"
            val isDark = themeSetting == "dark"
            val isHighContrast = settings["high_contrast"] == "true"
            val animationsEnabled = settings["animations"] != "false"

            AeroStrangeTheme(isDark = isDark, isHighContrast = isHighContrast) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // Screen Router
                        if (animationsEnabled) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "screen_routing"
                            ) { screen ->
                                RenderScreen(screen = screen, viewModel = viewModel)
                            }
                        } else {
                            RenderScreen(screen = currentScreen, viewModel = viewModel)
                        }

                        // Full Screen VoIP Calling Overlay (Renders on top of any active screen)
                        if (activeCall != null) {
                            CallOverlay(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenderScreen(screen: String, viewModel: ChatViewModel) {
    when (screen) {
        "auth" -> AuthScreen(viewModel = viewModel)
        "main_chats" -> MainScreen(viewModel = viewModel)
        "chat_view" -> ChatScreen(viewModel = viewModel)
        else -> AuthScreen(viewModel = viewModel)
    }
}
