package com.rashaad.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rashaad.messenger.navigation.MessengerApp
import com.rashaad.messenger.ui.theme.MessengerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The app's only Activity. Every screen is a Compose destination inside [MessengerApp].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15+ forces edge-to-edge for targetSdk >= 35; opting in explicitly keeps
        // behaviour consistent on older versions too. Screens handle insets via Scaffold.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MessengerTheme {
                MessengerApp()
            }
        }
    }
}
