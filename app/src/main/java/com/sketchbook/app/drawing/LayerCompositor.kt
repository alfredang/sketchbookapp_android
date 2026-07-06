package com.sketchbook.app.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Base64
import com.sketchbook.app.model.Layer
import com.sketchbook.app.model.Page
import com.sketchbook.app.model.SketchDocument
import java.io.ByteArrayOutputStream

/**
 * Rasterises layers, pages and whole documents. All bitmap math lives here so
 * rendering is consistent everywhere (canvas, thumbnails, fills, adjustments).
 */
object LayerCompositor {

    // Cache of decoded per-layer base images keyed by layer id (value = base64 identity + bitmap).
    private val imageCache = HashMap<String, Pair<String, Bitmap>>()

    fun decodeLayerImage(layer: Layer): Bitmap? {
        val b64 = layer.imageBase64 ?: return null
        imageCache[layer.id]?.let { (key, bmp) -> if (key === b64 || key == b64) return bmp }
        return runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()?.also { imageCache[layer.id] = b64 to it }
    }

    fun encodePng(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** Render one layer (base image + strokes, eraser-aware) into an ARGB bitmap. */
    fun renderLayer(layer: Layer, w: Int, h: Int, scale: Float = 1f): Bitmap {
        val bmp = Bitmap.createBitmap(
            (w * scale).toInt().coerceAtLeast(1),
            (h * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        canvas.scale(scale, scale)
        drawLayerContent(canvas, layer, w, h)
        return bmp
    }

    /** Draw a layer's content (image below, strokes above) at canvas coordinates. */
    fun drawLayerContent(canvas: Canvas, layer: Layer, w: Int, h: Int) {
        decodeLayerImage(layer)?.let { img ->
            canvas.drawBitmap(img, null, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        for (stroke in layer.strokes) {
            BrushRenderer.drawStroke(canvas, stroke)
        }
    }

    /**
     * Composite a page. When [includeBackground] is false only the artwork is rendered
     * (used by flood-fill boundary detection so template lines never fence fills).
     */
    fun renderPage(
        document: SketchDocument,
        page: Page,
        scale: Float = 1f,
        includeBackground: Boolean = true,
        includeReference: Boolean = false,
    ): Bitmap {
        val w = document.canvasWidth.toInt()
        val h = document.canvasHeight.toInt()
        val bmp = Bitmap.createBitmap(
            (w * scale).toInt().coerceAtLeast(1),
            (h * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        canvas.scale(scale, scale)
        if (includeBackground) {
            canvas.drawColor(BrushRenderer.parseColor(document.backgroundHex))
            TemplateRenderer.draw(canvas, document.template, w.toFloat(), h.toFloat())
        }
        for (layer in page.layers) {
            if (!layer.isVisible) continue
            if (layer.isReference && !includeReference) continue
            val layerBmp = renderLayer(layer, w, h)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawBitmap(layerBmp, null, Rect(0, 0, w, h), paint)
            layerBmp.recycle()
        }
        return bmp
    }

    /** Small PNG thumbnail (max edge [maxEdge]) of the current page, base64-encoded. */
    fun thumbnailBase64(document: SketchDocument, maxEdge: Int = 512): String {
        val scale = maxEdge.toFloat() / maxOf(document.canvasWidth, document.canvasHeight)
        val bmp = renderPage(document, document.currentPage, scale = scale)
        val b64 = encodePng(bmp)
        bmp.recycle()
        return b64
    }

    /** Flatten a layer (image + strokes) into a raster-only layer. */
    fun flattenLayer(layer: Layer, w: Int, h: Int): Layer {
        val bmp = renderLayer(layer, w, h)
        val flattened = layer.copy(imageBase64 = encodePng(bmp), strokes = emptyList())
        bmp.recycle()
        imageCache.remove(layer.id)
        return flattened
    }

    fun invalidateCache(layerId: String) {
        imageCache.remove(layerId)
    }

    fun clearCache() = imageCache.clear()
}
