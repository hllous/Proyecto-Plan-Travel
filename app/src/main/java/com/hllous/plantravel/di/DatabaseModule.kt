package com.hllous.plantravel.di

import android.content.Context
import androidx.room.Room
import com.hllous.plantravel.data.local.PlanTravelDatabase
import com.hllous.plantravel.data.local.dao.TravelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlanTravelDatabase {
        return Room.databaseBuilder(
            context,
            PlanTravelDatabase::class.java,
            "plan_travel.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    @Singleton
    fun provideTravelDao(database: PlanTravelDatabase): TravelDao = database.travelDao()
}


