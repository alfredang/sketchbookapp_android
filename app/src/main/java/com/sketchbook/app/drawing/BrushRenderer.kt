package com.sketchbook.app.drawing

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.sketchbook.app.model.BrushType
import com.sketchbook.app.model.Stroke
import com.sketchbook.app.model.StrokePoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Renders vector strokes onto an android.graphics.Canvas with per-brush character. */
object BrushRenderer {

    fun parseColor(hex: String): Int = runCatching {
        Color.parseColor(hex)
    }.getOrDefault(Color.BLACK)

    /** Draw a full stroke (committed or live preview) onto the canvas. */
    fun drawStroke(canvas: Canvas, stroke: Stroke) {
        if (stroke.points.isEmpty()) return
        if (stroke.isEraser) {
            drawPathStroke(canvas, stroke.points, eraserPaint(stroke.eraserWidth))
            return
        }
        when (stroke.brush) {
            BrushType.STARDUST -> drawStardust(canvas, stroke)
            else -> drawInk(canvas, stroke)
        }
    }

    private fun eraserPaint(width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = width
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private fun basePaint(stroke: Stroke): Paint {
        val brush = stroke.brush
        val color = parseColor(stroke.colorHex)
        val alpha = (Color.alpha(color) * brush.opacity).toInt().coerceIn(0, 255)
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = if (brush == BrushType.HIGHLIGHTER || brush == BrushType.MARKER)
                Paint.Cap.SQUARE else Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.width
            this.color = color
            this.alpha = alpha
            if (brush.softness > 0f) {
                val radius = (stroke.width * brush.softness).coerceAtLeast(1f)
                maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
            }
        }
    }

    private fun drawInk(canvas: Canvas, stroke: Stroke) {
        val paint = basePaint(stroke)
        if (stroke.brush.pressureSensitive && stroke.points.size > 1) {
            // Variable-width: render as consecutive segments with per-point width.
            val pts = stroke.points
            for (i in 1 until pts.size) {
                val a = pts[i - 1]
                val b = pts[i]
                val press = ((a.p + b.p) / 2f).coerceIn(0.05f, 1f)
                paint.strokeWidth = stroke.width * (0.45f + 0.75f * press)
                canvas.drawLine(a.x, a.y, b.x, b.y, paint)
            }
            // Round the tips
            paint.style = Paint.Style.FILL
            val first = pts.first(); val last = pts.last()
            canvas.drawCircle(first.x, first.y, stroke.width * (0.45f + 0.75f * first.p) / 2f, paint)
            canvas.drawCircle(last.x, last.y, stroke.width * (0.45f + 0.75f * last.p) / 2f, paint)
        } else {
            drawPathStroke(canvas, stroke.points, paint)
        }
    }

    private fun drawPathStroke(canvas: Canvas, points: List<StrokePoint>, paint: Paint) {
        if (points.size == 1) {
            val fill = Paint(paint).apply { style = Paint.Style.FILL }
            canvas.drawCircle(points[0].x, points[0].y, paint.strokeWidth / 2f, fill)
            return
        }
        canvas.drawPath(smoothPath(points), paint)
    }

    /** Quadratic-smoothed path through the sampled points. */
    fun smoothPath(points: List<StrokePoint>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].x, points[0].y)
        if (points.size == 2) {
            path.lineTo(points[1].x, points[1].y)
            return path
        }
        for (i in 1 until points.size - 1) {
            val midX = (points[i].x + points[i + 1].x) / 2f
            val midY = (points[i].y + points[i + 1].y) / 2f
            path.quadTo(points[i].x, points[i].y, midX, midY)
        }
        val last = points.last()
        path.lineTo(last.x, last.y)
        return path
    }

    /** Stardust: scatter dots / sparkles / stars along the stroke path. */
    private fun drawStardust(canvas: Canvas, stroke: Stroke) {
        val rnd = Random(stroke.seed)
        val color = parseColor(stroke.colorHex)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            this.color = color
        }
        val pts = stroke.points
        var stamped = 0
        val spacing = (stroke.width * 0.55f).coerceAtLeast(4f)
        var acc = 0f
        for (i in 1 until pts.size) {
            val a = pts[i - 1]; val b = pts[i]
            val d = hypot(b.x - a.x, b.y - a.y)
            acc += d
            while (acc >= spacing && stamped < 300) {
                acc -= spacing
                val t = rnd.nextFloat()
                val jx = (rnd.nextFloat() - 0.5f) * stroke.width * 1.6f
                val jy = (rnd.nextFloat() - 0.5f) * stroke.width * 1.6f
                val x = a.x + (b.x - a.x) * t + jx
                val y = a.y + (b.y - a.y) * t + jy
                val size = stroke.width * (0.10f + rnd.nextFloat() * 0.28f)
                val alpha = (120 + rnd.nextInt(135))
                paint.alpha = alpha
                strokePaint.alpha = alpha
                strokePaint.strokeWidth = (size * 0.35f).coerceAtLeast(1f)
                when {
                    rnd.nextFloat() < 0.5f -> canvas.drawCircle(x, y, size * 0.5f, paint)
                    rnd.nextFloat() < 0.7f -> drawRays(canvas, x, y, size, 4, rnd.nextFloat() * 180f, strokePaint)
                    else -> drawRays(canvas, x, y, size, 6, rnd.nextFloat() * 180f, strokePaint)
                }
                stamped++
            }
            if (stamped >= 300) break
        }
    }

    private fun drawRays(canvas: Canvas, cx: Float, cy: Float, size: Float, rays: Int, rotationDeg: Float, paint: Paint) {
        val r = size
        for (k in 0 until rays) {
            val ang = Math.toRadians(rotationDeg + k * (360.0 / rays))
            canvas.drawLine(
                cx, cy,
                cx + (r * cos(ang)).toFloat(),
                cy + (r * sin(ang)).toFloat(),
                paint
            )
        }
    }

    /** Angle helper used by callers that orient arrow heads etc. */
    fun segmentAngle(a: StrokePoint, b: StrokePoint): Float =
        atan2(b.y - a.y, b.x - a.x)
}
