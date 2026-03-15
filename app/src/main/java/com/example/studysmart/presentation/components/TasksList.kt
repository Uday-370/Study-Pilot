package com.example.studysmart.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studysmart.R
import com.example.studysmart.domain.model.Task
import com.example.studysmart.util.Priority
import com.example.studysmart.util.changeMillisToDateString
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun LazyListScope.tasksList(
    sectionTitle: String,
    emptyListText: String,
    emptyStateImageRes: Int = R.drawable.ic_self_improvement1, // 🚀 THE FIX: Dynamic Image Parameter
    tasks: List<Task>,
    onTaskCardClick: (Int?) -> Unit,
    onCheckBoxClick: (Task) -> Unit
) {
    item {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }

    if (tasks.isEmpty()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = emptyStateImageRes),
                    contentDescription = "taskList Image",
                    modifier = Modifier.size(190.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) // Soft watermark opacity
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = emptyListText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        items(tasks, key = { it.taskId ?: it.hashCode() }) { task ->
            TaskCard(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                task = task,
                subjectName = null,
                onCheckBoxClick = { onCheckBoxClick(task) },
                onClick = { onTaskCardClick(task.taskId) }
            )
        }
    }
}

@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    task: Task,
    subjectName: String? = null,
    onCheckBoxClick: () -> Unit,
    onClick: () -> Unit
) {
    val priority = Priority.fromInt(task.priority)

    val isOverdue = remember(task.dueDate, task.isComplete) {
        if (task.dueDate == null || task.isComplete) {
            false
        } else {
            val dueDate = Instant.ofEpochMilli(task.dueDate!!)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val today = LocalDate.now()
            dueDate.isBefore(today)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TaskCheckBox(
                isComplete = task.isComplete,
                borderColor = priority.color,
                onCheckBoxClick = onCheckBoxClick
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (task.isComplete) FontWeight.Normal else FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    ),
                    color = if (task.isComplete) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    textDecoration = if (task.isComplete) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    // 🚀 THE CRITICAL FIX: Smart Subtitle Builder
                    val dateString = task.dueDate?.changeMillisToDateString() ?: ""
                    val hasValidDate = dateString.isNotBlank() && dateString != "No date"

                    val subtitleText = buildString {
                        if (subjectName != null) append(subjectName)
                        if (subjectName != null && hasValidDate) append(" • ")
                        if (hasValidDate) append(dateString)
                    }

                    if (subtitleText.isNotBlank()) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (isOverdue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Overdue",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (!task.isComplete && priority.title.lowercase() != "none") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = priority.color.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, priority.color.copy(alpha = 0.2f)),
                    modifier = Modifier.width(72.dp)
                ) {
                    Text(
                        text = priority.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = priority.color.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}