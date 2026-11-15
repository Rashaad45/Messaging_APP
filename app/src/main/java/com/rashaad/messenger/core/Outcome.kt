package com.rashaad.messenger.core

/**
 * Result of an operation that can fail with a *typed*, expected error [E].
 *
 * Unlike kotlin.Result (which carries any Throwable), the error type is part of the
 * signature, so callers handle every failure case and the compiler checks `when` exhaustiveness.
 */
sealed interface Outcome<out T, out E> {
    data class Success<out T>(val value: T) : Outcome<T, Nothing>
    data class Failure<out E>(val error: E) : Outcome<Nothing, E>
}
