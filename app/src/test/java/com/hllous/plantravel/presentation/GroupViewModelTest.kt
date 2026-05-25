package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import com.hllous.plantravel.domain.usecase.CreateGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteMemberUseCase
import com.hllous.plantravel.domain.usecase.UpdateGroupNameUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GroupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
        holder: SelectedGroupHolder = SelectedGroupHolder()
    ) = GroupViewModel(
        repository = repo,
        sessionProvider = session,
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
        vm.createGroup("Viaje Mendoza", "Nico")
        assertEquals("fake-group-id", vm.selectedGroupId.value)
    }

    @Test
    fun deleteSelectedGroupClearsSelectedGroupId() {
        val holder = SelectedGroupHolder()
        holder.selectedGroupId.value = "group-10"
        val vm = viewModel(holder = holder)
        vm.deleteSelectedGroup()
        assertEquals(null, vm.selectedGroupId.value)
    }

    @Test
    fun deleteMemberShowsConfirmationMessage() {
        val vm = viewModel()
        vm.deleteMember("member-5")
        assertEquals("Integrante eliminado", vm.message.value)
    }
}
