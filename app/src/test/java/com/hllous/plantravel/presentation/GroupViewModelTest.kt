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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1")
    ) = GroupViewModel(
        repository = repo,
        selectedGroupHolder = SelectedGroupHolder(),
        sessionProvider = session,
        createGroupUseCase = CreateGroupUseCase(repo),
        updateGroupNameUseCase = UpdateGroupNameUseCase(repo),
        deleteGroupUseCase = DeleteGroupUseCase(repo),
        deleteMemberUseCase = DeleteMemberUseCase(repo),
        leaveGroupUseCase = LeaveGroupUseCase(repo)
    )

    // ─── Members auto-load ───────────────────────────────────────────────────────

    @Test
    fun membersLoadAutomaticallyWhenGroupLoaded() {
        val group = TravelGroup(id = "group-1", name = "Viaje")
        val members = listOf(GroupMember(id = "m1", groupId = "group-1", name = "Nico", userId = "user-1", role = MemberRole.ADMIN))
        val repo = FakeTravelRepository(initialGroups = listOf(group), initialMembers = mapOf("group-1" to members))
        val vm = viewModel(repo = repo)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        // No selectGroup() call — members must load from currentGroup automatically
        assertEquals(members, vm.members.value)
        job.cancel()
    }

    @Test
    fun membersAreEmptyWhenUserHasNoGroup() {
        val vm = viewModel()
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        assertTrue(vm.members.value.isEmpty())
        job.cancel()
    }

    // ─── Create group ────────────────────────────────────────────────────────────

    @Test
    fun createGroupExposesSelectedGroupId() {
        val vm = viewModel()
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        vm.createGroup("Viaje Mendoza")

        assertEquals("fake-group-id", vm.selectedGroupId.value)
        job.cancel()
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

    // ─── Delete group ────────────────────────────────────────────────────────────

    @Test
    fun deleteSelectedGroupClearsSelectedGroupId() {
        val group = TravelGroup(id = "g1", name = "Viaje")
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val vm = viewModel(repo = repo)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        vm.deleteSelectedGroup()

        assertNull(vm.selectedGroupId.value)
        job.cancel()
    }

    @Test
    fun deleteSelectedGroupWithNoSelectionShowsErrorMessage() {
        val vm = viewModel()
        vm.deleteSelectedGroup()
        assertEquals("Selecciona un grupo", vm.message.value)
    }

    @Test
    fun deleteGroupMakesCurrentGroupNullEvenWithoutRealtimeEvent() {
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

        assertNotNull(vm.currentGroup.value)

        vm.deleteSelectedGroup()

        assertNull(vm.currentGroup.value)
        job.cancel()
    }

    // ─── Update group name ────────────────────────────────────────────────────────

    @Test
    fun deleteMemberShowsConfirmationMessage() {
        val vm = viewModel()
        vm.deleteMember("member-5")
        assertEquals("Integrante eliminado", vm.message.value)
    }

    @Test
    fun updateSelectedGroupNameWithValidNameShowsSuccessMessage() {
        val group = TravelGroup(id = "group-1", name = "Viaje")
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val vm = viewModel(repo = repo)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        vm.updateSelectedGroupName("Nuevo Nombre")

        assertEquals("Nombre del grupo actualizado", vm.message.value)
        job.cancel()
    }

    @Test
    fun updateSelectedGroupNameWithBlankNameShowsErrorMessage() {
        val group = TravelGroup(id = "group-1", name = "Viaje")
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val vm = viewModel(repo = repo)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.currentGroup.collect { } }

        vm.updateSelectedGroupName("   ")

        assertEquals("Selecciona grupo y nombre valido", vm.message.value)
        job.cancel()
    }

    @Test
    fun membersStateUpdatesAfterConsumeInvite() {
        val groupId = "group-1"
        val userId = "user-1"
        val group = TravelGroup(id = groupId, name = "Viaje")
        val repo = FakeTravelRepository(initialGroups = listOf(group), consumeInviteResult = Result.success(groupId))
        val vm = viewModel(repo = repo)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        kotlinx.coroutines.runBlocking { repo.consumeInvite("CODE", userId, "Nico") }

        assertEquals(userId, repo.lastConsumeUserId)
        assertEquals(userId, vm.members.value.firstOrNull()?.userId)
        job.cancel()
    }

    // ─── currentGroup state machine ───────────────────────────────────────────────

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
        val vm = viewModel(repo = repo)
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
        val group = TravelGroup(id = groupId, name = "Viaje")
        val member = GroupMember(id = "m1", groupId = groupId, name = "Ana", userId = "user-2", role = MemberRole.USER)
        var subscriptionCount = 0
        val repo = FakeTravelRepository(
            initialGroups = listOf(group),
            customObserveMembers = {
                flow {
                    subscriptionCount++
                    emit(if (subscriptionCount >= 2) emptyList() else listOf(member))
                }
            }
        )
        val vm = viewModel(repo = repo)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        assertEquals(1, vm.members.value.size)

        vm.deleteMember("m1")

        assertTrue(vm.members.value.isEmpty())

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
        val vm = viewModel(repo = repo)
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

    // ─── Leave Group ─────────────────────────────────────────────────────────────

    @Test
    fun leaveGroupAsUserClearsSelectedGroup() {
        val groupId = "group-1"
        val group = TravelGroup(id = groupId, name = "Viaje")
        val members = listOf(GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.USER))
        val repo = FakeTravelRepository(initialGroups = listOf(group), initialMembers = mapOf(groupId to members))
        val vm = viewModel(repo = repo, session = FakeSessionProvider(userId = "user-1"))
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        vm.leaveGroup()

        assertNull(vm.selectedGroupId.value)
        job.cancel()
    }

    @Test
    fun leaveGroupAsUserShowsSuccessMessage() {
        val groupId = "group-1"
        val group = TravelGroup(id = groupId, name = "Viaje")
        val members = listOf(GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.USER))
        val repo = FakeTravelRepository(initialGroups = listOf(group), initialMembers = mapOf(groupId to members))
        val vm = viewModel(repo = repo, session = FakeSessionProvider(userId = "user-1"))
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        vm.leaveGroup()

        assertEquals("Abandonaste el grupo", vm.message.value)
        job.cancel()
    }

    @Test
    fun leaveGroupAsAdminShowsErrorMessage() {
        val groupId = "group-1"
        val group = TravelGroup(id = groupId, name = "Viaje")
        val members = listOf(GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.ADMIN))
        val repo = FakeTravelRepository(initialGroups = listOf(group), initialMembers = mapOf(groupId to members))
        val vm = viewModel(repo = repo, session = FakeSessionProvider(userId = "user-1"))
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
        val group = TravelGroup(id = groupId, name = "Viaje")
        val members = listOf(GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.USER))
        val repo = FakeTravelRepository(initialGroups = listOf(group), initialMembers = mapOf(groupId to members), leaveGroupThrows = true)
        val vm = viewModel(repo = repo, session = FakeSessionProvider(userId = "user-1"))
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job = scope.launch { vm.members.collect { } }

        vm.leaveGroup()

        assertEquals("Error al abandonar el grupo", vm.message.value)
        assertEquals(groupId, vm.selectedGroupId.value)
        job.cancel()
    }

    // ─── ADMIN kick confirmation ──────────────────────────────────────────────────

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
