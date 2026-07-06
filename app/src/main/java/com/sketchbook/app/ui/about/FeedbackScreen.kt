package com.sketchbook.app.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val FEEDBACK_WHATSAPP = "6588666375"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen() {
    val uri = LocalUriHandler.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Feedback") }) }) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("We'd love your feedback", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(
                "Tell us what brushes, templates or effects you'd love to see next.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            OutlinedTextField(
                title, { title = it }, label = { Text("Title") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                message, { message = it }, label = { Text("Message") },
                minLines = 5, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val body = buildString {
                        if (title.isNotBlank()) append("*").append(title.trim()).append("*\n")
                        append(message.trim())
                    }
                    val text = java.net.URLEncoder.encode(body, "UTF-8")
                    uri.openUri("https://wa.me/$FEEDBACK_WHATSAPP?text=$text")
                },
                enabled = title.isNotBlank() || message.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Send via WhatsApp")
            }
        }
    }
}
