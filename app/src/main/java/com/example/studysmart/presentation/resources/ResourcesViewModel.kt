package com.example.studysmart.presentation.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studysmart.domain.model.Resource
import com.example.studysmart.domain.repository.ResourceRepository
import com.example.studysmart.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository,
    subjectRepository: SubjectRepository
) : ViewModel() {

    // Only holds the final data, not the temporary typing state
    val resources = resourceRepository.getAllResources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects = subjectRepository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveResource(resource: Resource) {
        viewModelScope.launch {
            resourceRepository.insertResource(resource)
        }
    }

    fun deleteResource(resource: Resource) {
        viewModelScope.launch {
            resourceRepository.deleteResource(resource)
        }
    }
}