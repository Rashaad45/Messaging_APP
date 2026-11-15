package com.rashaad.messenger.domain.model

/**
 * The signed-in account (Firebase Auth), independent of whether a profile exists yet.
 * [suggestedDisplayName] comes from the Google account, if any, to prefill onboarding.
 */
data class AuthUser(val uid: String, val email: String?, val suggestedDisplayName: String?)
