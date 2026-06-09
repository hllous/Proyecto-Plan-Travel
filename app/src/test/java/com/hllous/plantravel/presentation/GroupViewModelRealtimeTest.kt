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
import com.hllous.plantravel.domain.usecase.EndTripUseCase
import com.hllous.plantravel.domain.usecase.LeaveGroupUseCase
import com.hllous.plantravel.domain.usecase.ReactivateTripUseCase
import com.hllous.plantravel.domain.usecase.UpdateGroupNameUseCase
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GroupViewModelRealtimeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository,
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
    ) = GroupViewModel(
        repository = repo,
        selectedGroupHolder = SelectedGroupHolder(),
        sessionProvider = session,
        createGroupUseCase = CreateGroupUseCase(repo),
        updateGroupNameUseCase = UpdateGroupNameUseCase(repo),
        deleteGroupUseCase = DeleteGroupUseCase(repo),
        deleteMemberUseCase = DeleteMemberUseCase(repo),
        leaveGroupUseCase = LeaveGroupUseCase(repo),
        endTripUseCase = EndTripUseCase(repo),
        reactivateTripUseCase = ReactivateTripUseCase(repo),
    )

    private fun warmUp(block: suspend () -> Unit): CoroutineScope {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope.launch { block() }
        return scope
    }

    // ─── Remote groups push ──────────────────────────────────────────────────────

    @Test
    fun groupsUpdateWhenRemoteGroupsPushed() {
        val repo = FakeTravelRepository()
        val vm = viewModel(repo = repo)
        val scope = warmUp { vm.groups.collect { } }

        assertTrue(vm.groups.value.isEmpty())

        repo.simulateRemoteGroupsPush(listOf(TravelGroup(id = "g1", name = "Viaje Remoto")))

        assertEquals(1, vm.groups.value.size)
        assertEquals("Viaje Remoto", vm.groups.value.first().name)

        scope.cancel()
    }

    @Test
    fun currentGroupUpdatesWhenRemoteGroupsPushed() {
        val repo = FakeTravelRepository()
        val vm = viewModel(repo = repo)
        val scope = warmUp {
            vm.groups.collect { }
            vm.currentGroup.collect { }
        }

        val remoteGroup = TravelGroup(id = "g1", name = "Viaje Bariloche")
        repo.simulateRemoteGroupsPush(listOf(remoteGroup))

        assertEquals(remoteGroup, vm.currentGroup.value)

        scope.cancel()
    }

    // ─── Remote member join push ─────────────────────────────────────────────────

    @Test
    fun membersUpdateWhenRemoteMemberJoinPushed() {
        val groupId = "group-1"
        val group = TravelGroup(id = groupId, name = "Viaje")
        val admin = GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.ADMIN)
        val repo = FakeTravelRepository(
            initialGroups = listOf(group),
            initialMembers = mapOf(groupId to listOf(admin)),
        )
        val vm = viewModel(repo = repo)
        val scope = warmUp { vm.members.collect { } }

        assertEquals(1, vm.members.value.size)

        val newMember = GroupMember(id = "m2", groupId = groupId, name = "Ana", userId = "user-2", role = MemberRole.USER)
        repo.simulateRemoteMemberJoin(groupId, newMember)

        assertEquals(2, vm.members.value.size)
        assertTrue(vm.members.value.any { it.name == "Ana" })

        scope.cancel()
    }

    @Test
    fun membersStillUpdateAfterLocalMutationThenRemoteJoin() {
        val groupId = "group-1"
        val group = TravelGroup(id = groupId, name = "Viaje")
        val admin = GroupMember(id = "m1", groupId = groupId, name = "Nico", userId = "user-1", role = MemberRole.ADMIN)
        val repo = FakeTravelRepository(
            initialGroups = listOf(group),
            initialMembers = mapOf(groupId to listOf(admin)),
        )
        val vm = viewModel(repo = repo)
        val scope = warmUp { vm.members.collect { } }

        vm.deleteMember("m1")

        val newMember = GroupMember(id = "m2", groupId = groupId, name = "Ana", userId = "user-2", role = MemberRole.USER)
        repo.simulateRemoteMemberJoin(groupId, newMember)

        assertTrue(vm.members.value.any { it.name == "Ana" })

        scope.cancel()
    }

    // ─── Joiner sees their group after consuming invite ──────────────────────────

    @Test
    fun groupsIncludeNewGroupAfterJoiningViaInvite() {
        val groupId = "group-1"
        val joiningGroup = TravelGroup(id = groupId, name = "Viaje Compartido")
        val repo = FakeTravelRepository(
            consumeInviteResult = Result.success(groupId),
            availableGroupsForJoin = listOf(joiningGroup),
        )
        val vm = viewModel(repo = repo)
        val scope = warmUp { vm.groups.collect { } }

        assertTrue(vm.groups.value.isEmpty())

        runBlocking { repo.consumeInvite("CODE", "user-2", "Ana") }

        assertEquals(1, vm.groups.value.size)
        assertEquals(groupId, vm.groups.value.first().id)

        scope.cancel()
    }

    @Test
    fun currentGroupBecomesNonNullAfterJoiningViaInvite() {
        val groupId = "group-1"
        val joiningGroup = TravelGroup(id = groupId, name = "Viaje Compartido")
        val repo = FakeTravelRepository(
            consumeInviteResult = Result.success(groupId),
            availableGroupsForJoin = listOf(joiningGroup),
        )
        val vm = viewModel(repo = repo)
        val scope = warmUp {
            vm.groups.collect { }
            vm.currentGroup.collect { }
        }

        runBlocking { repo.consumeInvite("CODE", "user-2", "Ana") }

        assertEquals(joiningGroup, vm.currentGroup.value)

        scope.cancel()
    }
}
