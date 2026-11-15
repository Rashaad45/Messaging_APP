package com.rashaad.messenger.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe routes for signed-in navigation: each destination is a @Serializable type
 * instead of a string path, so arguments (e.g. a conversation id) are checked by the compiler.
 * Sign-in and onboarding are not routes; they're chosen by SessionState (see MessengerApp).
 */
sealed interface Route {
    @Serializable
    data object Home : Route
}
