package com.example.scottrosenquist.chronowear

import android.graphics.Canvas

class Ticks {
    private var hourTick = Tick(Tick.Type.HOUR)
    private var minuteTick = Tick(Tick.Type.MINUTE)
    private var ticks = arrayOf(hourTick, minuteTick)

    var colour = 0
        set(value) {
            ticks.forEach {
                it.colour = value
            }
        }

    var antiAlias = true
        set(value) {
            ticks.forEach {
                it.antiAlias = value
            }
        }

    var watchFaceRadius = 0f
        set(value) {
            ticks.forEach {
                it.watchFaceRadius = value
            }
        }

    fun draw(canvas: Canvas) {
        for (degrees in 0 until 360 step 6) {
            when {
                degrees % 30 == 0 -> hourTick
                degrees % 6 == 0 -> minuteTick
                else -> null
            }?.draw(canvas, degrees.toFloat())
        }
    }
}
