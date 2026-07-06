package com.sketchbook.app.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sketchbook.app.data.AppSettings
import com.sketchbook.app.data.DocumentStore
import com.sketchbook.app.drawing.Adjustments
import com.sketchbook.app.drawing.BrushRenderer
import com.sketchbook.app.drawing.FloodFill
import com.sketchbook.app.drawing.LayerCompositor
import com.sketchbook.app.drawing.ShapeFactory
import com.sketchbook.app.model.BrushType
import com.sketchbook.app.model.Layer
import com.sketchbook.app.model.Page
import com.sketchbook.app.model.PencilGrade
import com.sketchbook.app.model.ShapeKind
import com.sketchbook.app.model.SketchDocument
import com.sketchbook.app.model.Stroke
import com.sketchbook.app.model.StrokePoint
import com.sketchbook.app.model.SymmetryMode
import com.sketchbook.app.model.TemplateKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class ToolMode { DRAW, ERASE, FILL, SHAPE }

data class PendingShape(
    val kind: ShapeKind,
    val rect: RectF,
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
)

/** All editor state + operations. Undo/redo is a bounded document-snapshot stack. */
class EditorViewModel(
    initial: SketchDocument,
    private val store: DocumentStore,
    val settings: AppSettings,
    private val scope: CoroutineScope,
) {
    var document by mutableStateOf(initial)
        private set

    // Tool state
    var toolMode by mutableStateOf(ToolMode.DRAW)
    var brush by mutableStateOf(settings.defaultBrush)
    var brushSize by mutableFloatStateOf(20f)
    var color by mutableStateOf(Color(0xFF111418))
    var eraseWidth by mutableFloatStateOf(settings.defaultEraseSize)
    var pencilGrade by mutableStateOf(settings.defaultPencilGrade)
    var symmetry by mutableStateOf(SymmetryMode.OFF)
    var fingerDrawing by mutableStateOf(settings.fingerDrawing)

    // Shape tool state
    var shapeKind by mutableStateOf(ShapeKind.RECTANGLE)
    var shapeStrokeWidth by mutableFloatStateOf(6f)
    var shapeFillEnabled by mutableStateOf(false)
    var shapeFillColor by mutableStateOf(Color(0xFF4C6EF5))
    var pendingShape by mutableStateOf<PendingShape?>(null)

    // Live stroke (canvas coordinates)
    var liveStroke by mutableStateOf<Stroke?>(null)
        private set

    // Undo/redo
    private val undoStack = ArrayDeque<SketchDocument>()
    private val redoStack = ArrayDeque<SketchDocument>()
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    var isBusy by mutableStateOf(false)

    // Bitmap cache: one rendered bitmap per layer of the current page.
    private val layerBitmaps = HashMap<String, Bitmap>()
    var bitmapVersion by mutableIntStateOf(0)
        private set

    private var autosaveJob: Job? = null

    val canvasW: Int get() = document.canvasWidth.toInt()
    val canvasH: Int get() = document.canvasHeight.toInt()
    val page: Page get() = document.currentPage
    val activeLayer: Layer get() = page.layers[page.activeLayerIndex.coerceIn(0, page.layers.size - 1)]

    // ---- Rendering support ----

    fun layerBitmap(layer: Layer): Bitmap =
        layerBitmaps.getOrPut(layer.id) { LayerCompositor.renderLayer(layer, canvasW, canvasH) }

    private fun invalidateLayer(id: String) {
        layerBitmaps.remove(id)?.recycle()
        LayerCompositor.invalidateCache(id)
        bitmapVersion++
    }

    private fun invalidateAllLayers() {
        layerBitmaps.values.forEach { it.recycle() }
        layerBitmaps.clear()
        bitmapVersion++
    }

    // ---- Undo ----

    private fun snapshot() {
        undoStack.addLast(document)
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
        updateUndoFlags()
    }

    private fun updateUndoFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(document)
        document = prev
        invalidateAllLayers()
        updateUndoFlags()
        scheduleAutosave()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(document)
        document = next
        invalidateAllLayers()
        updateUndoFlags()
        scheduleAutosave()
    }

    // ---- Document mutation helpers ----

    private fun updatePage(transform: (Page) -> Page) {
        val pages = document.pages.toMutableList()
        val idx = document.currentPageIndex.coerceIn(0, pages.size - 1)
        pages[idx] = transform(pages[idx])
        document = document.copy(pages = pages, currentPageIndex = idx)
        scheduleAutosave()
    }

    private fun updateActiveLayer(transform: (Layer) -> Layer) {
        updatePage { p ->
            val layers = p.layers.toMutableList()
            val i = p.activeLayerIndex.coerceIn(0, layers.size - 1)
            layers[i] = transform(layers[i])
            p.copy(layers = layers)
        }
    }

    // ---- Strokes ----

    fun colorHex(c: Color = color): String = String.format("#%08X", c.toArgb())

    fun startLiveStroke(x: Float, y: Float, pressure: Float) {
        if (activeLayer.isLocked || activeLayer.isReference) return
        val isErase = toolMode == ToolMode.ERASE
        val useGradeColor = brush == BrushType.PENCIL
        liveStroke = Stroke(
            brush = brush,
            colorHex = if (useGradeColor && !isErase) pencilGrade.hex else colorHex(),
            width = if (useGradeColor) pencilGrade.width.coerceAtLeast(brushSize * 0.35f) else brushSize,
            points = listOf(StrokePoint(x, y, pressure)),
            isEraser = isErase,
            eraserWidth = eraseWidth,
            seed = Random.nextLong(),
        )
    }

    fun addLivePoint(x: Float, y: Float, pressure: Float) {
        val s = liveStroke ?: return
        val last = s.points.last()
        if (kotlin.math.abs(last.x - x) < 0.8f && kotlin.math.abs(last.y - y) < 0.8f) return
        liveStroke = s.copy(points = s.points + StrokePoint(x, y, pressure))
    }

    fun cancelLiveStroke() {
        liveStroke = null
    }

    fun endLiveStroke() {
        val s = liveStroke ?: return
        liveStroke = null
        if (s.points.isEmpty()) return
        snapshot()
        val strokes = mutableListOf(s)
        strokes += mirroredStrokes(s)
        // Draw incrementally into the cached layer bitmap, then append to the model.
        val bmp = layerBitmap(activeLayer)
        val canvas = Canvas(bmp)
        strokes.forEach { BrushRenderer.drawStroke(canvas, it) }
        bitmapVersion++
        updateActiveLayer { it.copy(strokes = it.strokes + strokes) }
    }

    /** Symmetry copies of a stroke about the canvas centre axes. */
    fun mirroredStrokes(s: Stroke): List<Stroke> {
        val w = document.canvasWidth
        val h = document.canvasHeight
        fun mx(p: StrokePoint) = p.copy(x = w - p.x)
        fun my(p: StrokePoint) = p.copy(y = h - p.y)
        return when (symmetry) {
            SymmetryMode.OFF -> emptyList()
            SymmetryMode.VERTICAL -> listOf(s.copy(points = s.points.map(::mx)))
            SymmetryMode.HORIZONTAL -> listOf(s.copy(points = s.points.map(::my)))
            SymmetryMode.QUAD -> listOf(
                s.copy(points = s.points.map(::mx)),
                s.copy(points = s.points.map(::my)),
                s.copy(points = s.points.map { my(mx(it)) }),
            )
        }
    }

    // ---- Fill ----

    fun floodFill(x: Float, y: Float) {
        if (activeLayer.isLocked || activeLayer.isReference || isBusy) return
        val doc = document
        val fillArgb = color.toArgb()
        isBusy = true
        scope.launch {
            val patched = withContext(Dispatchers.Default) {
                // Boundary = artwork only, at full resolution.
                val boundary = LayerCompositor.renderPage(
                    doc, doc.currentPage,
                    includeBackground = false, includeReference = false
                )
                val patch = FloodFill.fill(boundary, x.toInt(), y.toInt(), fillArgb)
                boundary.recycle()
                patch
            }
            if (patched != null) {
                snapshot()
                // Flatten active layer and composite the fill patch on top.
                val flattenedBase = withContext(Dispatchers.Default) {
                    val base = LayerCompositor.renderLayer(activeLayer, canvasW, canvasH)
                    Canvas(base).drawBitmap(patched, 0f, 0f, Paint())
                    patched.recycle()
                    LayerCompositor.encodePng(base).also { base.recycle() }
                }
                updateActiveLayer { it.copy(imageBase64 = flattenedBase, strokes = emptyList()) }
                invalidateLayer(activeLayer.id)
            }
            isBusy = false
        }
    }

    // ---- Shapes ----

    fun commitPendingShape() {
        val ps = pendingShape ?: return
        pendingShape = null
        if (activeLayer.isLocked || activeLayer.isReference) return
        snapshot()
        val strokeHex = colorHex()
        val fillHex = colorHex(shapeFillColor)
        val doFill = shapeFillEnabled && ps.kind.canFill
        val strokeW = shapeStrokeWidth
        val active = activeLayer
        isBusy = true
        scope.launch {
            val encoded = withContext(Dispatchers.Default) {
                val base = LayerCompositor.renderLayer(active, canvasW, canvasH)
                val canvas = Canvas(base)
                val path = ShapeFactory.path(ps.kind, ps.rect, ps.rotation, ps.flipH, ps.flipV)
                if (doFill) {
                    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = BrushRenderer.parseColor(fillHex)
                    })
                }
                canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    color = BrushRenderer.parseColor(strokeHex)
                })
                LayerCompositor.encodePng(base).also { base.recycle() }
            }
            updateActiveLayer { it.copy(imageBase64 = encoded, strokes = emptyList()) }
            invalidateLayer(active.id)
            isBusy = false
        }
    }

    fun cancelPendingShape() {
        pendingShape = null
    }

    // ---- Layers ----

    fun setActiveLayer(index: Int) = updatePage { it.copy(activeLayerIndex = index.coerceIn(0, it.layers.size - 1)) }

    fun addLayer() {
        snapshot()
        updatePage { p ->
            val name = "Layer ${p.layers.size + 1}"
            p.copy(
                layers = p.layers + Layer(name = name),
                activeLayerIndex = p.layers.size
            )
        }
    }

    fun duplicateActiveLayer() {
        snapshot()
        updatePage { p ->
            val src = p.layers[p.activeLayerIndex]
            val copy = src.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = src.name + " copy"
            )
            val layers = p.layers.toMutableList()
            layers.add(p.activeLayerIndex + 1, copy)
            p.copy(layers = layers, activeLayerIndex = p.activeLayerIndex + 1)
        }
    }

    fun deleteLayer(index: Int) {
        if (page.layers.size <= 1) return
        snapshot()
        updatePage { p ->
            val layers = p.layers.toMutableList()
            layers.removeAt(index)
            p.copy(
                layers = layers,
                activeLayerIndex = p.activeLayerIndex.coerceIn(0, layers.size - 1)
            )
        }
    }

    fun moveLayer(from: Int, to: Int) {
        if (from == to) return
        snapshot()
        updatePage { p ->
            val layers = p.layers.toMutableList()
            if (from !in layers.indices || to !in layers.indices) return@updatePage p
            val item = layers.removeAt(from)
            layers.add(to, item)
            p.copy(layers = layers, activeLayerIndex = to)
        }
    }

    fun renameLayer(index: Int, name: String) = updatePage { p ->
        val layers = p.layers.toMutableList()
        layers[index] = layers[index].copy(name = name.ifBlank { layers[index].name })
        p.copy(layers = layers)
    }

    fun setLayerVisible(index: Int, visible: Boolean) = updatePage { p ->
        val layers = p.layers.toMutableList()
        layers[index] = layers[index].copy(isVisible = visible)
        p.copy(layers = layers)
    }

    fun setLayerLocked(index: Int, locked: Boolean) = updatePage { p ->
        val layers = p.layers.toMutableList()
        layers[index] = layers[index].copy(isLocked = locked)
        p.copy(layers = layers)
    }

    fun setLayerOpacity(index: Int, opacity: Float) = updatePage { p ->
        val layers = p.layers.toMutableList()
        layers[index] = layers[index].copy(opacity = opacity.coerceIn(0f, 1f))
        p.copy(layers = layers)
    }

    // ---- Pages ----

    fun goToPage(index: Int) {
        val i = index.coerceIn(0, document.pages.size - 1)
        if (i == document.currentPageIndex) return
        document = document.copy(currentPageIndex = i)
        invalidateAllLayers()
        scheduleAutosave()
    }

    fun addPage(after: Boolean) {
        snapshot()
        val pages = document.pages.toMutableList()
        val insertAt = if (after) document.currentPageIndex + 1 else document.currentPageIndex
        pages.add(insertAt, Page())
        document = document.copy(pages = pages, currentPageIndex = insertAt)
        invalidateAllLayers()
        scheduleAutosave()
    }

    fun deleteCurrentPage() {
        snapshot()
        if (document.pages.size <= 1) {
            // Last page: clear instead of remove.
            updatePage { Page(id = it.id) }
            invalidateAllLayers()
            return
        }
        val pages = document.pages.toMutableList()
        pages.removeAt(document.currentPageIndex)
        document = document.copy(
            pages = pages,
            currentPageIndex = document.currentPageIndex.coerceIn(0, pages.size - 1)
        )
        invalidateAllLayers()
        scheduleAutosave()
    }

    fun clearCurrentPage() {
        snapshot()
        updatePage { Page(id = it.id) }
        invalidateAllLayers()
    }

    fun clearAllPages() {
        snapshot()
        document = document.copy(
            pages = document.pages.map { Page(id = it.id) },
            currentPageIndex = document.currentPageIndex
        )
        invalidateAllLayers()
        scheduleAutosave()
    }

    fun deleteAllPages() {
        snapshot()
        document = document.copy(pages = listOf(Page()), currentPageIndex = 0)
        invalidateAllLayers()
        scheduleAutosave()
    }

    fun movePage(from: Int, to: Int) {
        if (from == to) return
        snapshot()
        val pages = document.pages.toMutableList()
        if (from !in pages.indices || to !in pages.indices) return
        val item = pages.removeAt(from)
        pages.add(to, item)
        document = document.copy(pages = pages, currentPageIndex = to)
        invalidateAllLayers()
        scheduleAutosave()
    }

    // ---- Template / transform ----

    fun setTemplate(kind: TemplateKind) {
        snapshot()
        document = document.copy(template = kind)
        scheduleAutosave()
    }

    fun flipActiveLayer(horizontal: Boolean) {
        if (activeLayer.isLocked) return
        snapshot()
        val active = activeLayer
        isBusy = true
        scope.launch {
            val encoded = withContext(Dispatchers.Default) {
                val base = LayerCompositor.renderLayer(active, canvasW, canvasH)
                val m = Matrix().apply {
                    if (horizontal) postScale(-1f, 1f, canvasW / 2f, canvasH / 2f)
                    else postScale(1f, -1f, canvasW / 2f, canvasH / 2f)
                }
                val flipped = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
                Canvas(flipped).drawBitmap(base, m, Paint(Paint.FILTER_BITMAP_FLAG))
                base.recycle()
                LayerCompositor.encodePng(flipped).also { flipped.recycle() }
            }
            updateActiveLayer { it.copy(imageBase64 = encoded, strokes = emptyList()) }
            invalidateLayer(active.id)
            isBusy = false
        }
    }

    // ---- Adjustments & filters (bake into active layer raster) ----

    fun applyAdjustment(name: String, transform: (Bitmap) -> Bitmap) {
        if (activeLayer.isLocked || isBusy) return
        snapshot()
        val active = activeLayer
        isBusy = true
        scope.launch {
            val encoded = withContext(Dispatchers.Default) {
                val base = LayerCompositor.renderLayer(active, canvasW, canvasH)
                val out = transform(base)
                if (out !== base) base.recycle()
                LayerCompositor.encodePng(out).also { out.recycle() }
            }
            updateActiveLayer { it.copy(imageBase64 = encoded, strokes = emptyList()) }
            invalidateLayer(active.id)
            isBusy = false
        }
    }

    // ---- Persistence ----

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = scope.launch {
            delay(3000)
            saveNow()
        }
    }

    suspend fun saveNow() {
        val doc = document
        val thumb = withContext(Dispatchers.Default) {
            runCatching { LayerCompositor.thumbnailBase64(doc) }.getOrNull()
        }
        val toSave = if (thumb != null) doc.copy(thumbnailBase64 = thumb) else doc
        document = store.save(toSave)
    }

    fun saveAndClose(onClosed: () -> Unit) {
        autosaveJob?.cancel()
        if (pendingShape != null) commitPendingShape()
        scope.launch {
            // Wait for any in-flight bake (shape commit) to finish before saving.
            while (isBusy) delay(50)
            saveNow()
            onClosed()
        }
    }

    fun dispose() {
        invalidateAllLayers()
        LayerCompositor.clearCache()
    }
}
