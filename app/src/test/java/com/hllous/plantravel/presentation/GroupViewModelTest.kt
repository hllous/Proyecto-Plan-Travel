package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.usecase.CreateGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteMemberUseCase
import com.hllous.plantravel.domain.usecase.LeaveGroupUseCase
import com.hllous.plantravel.domain.usecase.UpdateGroupNameUseCase
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        holder: SelectedGroupHolder = SelectedGroupHolder(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1")
    ) = GroupViewModel(
        repository = repo,
        selectedGroupHolder = holder,
        sessionProvider = session,
        createGroupUseCase = CreateGroupUseCase(repo),
        updateGroupNameUseCase = UpdateGroupNameUseCase(repo),
        deleteGroupUseCase = DeleteGroupUseCase(repo),
        deleteMemberUseCase = DeleteMemberUseCase(repo),
        leaveGroupUseCase = LeaveGroupUseCase(repo)
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

    // --- currentGroup state machine ---

    @Test
    fun currentGroupIsNullWhenGroupsIsEmpty() {
        val vm = viewModel()
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        assertNull(vm.currentGroup.value)

        job.cancel()
    }

    @Test
    fun currentGroupIsFirstGroupWhenOneGroupLoaded() {
        val group = TravelGroup(id = "g1", name = "Viaje a Mendoza")
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val vm = viewModel(repo = repo)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        assertEquals(group, vm.currentGroup.value)

        job.cancel()
    }

    @Test
    fun createGroupMakesCurrentGroupNonNull() {
        val vm = viewModel()
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        vm.createGroup("Viaje a Bariloche")

        assertNotNull(vm.currentGroup.value)
        assertEquals("Viaje a Bariloche", vm.currentGroup.value?.name)

        job.cancel()
    }

    @Test
    fun renameGroupUpdatesCurrentGroupNameEvenWithoutRealtime() {
        val oldGroup = TravelGroup(id = "g1", name = "Viaje Viejo")
        val newGroup = TravelGroup(id = "g1", name = "Viaje Nuevo")
        var subscriptionCount = 0
        val repo = FakeTravelRepository(
            customObserveGroups = {
                flow {
                    subscriptionCount++
                    emit(if (subscriptionCount >= 2) listOf(newGroup) else listOf(oldGroup))
                }
            }
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "g1" }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        assertEquals("Viaje Viejo", vm.currentGroup.value?.name)

        vm.updateSelectedGroupName("Viaje Nuevo")

        assertEquals("Viaje Nuevo", vm.currentGroup.value?.name)

        job.cancel()
    }

    @Test
    fun kickMemberRemovesMemberEvenWithoutRealtime() {
        val groupId = "g1"
        val member = GroupMember(id = "m1", groupId = groupId, name = "Ana", userId = "user-2", role = MemberRole.USER)
        var subscriptionCount = 0
        val repo = FakeTravelRepository(
            customObserveMembers = {
                flow {
                    subscriptionCount++
                    emit(if (subscriptionCount >= 2) emptyList() else listOf(member))
                }
            }
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = groupId }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        assertEquals(1, vm.members.value.size)

        vm.deleteMember("m1")

        assertTrue(vm.members.value.isEmpty())

        job.cancel()
    }

    @Test
    fun deleteGroupMakesCurrentGroupNullEvenWithoutRealtimeEvent() {
        // Simulate: group is in the loaded list, deleteGroup succeeds and sets selectedGroupId=null,
        // but realtime hasn't removed the group from the flow yet.
        val group = TravelGroup(id = "g1", name = "Viaje")
        var subscriptionCount = 0
        val repo = FakeTravelRepository(
            initialGroups = listOf(group),
            customObserveGroups = {
                flow {
                    subscriptionCount++
                    emit(if (subscriptionCount >= 2) emptyList() else listOf(group))
                }
            }
        )
        val vm = viewModel(repo = repo)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        assertNotNull(vm.currentGroup.value) // group is loaded

        vm.selectGroup("g1")
        vm.deleteSelectedGroup()

        assertNull(vm.currentGroup.value) // screen should return to no-group state

        job.cancel()
    }

    @Test
    fun leaveGroupMakesCurrentGroupNullEvenWithoutRealtimeEvent() {
        val groupId = "g1"
        val group = TravelGroup(id = groupId, name = "Viaje")
        val member = GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.USER)
        var subscriptionCount = 0
        val repo = FakeTravelRepository(
            initialGroups = listOf(group),
            initialMembers = mapOf(groupId to listOf(member)),
            customObserveGroups = {
                flow {
                    subscriptionCount++
                    emit(if (subscriptionCount >= 2) emptyList() else listOf(group))
                }
            }
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = groupId }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch {
            vm.currentGroup.collect { }
            vm.members.collect { }
        }

        assertNotNull(vm.currentGroup.value)

        vm.leaveGroup()

        assertNull(vm.currentGroup.value)

        job.cancel()
    }

    @Test
    fun selectedGroupIdSetToUnknownIdTriggersReloadMakingCurrentGroupNonNull() {
        // Simulate: selectedGroupId is set (e.g. createGroup succeeded) but realtime
        // hasn't fired yet — the first observeGroups subscription returns empty, the
        // second (triggered by reloadGroups) returns the group.
        val group = TravelGroup(id = "new-group-id", name = "Viaje")
        var subscriptionCount = 0
        val repo = FakeTravelRepository(
            customObserveGroups = {
                flow {
                    subscriptionCount++
                    emit(if (subscriptionCount >= 2) listOf(group) else emptyList())
                }
            }
        )
        val holder = SelectedGroupHolder()
        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        assertNull(vm.currentGroup.value)

        holder.selectedGroupId.value = "new-group-id"

        assertNotNull(vm.currentGroup.value)
        assertEquals("Viaje", vm.currentGroup.value?.name)

        job.cancel()
    }

    // --- Leave Group ---

    @Test
    fun leaveGroupAsUserClearsSelectedGroup() {
        val groupId = "group-1"
        val members = listOf(GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.USER))
        val repo = FakeTravelRepository(initialMembers = mapOf(groupId to members))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = groupId }
        val vm = viewModel(repo = repo, holder = holder, session = FakeSessionProvider(userId = "user-1"))
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        vm.leaveGroup()

        assertNull(vm.selectedGroupId.value)
        job.cancel()
    }

    @Test
    fun leaveGroupAsUserShowsSuccessMessage() {
        val groupId = "group-1"
        val members = listOf(GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.USER))
        val repo = FakeTravelRepository(initialMembers = mapOf(groupId to members))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = groupId }
        val vm = viewModel(repo = repo, holder = holder, session = FakeSessionProvider(userId = "user-1"))
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        vm.leaveGroup()

        assertEquals("Abandonaste el grupo", vm.message.value)
        job.cancel()
    }

    @Test
    fun leaveGroupAsAdminShowsErrorMessage() {
        val groupId = "group-1"
        val members = listOf(GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.ADMIN))
        val repo = FakeTravelRepository(initialMembers = mapOf(groupId to members))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = groupId }
        val vm = viewModel(repo = repo, holder = holder, session = FakeSessionProvider(userId = "user-1"))
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        vm.leaveGroup()

        assertEquals("El administrador no puede abandonar el grupo", vm.message.value)
        assertEquals(groupId, vm.selectedGroupId.value)
        job.cancel()
    }

    @Test
    fun leaveGroupWithNoGroupSelectedShowsErrorMessage() {
        val vm = viewModel()
        vm.leaveGroup()
        assertEquals("Selecciona un grupo", vm.message.value)
    }

    @Test
    fun leaveGroupNetworkErrorShowsErrorMessage() {
        val groupId = "group-1"
        val members = listOf(GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.USER))
        val repo = FakeTravelRepository(initialMembers = mapOf(groupId to members), leaveGroupThrows = true)
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = groupId }
        val vm = viewModel(repo = repo, holder = holder, session = FakeSessionProvider(userId = "user-1"))
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        vm.leaveGroup()

        assertEquals("Error al abandonar el grupo", vm.message.value)
        assertEquals(groupId, vm.selectedGroupId.value)
        job.cancel()
    }

    // --- ADMIN kick confirmation ---

    @Test
    fun requestKickMemberSetsPendingMemberId() {
        val vm = viewModel()
        vm.requestKickMember("m5")
        assertEquals("m5", vm.pendingKickMemberId.value)
    }

    @Test
    fun confirmKickCallsDeleteAndClearsPending() {
        val vm = viewModel()
        vm.requestKickMember("m5")
        vm.confirmKick()
        assertNull(vm.pendingKickMemberId.value)
        assertEquals("Integrante eliminado", vm.message.value)
    }

    @Test
    fun cancelKickClearsPendingWithoutDeleting() {
        val vm = viewModel()
        vm.requestKickMember("m5")
        vm.cancelKick()
        assertNull(vm.pendingKickMemberId.value)
        assertNull(vm.message.value)
    }
}
