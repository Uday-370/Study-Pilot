package com.example.studysmart.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.Color
import com.example.studysmart.ui.theme.ErrorRed
import com.example.studysmart.ui.theme.SuccessGreen
// Using our new SuccessGreen for Low
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// A modern, sophisticated Orange for Medium priority
val MutedOrange = Color(0xFFE67E22)

enum class Priority(val title: String, val color: Color, val value: Int) {
    LOW(title = "Low", color = SuccessGreen, value = 0),
    MEDIUM(title = "Medium", color = MutedOrange, value = 1),
    HIGH(title = "High", color = ErrorRed, value = 2);

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.value == value } ?: MEDIUM
    }
}

fun Long?.changeMillisToDateString(): String {
    val date: LocalDate = this?.let {
        Instant
            .ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    } ?: LocalDate.now()
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
}

fun Long.toHours(): Float {
    val hours = this.toFloat() / 3600f
    return "%.2f".format(hours).toFloat()
}

sealed class SnackbarEvent {
    data class ShowSnackbar(
        val message: String,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarEvent()

    data object NavigateUp: SnackbarEvent()
}

fun Int.pad(): String {
    return this.toString().padStart(length = 2, padChar = '0')
}