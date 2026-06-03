package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.presentation.profile.ProfileViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
    ) = ProfileViewModel(repository = repo, sessionProvider = session)

    @Test
    fun mpAliasIsLoadedOnInit() {
        val repo = FakeTravelRepository(mpAliasByUserId = mapOf("user-1" to "nico.llousas"))
        val vm = viewModel(repo = repo)

        assertEquals("nico.llousas", vm.mpAlias.value)
    }

    @Test
    fun updateMpAliasSuccessUpdatesState() {
        val vm = viewModel()

        vm.updateMpAlias("nico.new")

        assertEquals("nico.new", vm.mpAlias.value)
        assertEquals("Alias actualizado", vm.message.value)
    }

    @Test
    fun updateMpAliasNetworkErrorShowsMessage() {
        val repo = FakeTravelRepository(updateMpAliasThrows = true)
        val vm = viewModel(repo = repo)

        vm.updateMpAlias("nico.new")

        assertEquals("Error al guardar alias", vm.message.value)
    }

    @Test
    fun clearMessageNullsMessage() {
        val vm = viewModel()
        vm.updateMpAlias("x")

        vm.clearMessage()

        assertNull(vm.message.value)
    }
}
