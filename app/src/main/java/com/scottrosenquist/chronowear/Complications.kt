package com.scottrosenquist.chronowear

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.support.wearable.complications.ComplicationData
import kotlin.reflect.KProperty

const val LEFT_COMPLICATION_ID = 0
const val TOP_COMPLICATION_ID = 1
const val RIGHT_COMPLICATION_ID = 2
const val BOTTOM_COMPLICATION_ID = 3

val COMPLICATION_IDS = intArrayOf(
        LEFT_COMPLICATION_ID,
        TOP_COMPLICATION_ID,
        RIGHT_COMPLICATION_ID,
        BOTTOM_COMPLICATION_ID
)

val COMPLICATION_SUPPORTED_TYPES = arrayOf(
        intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
        ),
        intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
        ),
        intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
        ),
        intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
        )
)

val COMPLICATION_POSITIONS = arrayOf(
        Point()
)

//    val center = width / 2
//    val left = width / 4
//    val top = width / 4
//    val right = width * 3 / 4
//    val bottom = width * 3 / 4
//    val halfAComplication = width / 8
//
//    fun complicationRect(horizontal: Int, vertical: Int) = Rect(
//            horizontal - halfAComplication,
//            vertical - halfAComplication,
//            horizontal + halfAComplication,
//            vertical + halfAComplication
//    )
//
//    complicationDrawableSparseArray[LEFT_COMPLICATION_ID].bounds = complicationRect(left, center)
//    complicationDrawableSparseArray[TOP_COMPLICATION_ID].bounds = complicationRect(center, top)
//    complicationDrawableSparseArray[RIGHT_COMPLICATION_ID].bounds = complicationRect(right, center)
//    complicationDrawableSparseArray[BOTTOM_COMPLICATION_ID].bounds = complicationRect(center, bottom)

class Complications(chronowear: Chronowear) {

//    val leftDialComplication: Complication? = null
//    val topDialComplication: Complication? = null
//    val rightDialComplication: Complication? = null
//    val bottomDialComplication: Complication? = null

//    val complicationsMap = mapOf(
//            LEFT_COMPLICATION_ID to leftDialComplication,
//            TOP_COMPLICATION_ID to topDialComplication,
//            RIGHT_COMPLICATION_ID to rightDialComplication,
//            BOTTOM_COMPLICATION_ID to bottomDialComplication
//    )

    var context: Context = chronowear
        set(value) {
            field = value
            activeComplicationsMutableMap.forEach {
                it.value.context = context
            }
        }


    private val activeComplicationsMutableMap = mutableMapOf<Int, Complication>()

    fun updateComplicationData(complicationId: Int, complicationData: ComplicationData) {
        if (complicationId !in activeComplicationsMutableMap) {
            initializeComplication(complicationId)
        }
        activeComplicationsMutableMap[complicationId]?.complicationData = complicationData
    }

    private fun initializeComplication(complicationId: Int) {
//        = complicationsMap[complicationId]
//        val tempComplication = Complication(context).apply {
//            this.lowBitAmbient = lowBitAmbient
//            this.burnInProtection = burnInProtection
//            this.ambient = ambient
//            this.watchFaceRadius = watchFaceRadius
//        }
        activeComplicationsMutableMap[complicationId] = Complication(context).apply {
            //this.context = context
            this.lowBitAmbient = lowBitAmbient
            this.burnInProtection = burnInProtection
            this.ambient = ambient
            this.watchFaceRadius = watchFaceRadius
        }
    }

    var lowBitAmbient = false
        set(value) {
            field = value
            activeComplicationsMutableMap.forEach {
                it.value.lowBitAmbient = lowBitAmbient
            }
        }

    var burnInProtection = false
        set(value) {
            field = value
            activeComplicationsMutableMap.forEach {
                it.value.burnInProtection = burnInProtection
            }
        }

    var ambient = false
        set(value) {
            field = value
            activeComplicationsMutableMap.forEach {
                it.value.ambient = ambient
            }
        }

    var watchFaceRadius = 0f
        set(value) {
            field = value
            activeComplicationsMutableMap.forEach {
                it.value.watchFaceRadius = watchFaceRadius
                // it.value.
            }
        }

    fun tap(x: Int, y: Int) {
        activeComplicationsMutableMap.forEach {
            it.value.tap(x, y)
        }
//        COMPLICATION_IDS.forEach {
//            if (complicationDrawableSparseArray[it].bounds.contains(x, y)) {
//                if (activeComplicationDataSparseArray[it] == null) {
//                    startActivity(ComplicationHelperActivity.createProviderChooserHelperIntent(this@Chronowear, ComponentName(this@Chronowear, Chronowear::class.java), it, *COMPLICATION_SUPPORTED_TYPES[it]))
//                } else {
//                    if (activeComplicationDataSparseArray[it].type == ComplicationData.TYPE_NO_PERMISSION) {
//                        startActivity(ComplicationHelperActivity.createPermissionRequestHelperIntent(this@Chronowear, ComponentName(this@Chronowear, Chronowear::class.java)))
//                    } else {
//                        activeComplicationDataSparseArray[it].tapAction?.send()
//                    }
//                }
//            }
//        }
    }

    fun draw(canvas: Canvas, now: Long) {
        activeComplicationsMutableMap.forEach {
            it.value.draw(canvas, now)
        }
    }

    private class Position(var complicationId: Int) {
        operator fun getValue(complications: Complications, property: KProperty<*>): PointF {
            val center = complications.watchFaceRadius
            val x = when (complicationId) {
                LEFT_COMPLICATION_ID -> center / 2f
                RIGHT_COMPLICATION_ID -> - center / 2f
                else -> 0f
            }
            val y = when (complicationId) {
                TOP_COMPLICATION_ID -> center / 2f
                BOTTOM_COMPLICATION_ID -> - center / 2f
                else -> 0f
            }
            return PointF(x, y)
//            return complications.watchFaceRadius * when (complicationId) {
//                Type.HOUR -> hourRatio
//                Type.MINUTE -> minuteRatio
//                Type.SECOND -> secondRatio
//            }
        }
    }

//    val center = width / 2
//    val left = width / 4
//    val top = width / 4
//    val right = width * 3 / 4
//    val bottom = width * 3 / 4
//    val halfAComplication = width / 8
//
//    fun complicationRect(horizontal: Int, vertical: Int) = Rect(
//            horizontal - halfAComplication,
//            vertical - halfAComplication,
//            horizontal + halfAComplication,
//            vertical + halfAComplication
//    )
//
//    complicationDrawableSparseArray[LEFT_COMPLICATION_ID].bounds = complicationRect(left, center)
//    complicationDrawableSparseArray[TOP_COMPLICATION_ID].bounds = complicationRect(center, top)
//    complicationDrawableSparseArray[RIGHT_COMPLICATION_ID].bounds = complicationRect(right, center)
//    complicationDrawableSparseArray[BOTTOM_COMPLICATION_ID].bounds = complicationRect(center, bottom)


//    val center = width * 0.5f
//    val distanceFromCenter = width * 0.20f
//    val left = center - distanceFromCenter
//    val top = center - distanceFromCenter
//    val right = center + distanceFromCenter
//    val bottom = center + distanceFromCenter
//    val halfAComplication = width * 0.125f
//
//    fun complicationRect(horizontal: Float, vertical: Float) = Rect(
//            (horizontal - halfAComplication).roundToInt(),
//            (vertical - halfAComplication).roundToInt(),
//            (horizontal + halfAComplication).roundToInt(),
//            (vertical + halfAComplication).roundToInt()
//    )
//
//    complicationDrawableSparseArray[LEFT_COMPLICATION_ID].bounds = complicationRect(left, center)
//    complicationDrawableSparseArray[TOP_COMPLICATION_ID].bounds = complicationRect(center, top)
//    complicationDrawableSparseArray[RIGHT_COMPLICATION_ID].bounds = complicationRect(right, center)
//    complicationDrawableSparseArray[BOTTOM_COMPLICATION_ID].bounds = complicationRect(center, bottom)

}