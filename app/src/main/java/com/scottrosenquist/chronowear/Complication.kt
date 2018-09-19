package com.scottrosenquist.chronowear

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.rendering.ComplicationDrawable
import kotlin.reflect.KProperty

class Complication {
//    val id: Int? = null

//    val supportedTypes: IntArray? = null

    var context: Context? = null

    private operator fun ComplicationDrawable.getValue(complication: Complication, property: KProperty<*>): ComplicationDrawable {
        return ComplicationDrawable(context)
    }

    var complicationData: ComplicationData? = null
        set(value) {
            field = value
            complicationDrawable.setComplicationData(value)
        }

    val complicationDrawable by ComplicationDrawable()

//    val supportedComplicationDataTypes = intArrayOf(
//            ComplicationData.TYPE_RANGED_VALUE,
//            ComplicationData.TYPE_ICON,
//            ComplicationData.TYPE_SHORT_TEXT,
//            ComplicationData.TYPE_SMALL_IMAGE
//    )

    var lowBitAmbient = false
        set(value) {
            field = value
            complicationDrawable.setLowBitAmbient(lowBitAmbient)
        }

    var burnInProtection = false
        set(value) {
            field = value
            complicationDrawable.setBurnInProtection(burnInProtection)
        }

    var ambient = false
        set(value) {
            field = value
            complicationDrawable.setInAmbientMode(ambient)
        }

    var watchFaceRadius = 0f
        set(value) {
            field = value
        }

    var position: PointF = PointF()

    fun draw(canvas: Canvas, now: Long) {
        canvas.save()
        canvas.translate(position.x, position.y)

//        canvas.drawText()
//        complicationDrawable.draw(canvas, now)
        canvas.restore()
    }

    fun tap(x: Int, y: Int) {
//        if (complicationDrawableSparseArray[it].bounds.contains(x, y)) {
//            if (activeComplicationDataSparseArray[it] == null) {
//                startActivity(ComplicationHelperActivity.createProviderChooserHelperIntent(this@Chronowear, ComponentName(this@Chronowear, Chronowear::class.java), it, *COMPLICATION_SUPPORTED_TYPES[it]))
//            } else {
//                if (activeComplicationDataSparseArray[it].type == ComplicationData.TYPE_NO_PERMISSION) {
//                    startActivity(ComplicationHelperActivity.createPermissionRequestHelperIntent(this@Chronowear, ComponentName(this@Chronowear, Chronowear::class.java)))
//                } else {
//                    activeComplicationDataSparseArray[it].tapAction?.send()
//                }
//            }
//        }
    }


}


