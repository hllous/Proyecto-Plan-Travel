package com.hllous.plantravel.data.repository

import com.hllous.plantravel.data.local.SeedData
import com.hllous.plantravel.data.local.dao.TravelDao
import com.hllous.plantravel.data.local.entity.ExpenseItemEntity
import com.hllous.plantravel.data.local.entity.GroupEntity
import com.hllous.plantravel.data.local.entity.InviteTokenEntity
import com.hllous.plantravel.data.local.entity.ItemAssignmentEntity
import com.hllous.plantravel.data.local.entity.MemberEntity
import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import com.hllous.plantravel.domain.settlement.ExpenseAssignmentPolicy
import com.hllous.plantravel.domain.settlement.ExpenseSettlementCalculator
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TravelRepositoryImpl @Inject constructor(
    private val dao: TravelDao
) : TravelRepository {

    private var seedLoaded = false
    private val settlementCalculator = ExpenseSettlementCalculator()
    private val assignmentPolicy = ExpenseAssignmentPolicy()

    override fun observeGroups(): Flow<List<TravelGroup>> {
        return dao.observeGroups().map { groups ->
            groups.map { TravelGroup(id = it.id, name = it.name, adminMemberId = it.adminMemberId) }
        }
    }

    override fun observeMembers(groupId: Long): Flow<List<GroupMember>> {
        return dao.observeMembers(groupId).map { members ->
            members.map {
                GroupMember(
                    id = it.id,
                    groupId = it.groupId,
                    name = it.name,
                    role = if (it.role == MemberRole.ADMIN.name) MemberRole.ADMIN else MemberRole.USER
                )
            }
        }
    }

    override fun observeInvites(groupId: Long): Flow<List<InviteToken>> {
        return dao.observeInvites(groupId).map { invites ->
            invites.map {
                InviteToken(
                    code = it.code,
                    groupId = it.groupId,
                    link = it.link,
                    expiresAtMillis = it.expiresAtMillis
                )
            }
        }
    }

    override suspend fun createGroup(groupName: String, adminName: String): Long {
        val groupId = dao.insertGroup(GroupEntity(name = groupName))
        val adminId = dao.insertMember(
            MemberEntity(groupId = groupId, name = adminName, role = MemberRole.ADMIN.name)
        )
        dao.setAdminMember(groupId, adminId)
        return groupId
    }

    override suspend fun updateGroupName(groupId: Long, name: String) {
        dao.updateGroupName(groupId, name)
    }

    override suspend fun addMember(groupId: Long, memberName: String, role: MemberRole): Long {
        return dao.insertMember(
            MemberEntity(groupId = groupId, name = memberName, role = role.name)
        )
    }

    override suspend fun deleteMember(memberId: Long) {
        dao.deleteAssignmentsForMember(memberId)
        dao.deleteMember(memberId)
    }

    override suspend fun deleteGroup(groupId: Long) {
        dao.deleteAssignmentsForGroup(groupId)
        dao.deleteExpenseItemsByGroup(groupId)
        dao.deleteInvitesByGroup(groupId)
        dao.deleteMembersByGroup(groupId)
        dao.deleteGroup(groupId)
    }

    override suspend fun generateInvite(groupId: Long): InviteToken {
        val code = UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.ROOT)
        val now = System.currentTimeMillis()
        val expiresAt = now + (24 * 60 * 60 * 1000)
        val link = "https://plantravel.app/invite/$code"
        dao.insertInvite(
            InviteTokenEntity(
                code = code,
                groupId = groupId,
                link = link,
                createdAtMillis = now,
                expiresAtMillis = expiresAt
            )
        )
        return InviteToken(code = code, groupId = groupId, link = link, expiresAtMillis = expiresAt)
    }

    override suspend fun deleteInvite(code: String) {
        dao.deleteInvite(code)
    }

    override suspend fun consumeInvite(code: String, memberName: String): Result<Long> {
        val invite = dao.getInviteByCode(code)
            ?: return Result.failure(IllegalArgumentException("Codigo de invitacion invalido"))

        if (invite.expiresAtMillis < System.currentTimeMillis()) {
            return Result.failure(IllegalStateException("El codigo expiro"))
        }

        val memberId = dao.insertMember(
            MemberEntity(
                groupId = invite.groupId,
                name = memberName,
                role = MemberRole.USER.name
            )
        )

        return Result.success(memberId)
    }

    override suspend fun getRegions(): List<String> {
        ensureSeedData()
        return dao.getRegions()
    }

    override suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation> {
        ensureSeedData()
        return dao.getRecommendationsByRegion(region)
            .distinctBy { it.destination }
            .map {
            DestinationRecommendation(
                region = it.region,
                destination = it.destination,
                recommendation = it.recommendation
            )
        }
    }

    override fun observeExpenseItems(groupId: Long): Flow<List<ExpenseItem>> {
        return dao.observeExpenseItems(groupId).map { items ->
            items.map {
                ExpenseItem(
                    id = it.id,
                    groupId = it.groupId,
                    name = it.name,
                    totalPriceCents = it.totalPriceCents,
                    quantity = it.quantity
                )
            }
        }
    }

    override fun observeAssignments(groupId: Long): Flow<List<ItemAssignment>> {
        return dao.observeAssignments(groupId).map { assignments ->
            assignments.map {
                ItemAssignment(itemId = it.itemId, memberId = it.memberId, quantity = it.quantity)
            }
        }
    }

    override suspend fun addExpenseItem(groupId: Long, itemName: String, totalPriceCents: Long, quantity: Int): Long {
        return dao.insertExpenseItem(
            ExpenseItemEntity(
                groupId = groupId,
                name = itemName,
                totalPriceCents = max(0L, totalPriceCents),
                quantity = max(1, quantity)
            )
        )
    }

    override suspend fun assignItemToMember(itemId: Long, memberId: Long, quantity: Int): AssignmentOutcome {
        val item = dao.getExpenseItem(itemId)?.let {
            ExpenseItem(
                id = it.id,
                groupId = it.groupId,
                name = it.name,
                totalPriceCents = it.totalPriceCents,
                quantity = it.quantity
            )
        } ?: error("Expense item $itemId not found — this is a programming error")
        val assignments = dao.getAssignmentsForItem(itemId).map {
            ItemAssignment(itemId = it.itemId, memberId = it.memberId, quantity = it.quantity)
        }

        val outcome = assignmentPolicy.validate(item, assignments, memberId, quantity)
        if (outcome is AssignmentOutcome.Accepted) {
            dao.upsertAssignment(
                ItemAssignmentEntity(
                    itemId = itemId,
                    memberId = memberId,
                    quantity = quantity
                )
            )
        }
        return outcome
    }

    override suspend fun deleteExpenseItem(itemId: Long) {
        dao.deleteAssignmentsForItem(itemId)
        dao.deleteExpenseItem(itemId)
    }

    override suspend fun calculateSettlement(groupId: Long): SettlementResult {
        val members = dao.getMembers(groupId).map {
            GroupMember(
                id = it.id,
                groupId = it.groupId,
                name = it.name,
                role = if (it.role == MemberRole.ADMIN.name) MemberRole.ADMIN else MemberRole.USER
            )
        }
        val items = dao.getExpenseItems(groupId).map {
            ExpenseItem(
                id = it.id,
                groupId = it.groupId,
                name = it.name,
                totalPriceCents = it.totalPriceCents,
                quantity = it.quantity
            )
        }
        val assignments = dao.getAssignmentDetails(groupId).map {
            ItemAssignment(itemId = it.itemId, memberId = it.memberId, quantity = it.quantity)
        }

        return settlementCalculator.calculate(
            members = members,
            items = items,
            assignments = assignments
        )
    }

    private suspend fun ensureSeedData() {
        if (seedLoaded) return
        if (dao.countRecommendations() == 0) {
            dao.insertRecommendations(SeedData.argentinaRecommendations)
        }
        seedLoaded = true
    }
}

