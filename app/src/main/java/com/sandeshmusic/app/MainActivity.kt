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
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.sandeshmusic.app.navigation.AppNavigation
import com.sandeshmusic.app.ui.theme.SandeshMusicTheme
import com.sandeshmusic.app.viewmodel.AuthViewModel
import com.sandeshmusic.app.viewmodel.MusicViewModel
import com.sandeshmusic.app.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val app by lazy { application as SandeshMusicApplication }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory(app.authRepository, app.musicRepository)
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
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

            SandeshMusicTheme {
                AppNavigation(
                    musicViewModel = musicViewModel,
                    playerViewModel = playerViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
