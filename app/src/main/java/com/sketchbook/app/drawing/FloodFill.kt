package com.sketchbook.app.drawing

import android.graphics.Bitmap
import android.graphics.Color
import java.util.ArrayDeque

/**
 * Scanline (span) flood fill, mirroring the iOS implementation: the boundary is the
 * composited ARTWORK only (template/background excluded), tolerance 0.12 per channel.
 * Returns a transparent bitmap patch containing only the filled pixels.
 */
object FloodFill {

    fun fill(
        boundary: Bitmap,
        startX: Int,
        startY: Int,
        fillColor: Int,
        tolerance: Float = 0.12f,
    ): Bitmap? {
        val w = boundary.width
        val h = boundary.height
        if (startX !in 0 until w || startY !in 0 until h) return null

        val pixels = IntArray(w * h)
        boundary.getPixels(pixels, 0, w, 0, 0, w, h)

        val target = pixels[startY * w + startX]
        val tol = (tolerance * 255).toInt()

        fun matches(c: Int): Boolean {
            val dr = Color.red(c) - Color.red(target)
            val dg = Color.green(c) - Color.green(target)
            val db = Color.blue(c) - Color.blue(target)
            val da = Color.alpha(c) - Color.alpha(target)
            return (if (dr < 0) -dr else dr) <= tol &&
                (if (dg < 0) -dg else dg) <= tol &&
                (if (db < 0) -db else db) <= tol &&
                (if (da < 0) -da else da) <= tol
        }

        // Don't fill if the tap target is already (near) the fill colour.
        if (matches(fillColor) && Color.alpha(target) > 0) return null

        val filled = BooleanArray(w * h)
        val patch = IntArray(w * h)
        val stack = ArrayDeque<Int>()
        stack.push(startY * w + startX)

        var count = 0
        while (stack.isNotEmpty()) {
            val idx = stack.pop()
            val y = idx / w
            var x = idx % w
            if (filled[idx] || !matches(pixels[idx])) continue
            // walk left
            while (x > 0 && !filled[y * w + x - 1] && matches(pixels[y * w + x - 1])) x--
            var spanUp = false
            var spanDown = false
            var xi = x
            while (xi < w && !filled[y * w + xi] && matches(pixels[y * w + xi])) {
                val i = y * w + xi
                filled[i] = true
                patch[i] = fillColor
                count++
                if (y > 0) {
                    val up = (y - 1) * w + xi
                    val m = !filled[up] && matches(pixels[up])
                    if (m && !spanUp) { stack.push(up); spanUp = true }
                    else if (!m) spanUp = false
                }
                if (y < h - 1) {
                    val dn = (y + 1) * w + xi
                    val m = !filled[dn] && matches(pixels[dn])
                    if (m && !spanDown) { stack.push(dn); spanDown = true }
                    else if (!m) spanDown = false
                }
                xi++
            }
        }
        if (count == 0) return null

        // Dilate the patch 1px so the fill tucks under anti-aliased stroke edges.
        val dilated = patch.copyOf()
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                if (patch[i] == 0) {
                    val n = (x > 0 && patch[i - 1] != 0) ||
                        (x < w - 1 && patch[i + 1] != 0) ||
                        (y > 0 && patch[i - w] != 0) ||
                        (y < h - 1 && patch[i + w] != 0)
                    if (n) dilated[i] = fillColor
                }
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(dilated, 0, w, 0, 0, w, h)
        return out
    }
}
