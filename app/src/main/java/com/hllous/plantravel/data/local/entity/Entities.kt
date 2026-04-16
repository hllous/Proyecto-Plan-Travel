package com.hllous.plantravel.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val adminMemberId: Long? = null
)

@Entity(tableName = "members", indices = [Index("groupId")])
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val role: String
)

@Entity(tableName = "invite_tokens", indices = [Index("groupId")])
data class InviteTokenEntity(
    @PrimaryKey val code: String,
    val groupId: Long,
    val link: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long
)

@Entity(tableName = "destination_recommendations", indices = [Index("region")])
data class DestinationRecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val region: String,
    val destination: String,
    val recommendation: String
)

@Entity(tableName = "expense_items", indices = [Index("groupId")])
data class ExpenseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val totalPriceCents: Long,
    val quantity: Int
)

@Entity(
    tableName = "item_assignments",
    primaryKeys = ["itemId", "memberId"],
    indices = [Index("memberId")]
)
data class ItemAssignmentEntity(
    val itemId: Long,
    val memberId: Long,
    val quantity: Int
)

data class AssignmentDetailEntity(
    val itemId: Long,
    val memberId: Long,
    val quantity: Int,
    val totalPriceCents: Long,
    val itemQuantity: Int
)

