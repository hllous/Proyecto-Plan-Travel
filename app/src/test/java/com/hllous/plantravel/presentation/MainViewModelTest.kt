package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.ConsumeInviteFailure
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.usecase.ConsumeInviteUseCase
import com.hllous.plantravel.domain.usecase.DeleteInviteUseCase
import com.hllous.plantravel.domain.usecase.GenerateInviteUseCase
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun consumeInviteShowsExpiredMessageWhenExpired() {
        val repo = FakeTravelRepository(consumeInviteResult = Result.failure(ConsumeInviteFailure.Expired))
        val vm = viewModel(repo = repo)

        vm.consumeInvite("EXPIREDCODE")

        assertEquals("El codigo de invitacion vencio", vm.message.value)
    }

    @Test
    fun consumeInviteShowsAlreadyMemberMessage() {
        val repo = FakeTravelRepository(consumeInviteResult = Result.failure(ConsumeInviteFailure.AlreadyMember))
        val vm = viewModel(repo = repo)

        vm.consumeInvite("DUPCODE")

        assertEquals("Ya sos miembro de este grupo", vm.message.value)
    }

    @Test
    fun consumeInviteWithBlankCodeShowsRequiredMessage() {
        val vm = viewModel()

        vm.consumeInvite("  ")

        assertEquals("Codigo de invitacion requerido", vm.message.value)
    }

    @Test
    fun generateInviteWithNoGroupSelectedShowsSelectGroupMessage() {
        val vm = viewModel()

        vm.generateInvite()

        assertEquals("Selecciona un grupo", vm.message.value)
    }

    @Test
    fun generateInviteSuccessShowsMessage() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(holder = holder)

        vm.generateInvite()

        assertEquals("Invitacion generada", vm.message.value)
    }

    @Test
    fun generateInviteShowsErrorMessageOnFailure() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val repo = FakeTravelRepository(generateInviteThrows = true)
        val vm = viewModel(repo = repo, holder = holder)

        vm.generateInvite()

        assertEquals("Error al generar invitacion", vm.message.value)
    }

    @Test
    fun deleteInviteSuccessShowsMessage() {
        val vm = viewModel()

        vm.deleteInvite("FAKECODE")

        assertEquals("Invitacion eliminada", vm.message.value)
    }

    @Test
    fun deleteInviteShowsErrorMessageOnFailure() {
        val repo = FakeTravelRepository(deleteInviteThrows = true)
        val vm = viewModel(repo = repo)

        vm.deleteInvite("FAKECODE")

        assertEquals("Error al eliminar invitacion", vm.message.value)
    }

    @Test
    fun invitesUpdateWhenRemoteInvitePushed() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val repo = FakeTravelRepository()
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.invites.collect { } }

        assertTrue(vm.invites.value.isEmpty())

        val remoteInvite = InviteToken(code = "REMOTE1", groupId = "group-1", link = "plantravel://invite/REMOTE1", expiresAtMillis = Long.MAX_VALUE)
        repo.simulateRemoteInvitesPush("group-1", listOf(remoteInvite))

        assertEquals(1, vm.invites.value.size)
        assertEquals("REMOTE1", vm.invites.value.first().code)

        job.cancel()
    }

    @Test
    fun invitesClearWhenRemoteDeletePushed() {
        val invite = InviteToken(code = "CODE1", groupId = "group-1", link = "plantravel://invite/CODE1", expiresAtMillis = Long.MAX_VALUE)
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val repo = FakeTravelRepository()
        repo.simulateRemoteInvitesPush("group-1", listOf(invite))
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.invites.collect { } }

        assertEquals(1, vm.invites.value.size)

        repo.simulateRemoteInvitesPush("group-1", emptyList())

        assertTrue(vm.invites.value.isEmpty())

        job.cancel()
    }

    @Test
    fun generateInviteShowsCodeEvenWithoutRealtime() {
        val invite = InviteToken(code = "ABC123", groupId = "g1", link = "plantravel://invite/ABC123", expiresAtMillis = Long.MAX_VALUE)
        var subscriptionCount = 0
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "g1" }
        val repo = FakeTravelRepository(
            customObserveInvites = {
                flow {
                    subscriptionCount++
                    emit(if (subscriptionCount >= 2) listOf(invite) else emptyList())
                }
            }
        )
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.invites.collect { } }

        assertTrue(vm.invites.value.isEmpty())

        vm.generateInvite()

        assertTrue(vm.invites.value.isNotEmpty())
        assertEquals("ABC123", vm.invites.value.first().code)

        job.cancel()
    }

    @Test
    fun deleteInviteRemovesCodeEvenWithoutRealtime() {
        val invite = InviteToken(code = "ABC123", groupId = "g1", link = "plantravel://invite/ABC123", expiresAtMillis = Long.MAX_VALUE)
        var subscriptionCount = 0
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "g1" }
        val repo = FakeTravelRepository(
            customObserveInvites = {
                flow {
                    subscriptionCount++
                    emit(if (subscriptionCount >= 2) emptyList() else listOf(invite))
                }
            }
        )
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.invites.collect { } }

        assertEquals(1, vm.invites.value.size)

        vm.deleteInvite("ABC123")

        assertTrue(vm.invites.value.isEmpty())

        job.cancel()
    }
}
