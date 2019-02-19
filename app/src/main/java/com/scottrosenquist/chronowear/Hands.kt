package com.scottrosenquist.chronowear

import android.graphics.Canvas

class Hands {

    private var hourHand = Hand(Hand.Type.HOUR)
    private var minuteHand = Hand(Hand.Type.MINUTE)
    private var secondHand = Hand(Hand.Type.SECOND)
    private var hands = arrayOf(hourHand, minuteHand, secondHand)

    var ambientColour = 0
        set(value) {
            hands.forEach {
                it.colour = value
            }
        }

    var primaryColour = 0
        set(value) {
            hourHand.colour = value
            minuteHand.colour = value
        }

    var accentColour = 0
        set(value) {
            secondHand.colour = value
        }

    var antiAlias = true
        set(value) {
            hands.forEach {
                it.antiAlias = value
            }
        }

    var watchFaceRadius = 0f
        set(value) {
            hands.forEach {
                it.watchFaceRadius = value
            }
        }

    fun draw(canvas: Canvas, hourRotation: Float, minuteRotation: Float, secondRotation: Float?, previousSecondRotation: Float?) {
        hourHand.draw(canvas, hourRotation)
        minuteHand.draw(canvas, minuteRotation)
        secondRotation?.let { secondHand.draw(canvas, it, previousSecondRotation) }
    }

}
