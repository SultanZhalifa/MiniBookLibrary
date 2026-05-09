package com.example.minibooklibrary.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.minibooklibrary.data.repository.AuthResult
import com.example.minibooklibrary.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.every
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(testDispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val userRepo: UserRepository = mockk(relaxed = true)
    private fun newVm() = AuthViewModel(userRepo)

    @Test
    fun `mismatched passwords produce inline error`() = runTest(testDispatcher) {
        val vm = newVm()
        vm.register("alice", "alice@example.com", "abc123", "differs")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Passwords don't match", vm.uiState.value.errorMessage)
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
    fun `failed login surfaces error message`() = runTest(testDispatcher) {
        coEvery { userRepo.login(any(), any()) } returns
            AuthResult.Failure(AuthResult.FailureReason.INVALID_CREDENTIALS, "nope")
        val vm = newVm()
        vm.login("alice", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals("nope", vm.uiState.value.errorMessage)
    }

    @Test
    fun `consumeError clears the error after the UI handles it`() = runTest(testDispatcher) {
        coEvery { userRepo.login(any(), any()) } returns
            AuthResult.Failure(AuthResult.FailureReason.INVALID_CREDENTIALS, "nope")
        val vm = newVm()
        vm.login("a", "b")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.consumeError()
        assertEquals(null, vm.uiState.value.errorMessage)
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
