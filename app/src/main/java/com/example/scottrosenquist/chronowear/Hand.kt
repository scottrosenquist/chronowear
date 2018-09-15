package com.example.scottrosenquist.chronowear

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.reflect.KProperty

class Hand(val type: Type) {

    enum class Type { HOUR, MINUTE, SECOND }

    val handPaint = Paint()

    val circlePaint = Paint().apply {
        style = when (type) {
            Type.HOUR, Type.MINUTE -> Paint.Style.STROKE
            Type.SECOND -> Paint.Style.FILL
        }
    }

    val length by Ratios(0.5f, 0.77f, 0.88f)
    val width by Ratios(0.05f, 0.035f, 0.02f)
    val circleRadius by Ratios(0.04f, 0.04f, 0.03f)

    var watchFaceRadius = 0f
        set(value) {
            field = value
            handPaint.strokeWidth = width
            circlePaint.strokeWidth = ratio(0.02f)
        }

    var colour = 0
        set(value) {
            handPaint.color = value
            circlePaint.color = value
        }

    var antiAlias = true
        set(value) {
            handPaint.isAntiAlias = value
            circlePaint.isAntiAlias = value
        }

    fun draw(canvas: Canvas, rotation: Float) {
        val centerX = watchFaceRadius
        val centerY = watchFaceRadius

        canvas.save()

        canvas.rotate(rotation, centerX, centerY)
        canvas.drawLine(
                centerX,
                centerY - circleRadius,
                centerX,
                centerY - length,
                handPaint)

        canvas.restore()

        canvas.drawCircle(
                centerX,
                centerY,
                circleRadius,
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
