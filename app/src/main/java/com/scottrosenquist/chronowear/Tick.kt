package com.scottrosenquist.chronowear

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.reflect.KProperty

class Tick(val type: Type) {

    enum class Type { HOUR, MINUTE }

    val paint = Paint()

    val length by Ratios(0.1f, 0.033f)
    val width by Ratios(0.015f, 0.01f)
    val margin by Ratios(0.05f, 0.05f)

    var watchFaceRadius = 0f
        set(value) {
            field = value
            paint.strokeWidth = width
        }

    var colour = 0
        set(value) {
            paint.color = value
        }

    var antiAlias = true
        set(value) {
            paint.isAntiAlias = value
        }

    fun draw(canvas: Canvas, rotation: Float) {
        val centerX = watchFaceRadius
        val centerY = watchFaceRadius

        canvas.save()

        canvas.rotate(rotation, centerX, centerY)

        canvas.drawLine(
                centerX,
                margin,
                centerX,
                margin + length,
                paint
        )

        canvas.restore()
    }

    private class Ratios(var hourRatio: Float, var minuteRatio: Float) {
//        , var minuteRatio: Float, var secondRatio: Float
        operator fun getValue(tick: Tick, property: KProperty<*>): Float {
            return tick.watchFaceRadius * when (tick.type) {
                Type.HOUR -> hourRatio
                Type.MINUTE -> minuteRatio
//                Type.SECOND -> secondRatio
            }
        }
    }
}