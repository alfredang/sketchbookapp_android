package com.sketchbook.app.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sketchbook.app.BuildConfig

private const val DEVELOPER_URL = "https://www.tertiaryinfotech.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val uri = LocalUriHandler.current
    Scaffold(topBar = { TopAppBar(title = { Text("About") }) }) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Sketchbook Studio", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "A natural drawing studio for your tablet. Sketch with pressure-sensitive " +
                            "brushes and a stylus, work in layers, use symmetry and templates, fill " +
                            "with color, and apply adjustments and filter effects. Your sketchbooks " +
                            "are saved on your device with multi-page support.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
            Text(
                "DEVELOPER", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tertiary Infotech Academy Pte. Ltd.", fontWeight = FontWeight.SemiBold)
                    Row(
                        Modifier.fillMaxWidth().clickable { uri.openUri(DEVELOPER_URL) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Language, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("tertiaryinfotech.com", textDecoration = TextDecoration.Underline)
                    }
                }
            }
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Version", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
