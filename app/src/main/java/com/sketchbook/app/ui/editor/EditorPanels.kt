package com.sketchbook.app.ui.editor

import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sketchbook.app.drawing.Adjustments
import com.sketchbook.app.model.BrushType
import com.sketchbook.app.model.PencilGrade
import com.sketchbook.app.model.ShapeKind
import kotlin.math.roundToInt

// ---------------------------------------------------------------- Brush picker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrushPickerSheet(vm: EditorViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("SIZE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("${vm.brushSize.toInt()} pt", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(value = vm.brushSize, onValueChange = { vm.brushSize = it }, valueRange = 1f..60f)

            LazyColumn(Modifier.height(420.dp)) {
                BrushType.Category.entries.forEach { cat ->
                    item {
                        Text(
                            cat.title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                        )
                    }
                    items(BrushType.entries.filter { it.category == cat }) { b ->
                        val selected = vm.brush == b
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    vm.brush = b
                                    vm.toolMode = ToolMode.DRAW
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(b.title, modifier = Modifier.weight(1f))
                            if (selected) Icon(
                                Icons.Filled.Check, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                item {
                    Text(
                        "PENCILS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                items(PencilGrade.entries) { g ->
                    val selected = vm.brush == BrushType.PENCIL && vm.pencilGrade == g
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .clickable {
                                vm.brush = BrushType.PENCIL
                                vm.pencilGrade = g
                                vm.toolMode = ToolMode.DRAW
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(g.title, modifier = Modifier.weight(1f))
                        if (selected) Icon(
                            Icons.Filled.Check, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Shapes picker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapesPickerSheet(vm: EditorViewModel, onDismiss: () -> Unit) {
    var showFillColor by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Shapes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Drag on the canvas to place the shape, then move, resize, rotate or flip it.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ShapeKind.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { s ->
                        val selected = vm.shapeKind == s
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(11.dp)
                                )
                                .clickable { vm.shapeKind = s; vm.toolMode = ToolMode.SHAPE },
                        ) { Text(s.title, fontSize = 12.sp) }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Text("OUTLINE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Width")
                Spacer(Modifier.width(12.dp))
                Slider(
                    value = vm.shapeStrokeWidth, onValueChange = { vm.shapeStrokeWidth = it },
                    valueRange = 1f..40f, modifier = Modifier.weight(1f)
                )
                Text("${vm.shapeStrokeWidth.toInt()} pt")
            }

            Text("FILL", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Fill shape")
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = vm.shapeFillColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(32.dp).clickable(enabled = vm.shapeKind.canFill) {
                        showFillColor = true
                    },
                ) {}
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = vm.shapeFillEnabled && vm.shapeKind.canFill,
                    onCheckedChange = { vm.shapeFillEnabled = it },
                    enabled = vm.shapeKind.canFill,
                )
            }
        }
    }
    if (showFillColor) ColorPickerDialog(
        initial = vm.shapeFillColor,
        onDismiss = { showFillColor = false },
        onPick = { vm.shapeFillColor = it },
    )
}

// ---------------------------------------------------------------- Color picker

private val PALETTE = listOf(
    0xFF111418, 0xFF5B5F66, 0xFF9AA0A6, 0xFFFFFFFF,
    0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047,
    0xFF1E88E5, 0xFF3949AB, 0xFF8E24AA, 0xFFD81B60,
    0xFF6D4C41, 0xFF00897B, 0xFF00ACC1, 0xFFC0CA33,
).map { Color(it) }

@Composable
fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit,
) {
    var current by remember { mutableStateOf(initial) }
    val hsv = remember(initial) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(initial.toArgb(), arr)
        arr
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var alpha by remember { mutableFloatStateOf(initial.alpha) }

    fun rebuild() {
        val argb = android.graphics.Color.HSVToColor(
            (alpha * 255).toInt().coerceIn(0, 255),
            floatArrayOf(hue, sat, value)
        )
        current = Color(argb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PALETTE.chunked(8).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            Surface(
                                shape = CircleShape,
                                color = c,
                                border = BorderStroke(
                                    if (current.toArgb() == c.toArgb()) 3.dp else 1.dp,
                                    if (current.toArgb() == c.toArgb()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.size(30.dp).clickable {
                                    current = c
                                    val arr = FloatArray(3)
                                    android.graphics.Color.colorToHSV(c.toArgb(), arr)
                                    hue = arr[0]; sat = arr[1]; value = arr[2]; alpha = 1f
                                },
                            ) {}
                        }
                    }
                }
                Text("Hue"); Slider(value = hue, onValueChange = { hue = it; rebuild() }, valueRange = 0f..360f)
                Text("Saturation"); Slider(value = sat, onValueChange = { sat = it; rebuild() }, valueRange = 0f..1f)
                Text("Brightness"); Slider(value = value, onValueChange = { value = it; rebuild() }, valueRange = 0f..1f)
                Text("Opacity"); Slider(value = alpha, onValueChange = { alpha = it; rebuild() }, valueRange = 0.05f..1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Preview")
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        shape = CircleShape, color = current,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(36.dp),
                    ) {}
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(current); onDismiss() }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------- Slider dialog

@Composable
fun SliderDialog(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var v by remember { mutableFloatStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("${v.toInt()} pt", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(value = v, onValueChange = { v = it; onChange(it) }, valueRange = range)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

// ---------------------------------------------------------------- Adjustments

private data class AdjustmentSpec(
    val name: String,
    val params: List<Triple<String, ClosedFloatingPointRange<Float>, Float>>, // label, range, default
    val apply: (android.graphics.Bitmap, List<Float>) -> android.graphics.Bitmap,
)

private val ADJUSTMENTS = listOf(
    AdjustmentSpec(
        "Hue / Saturation / Brightness",
        listOf(
            Triple("Hue", -180f..180f, 0f),
            Triple("Saturation", 0f..2f, 1f),
            Triple("Brightness", -0.5f..0.5f, 0f),
        )
    ) { b, p -> Adjustments.hueSaturationBrightness(b, p[0], p[1], p[2]) },
    AdjustmentSpec(
        "Colour Balance",
        listOf(
            Triple("Warmth", 3000f..10000f, 6500f),
            Triple("Tint", -100f..100f, 0f),
        )
    ) { b, p -> Adjustments.colorBalance(b, p[0], p[1]) },
    AdjustmentSpec(
        "Gaussian Blur",
        listOf(Triple("Radius", 0f..40f, 8f))
    ) { b, p -> Adjustments.blur(b, p[0]) },
    AdjustmentSpec(
        "Noise",
        listOf(Triple("Amount", 0f..1f, 0.25f))
    ) { b, p -> Adjustments.noise(b, p[0]) },
    AdjustmentSpec(
        "Sharpen",
        listOf(Triple("Amount", 0f..2f, 0.7f))
    ) { b, p -> Adjustments.sharpen(b, p[0]) },
    AdjustmentSpec(
        "Mosaic",
        listOf(Triple("Cell Size", 4f..30f, 16f))
    ) { b, p -> Adjustments.pixellate(b, p[0].toInt()) },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentsSheet(vm: EditorViewModel, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf<AdjustmentSpec?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val spec = selected
        if (spec == null) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("Adjustments", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Applied to the active layer.", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ADJUSTMENTS.forEach { a ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = a }
                            .padding(vertical = 12.dp),
                    ) { Text(a.name) }
                    HorizontalDivider()
                }
            }
        } else {
            val values = remember(spec) {
                spec.params.map { mutableFloatStateOf(it.third) }
            }
            Column(
                Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(spec.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                spec.params.forEachIndexed { i, (label, range, _) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(label)
                        Spacer(Modifier.weight(1f))
                        val v = values[i].floatValue
                        Text(
                            if (range.endInclusive - range.start <= 4f) String.format("%.2f", v)
                            else v.toInt().toString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = values[i].floatValue,
                        onValueChange = { values[i].floatValue = it },
                        valueRange = range,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { selected = null }) { Text("Back") }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        val params = values.map { it.floatValue }
                        vm.applyAdjustment(spec.name) { bmp -> spec.apply(bmp, params) }
                        onDismiss()
                    }) { Text("Apply") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Layers panel

@Composable
fun LayersPanel(vm: EditorViewModel, onClose: () -> Unit) {
    var renaming by remember { mutableStateOf(-1) }
    var renameText by remember { mutableStateOf("") }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(280.dp).fillMaxHeight(),
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Layers", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.duplicateActiveLayer() }) {
                    Icon(Icons.Filled.ContentCopy, "Duplicate layer", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { vm.addLayer() }) {
                    Icon(Icons.Filled.Add, "Add layer")
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "Close")
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            val layers = vm.page.layers
            // Top of the stack first (like iOS/Procreate).
            LazyColumn {
                itemsIndexed(layers.asReversed()) { revIndex, layer ->
                    val index = layers.size - 1 - revIndex
                    val active = index == vm.page.activeLayerIndex
                    var menu by remember(layer.id) { mutableStateOf(false) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .clickable { vm.setActiveLayer(index) }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { vm.setLayerVisible(index, !layer.isVisible) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                if (layer.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                "Visibility", modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Thumbnail
                        val bmp = remember(layer, vm.bitmapVersion) {
                            runCatching {
                                val full = vm.layerBitmap(layer)
                                android.graphics.Bitmap.createScaledBitmap(full, 66, 50, true)
                            }.getOrNull()
                        }
                        Box(
                            Modifier
                                .size(width = 44.dp, height = 34.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        ) {
                            bmp?.let {
                                Image(
                                    it.asImageBitmap(), null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.padding(1.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(layer.name, fontSize = 13.sp, maxLines = 1)
                            if (layer.opacity < 1f) Text(
                                "${(layer.opacity * 100).roundToInt()}%",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (layer.isLocked) Icon(
                            Icons.Filled.Lock, null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box {
                            IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) {
                                Text("⋯")
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Rename…") }, onClick = {
                                    menu = false; renaming = index; renameText = layer.name
                                })
                                DropdownMenuItem(
                                    text = { Text(if (layer.isLocked) "Unlock" else "Lock") },
                                    leadingIcon = {
                                        Icon(if (layer.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock, null)
                                    },
                                    onClick = { menu = false; vm.setLayerLocked(index, !layer.isLocked) })
                                listOf(1f, 0.75f, 0.5f, 0.25f).forEach { op ->
                                    DropdownMenuItem(
                                        text = { Text("Opacity ${(op * 100).toInt()}%") },
                                        onClick = { menu = false; vm.setLayerOpacity(index, op) })
                                }
                                DropdownMenuItem(
                                    text = { Text("Move Up") },
                                    leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) },
                                    onClick = { menu = false; vm.moveLayer(index, (index + 1).coerceAtMost(layers.size - 1)) })
                                DropdownMenuItem(
                                    text = { Text("Move Down") },
                                    leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) },
                                    onClick = { menu = false; vm.moveLayer(index, (index - 1).coerceAtLeast(0)) })
                                DropdownMenuItem(
                                    text = { Text("Delete Layer", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menu = false; vm.deleteLayer(index) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (renaming >= 0) {
        AlertDialog(
            onDismissRequest = { renaming = -1 },
            title = { Text("Rename Layer") },
            text = {
                OutlinedTextField(renameText, { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameLayer(renaming, renameText); renaming = -1
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renaming = -1 }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------- Pages sidebar

@Composable
fun PagesSidebar(vm: EditorViewModel, onCollapse: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(168.dp).fillMaxHeight(),
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pages", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.addPage(after = true) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Add, "Add page", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onCollapse, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Close, "Collapse", modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(vm.document.pages) { index, page ->
                    val current = index == vm.document.currentPageIndex
                    val aspect = vm.document.canvasWidth / vm.document.canvasHeight
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((140 / aspect).coerceIn(60f, 190f).dp)
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .border(
                                    if (current) 3.dp else 1.dp,
                                    if (current) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { vm.goToPage(index) },
                        ) {
                            // Lightweight preview: render at small scale (only for non-current pages occasionally)
                            if (vm.document.pages.size > 1) {
                                IconButton(
                                    onClick = { vm.goToPage(index); vm.deleteCurrentPage() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                                ) {
                                    Icon(
                                        Icons.Filled.Delete, "Delete page",
                                        tint = Color.White, modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Text("${index + 1}", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Shape editing overlay

@Composable
fun ShapeEditingOverlay(
    vm: EditorViewModel,
    pending: PendingShape,
    totalScale: Float,
    canvasOrigin: Offset,
) {
    val density = LocalDensity.current

    // Screen-space rect of the pending shape.
    val left = canvasOrigin.x + pending.rect.left * totalScale
    val top = canvasOrigin.y + pending.rect.top * totalScale
    val width = pending.rect.width() * totalScale
    val height = pending.rect.height() * totalScale

    // Selection box (move by drag)
    Box(
        Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(
                with(density) { width.toDp() },
                with(density) { height.toDp() }
            )
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            .pointerInput(pending.kind) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val d = drag / totalScale
                    val r = RectF(vm.pendingShape?.rect ?: return@detectDragGestures)
                    r.offset(d.x, d.y)
                    vm.pendingShape = vm.pendingShape?.copy(rect = r)
                }
            }
    )

    // Resize handle (bottom-right corner)
    Box(
        Modifier
            .offset {
                IntOffset(
                    (left + width - 14).roundToInt(),
                    (top + height - 14).roundToInt()
                )
            }
            .size(28.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .pointerInput(pending.kind) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val d = drag / totalScale
                    val r = RectF(vm.pendingShape?.rect ?: return@detectDragGestures)
                    r.right = (r.right + d.x).coerceAtLeast(r.left + 8)
                    r.bottom = (r.bottom + d.y).coerceAtLeast(r.top + 8)
                    vm.pendingShape = vm.pendingShape?.copy(rect = r)
                }
            }
    )

    // Action bar
    Surface(
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.offset {
            IntOffset(left.roundToInt(), (top - 56).coerceAtLeast(4f).roundToInt())
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                vm.pendingShape = vm.pendingShape?.copy(flipH = !(vm.pendingShape?.flipH ?: false))
            }) { Icon(Icons.Filled.Flip, "Flip horizontal") }
            IconButton(onClick = {
                vm.pendingShape = vm.pendingShape?.copy(flipV = !(vm.pendingShape?.flipV ?: false))
            }) {
                Icon(Icons.Filled.Flip, "Flip vertical",
                    modifier = Modifier.graphicsLayer(rotationZ = 90f))
            }
            // Rotation nudge buttons (15° steps)
            TextButton(onClick = {
                vm.pendingShape = vm.pendingShape?.copy(rotation = (vm.pendingShape?.rotation ?: 0f) - 15f)
            }) { Text("⟲") }
            TextButton(onClick = {
                vm.pendingShape = vm.pendingShape?.copy(rotation = (vm.pendingShape?.rotation ?: 0f) + 15f)
            }) { Text("⟳") }
            IconButton(onClick = { vm.cancelPendingShape() }) {
                Icon(Icons.Filled.Close, "Cancel", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = { vm.commitPendingShape() }) {
                Icon(Icons.Filled.Check, "Commit", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

