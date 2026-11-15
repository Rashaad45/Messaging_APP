package com.rashaad.messenger.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rashaad.messenger.R
import com.rashaad.messenger.domain.model.UserProfile
import com.rashaad.messenger.domain.session.SessionState
import com.rashaad.messenger.ui.auth.SignInRoute
import com.rashaad.messenger.ui.home.HomeScreen
import com.rashaad.messenger.ui.onboarding.ProfileSetupRoute
import com.rashaad.messenger.ui.session.SessionViewModel

/**
 * App root. The session state, not the back stack, decides whether the user sees sign-in,
 * onboarding, or the main app. This means "Back" can never return to the sign-in screen
 * after signing in, and signing out anywhere instantly shows sign-in again.
 */
@Composable
fun MessengerApp(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = session) {
            SessionState.Loading -> FullScreenLoading()
            SessionState.SignedOut -> SignInRoute()
            is SessionState.NeedsProfile -> ProfileSetupRoute(onSignOut = sessionViewModel::signOut)
            is SessionState.SignedIn -> MainNavHost(profile = state.profile, onSignOut = sessionViewModel::signOut)
        }
    }
}

/** Navigation for signed-in users. Grows in M3/M4 (conversation list, search, chat). */
@Composable
private fun MainNavHost(profile: UserProfile, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.Home) {
        composable<Route.Home> {
            HomeScreen(profile = profile, onSignOut = onSignOut)
        }
    }
}

@Composable
private fun FullScreenLoading() {
    val label = stringResource(R.string.loading)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = label })
    }
}
