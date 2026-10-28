package com.example.minibooklibrary.ui.auth

import app.cash.turbine.test
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.repository.AuthResult
import com.example.minibooklibrary.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(testDispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val userRepo: UserRepository = mockk(relaxed = true)
    private fun newVm() = AuthViewModel(userRepo)

    @Test
    fun `mismatched passwords produce an error on the confirm field`() = runTest(testDispatcher) {
        val vm = newVm()
        vm.register("alice", "alice@example.com", "abc123", "differs")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            AuthError(AuthField.CONFIRM_PASSWORD, R.string.auth_error_password_mismatch),
            vm.uiState.value.error
        )
    }

    @Test
    fun `register validation errors land on the field they belong to`() = runTest(testDispatcher) {
        coEvery { userRepo.register(any(), any(), any()) } returns
            AuthResult.Failure(AuthResult.FailureReason.INVALID_EMAIL)
        val vm = newVm()
        vm.register("alice", "nope", "abc123", "abc123")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthField.EMAIL, vm.uiState.value.error?.field)
    }

    @Test
    fun `successful login emits LoggedIn event and unsets loading`() = runTest(testDispatcher) {
        coEvery { userRepo.login("alice", "abc123") } returns
            AuthResult.Success(7L, "alice")
        val vm = newVm()

        vm.events.test {
            vm.login("alice", "abc123")
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is AuthEvent.LoggedIn)
            assertEquals(7L, (event as AuthEvent.LoggedIn).userId)
            assertEquals(false, vm.uiState.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed login surfaces a credentials error on the password field`() = runTest(testDispatcher) {
        coEvery { userRepo.login(any(), any()) } returns
            AuthResult.Failure(AuthResult.FailureReason.INVALID_CREDENTIALS)
        val vm = newVm()
        vm.login("alice", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()
        val error = vm.uiState.value.error
        assertNotNull(error)
        assertEquals(AuthField.PASSWORD, error!!.field)
        assertEquals(R.string.auth_error_invalid_credentials, error.messageRes)
    }

    @Test
    fun `consumeError clears the error after the UI handles it`() = runTest(testDispatcher) {
        coEvery { userRepo.login(any(), any()) } returns
            AuthResult.Failure(AuthResult.FailureReason.INVALID_CREDENTIALS)
        val vm = newVm()
        vm.login("a", "b")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.consumeError()
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun `logout clears session and emits LoggedOut`() = runTest(testDispatcher) {
        // userRepo is relaxed; logout() needs no explicit stubbing.
        val vm = newVm()
        vm.events.test {
            vm.logout()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(AuthEvent.LoggedOut, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
