package com.locksite.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.locksite.app.presentation.navigation.LockSiteApp
import com.locksite.app.ui.theme.LockSiteTheme

/**
 * Single entry activity. The whole UI is Compose; the shell lives in [LockSiteApp].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as LockSiteApplication).container

        setContent {
            LockSiteTheme {
                LockSiteApp(container = container)
            }
        }
    }
}
