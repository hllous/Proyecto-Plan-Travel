package com.hllous.plantravel.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hllous.plantravel.data.local.entity.AssignmentDetailEntity
import com.hllous.plantravel.data.local.entity.DestinationRecommendationEntity
import com.hllous.plantravel.data.local.entity.ExpenseItemEntity
import com.hllous.plantravel.data.local.entity.GroupEntity
import com.hllous.plantravel.data.local.entity.InviteTokenEntity
import com.hllous.plantravel.data.local.entity.ItemAssignmentEntity
import com.hllous.plantravel.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {
    @Query("SELECT * FROM `groups` ORDER BY id DESC")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Insert
    suspend fun insertGroup(group: GroupEntity): Long

    @Query("UPDATE `groups` SET adminMemberId = :adminMemberId WHERE id = :groupId")
    suspend fun setAdminMember(groupId: Long, adminMemberId: Long)

    @Insert
    suspend fun insertMember(member: MemberEntity): Long

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY id ASC")
    fun observeMembers(groupId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE groupId = :groupId AND userId = :userId LIMIT 1")
    suspend fun getMemberByUserId(groupId: Long, userId: String): MemberEntity?

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY id ASC")
    suspend fun getMembers(groupId: Long): List<MemberEntity>

    @Query("SELECT * FROM `groups` WHERE id = :groupId LIMIT 1")
    suspend fun getGroup(groupId: Long): GroupEntity?

    @Query("UPDATE `groups` SET name = :name WHERE id = :groupId")
    suspend fun updateGroupName(groupId: Long, name: String)

    @Query("DELETE FROM members WHERE id = :memberId")
    suspend fun deleteMember(memberId: Long)

    @Query("DELETE FROM members WHERE groupId = :groupId")
    suspend fun deleteMembersByGroup(groupId: Long)

    @Query("DELETE FROM `groups` WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvite(inviteToken: InviteTokenEntity)

    @Query("SELECT * FROM invite_tokens WHERE groupId = :groupId ORDER BY createdAtMillis DESC")
    fun observeInvites(groupId: Long): Flow<List<InviteTokenEntity>>

    @Query("SELECT * FROM invite_tokens WHERE code = :code LIMIT 1")
    suspend fun getInviteByCode(code: String): InviteTokenEntity?

    @Query("DELETE FROM invite_tokens WHERE code = :code")
    suspend fun deleteInvite(code: String)

    @Query("DELETE FROM invite_tokens WHERE groupId = :groupId")
    suspend fun deleteInvitesByGroup(groupId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecommendations(items: List<DestinationRecommendationEntity>)

    @Query("SELECT DISTINCT region FROM destination_recommendations ORDER BY region ASC")
    suspend fun getRegions(): List<String>

    @Query("SELECT * FROM destination_recommendations WHERE region = :region ORDER BY destination ASC")
    suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendationEntity>

    @Query("SELECT COUNT(*) FROM destination_recommendations")
    suspend fun countRecommendations(): Int

    @Insert
    suspend fun insertExpenseItem(item: ExpenseItemEntity): Long

    @Query("SELECT * FROM expense_items WHERE groupId = :groupId ORDER BY id DESC")
    fun observeExpenseItems(groupId: Long): Flow<List<ExpenseItemEntity>>

    @Query("SELECT * FROM expense_items WHERE groupId = :groupId")
    suspend fun getExpenseItems(groupId: Long): List<ExpenseItemEntity>

    @Query("SELECT * FROM expense_items WHERE id = :itemId LIMIT 1")
    suspend fun getExpenseItem(itemId: Long): ExpenseItemEntity?

    @Query("DELETE FROM expense_items WHERE groupId = :groupId")
    suspend fun deleteExpenseItemsByGroup(groupId: Long)

    @Query("DELETE FROM item_assignments WHERE itemId = :itemId")
    suspend fun deleteAssignmentsForItem(itemId: Long)

    @Query("DELETE FROM item_assignments WHERE memberId = :memberId")
    suspend fun deleteAssignmentsForMember(memberId: Long)

    @Query("DELETE FROM item_assignments WHERE itemId IN (SELECT id FROM expense_items WHERE groupId = :groupId)")
    suspend fun deleteAssignmentsForGroup(groupId: Long)

    @Query("DELETE FROM expense_items WHERE id = :itemId")
    suspend fun deleteExpenseItem(itemId: Long)

    @Upsert
    suspend fun upsertAssignment(assignment: ItemAssignmentEntity)

    @Query("SELECT * FROM item_assignments WHERE itemId = :itemId")
    suspend fun getAssignmentsForItem(itemId: Long): List<ItemAssignmentEntity>

    @Query(
        """
        SELECT ia.itemId, ia.memberId, ia.quantity, ei.totalPriceCents, ei.quantity AS itemQuantity
        FROM item_assignments ia
        INNER JOIN expense_items ei ON ia.itemId = ei.id
        WHERE ei.groupId = :groupId
        """
    )
    suspend fun getAssignmentDetails(groupId: Long): List<AssignmentDetailEntity>

    @Query(
        """
        SELECT ia.*
        FROM item_assignments ia
        INNER JOIN expense_items ei ON ia.itemId = ei.id
        WHERE ei.groupId = :groupId
        ORDER BY ia.itemId ASC
        """
    )
    fun observeAssignments(groupId: Long): Flow<List<ItemAssignmentEntity>>
}

