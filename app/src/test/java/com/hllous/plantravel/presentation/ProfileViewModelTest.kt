package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeAuthRepository
import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.presentation.profile.ProfileViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        authRepo: FakeAuthRepository = FakeAuthRepository(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
    ) = ProfileViewModel(repository = repo, authRepository = authRepo, sessionProvider = session)

    @Test
    fun profileDetailsAreLoadedOnInit() {
        val repo = FakeTravelRepository(mpAliasByUserId = mapOf("user-1" to "nico.llousas"))
        val authRepo = FakeAuthRepository(displayName = "Nico", phone = "+5491100000000", mpAlias = "nico.llousas")
        val vm = viewModel(repo = repo, authRepo = authRepo)

        assertEquals("Nico", vm.editorState.value.displayName)
        assertEquals("+5491100000000", vm.editorState.value.phone)
        assertEquals("nico.llousas", vm.editorState.value.mpAlias)
    }

    @Test
    fun saveProfileSuccessUpdatesState() {
        val vm = viewModel()

        vm.saveProfile("Nico", "+5491100000000", "nico.new")

        assertEquals("Nico", vm.editorState.value.displayName)
        assertEquals("+5491100000000", vm.editorState.value.phone)
        assertEquals("nico.new", vm.editorState.value.mpAlias)
        assertEquals("Perfil actualizado", vm.message.value)
        assertTrue(vm.profileUpdated.value)
    }

    @Test
    fun saveProfileNetworkErrorShowsMessage() {
        val authRepo = FakeAuthRepository(updateProfileResult = Result.failure(Exception("network error")))
        val vm = viewModel(authRepo = authRepo)

        vm.saveProfile("Nico", "+5491100000000", "nico.new")

        assertEquals("Error al guardar perfil", vm.message.value)
    }

    @Test
    fun saveProfileWithBlankNameShowsValidationMessage() {
        val vm = viewModel()

        vm.saveProfile("   ", "+5491100000000", "nico.new")

        assertEquals("El nombre no puede estar vacío", vm.message.value)
    }

    @Test
    fun clearMessageNullsMessage() {
        val vm = viewModel()
        vm.saveProfile("Nico", "", "x")

        vm.clearMessage()

        assertNull(vm.message.value)
    }
}
