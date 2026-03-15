package com.example.studysmart.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.studysmart.presentation.destinations.DashboardScreenRouteDestination
import com.example.studysmart.presentation.destinations.CalendarScreenRouteDestination
// Note: We will create these destinations in the next steps
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class BottomBarItem(
    val direction: DirectionDestinationSpec,
    val icon: ImageVector,
    val label: String
) {
    Dashboard(
        direction = DashboardScreenRouteDestination,
        icon = Icons.Default.Home,
        label = "Home"
    ),
    Calendar(
        direction = CalendarScreenRouteDestination,
        icon = Icons.Default.DateRange,
        label = "Agenda"
    ),
    Resources(
        direction = com.example.studysmart.presentation.destinations.ResourcesScreenRouteDestination, // Update this line!
        icon = Icons.Default.LibraryBooks,
        label = "Resources"
    ),
    Account(
        direction = com.example.studysmart.presentation.destinations.AccountScreenRouteDestination, // Placeholder
        icon = Icons.Default.Person,
        label = "Account"
    )
}