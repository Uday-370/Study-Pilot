package com.example.studysmart.presentation.calendar

import java.time.LocalDate

data class CalendarState(
    val selectedDate: LocalDate = LocalDate.now()
)