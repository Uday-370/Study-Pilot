package com.example.studysmart.presentation.calendar

import java.time.LocalDate

sealed class CalendarEvent {
    data class OnDateSelected(val date: LocalDate) : CalendarEvent()
    data class OnTaskIsCompleteChange(val task: com.example.studysmart.domain.model.Task) : CalendarEvent()
}