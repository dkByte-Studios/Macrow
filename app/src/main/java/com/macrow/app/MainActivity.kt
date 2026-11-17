package com.macrow.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.macrow.app.ui.MacrowRoot
import com.macrow.app.ui.theme.MacrowTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Each time the app comes to the front, take the daily backup if one is due.
    override fun onStart() {
        super.onStart()
        val app = application as MacrowApp
        app.appScope.launch { app.backup.autoBackupIfDue() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val repo = (application as MacrowApp).repo
        // Hold the splash until settings load, so a dark-mode user never sees a light flash
        // and a new user lands on setup instead of an empty diary.
        val settings = repo.settingsFlow.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
        splash.setKeepOnScreenCondition { settings.value == null }
        setContent {
            val s by settings.collectAsStateWithLifecycle()
            // Nothing is drawn until settings are read: the splash covers this moment, and the start screen
            // (setup or diary) is decided from the real values, never from defaults.
            val current = s ?: return@setContent
            DisposableEffect(current.darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { current.darkMode },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { current.darkMode },
                )
                onDispose { }
            }
            MacrowTheme(current.darkMode) { MacrowRoot(current) }
        }
    }
}
