package com.hllous.plantravel.di

import com.hllous.plantravel.data.repository.TravelRepositoryImpl
import com.hllous.plantravel.domain.repository.TravelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTravelRepository(impl: TravelRepositoryImpl): TravelRepository
}

