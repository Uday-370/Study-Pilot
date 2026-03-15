package com.example.studysmart.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Resource")
data class Resource(
    @PrimaryKey(autoGenerate = true)
    val resourceId: Int? = null,
    val subjectId: Int, // Links this resource to a specific class
    val name: String,   // e.g., "Midterm Syllabus" or "Math Lecture 1"
    val uri: String     // Stores either a web URL (https://...) or a local file URI
)