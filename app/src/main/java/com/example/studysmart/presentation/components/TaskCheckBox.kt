package com.example.studysmart.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TaskCheckBox(
    isComplete: Boolean,
    borderColor: Color,
    onCheckBoxClick: () -> Unit
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isComplete) borderColor.copy(alpha = 0.15f) else Color.Transparent,
        label = "checkboxBgColor"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isComplete) Color.Transparent else borderColor.copy(alpha = 0.5f),
        label = "checkboxBorderColor"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(animatedBgColor)
            .border(
                width = 1.5.dp,
                color = animatedBorderColor,
                shape = CircleShape
            )
            .clickable { onCheckBoxClick() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isComplete) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Rounded.Check,
                contentDescription = "Task Complete",
                tint = borderColor.copy(alpha = 0.8f)
            )
        }
    }
}

