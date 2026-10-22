package com.example.vokasilearning.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/** A single, short acknowledgement after consequential completed actions. */
@Composable
internal fun ActionFeedbackDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = Ink) },
        text = { Text(message, color = Subtle) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Mengerti", color = Navy, fontWeight = FontWeight.Bold)
            }
        }
    )
}
