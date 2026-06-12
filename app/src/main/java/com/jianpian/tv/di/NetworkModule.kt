package com.jianpian.tv.di

import com.jianpian.tv.data.remote.VodjpApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideVodjpApi(): VodjpApi = VodjpApi()
}
