package com.rashaad.messenger.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe routes: each destination is a @Serializable type instead of a string path,
 * so arguments (e.g. a conversation id) are checked by the compiler.
 */
sealed interface Route {
    @Serializable
    data object Welcome : Route
}
