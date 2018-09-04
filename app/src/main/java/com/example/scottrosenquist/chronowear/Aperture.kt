package com.example.scottrosenquist.chronowear

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.reflect.KProperty

/*
What different forms can aperatures come in?
Square/Rectangle
Circle
Arc
*/

abstract class Aperture {

    enum class Position { TWELVE, NINE, SIX }

    val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
    }

    val borderWidth by Ratio(0.01f)

    var watchFaceRadius = 0f
        set(value) {
            field = value
            borderPaint.strokeWidth = borderWidth
        }

    var antiAlias = true
        set(value) {
            borderPaint.isAntiAlias = value
        }

    val x by Ratio(1f)
    val y by Ratio(1f)



//    val x = 200f
//    val y = 200f

    protected fun draw(canvas: Canvas, drawAperture: Canvas.() -> Unit) {
//        val centerX = watchFaceRadius
//        val centerY = watchFaceRadius

        canvas.save()

        canvas.translate(x, y)

//        canvas.drawCircle(0f, 0f, 10f, borderPaint)

        canvas.drawAperture()

        canvas.restore()
    }
}

open class RectangleAperture : Aperture() {

    val width by Ratio(0.1f)
    val height by Ratio(0.1f)

//    val width = 10f
//    val height = 10f


    fun draw(canvas: Canvas) {
        super.draw(canvas) {
//            this.drawCircle(0f, 0f, 20f, borderPaint)
//            this.drawText("wft", 0f, 0f, borderPaint)
            this.drawRect(
                    -this@RectangleAperture.width/2f,
                    -this@RectangleAperture.height/2f,
                    this@RectangleAperture.width/2f,
                    this@RectangleAperture.height/2f,
                    borderPaint
            )
//            this.drawRect(0f, 0f, 20f, 20f, borderPaint)

        }
    }
}

class SquareAperture : RectangleAperture() {

}

class CircleAperture

class ArcAperture

private class Ratio(val ratio: Float) {
    operator fun getValue(aperture: Aperture, property: KProperty<*>): Float {
        return aperture.watchFaceRadius * ratio
    }
}