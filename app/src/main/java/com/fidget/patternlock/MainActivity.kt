package com.fidget.patternlock

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
        if (Build.VERSION.SDK_INT >= 28) {
            // Draw into the camera cut-out too, so there is no black strip at the top.
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
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

    /** Full screen: no status bar, no navigation bar. A swipe from the edge brings them back for a moment. */
    private fun enterImmersive() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersive()
    }

    override fun onResume() {
        super.onResume()
        env.music.foreground = true
        enterImmersive()
    }

    override fun onPause() {
        super.onPause()
        env.music.foreground = false
        env.settings.persistTotal()
    }
}
