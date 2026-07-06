package com.sketchbook.app.drawing

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import com.sketchbook.app.model.ShapeKind
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Builds android.graphics.Paths for the vector shape tool (canvas coordinates). */
object ShapeFactory {

    /**
     * Path for [kind] inside [rect], rotated by [rotationDeg] about the rect centre,
     * optionally flipped.
     */
    fun path(
        kind: ShapeKind,
        rect: RectF,
        rotationDeg: Float = 0f,
        flipH: Boolean = false,
        flipV: Boolean = false,
    ): Path {
        val p = basePath(kind, rect)
        val m = Matrix()
        val cx = rect.centerX()
        val cy = rect.centerY()
        if (flipH || flipV) {
            m.postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f, cx, cy)
        }
        m.postRotate(rotationDeg, cx, cy)
        p.transform(m)
        return p
    }

    private fun basePath(kind: ShapeKind, r: RectF): Path {
        val p = Path()
        val cx = r.centerX(); val cy = r.centerY()
        when (kind) {
            ShapeKind.RECTANGLE -> p.addRect(r, Path.Direction.CW)
            ShapeKind.ELLIPSE -> p.addOval(r, Path.Direction.CW)
            ShapeKind.TRIANGLE -> {
                p.moveTo(cx, r.top)
                p.lineTo(r.right, r.bottom)
                p.lineTo(r.left, r.bottom)
                p.close()
            }
            ShapeKind.DIAMOND -> {
                p.moveTo(cx, r.top)
                p.lineTo(r.right, cy)
                p.lineTo(cx, r.bottom)
                p.lineTo(r.left, cy)
                p.close()
            }
            ShapeKind.STAR -> polygonStar(p, cx, cy, r.width() / 2f, r.height() / 2f, 5, 0.42f)
            ShapeKind.PENTAGON -> polygon(p, cx, cy, r.width() / 2f, r.height() / 2f, 5)
            ShapeKind.HEXAGON -> polygon(p, cx, cy, r.width() / 2f, r.height() / 2f, 6)
            ShapeKind.ARROW -> {
                // Right-pointing arrow: shaft + head.
                val headW = r.width() * 0.38f
                val shaftH = r.height() * 0.44f
                val shaftTop = cy - shaftH / 2f
                val shaftBottom = cy + shaftH / 2f
                p.moveTo(r.left, shaftTop)
                p.lineTo(r.right - headW, shaftTop)
                p.lineTo(r.right - headW, r.top)
                p.lineTo(r.right, cy)
                p.lineTo(r.right - headW, r.bottom)
                p.lineTo(r.right - headW, shaftBottom)
                p.lineTo(r.left, shaftBottom)
                p.close()
            }
            ShapeKind.HEART -> heart(p, r)
            ShapeKind.LINE -> {
                p.moveTo(r.left, r.bottom)
                p.lineTo(r.right, r.top)
            }
        }
        return p
    }

    private fun polygon(p: Path, cx: Float, cy: Float, rx: Float, ry: Float, sides: Int) {
        for (i in 0 until sides) {
            val ang = Math.toRadians(-90.0 + i * 360.0 / sides)
            val x = cx + rx * cos(ang).toFloat()
            val y = cy + ry * sin(ang).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
    }

    private fun polygonStar(p: Path, cx: Float, cy: Float, rx: Float, ry: Float, points: Int, innerRatio: Float) {
        val total = points * 2
        for (i in 0 until total) {
            val outer = i % 2 == 0
            val rrx = if (outer) rx else rx * innerRatio
            val rry = if (outer) ry else ry * innerRatio
            val ang = Math.toRadians(-90.0 + i * 360.0 / total)
            val x = cx + rrx * cos(ang).toFloat()
            val y = cy + rry * sin(ang).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
    }

    private fun heart(p: Path, r: RectF) {
        val w = r.width(); val h = r.height()
        val cx = r.centerX()
        p.moveTo(cx, r.top + h * 0.30f)
        // left lobe
        p.cubicTo(
            cx - w * 0.10f, r.top - h * 0.05f,
            r.left - w * 0.05f, r.top + h * 0.25f,
            r.left + w * 0.05f, r.top + h * 0.55f
        )
        p.cubicTo(
            r.left + w * 0.15f, r.top + h * 0.75f,
            cx - w * 0.15f, r.top + h * 0.85f,
            cx, r.bottom
        )
        // right lobe (mirror)
        p.cubicTo(
            cx + w * 0.15f, r.top + h * 0.85f,
            r.right - w * 0.15f, r.top + h * 0.75f,
            r.right - w * 0.05f, r.top + h * 0.55f
        )
        p.cubicTo(
            r.right + w * 0.05f, r.top + h * 0.25f,
            cx + w * 0.10f, r.top - h * 0.05f,
            cx, r.top + h * 0.30f
        )
        p.close()
    }

    /** A representative min dimension used to clamp tiny drags. */
    fun minSize(rect: RectF): Float = min(rect.width(), rect.height())
}
