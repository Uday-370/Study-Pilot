package com.example.studysmart.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// The "Sage & Linen" Calm Aesthetic Palette
private val CalmEarthyColorScheme = lightColorScheme(
    background = Color(0xFFFAF9F6),     // Warm Linen / Off-White (Very calming)
    surface = Color(0xFFFFFFFF),        // Pure white for elevated cards to pop slightly
    surfaceVariant = Color(0xFFEFEFEF), // Soft gray-beige for unselected pills/backgrounds

    primary = Color(0xFF6B8A7A),        // Deep, calming Sage Green
    secondary = Color(0xFFB0B0B0),      // Soft, airy gray for secondary text
    error = Color(0xFFD48A88),          // Muted Terracotta/Rose instead of harsh red

    onBackground = Color(0xFF2D302E),   // Deep Charcoal (Much softer on eyes than pure black)
    onSurface = Color(0xFF2D302E),
    onSurfaceVariant = Color(0xFF6D7470) // Medium earthy gray
)

@Composable
fun StudySmartTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = CalmEarthyColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            // Turn the status bar icons (battery, wifi) DARK so they show up on the light background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}