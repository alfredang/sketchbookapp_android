package com.sketchbook.app.ui.editor

import android.graphics.Paint
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sketchbook.app.data.AppSettings
import com.sketchbook.app.data.DocumentStore
import com.sketchbook.app.drawing.BrushRenderer
import com.sketchbook.app.drawing.TemplateRenderer
import com.sketchbook.app.model.SketchDocument
import com.sketchbook.app.model.SymmetryMode
import com.sketchbook.app.model.TemplateKind
import com.sketchbook.app.ui.theme.BrandSecondary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun EditorScreen(
    initialDocument: SketchDocument,
    store: DocumentStore,
    settings: AppSettings,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vm = remember { EditorViewModel(initialDocument, store, settings, scope) }
    DisposableEffect(Unit) { onDispose { vm.dispose() } }

    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    // Zoom: 1 = fit; user zoom 1..5 on top of the fit scale.
    var zoom by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val fitScale = remember(viewSize, vm.document.canvasWidth, vm.document.canvasHeight) {
        if (viewSize == IntSize.Zero) 1f
        else min(
            (viewSize.width - 32f) / vm.document.canvasWidth,
            (viewSize.height - 32f) / vm.document.canvasHeight,
        ).coerceAtLeast(0.01f)
    }
    val totalScale = fitScale * zoom

    fun canvasOrigin(): Offset {
        // Canvas top-left in view coordinates (centered at fit, then panned).
        val cw = vm.document.canvasWidth * totalScale
        val ch = vm.document.canvasHeight * totalScale
        return Offset(
            (viewSize.width - cw) / 2f + offset.x,
            (viewSize.height - ch) / 2f + offset.y,
        )
    }

    fun toCanvas(p: Offset): Offset {
        val o = canvasOrigin()
        return Offset((p.x - o.x) / totalScale, (p.y - o.y) / totalScale)
    }

    val closeAction: () -> Unit = { vm.saveAndClose(onClose) }
    BackHandler { closeAction() }

    var showBrushPicker by remember { mutableStateOf(false) }
    var showShapesPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showSizePopover by remember { mutableStateOf(false) }
    var showEraserPopover by remember { mutableStateOf(false) }
    var showAdjustments by remember { mutableStateOf(false) }
    var showLayers by remember { mutableStateOf(false) }
    var showPages by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        EditorToolbar(
            vm = vm,
            onBack = closeAction,
            onOpenBrushPicker = { showBrushPicker = true },
            onOpenShapesPicker = { showShapesPicker = true },
            onOpenColorPicker = { showColorPicker = true },
            onOpenSizePopover = { showSizePopover = true },
            onOpenEraserPopover = { showEraserPopover = true },
            onOpenAdjustments = { showAdjustments = true },
            onToggleLayers = { showLayers = !showLayers },
            onFitCanvas = { zoom = 1f; offset = Offset.Zero },
        )
        HorizontalDivider()

        Row(Modifier.fillMaxSize().weight(1f)) {
            if (showPages) {
                PagesSidebar(vm = vm, onCollapse = { showPages = false })
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .onSizeChanged { viewSize = it }
            ) {
                DrawingCanvas(
                    vm = vm,
                    totalScale = totalScale,
                    canvasOriginProvider = ::canvasOrigin,
                    toCanvas = ::toCanvas,
                    onZoomPan = { zoomChange, panChange, centroid ->
                        val oldScale = fitScale * zoom
                        val newZoom = (zoom * zoomChange).coerceIn(1f, 5f)
                        val newScale = fitScale * newZoom
                        // Keep the centroid stable while zooming.
                        val o = canvasOrigin()
                        val canvasPt = Offset((centroid.x - o.x) / oldScale, (centroid.y - o.y) / oldScale)
                        zoom = newZoom
                        val cw = vm.document.canvasWidth * newScale
                        val ch = vm.document.canvasHeight * newScale
                        val baseX = (viewSize.width - cw) / 2f
                        val baseY = (viewSize.height - ch) / 2f
                        offset = Offset(
                            centroid.x - canvasPt.x * newScale - baseX + panChange.x,
                            centroid.y - canvasPt.y * newScale - baseY + panChange.y,
                        )
                        if (newZoom <= 1.001f) offset = Offset.Zero
                    },
                )

                vm.pendingShape?.let { ps ->
                    ShapeEditingOverlay(
                        vm = vm,
                        pending = ps,
                        totalScale = totalScale,
                        canvasOrigin = canvasOrigin(),
                    )
                }

                // Pages toggle affordance
                if (!showPages) {
                    Surface(
                        onClick = { showPages = true },
                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.CenterStart).width(24.dp).height(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("›") }
                    }
                }

                PageControl(vm = vm, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp))

                if (vm.isBusy) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center).size(44.dp))
                }
            }

            if (showLayers) {
                LayersPanel(vm = vm, onClose = { showLayers = false })
            }
        }
    }

    if (showBrushPicker) BrushPickerSheet(vm) { showBrushPicker = false }
    if (showShapesPicker) ShapesPickerSheet(vm) { showShapesPicker = false }
    if (showColorPicker) ColorPickerDialog(
        initial = vm.color,
        onDismiss = { showColorPicker = false },
        onPick = { vm.color = it },
    )
    if (showSizePopover) SliderDialog(
        title = "Brush Size",
        value = vm.brushSize, range = 1f..60f,
        onChange = { vm.brushSize = it },
        onDismiss = { showSizePopover = false },
    )
    if (showEraserPopover) SliderDialog(
        title = "Eraser Size",
        value = vm.eraseWidth, range = 4f..120f,
        onChange = { vm.eraseWidth = it },
        onDismiss = { showEraserPopover = false },
    )
    if (showAdjustments) AdjustmentsSheet(vm) { showAdjustments = false }
}

// ---------------------------------------------------------------- Canvas

@Composable
private fun DrawingCanvas(
    vm: EditorViewModel,
    totalScale: Float,
    canvasOriginProvider: () -> Offset,
    toCanvas: (Offset) -> Offset,
    onZoomPan: (Float, Offset, Offset) -> Unit,
) {
    val bitmapVersion = vm.bitmapVersion // read to trigger redraws on stroke commits

    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(vm.toolMode, vm.fingerDrawing) {
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    val isStylus = first.type == PointerType.Stylus
                    val canDraw = isStylus || vm.fingerDrawing
                    var strokeStarted = false
                    var strokeCancelled = false
                    var transforming = false
                    var transformed = false
                    var maxPointers = 1
                    val startTime = first.uptimeMillis
                    val startPos = first.position
                    var moved = false

                    val drawingTool = vm.toolMode == ToolMode.DRAW || vm.toolMode == ToolMode.ERASE
                    if (canDraw && drawingTool && vm.pendingShape == null) {
                        val c = toCanvas(first.position)
                        vm.startLiveStroke(c.x, c.y, first.pressure)
                        strokeStarted = true
                    }

                    var shapeStart: Offset? = null
                    if (canDraw && vm.toolMode == ToolMode.SHAPE && vm.pendingShape == null) {
                        shapeStart = toCanvas(first.position)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        maxPointers = max(maxPointers, pressed.size)

                        if (pressed.size >= 2) {
                            if (strokeStarted && !strokeCancelled) {
                                vm.cancelLiveStroke(); strokeCancelled = true
                            }
                            transforming = true
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            if (abs(zoomChange - 1f) > 0.004f || panChange.getDistance() > 1.5f) {
                                transformed = true
                            }
                            if (centroid != Offset.Unspecified) {
                                onZoomPan(zoomChange, panChange, centroid)
                            }
                            event.changes.forEach { it.consume() }
                        } else if (pressed.size == 1 && !transforming) {
                            val change = pressed[0]
                            if (change.positionChanged()) {
                                if ((change.position - startPos).getDistance() > 6f) moved = true
                                if (strokeStarted && !strokeCancelled) {
                                    val c = toCanvas(change.position)
                                    vm.addLivePoint(c.x, c.y, change.pressure)
                                    change.consume()
                                } else if (shapeStart != null) {
                                    val c = toCanvas(change.position)
                                    val r = android.graphics.RectF(
                                        min(shapeStart.x, c.x), min(shapeStart.y, c.y),
                                        max(shapeStart.x, c.x), max(shapeStart.y, c.y),
                                    )
                                    if (r.width() > 4 && r.height() > 4) {
                                        vm.pendingShape = PendingShape(vm.shapeKind, r)
                                    }
                                    change.consume()
                                }
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }

                    if (strokeStarted && !strokeCancelled) {
                        vm.endLiveStroke()
                    } else if (!strokeStarted && !transformed && transforming.not() && !moved) {
                        // Single-pointer tap
                        if (vm.toolMode == ToolMode.FILL && canDraw) {
                            val c = toCanvas(startPos)
                            if (c.x >= 0 && c.y >= 0 && c.x < vm.canvasW && c.y < vm.canvasH) {
                                vm.floodFill(c.x, c.y)
                            }
                        }
                    } else if (transforming && !transformed) {
                        // Multi-finger tap without movement: undo / redo shortcuts.
                        when (maxPointers) {
                            2 -> vm.undo()
                            3 -> vm.redo()
                        }
                    }
                }
            }
    ) {
        val origin = canvasOriginProvider()
        val doc = vm.document
        val w = doc.canvasWidth
        val h = doc.canvasHeight

        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            nc.save()
            nc.translate(origin.x, origin.y)
            nc.scale(totalScale, totalScale)
            nc.clipRect(0f, 0f, w, h)

            // Paper + template
            nc.drawColor(BrushRenderer.parseColor(doc.backgroundHex))
            TemplateRenderer.draw(nc, doc.template, w, h)

            // Layers (bottom -> top); reference layers shown at their opacity while editing.
            val page = vm.page
            page.layers.forEachIndexed { index, layer ->
                if (!layer.isVisible) return@forEachIndexed
                val bmp = vm.layerBitmap(layer)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                }
                nc.drawBitmap(bmp, null, Rect(0, 0, w.toInt(), h.toInt()), paint)

                // Live stroke rides on the active layer.
                if (index == page.activeLayerIndex) {
                    vm.liveStroke?.let { live ->
                        if (live.isEraser) {
                            // Approximate live preview for the eraser: translucent paper-colour trace.
                            val preview = live.copy(
                                isEraser = false,
                                brush = com.sketchbook.app.model.BrushType.MONOLINE,
                                colorHex = doc.backgroundHex,
                                width = live.eraserWidth,
                            )
                            BrushRenderer.drawStroke(nc, preview)
                        } else {
                            BrushRenderer.drawStroke(nc, live)
                            vm.mirroredStrokes(live).forEach { BrushRenderer.drawStroke(nc, it) }
                        }
                    }
                }
            }

            // Symmetry guides
            if (vm.symmetry != SymmetryMode.OFF) {
                val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f / totalScale
                    color = android.graphics.Color.argb(
                        178,
                        (BrandSecondary.red * 255).toInt(),
                        (BrandSecondary.green * 255).toInt(),
                        (BrandSecondary.blue * 255).toInt(),
                    )
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 10f), 0f)
                }
                if (vm.symmetry == SymmetryMode.VERTICAL || vm.symmetry == SymmetryMode.QUAD) {
                    nc.drawLine(w / 2f, 0f, w / 2f, h, guide)
                }
                if (vm.symmetry == SymmetryMode.HORIZONTAL || vm.symmetry == SymmetryMode.QUAD) {
                    nc.drawLine(0f, h / 2f, w, h / 2f, guide)
                }
            }

            nc.restore()
        }
    }
}

// ---------------------------------------------------------------- Toolbar

@Composable
private fun EditorToolbar(
    vm: EditorViewModel,
    onBack: () -> Unit,
    onOpenBrushPicker: () -> Unit,
    onOpenShapesPicker: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenSizePopover: () -> Unit,
    onOpenEraserPopover: () -> Unit,
    onOpenAdjustments: () -> Unit,
    onToggleLayers: () -> Unit,
    onFitCanvas: () -> Unit,
) {
    var templateMenu by remember { mutableStateOf(false) }
    var transformMenu by remember { mutableStateOf(false) }
    var overflowMenu by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(
                icon = Icons.Filled.Brush, label = "Brush",
                active = vm.toolMode == ToolMode.DRAW,
            ) {
                if (vm.toolMode == ToolMode.DRAW) onOpenBrushPicker()
                else vm.toolMode = ToolMode.DRAW
            }
            ToolButton(
                icon = Icons.Filled.AutoFixHigh, label = "Eraser",
                active = vm.toolMode == ToolMode.ERASE,
            ) {
                if (vm.toolMode == ToolMode.ERASE) onOpenEraserPopover()
                else vm.toolMode = ToolMode.ERASE
            }
            // Color well
            IconButton(onClick = onOpenColorPicker) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(vm.color, CircleShape)
                        .background(Color.Transparent)
                )
            }
            ToolButton(icon = Icons.Filled.LineWeight, label = "Size", active = false, onClick = onOpenSizePopover)
            ToolButton(
                icon = Icons.Filled.Category, label = "Shapes",
                active = vm.toolMode == ToolMode.SHAPE,
            ) {
                if (vm.toolMode == ToolMode.SHAPE) onOpenShapesPicker()
                else vm.toolMode = ToolMode.SHAPE
            }
            ToolButton(
                icon = Icons.Filled.FormatColorFill, label = "Fill",
                active = vm.toolMode == ToolMode.FILL,
            ) { vm.toolMode = ToolMode.FILL }
            ToolButton(icon = Icons.Filled.AutoFixHigh, label = "Adjust", active = false, onClick = onOpenAdjustments)

            Box {
                ToolButton(icon = Icons.Filled.GridOn, label = "Template", active = false) { templateMenu = true }
                DropdownMenu(expanded = templateMenu, onDismissRequest = { templateMenu = false }) {
                    TemplateKind.entries.forEach { tk ->
                        DropdownMenuItem(
                            text = { Text(if (tk == vm.document.template) "✓ ${tk.title}" else tk.title) },
                            onClick = { templateMenu = false; vm.setTemplate(tk) })
                    }
                }
            }
            Box {
                ToolButton(icon = Icons.Filled.CropRotate, label = "Transform", active = false) { transformMenu = true }
                DropdownMenu(expanded = transformMenu, onDismissRequest = { transformMenu = false }) {
                    DropdownMenuItem(text = { Text("Flip Horizontal") },
                        onClick = { transformMenu = false; vm.flipActiveLayer(true) })
                    DropdownMenuItem(text = { Text("Flip Vertical") },
                        onClick = { transformMenu = false; vm.flipActiveLayer(false) })
                    DropdownMenuItem(text = { Text("Fit to Canvas") },
                        onClick = { transformMenu = false; onFitCanvas() })
                }
            }
            ToolButton(
                icon = if (vm.fingerDrawing) Icons.Filled.TouchApp else Icons.Filled.PanTool,
                label = "Finger", active = vm.fingerDrawing,
            ) { vm.fingerDrawing = !vm.fingerDrawing }
        }

        IconButton(onClick = { vm.undo() }, enabled = vm.canUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
        }
        IconButton(onClick = { vm.redo() }, enabled = vm.canRedo) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
        }
        IconButton(onClick = onToggleLayers) {
            Icon(Icons.Filled.Layers, contentDescription = "Layers")
        }
        Box {
            IconButton(onClick = { overflowMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = overflowMenu, onDismissRequest = { overflowMenu = false }) {
                Text(
                    "Symmetry", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                SymmetryMode.entries.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(if (m == vm.symmetry) "✓ ${m.title}" else m.title) },
                        onClick = { overflowMenu = false; vm.symmetry = m })
                }
                HorizontalDivider()
                Text(
                    "Filter Effect", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                FilterMenuItems(vm) { overflowMenu = false }
            }
        }
    }
}

@Composable
private fun FilterMenuItems(vm: EditorViewModel, dismiss: () -> Unit) {
    DropdownMenuItem(text = { Text("Mono") }, onClick = {
        dismiss(); vm.applyAdjustment("Mono") { com.sketchbook.app.drawing.Adjustments.mono(it) }
    })
    DropdownMenuItem(text = { Text("Sepia") }, onClick = {
        dismiss(); vm.applyAdjustment("Sepia") { com.sketchbook.app.drawing.Adjustments.sepia(it) }
    })
    DropdownMenuItem(text = { Text("Vibrant") }, onClick = {
        dismiss(); vm.applyAdjustment("Vibrant") { com.sketchbook.app.drawing.Adjustments.vibrant(it) }
    })
    DropdownMenuItem(text = { Text("Invert") }, onClick = {
        dismiss(); vm.applyAdjustment("Invert") { com.sketchbook.app.drawing.Adjustments.invert(it) }
    })
    DropdownMenuItem(text = { Text("Soft Blur") }, onClick = {
        dismiss(); vm.applyAdjustment("Soft Blur") { com.sketchbook.app.drawing.Adjustments.blur(it, 6f) }
    })
    DropdownMenuItem(text = { Text("Mosaic") }, onClick = {
        dismiss(); vm.applyAdjustment("Mosaic") { com.sketchbook.app.drawing.Adjustments.pixellate(it, 16) }
    })
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.padding(horizontal = 2.dp).size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon, contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ---------------------------------------------------------------- Page control

@Composable
private fun PageControl(vm: EditorViewModel, modifier: Modifier = Modifier) {
    var pagesMenu by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                onClick = { vm.goToPage(vm.document.currentPageIndex - 1) },
                enabled = vm.document.currentPageIndex > 0,
            ) { Text("▲") }
            Text(
                "Page ${vm.document.currentPageIndex + 1} / ${vm.document.pages.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(
                onClick = { vm.goToPage(vm.document.currentPageIndex + 1) },
                enabled = vm.document.currentPageIndex < vm.document.pages.size - 1,
            ) { Text("▼") }
            Box {
                IconButton(onClick = { pagesMenu = true }) { Text("⋯") }
                DropdownMenu(expanded = pagesMenu, onDismissRequest = { pagesMenu = false }) {
                    DropdownMenuItem(text = { Text("Add Page Before") },
                        onClick = { pagesMenu = false; vm.addPage(after = false) })
                    DropdownMenuItem(text = { Text("Add Page After") },
                        onClick = { pagesMenu = false; vm.addPage(after = true) })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Clear Current Page") },
                        onClick = { pagesMenu = false; vm.clearCurrentPage() })
                    DropdownMenuItem(text = { Text("Clear All Pages") },
                        onClick = { pagesMenu = false; vm.clearAllPages() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Delete Current Page", color = MaterialTheme.colorScheme.error) },
                        onClick = { pagesMenu = false; vm.deleteCurrentPage() })
                    DropdownMenuItem(text = { Text("Delete All Pages", color = MaterialTheme.colorScheme.error) },
                        onClick = { pagesMenu = false; vm.deleteAllPages() })
                }
            }
        }
    }
}
