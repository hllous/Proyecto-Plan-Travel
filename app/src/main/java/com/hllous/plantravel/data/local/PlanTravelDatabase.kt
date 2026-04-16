package com.hllous.plantravel.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hllous.plantravel.data.local.dao.TravelDao
import com.hllous.plantravel.data.local.entity.DestinationRecommendationEntity
import com.hllous.plantravel.data.local.entity.ExpenseItemEntity
import com.hllous.plantravel.data.local.entity.GroupEntity
import com.hllous.plantravel.data.local.entity.InviteTokenEntity
import com.hllous.plantravel.data.local.entity.ItemAssignmentEntity
import com.hllous.plantravel.data.local.entity.MemberEntity

@Database(
    entities = [
        GroupEntity::class,
        MemberEntity::class,
        InviteTokenEntity::class,
        DestinationRecommendationEntity::class,
        ExpenseItemEntity::class,
        ItemAssignmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PlanTravelDatabase : RoomDatabase() {
    abstract fun travelDao(): TravelDao
}

