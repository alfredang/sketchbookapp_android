package com.sketchbook.app.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Layer adjustments & one-shot filters (applied to a layer's rasterised image),
 * mirroring the iOS AdjustmentEngine / FilterEngine subset that translates to Android.
 */
object Adjustments {

    // ---- Colour ----

    /** hue in degrees (−180…180), saturation 0…2, brightness −0.5…0.5. */
    fun hueSaturationBrightness(src: Bitmap, hue: Float, saturation: Float, brightness: Float): Bitmap {
        val cm = ColorMatrix()
        cm.setSaturation(saturation)
        val hueMatrix = ColorMatrix().apply { setRotate(0, hue); setRotate(1, hue); setRotate(2, hue) }
        cm.postConcat(hueMatrix)
        val b = brightness * 255f
        cm.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f,
        )))
        return applyColorMatrix(src, cm)
    }

    /** warmth 3000…10000K (6500 neutral), tint −100…100. */
    fun colorBalance(src: Bitmap, warmth: Float, tint: Float): Bitmap {
        val t = ((warmth - 6500f) / 3500f).coerceIn(-1f, 1f)   // + = warmer
        val g = (tint / 100f).coerceIn(-1f, 1f)                // + = green
        val cm = ColorMatrix(floatArrayOf(
            1f + 0.18f * t, 0f, 0f, 0f, 0f,
            0f, 1f + 0.10f * g, 0f, 0f, 0f,
            0f, 0f, 1f - 0.18f * t, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ))
        return applyColorMatrix(src, cm)
    }

    // ---- Filters (one-shot) ----

    fun mono(src: Bitmap): Bitmap = applyColorMatrix(src, ColorMatrix().apply { setSaturation(0f) })

    fun sepia(src: Bitmap): Bitmap {
        val cm = ColorMatrix().apply { setSaturation(0f) }
        cm.postConcat(ColorMatrix(floatArrayOf(
            1.10f, 0f, 0f, 0f, 18f,
            0f, 0.95f, 0f, 0f, 6f,
            0f, 0f, 0.72f, 0f, -12f,
            0f, 0f, 0f, 1f, 0f,
        )))
        return applyColorMatrix(src, cm)
    }

    fun vibrant(src: Bitmap): Bitmap = applyColorMatrix(src, ColorMatrix().apply { setSaturation(1.6f) })

    fun invert(src: Bitmap): Bitmap = applyColorMatrix(src, ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    )))

    private fun applyColorMatrix(src: Bitmap, cm: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    // ---- Blur ----

    /** Fast box-blur approximation of a gaussian (3 passes). Radius in px. */
    fun blur(src: Bitmap, radius: Float): Bitmap {
        val r = radius.roundToInt().coerceIn(0, 60)
        if (r == 0) return src.copy(Bitmap.Config.ARGB_8888, true)
        // Downscale for large radii to keep it fast, then upscale.
        val scale = if (r > 12) 0.5f else 1f
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        var bmp = Bitmap.createScaledBitmap(src, w, h, true)
        val pr = (r * scale).roundToInt().coerceAtLeast(1)
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        repeat(3) {
            boxBlurH(px, w, h, pr)
            boxBlurV(px, w, h, pr)
        }
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        return if (scale != 1f) Bitmap.createScaledBitmap(bmp, src.width, src.height, true) else bmp
    }

    private fun boxBlurH(px: IntArray, w: Int, h: Int, r: Int) {
        val tmp = IntArray(w)
        for (y in 0 until h) {
            val row = y * w
            var a = 0; var rr = 0; var gg = 0; var bb = 0
            var count = 0
            for (x in -r..r) {
                val c = px[row + x.coerceIn(0, w - 1)]
                a += Color.alpha(c); rr += Color.red(c); gg += Color.green(c); bb += Color.blue(c)
                count++
            }
            for (x in 0 until w) {
                tmp[x] = Color.argb(a / count, rr / count, gg / count, bb / count)
                val rem = px[row + (x - r).coerceIn(0, w - 1)]
                val add = px[row + (x + r + 1).coerceIn(0, w - 1)]
                a += Color.alpha(add) - Color.alpha(rem)
                rr += Color.red(add) - Color.red(rem)
                gg += Color.green(add) - Color.green(rem)
                bb += Color.blue(add) - Color.blue(rem)
            }
            System.arraycopy(tmp, 0, px, row, w)
        }
    }

    private fun boxBlurV(px: IntArray, w: Int, h: Int, r: Int) {
        val tmp = IntArray(h)
        for (x in 0 until w) {
            var a = 0; var rr = 0; var gg = 0; var bb = 0
            var count = 0
            for (y in -r..r) {
                val c = px[y.coerceIn(0, h - 1) * w + x]
                a += Color.alpha(c); rr += Color.red(c); gg += Color.green(c); bb += Color.blue(c)
                count++
            }
            for (y in 0 until h) {
                tmp[y] = Color.argb(a / count, rr / count, gg / count, bb / count)
                val rem = px[(y - r).coerceIn(0, h - 1) * w + x]
                val add = px[(y + r + 1).coerceIn(0, h - 1) * w + x]
                a += Color.alpha(add) - Color.alpha(rem)
                rr += Color.red(add) - Color.red(rem)
                gg += Color.green(add) - Color.green(rem)
                bb += Color.blue(add) - Color.blue(rem)
            }
            for (y in 0 until h) px[y * w + x] = tmp[y]
        }
    }

    // ---- Stylize ----

    /** amount 0…1: monochrome noise blended into non-transparent pixels. */
    fun noise(src: Bitmap, amount: Float): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width; val h = out.height
        val px = IntArray(w * h)
        out.getPixels(px, 0, w, 0, 0, w, h)
        val rnd = Random(42)
        val strength = (amount * 80f)
        for (i in px.indices) {
            val c = px[i]
            val alpha = Color.alpha(c)
            if (alpha == 0) continue
            val n = ((rnd.nextFloat() - 0.5f) * 2f * strength).toInt()
            px[i] = Color.argb(
                alpha,
                (Color.red(c) + n).coerceIn(0, 255),
                (Color.green(c) + n).coerceIn(0, 255),
                (Color.blue(c) + n).coerceIn(0, 255),
            )
        }
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    /** amount 0…2: unsharp-mask sharpen (blur + weighted subtract). */
    fun sharpen(src: Bitmap, amount: Float): Bitmap {
        val blurred = blur(src, 4f)
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width; val h = out.height
        val a = IntArray(w * h); val b = IntArray(w * h)
        out.getPixels(a, 0, w, 0, 0, w, h)
        blurred.getPixels(b, 0, w, 0, 0, w, h)
        val k = amount
        for (i in a.indices) {
            val ca = a[i]; val cb = b[i]
            val alpha = Color.alpha(ca)
            if (alpha == 0) continue
            a[i] = Color.argb(
                alpha,
                (Color.red(ca) + (k * (Color.red(ca) - Color.red(cb))).toInt()).coerceIn(0, 255),
                (Color.green(ca) + (k * (Color.green(ca) - Color.green(cb))).toInt()).coerceIn(0, 255),
                (Color.blue(ca) + (k * (Color.blue(ca) - Color.blue(cb))).toInt()).coerceIn(0, 255),
            )
        }
        out.setPixels(a, 0, w, 0, 0, w, h)
        return out
    }

    /** Pixellate with cell size in px (Mosaic filter). */
    fun pixellate(src: Bitmap, cell: Int): Bitmap {
        val c = cell.coerceAtLeast(2)
        val small = Bitmap.createScaledBitmap(
            src,
            (src.width / c).coerceAtLeast(1),
            (src.height / c).coerceAtLeast(1),
            false
        )
        return Bitmap.createScaledBitmap(small, src.width, src.height, false)
    }
}
