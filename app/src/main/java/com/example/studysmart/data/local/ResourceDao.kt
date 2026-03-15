package com.example.studysmart.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.studysmart.domain.model.Resource
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: Resource)

    @Delete
    suspend fun deleteResource(resource: Resource)

    // Gets all resources across the entire app
    @Query("SELECT * FROM Resource")
    fun getAllResources(): Flow<List<Resource>>

    // Gets resources for only ONE specific subject
    @Query("SELECT * FROM Resource WHERE subjectId = :subjectId")
    fun getResourcesForSubject(subjectId: Int): Flow<List<Resource>>

    // 🚀 THE FIX: This allows the Repository to bulk-delete files when a Subject is deleted!
    @Query("DELETE FROM Resource WHERE subjectId = :subjectId")
    suspend fun deleteResourcesBySubjectId(subjectId: Int)
}