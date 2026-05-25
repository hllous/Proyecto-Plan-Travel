package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.usecase.CreateGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteMemberUseCase
import com.hllous.plantravel.domain.usecase.UpdateGroupNameUseCase
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        holder: SelectedGroupHolder = SelectedGroupHolder()
    ) = GroupViewModel(
        repository = repo,
        selectedGroupHolder = holder,
        createGroupUseCase = CreateGroupUseCase(repo),
        updateGroupNameUseCase = UpdateGroupNameUseCase(repo),
        deleteGroupUseCase = DeleteGroupUseCase(repo),
        deleteMemberUseCase = DeleteMemberUseCase(repo)
    )

    @Test
    fun selectGroupUpdatesSelectedGroupId() {
        val vm = viewModel()
        vm.selectGroup("group-42")
        assertEquals("group-42", vm.selectedGroupId.value)
    }

    @Test
    fun createGroupSelectsTheNewGroup() {
        val vm = viewModel()
        vm.createGroup("Viaje Mendoza")
        assertEquals("fake-group-id", vm.selectedGroupId.value)
    }

    @Test
    fun createGroupNetworkErrorShowsErrorMessage() {
        val repo = FakeTravelRepository(createGroupThrows = true)
        val vm = viewModel(repo = repo)

        vm.createGroup("Viaje Mendoza")

        assertEquals("Error al crear grupo", vm.message.value)
        assertNull(vm.selectedGroupId.value)
    }

    @Test
    fun createGroupWithBlankNameShowsErrorMessage() {
        val vm = viewModel()
        vm.createGroup("   ")
        assertEquals("Completa el nombre del grupo", vm.message.value)
        assertNull(vm.selectedGroupId.value)
    }

    @Test
    fun deleteSelectedGroupClearsSelectedGroupId() {
        val holder = SelectedGroupHolder()
        holder.selectedGroupId.value = "group-10"
        val vm = viewModel(holder = holder)
        vm.deleteSelectedGroup()
        assertNull(vm.selectedGroupId.value)
    }

    @Test
    fun deleteSelectedGroupWithNoSelectionShowsErrorMessage() {
        val vm = viewModel()
        vm.deleteSelectedGroup()
        assertEquals("Selecciona un grupo", vm.message.value)
    }

    @Test
    fun deleteMemberShowsConfirmationMessage() {
        val vm = viewModel()
        vm.deleteMember("member-5")
        assertEquals("Integrante eliminado", vm.message.value)
    }

    @Test
    fun updateSelectedGroupNameWithValidNameShowsSuccessMessage() {
        val holder = SelectedGroupHolder()
        holder.selectedGroupId.value = "group-1"
        val vm = viewModel(holder = holder)
        vm.updateSelectedGroupName("Nuevo Nombre")
        assertEquals("Nombre del grupo actualizado", vm.message.value)
    }

    @Test
    fun updateSelectedGroupNameWithBlankNameShowsErrorMessage() {
        val holder = SelectedGroupHolder()
        holder.selectedGroupId.value = "group-1"
        val vm = viewModel(holder = holder)
        vm.updateSelectedGroupName("   ")
        assertEquals("Selecciona grupo y nombre valido", vm.message.value)
    }

    @Test
    fun selectingGroupUpdatesMemberList() {
        val members = listOf(GroupMember(id = "m1", groupId = "group-1", name = "Nico", userId = "user-1", role = MemberRole.ADMIN))
        val repo = FakeTravelRepository(initialMembers = mapOf("group-1" to members))
        val holder = SelectedGroupHolder()
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        vm.selectGroup("group-1")

        assertEquals(members, vm.members.value)
        job.cancel()
    }

    @Test
    fun membersStateUpdatesAfterConsumeInvite() {
        val groupId = "group-1"
        val userId = "user-1"
        val repo = FakeTravelRepository(consumeInviteResult = Result.success(groupId))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = groupId }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        kotlinx.coroutines.runBlocking { repo.consumeInvite("CODE", userId, "Nico") }

        assertEquals(userId, repo.lastConsumeUserId)
        assertEquals(userId, vm.members.value.firstOrNull()?.userId)
        job.cancel()
    }
}
