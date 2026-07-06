package com.sketchbook.app.ui.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sketchbook.app.data.AppSettings
import com.sketchbook.app.model.CanvasPreset
import com.sketchbook.app.model.PaperColor
import com.sketchbook.app.model.SketchDocument
import com.sketchbook.app.model.TemplateKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSketchSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onCreate: (SketchDocument) -> Unit,
) {
    var title by remember { mutableStateOf("Untitled") }
    var landscape by remember { mutableStateOf(settings.defaultLandscape) }
    var preset by remember { mutableStateOf(CanvasPreset.STANDARD) }
    var paper by remember { mutableStateOf(PaperColor.WHITE) }
    var template by remember { mutableStateOf(settings.defaultTemplate) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New Sketch", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = {
                    val t = title.ifBlank { "Untitled" }
                    onCreate(
                        SketchDocument(
                            title = t,
                            template = template,
                            canvasWidth = preset.width(landscape).toFloat(),
                            canvasHeight = preset.height(landscape).toFloat(),
                            backgroundHex = paper.hex,
                        )
                    )
                }) { Text("Create", fontWeight = FontWeight.Bold) }
            }

            OutlinedTextField(
                title, { title = it }, label = { Text("Title") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Landscape")
                Spacer(Modifier.weight(1f))
                Switch(checked = landscape, onCheckedChange = { landscape = it })
            }

            SectionLabel("CANVAS SIZE")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CanvasPreset.entries.forEach { p ->
                    val selected = p == preset
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { preset = p }
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(11.dp)
                            )
                            .padding(vertical = 10.dp),
                    ) {
                        val w = p.width(landscape).toFloat()
                        val h = p.height(landscape).toFloat()
                        Box(
                            Modifier
                                .height(36.dp)
                                .aspectRatio(w / h)
                                .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(p.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(p.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            SectionLabel("PAPER")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PaperColor.entries.forEach { pc ->
                    val selected = pc == paper
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(android.graphics.Color.parseColor(pc.hex)),
                            border = BorderStroke(
                                if (selected) 3.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.size(44.dp).clickable { paper = pc },
                        ) {}
                        Text(pc.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            SectionLabel("TEMPLATE")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TemplateKind.entries.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { tk ->
                            val selected = tk == template
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { template = tk }
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent,
                                        RoundedCornerShape(11.dp)
                                    )
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(11.dp)
                                    ),
                            ) {
                                Text(tk.title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
