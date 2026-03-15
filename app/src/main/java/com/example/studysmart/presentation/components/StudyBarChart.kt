package com.example.studysmart.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudyBarChart(
    modifier: Modifier = Modifier,
    data: List<Float>, // 7 days of data
    labels: List<String> // 7 day labels (Mon, Tue, etc.)
) {
    // Determine the highest study day to scale the bars properly.
    // If no data, default to 1f to avoid dividing by zero.
    val maxDataValue = data.maxOrNull()?.takeIf { it > 0f } ?: 1f

    // Trigger for the grow animation
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Text(
            text = "This Week's Focus",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, value ->
                // Animate the bar height
                val animatedHeightRatio by animateFloatAsState(
                    targetValue = if (startAnimation) (value / maxDataValue) else 0f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "barHeight"
                )

                val barColor = MaterialTheme.colorScheme.primary

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // The actual bar drawn via Canvas
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(22.dp)
                        ) {
                            // Draw background track (light grey)
                            drawRoundRect(
                                color = barColor.copy(alpha = 0.1f),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(50f, 50f)
                            )

                            // Draw the active animated data bar
                            val barHeight = size.height * animatedHeightRatio
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(0f, size.height - barHeight),
                                size = Size(size.width, barHeight),
                                cornerRadius = CornerRadius(50f, 50f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // The X-Axis Label
                    Text(
                        text = labels.getOrNull(index) ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}