package com.example.studysmart.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.studysmart.data.local.AppDatabase
import com.example.studysmart.data.local.ResourceDao
import com.example.studysmart.data.local.SessionDao
import com.example.studysmart.data.local.SubjectDao
import com.example.studysmart.data.local.TaskDao
import com.example.studysmart.data.repository.ResourceRepositoryImpl
import com.example.studysmart.domain.repository.ResourceRepository
import com.example.studysmart.util.StreakManager
import com.example.studysmart.util.UserPreferences
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
    fun provideDatabase(
        application: Application
    ): AppDatabase {
        return Room
            .databaseBuilder(
                application,
                AppDatabase::class.java,
                "studysmart.db"
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideSubjectDao(database: AppDatabase): SubjectDao {
        return database.subjectDao()
    }

    @Provides
    @Singleton
    fun provideTaskDaoDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun provideStreakManager(@ApplicationContext context: Context): StreakManager {
        return StreakManager(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideResourceDao(database: AppDatabase): ResourceDao {
        return database.resourceDao()
    }

    @Provides
    @Singleton
    fun provideResourceRepository(resourceDao: ResourceDao): ResourceRepository {
        return ResourceRepositoryImpl(resourceDao)
    }
}