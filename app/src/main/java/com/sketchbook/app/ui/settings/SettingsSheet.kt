package com.sketchbook.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sketchbook.app.data.AppSettings
import com.sketchbook.app.model.BrushType
import com.sketchbook.app.model.PencilGrade
import com.sketchbook.app.model.TemplateKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onThemeChanged: (String) -> Unit,
) {
    var theme by remember { mutableStateOf(settings.theme) }
    var fingerDrawing by remember { mutableStateOf(settings.fingerDrawing) }
    var haptics by remember { mutableStateOf(settings.haptics) }
    var brush by remember { mutableStateOf(settings.defaultBrush) }
    var grade by remember { mutableStateOf(settings.defaultPencilGrade) }
    var eraseSize by remember { mutableFloatStateOf(settings.defaultEraseSize) }
    var template by remember { mutableStateOf(settings.defaultTemplate) }
    var landscape by remember { mutableStateOf(settings.defaultLandscape) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Done") }
            }

            Section("APPEARANCE")
            PickerRow("Theme", theme.replaceFirstChar { it.uppercase() },
                listOf("Light", "Dark", "System")) {
                theme = it.lowercase()
                settings.theme = theme
                onThemeChanged(theme)
            }

            Section("DRAWING")
            SwitchRow("Finger Drawing", fingerDrawing) {
                fingerDrawing = it; settings.fingerDrawing = it
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Palm Rejection")
                Spacer(Modifier.weight(1f))
                Text("Always On", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SwitchRow("Haptic Feedback", haptics) { haptics = it; settings.haptics = it }

            Section("TOOL DEFAULTS")
            PickerRow("Brush", brush.title, BrushType.entries.map { it.title }) { sel ->
                BrushType.entries.firstOrNull { it.title == sel }?.let {
                    brush = it; settings.defaultBrush = it
                }
            }
            PickerRow("Pencil Grade", grade.title, PencilGrade.entries.map { it.title }) { sel ->
                PencilGrade.entries.firstOrNull { it.title == sel }?.let {
                    grade = it; settings.defaultPencilGrade = it
                }
            }
            Column {
                Row(Modifier.fillMaxWidth()) {
                    Text("Eraser Size")
                    Spacer(Modifier.weight(1f))
                    Text("${eraseSize.toInt()} px", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value = eraseSize,
                    onValueChange = { eraseSize = it },
                    onValueChangeFinished = { settings.defaultEraseSize = eraseSize },
                    valueRange = 6f..80f,
                )
            }

            Section("NEW SKETCH DEFAULTS")
            PickerRow("Template", template.title, TemplateKind.entries.map { it.title }) { sel ->
                TemplateKind.entries.firstOrNull { it.title == sel }?.let {
                    template = it; settings.defaultTemplate = it
                }
            }
            SwitchRow("Landscape", landscape) { landscape = it; settings.defaultLandscape = it }

            Section("STORAGE")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("On this device")
                Spacer(Modifier.weight(1f))
                Text("Local", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PickerRow(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { expanded = true }) { Text(value) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = {
                    expanded = false; onSelect(opt)
                })
            }
        }
    }
}
