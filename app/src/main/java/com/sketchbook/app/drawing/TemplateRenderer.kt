package com.sketchbook.app.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.sketchbook.app.model.TemplateKind

/** Draws page templates (ruled/grid/dots/…) behind the artwork, mirroring iOS. */
object TemplateRenderer {

    private fun linePaint(alpha: Int = 26) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(alpha, 0, 0, 0)
    }

    fun draw(canvas: Canvas, kind: TemplateKind, w: Float, h: Float) {
        when (kind) {
            TemplateKind.BLANK -> Unit
            TemplateKind.RULED -> ruled(canvas, w, h)
            TemplateKind.RING_FILE -> ringFile(canvas, w, h)
            TemplateKind.GRID -> grid(canvas, w, h)
            TemplateKind.DOT_GRID -> dotGrid(canvas, w, h)
            TemplateKind.ISOMETRIC -> isometric(canvas, w, h)
            TemplateKind.STORYBOARD -> storyboard(canvas, w, h)
            TemplateKind.MUSIC_STAFF -> musicStaff(canvas, w, h)
        }
    }

    private fun ruled(canvas: Canvas, w: Float, h: Float, gap: Float = 48f) {
        val p = linePaint()
        var y = gap
        while (y < h) {
            canvas.drawLine(0f, y, w, y, p)
            y += gap
        }
    }

    private fun ringFile(canvas: Canvas, w: Float, h: Float) {
        ruled(canvas, w, h)
        // red margin
        val margin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(90, 229, 57, 53)
        }
        canvas.drawLine(120f, 0f, 120f, h, margin)
        // binder holes down the left
        val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(38, 0, 0, 0)
        }
        val count = 7
        for (i in 0 until count) {
            val y = h * (i + 1) / (count + 1f)
            canvas.drawCircle(56f, y, 18f, hole)
        }
    }

    private fun grid(canvas: Canvas, w: Float, h: Float, gap: Float = 48f) {
        val p = linePaint()
        var x = gap
        while (x < w) { canvas.drawLine(x, 0f, x, h, p); x += gap }
        var y = gap
        while (y < h) { canvas.drawLine(0f, y, w, y, p); y += gap }
    }

    private fun dotGrid(canvas: Canvas, w: Float, h: Float, gap: Float = 48f) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(46, 0, 0, 0)
        }
        var y = gap
        while (y < h) {
            var x = gap
            while (x < w) { canvas.drawCircle(x, y, 2f, p); x += gap }
            y += gap
        }
    }

    private fun isometric(canvas: Canvas, w: Float, h: Float, gap: Float = 56f) {
        val p = linePaint()
        val slope = Math.tan(Math.toRadians(30.0)).toFloat()
        // horizontals
        var y = gap
        while (y < h) { canvas.drawLine(0f, y, w, y, p); y += gap }
        // 30° diagonals both directions
        var c = -w * slope
        while (c < h + w * slope) {
            canvas.drawLine(0f, c, w, c + w * slope, p)
            canvas.drawLine(0f, c + w * slope, w, c, p)
            c += gap
        }
    }

    private fun storyboard(canvas: Canvas, w: Float, h: Float) {
        val p = linePaint(40)
        p.strokeWidth = 3f
        val pad = 60f
        val gapBetween = 40f
        val cols = 2; val rows = 3
        val cw = (w - pad * 2 - gapBetween * (cols - 1)) / cols
        val ch = (h - pad * 2 - gapBetween * (rows - 1)) / rows
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val left = pad + c * (cw + gapBetween)
                val top = pad + r * (ch + gapBetween)
                canvas.drawRect(left, top, left + cw, top + ch, p)
            }
        }
    }

    private fun musicStaff(canvas: Canvas, w: Float, h: Float) {
        val p = linePaint(38)
        val lineGap = 16f
        val blockGap = 90f
        var y = blockGap
        while (y + lineGap * 4 < h) {
            for (i in 0 until 5) {
                canvas.drawLine(40f, y + i * lineGap, w - 40f, y + i * lineGap, p)
            }
            y += lineGap * 4 + blockGap
        }
    }
}
