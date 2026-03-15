package com.example.studysmart.data.repository

import com.example.studysmart.data.local.ResourceDao
import com.example.studysmart.domain.model.Resource
import com.example.studysmart.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ResourceRepositoryImpl @Inject constructor(
    private val resourceDao: ResourceDao
) : ResourceRepository {

    override suspend fun insertResource(resource: Resource) {
        resourceDao.insertResource(resource)
    }

    override suspend fun deleteResource(resource: Resource) {
        resourceDao.deleteResource(resource)
    }

    override fun getAllResources(): Flow<List<Resource>> {
        return resourceDao.getAllResources()
    }

    override fun getResourcesForSubject(subjectId: Int): Flow<List<Resource>> {
        return resourceDao.getResourcesForSubject(subjectId)
    }
}