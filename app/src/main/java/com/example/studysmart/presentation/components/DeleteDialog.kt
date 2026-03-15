package com.example.studysmart.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DeleteDialog(
    isOpen: Boolean,
    title: String,
    bodyText: String,
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit
) {
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            shape = MaterialTheme.shapes.large, // Matches the new AddSubject dialog
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF94A3B8) // Premium Slate Gray (Looks intentional, not default)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmButtonClick) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error // Makes the destructive action red
                    )
                }
            }
        )
    }
}

@Preview
@Composable
private fun DeleteDialogPreview() {
    MaterialTheme {
        DeleteDialog(
            isOpen = true,
            title = "Delete Session?",
            bodyText = "Are you sure you want to delete this session? Your studied hours will be reduced by this session time. This action cannot be undone.",
            onDismissRequest = {},
            onConfirmButtonClick = {}
        )
    }
}