package com.hllous.plantravel.domain.usecase

import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.ConsumeInviteFailure
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupName: String): String {
        return repository.createGroup(groupName.trim())
    }
}

class UpdateGroupNameUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: String, name: String) {
        repository.updateGroupName(groupId, name.trim())
    }
}

class DeleteMemberUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(memberId: String) {
        repository.deleteMember(memberId)
    }
}

class DeleteGroupUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: String) {
        repository.deleteGroup(groupId)
    }
}

class EndTripUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: String) {
        repository.endTrip(groupId)
    }
}

class ReactivateTripUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: String) {
        repository.reactivateTrip(groupId)
    }
}

class LeaveGroupUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: String) {
        repository.leaveGroup(groupId)
    }
}

class GenerateInviteUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: String) = repository.generateInvite(groupId)
}

class DeleteInviteUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(code: String) {
        repository.deleteInvite(code)
    }
}

class ConsumeInviteUseCase @Inject constructor(
    private val repository: TravelRepository,
    private val sessionProvider: SessionProvider
) {
    suspend operator fun invoke(code: String): Result<String> {
        val userId = sessionProvider.userId
            ?: return Result.failure(ConsumeInviteFailure.Unauthenticated)
        val displayName = sessionProvider.displayName.orEmpty()
        return repository.consumeInvite(code.trim(), userId, displayName)
    }
}

class AddExpenseItemUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(expenseGroupId: String, name: String, totalPriceCents: Long, quantity: Int): String {
        return repository.addExpenseItem(expenseGroupId, name.trim(), totalPriceCents, quantity)
    }
}

class AssignItemToMemberUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(itemId: String, memberId: String, quantity: Int): AssignmentOutcome {
        return repository.assignItemToMember(itemId, memberId, quantity)
    }
}

class DeleteExpenseItemUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(itemId: String) {
        repository.deleteExpenseItem(itemId)
    }
}

class CreateExpenseGroupUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(groupId: String, name: String, category: String? = null): String =
        repository.createExpenseGroup(groupId, name.trim(), category)
}

class UpdateExpenseGroupNameUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(expenseGroupId: String, name: String) {
        repository.updateExpenseGroupName(expenseGroupId, name.trim())
    }
}

class DeleteExpenseGroupUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(expenseGroupId: String) {
        repository.deleteExpenseGroup(expenseGroupId)
    }
}

class FinalizeExpenseGroupUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(expenseGroupId: String) {
        repository.finalizeExpenseGroup(expenseGroupId)
    }
}

class SetExpenseGroupPinnedUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(expenseGroupId: String, pinned: Boolean) {
        repository.setExpenseGroupPinned(expenseGroupId, pinned)
    }
}

class GetMpAliasUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(userId: String): String? = repository.getMpAlias(userId)
}

class UpdateMpAliasUseCase @Inject constructor(
    private val repository: TravelRepository
) {
    suspend operator fun invoke(alias: String) = repository.updateMpAlias(alias)
}
