package com.example.studysmart.domain.repository

import com.example.studysmart.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface ResourceRepository {
    suspend fun insertResource(resource: Resource)
    suspend fun deleteResource(resource: Resource)
    fun getAllResources(): Flow<List<Resource>>
    fun getResourcesForSubject(subjectId: Int): Flow<List<Resource>>
}