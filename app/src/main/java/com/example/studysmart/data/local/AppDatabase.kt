package com.example.studysmart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // Add this import
import com.example.studysmart.domain.model.Resource
import com.example.studysmart.domain.model.Session
import com.example.studysmart.domain.model.Subject
import com.example.studysmart.domain.model.Task

@Database(
    entities = [Subject::class, Session::class, Task::class, Resource::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(ColorListConverter::class) // <-- ADD THIS LINE HERE!
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun taskDao(): TaskDao
    abstract fun resourceDao(): ResourceDao
}