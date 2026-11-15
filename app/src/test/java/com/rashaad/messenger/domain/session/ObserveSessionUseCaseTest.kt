package com.rashaad.messenger.domain.session

import app.cash.turbine.test
import com.rashaad.messenger.domain.model.AuthUser
import com.rashaad.messenger.domain.model.UserProfile
import com.rashaad.messenger.testing.FakeAuthRepository
import com.rashaad.messenger.testing.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSessionUseCaseTest {

    private val auth = FakeAuthRepository()
    private val users = FakeUserRepository()
    private val observeSession = ObserveSessionUseCase(auth, users)
    private val me = AuthUser("me", "me@example.com", null)

    @Test
    fun `full journey - signed out, needs profile, signed in, signed out`() = runTest {
        observeSession().test {
            assertEquals(SessionState.SignedOut, awaitItem())

            auth.setUser(me)
            assertEquals(SessionState.Loading, awaitItem()) // waiting for the server
            users.markLoaded("me")
            assertEquals(SessionState.NeedsProfile(me), awaitItem())

            users.createProfile("me", "ada", "Ada")
            assertEquals(SessionState.SignedIn(UserProfile("me", "ada", "Ada")), awaitItem())

            auth.signOut()
            assertEquals(SessionState.SignedOut, awaitItem())
        }
    }

    @Test
    fun `existing user goes straight to signed in`() = runTest {
        users.addProfile(UserProfile("me", "ada", "Ada"))
        auth.setUser(me)

        observeSession().test {
            assertEquals(SessionState.Loading, awaitItem())
            assertEquals(SessionState.SignedIn(UserProfile("me", "ada", "Ada")), awaitItem())
        }
    }

    @Test
    fun `stays loading until the server answers`() = runTest {
        auth.setUser(me)
        observeSession().test {
            assertEquals(SessionState.Loading, awaitItem())
            expectNoEvents() // no premature NeedsProfile from an empty cache
        }
    }
}
