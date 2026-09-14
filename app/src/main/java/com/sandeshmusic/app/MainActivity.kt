package com.sandeshmusic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sandeshmusic.app.data.preferences.ThemeMode
import com.sandeshmusic.app.navigation.AppNavigation
import com.sandeshmusic.app.ui.splash.SplashScreen
import com.sandeshmusic.app.ui.theme.SandeshMusicTheme
import com.sandeshmusic.app.viewmodel.AuthViewModel
import com.sandeshmusic.app.viewmodel.MusicViewModel
import com.sandeshmusic.app.viewmodel.PlayerViewModel
import com.sandeshmusic.app.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val app by lazy { application as SandeshMusicApplication }

    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModel.Factory(app.themePreferencesRepository)
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory(app.authRepository, app.musicRepository, app.googleAuthManager)
    }

    private val musicViewModel: MusicViewModel by viewModels {
        MusicViewModel.Factory(app.musicRepository)
    }

    private val playerViewModel: PlayerViewModel by viewModels {
        PlayerViewModel.Factory(app.playerController)
    }

    override fun onResume() {
        super.onResume()
        authViewModel.reloadUserSilently()
        com.sandeshmusic.app.inappmessaging.InAppMessagingHelper.triggerCustomCampaignTrigger(this, "app_foreground")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize AndroidX System SplashScreen (Android 12+ and backward compatible API)
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var showSplashScreen by remember { mutableStateOf(true) }

            val themeMode by themeViewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Permission result handled gracefully */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            SandeshMusicTheme(darkTheme = isDarkTheme) {
                Crossfade(
                    targetState = showSplashScreen,
                    animationSpec = tween(500),
                    label = "splash_crossfade"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onSplashFinished = {
                                showSplashScreen = false
                            }
                        )
                    } else {
                        AppNavigation(
                            musicViewModel = musicViewModel,
                            playerViewModel = playerViewModel,
                            authViewModel = authViewModel,
                            themeViewModel = themeViewModel
                        )
                    }
                }
            }
        }
    }
}
