package com.fidget.patternlock

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.fidget.patternlock.data.FidgetEnv
import com.fidget.patternlock.ui.FidgetApp
import com.fidget.patternlock.ui.LocalEnv

class MainActivity : ComponentActivity() {

    private lateinit var env: FidgetEnv

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        env = (application as FidgetApplication).env
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContent {
            CompositionLocalProvider(LocalEnv provides env) {
                FidgetApp(env)
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = false
                        isAppearanceLightNavigationBars = false
                    }
                    window.decorView.setBackgroundColor(env.settings.theme.bgTop)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        env.settings.persistTotal()
    }
}
