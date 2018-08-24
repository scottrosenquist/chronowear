package com.example.scottrosenquist.chronowear

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.reflect.KProperty

class Hand(val type: Type) {

    enum class Type { HOUR, MINUTE, SECOND }

    val handPaint = Paint()
    val handBorderPaint = Paint().apply {
        color = Color.BLACK
    }

    val circlePaint = Paint().apply {
        style = when (type) {
            Type.HOUR, Type.MINUTE -> Paint.Style.STROKE
            Type.SECOND -> Paint.Style.FILL_AND_STROKE
        }
    }
    val circleBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
    }

    val length by Ratios(0.5f, 0.77f, 0.88f)
    val width by Ratios(0.05f, 0.035f, 0.02f)
    val circleRadius by Ratios(0.08f, 0.055f, 0.03f)
    val circleWidth by Ratios(0.025f, 0.025f, 0.025f)
    val border by Ratios(0.01f, 0.01f, 0.01f)

    var watchFaceRadius = 0f
        set(value) {
            field = value * 1f
            handPaint.strokeWidth = width
            handBorderPaint.strokeWidth = width + border * 2f
            circlePaint.strokeWidth = circleWidth
            circleBorderPaint.strokeWidth = border
        }

    var colour = 0
        set(value) {
            handPaint.color = value
            circlePaint.color = value
        }

    var antiAlias = true
        set(value) {
            handPaint.isAntiAlias = value
            handBorderPaint.isAntiAlias = value
            circlePaint.isAntiAlias = value
            circleBorderPaint.isAntiAlias = value
        }

    fun draw(canvas: Canvas, rotation: Float) {
        val centerX = watchFaceRadius / 1f
        val centerY = watchFaceRadius / 1f

        canvas.drawCircle(
                centerX,
                centerY,
                circleRadius + border / 2f,
                circleBorderPaint)

        canvas.save()

        canvas.rotate(rotation, centerX, centerY)
        canvas.drawLine(
                centerX,
                centerY - circleRadius + circleWidth,
                centerX,
                centerY - length - border,
                handBorderPaint)

        canvas.drawLine(
                centerX,
                centerY - circleRadius + circleWidth,
                centerX,
                centerY - length,
                handPaint)

        canvas.restore()

        canvas.drawCircle(
                centerX,
                centerY,
                circleRadius - circleWidth / 2f,
                circlePaint)
    }

    fun ratio(ratio: Float) = watchFaceRadius * ratio

    private class Ratios(var hourRatio: Float, var minuteRatio: Float, var secondRatio: Float) {
        operator fun getValue(hand: Hand, property: KProperty<*>): Float {
            return hand.watchFaceRadius * when (hand.type) {
                Type.HOUR -> hourRatio
                Type.MINUTE -> minuteRatio
                Type.SECOND -> secondRatio
            }
        }
    }

}
