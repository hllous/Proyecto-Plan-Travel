package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.ConsumeInviteFailure
import com.hllous.plantravel.domain.usecase.ConsumeInviteUseCase

import com.hllous.plantravel.domain.usecase.DeleteInviteUseCase
import com.hllous.plantravel.domain.usecase.GenerateInviteUseCase
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1", displayName = "Test"),
        holder: SelectedGroupHolder = SelectedGroupHolder()
    ): MainViewModel {
        return MainViewModel(
            repository = repo,
            selectedGroupHolder = holder,
            generateInviteUseCase = GenerateInviteUseCase(repo),
            deleteInviteUseCase = DeleteInviteUseCase(repo),
            consumeInviteUseCase = ConsumeInviteUseCase(repo, session),
        )
    }

    @Test
    fun consumeInviteShowsSuccessMessageOnJoin() {
        val repo = FakeTravelRepository(consumeInviteResult = Result.success("group-1"))
        val vm = viewModel(repo = repo)

        vm.consumeInvite("VALIDCODE")

        assertEquals("Te uniste al grupo", vm.message.value)
    }

    @Test
    fun consumeInviteShowsFallbackMessageWhenExpired() {
        val repo = FakeTravelRepository(consumeInviteResult = Result.failure(ConsumeInviteFailure.Expired))
        val vm = viewModel(repo = repo)

        vm.consumeInvite("EXPIREDCODE")

        assertEquals("No se pudo usar el QR", vm.message.value)
    }

    @Test
    fun consumeInviteWithBlankCodeShowsRequiredMessage() {
        val vm = viewModel()

        vm.consumeInvite("  ")

        assertEquals("Codigo de invitacion requerido", vm.message.value)
    }
}
