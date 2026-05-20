package com.example.pocspeed

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import org.tensorflow.lite.task.vision.detector.Detection

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.GREEN
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        textSize = 38f
        isAntiAlias = true
    }

    private val textBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0xAA000000.toInt()
    }

    private var detections: List<Detection> = emptyList()
    private var imageWidth = 1
    private var imageHeight = 1

    fun setResults(detections: List<Detection>, imageWidth: Int, imageHeight: Int) {
        this.detections = detections
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (detections.isEmpty()) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        for (detection in detections) {
            val box = detection.boundingBox
            val scaledBox = RectF(
                box.left * scaleX,
                box.top * scaleY,
                box.right * scaleX,
                box.bottom * scaleY
            )

            canvas.drawRect(scaledBox, boxPaint)

            val category = detection.categories.firstOrNull()
            val label = category?.label ?: "object"
            val score = category?.score ?: 0f
            val text = "$label ${(score * 100).toInt()}%"

            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.textSize + 12f
            val bgRect = RectF(
                scaledBox.left,
                (scaledBox.top - textHeight).coerceAtLeast(0f),
                scaledBox.left + textWidth + 20f,
                scaledBox.top
            )

            canvas.drawRect(bgRect, textBackgroundPaint)
            canvas.drawText(text, bgRect.left + 10f, bgRect.bottom - 8f, textPaint)
        }
    }
}
