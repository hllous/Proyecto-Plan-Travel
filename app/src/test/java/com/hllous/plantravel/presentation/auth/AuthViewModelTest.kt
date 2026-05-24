package com.hllous.plantravel.presentation.auth

import com.hllous.plantravel.FakeAuthRepository
import com.hllous.plantravel.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(repo: FakeAuthRepository) = AuthViewModel(repo)

    // --- Init / session observation ---

    @Test
    fun initWithNullUserId_emitsUnauthenticated() {
        val repo = FakeAuthRepository()
        val vm = viewModel(repo)

        repo.userIdFlow.tryEmit(null)

        assertEquals(AuthState.Unauthenticated, vm.state.value)
    }

    @Test
    fun initWithUserIdAndDisplayName_emitsAuthenticated() {
        val repo = FakeAuthRepository(displayName = "Ana")
        val vm = viewModel(repo)

        repo.userIdFlow.tryEmit("user-123")

        assertEquals(AuthState.Authenticated("user-123", "Ana"), vm.state.value)
    }

    @Test
    fun initWithUserIdAndNoDisplayName_emitsNeedsProfileSetup() {
        val repo = FakeAuthRepository(displayName = null)
        val vm = viewModel(repo)

        repo.userIdFlow.tryEmit("user-123")

        assertEquals(AuthState.NeedsProfileSetup("user-123"), vm.state.value)
    }

    // --- login ---

    @Test
    fun login_onFailure_emitsError() {
        val repo = FakeAuthRepository(loginResult = Result.failure(Exception("Credenciales inválidas")))
        val vm = viewModel(repo)

        vm.login("a@b.com", "wrong")

        assertEquals(AuthState.Error("Credenciales inválidas"), vm.state.value)
    }

    // --- register ---

    @Test
    fun register_onFailure_emitsError() {
        val repo = FakeAuthRepository(registerResult = Result.failure(Exception("Email ya registrado")))
        val vm = viewModel(repo)

        vm.register("a@b.com", "pass")

        assertEquals(AuthState.Error("Email ya registrado"), vm.state.value)
    }

    // --- createProfile ---

    @Test
    fun createProfile_onSuccess_emitsAuthenticated() {
        val repo = FakeAuthRepository(createProfileResult = Result.success(Unit))
        val vm = viewModel(repo)
        repo.userIdFlow.tryEmit("user-123")

        vm.createProfile("Ana", "+5491100000000")

        assertEquals(AuthState.Authenticated("user-123", "Ana"), vm.state.value)
    }

    @Test
    fun createProfile_onFailure_emitsError() {
        val repo = FakeAuthRepository(createProfileResult = Result.failure(Exception("Error al guardar")))
        val vm = viewModel(repo)
        repo.userIdFlow.tryEmit("user-123")

        vm.createProfile("Ana", "+5491100000000")

        assertEquals(AuthState.Error("Error al guardar"), vm.state.value)
    }

    @Test
    fun createProfile_whenNotInNeedsProfileSetup_doesNothing() {
        val repo = FakeAuthRepository()
        val vm = viewModel(repo)
        repo.userIdFlow.tryEmit(null)

        vm.createProfile("Ana", "+5491100000000")

        assertEquals(AuthState.Unauthenticated, vm.state.value)
    }

    // --- logout ---

    @Test
    fun logout_callsRepositoryLogout() {
        val repo = FakeAuthRepository()
        val vm = viewModel(repo)

        vm.logout()

        assertEquals(true, repo.logoutCalled)
    }

    // --- clearError ---

    @Test
    fun clearError_resetsToUnauthenticated() {
        val repo = FakeAuthRepository(loginResult = Result.failure(Exception("fail")))
        val vm = viewModel(repo)
        vm.login("a@b.com", "wrong")

        vm.clearError()

        assertEquals(AuthState.Unauthenticated, vm.state.value)
    }
}
