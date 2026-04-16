package com.hllous.plantravel.domain.usecase

import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.repository.TravelRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupName: String, adminName: String): Long {
        return repository.createGroup(groupName.trim(), adminName.trim())
    }
}

class UpdateGroupNameUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: Long, name: String) {
        repository.updateGroupName(groupId, name.trim())
    }
}

class AddMemberUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: Long, memberName: String, role: MemberRole = MemberRole.USER): Long {
        return repository.addMember(groupId, memberName.trim(), role)
    }
}

class DeleteMemberUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(memberId: Long) {
        repository.deleteMember(memberId)
    }
}

class DeleteGroupUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: Long) {
        repository.deleteGroup(groupId)
    }
}

class GenerateInviteUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: Long) = repository.generateInvite(groupId)
}

class DeleteInviteUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(code: String) {
        repository.deleteInvite(code)
    }
}

class ConsumeInviteUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(code: String, memberName: String) = repository.consumeInvite(code.trim(), memberName.trim())
}

class AddExpenseItemUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: Long, itemName: String, totalPriceCents: Long, quantity: Int): Long {
        return repository.addExpenseItem(groupId, itemName.trim(), totalPriceCents, quantity)
    }
}

class AssignItemToMemberUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(itemId: Long, memberId: Long, quantity: Int) {
        repository.assignItemToMember(itemId, memberId, quantity)
    }
}

class DeleteExpenseItemUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(itemId: Long) {
        repository.deleteExpenseItem(itemId)
    }
}

class CalculateSettlementUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: Long) = repository.calculateSettlement(groupId)
}

