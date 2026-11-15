package com.rashaad.messenger.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rashaad.messenger.ui.welcome.WelcomeScreen

@Composable
fun MessengerNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.Welcome) {
        composable<Route.Welcome> {
            WelcomeScreen()
        }
    }
}
