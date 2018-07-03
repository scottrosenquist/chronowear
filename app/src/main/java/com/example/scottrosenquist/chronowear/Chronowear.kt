package com.example.scottrosenquist.chronowear

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder

import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.TimeZone
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationHelperActivity
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.util.SparseArray

val preferences: Preferences by lazy {
    Chronowear.nonNullPreferences!!
}

const val LEFT_COMPLICATION_ID = 0
const val TOP_COMPLICATION_ID = 1
const val RIGHT_COMPLICATION_ID = 2
const val BOTTOM_COMPLICATION_ID = 3

val COMPLICATION_IDS = intArrayOf(LEFT_COMPLICATION_ID, TOP_COMPLICATION_ID, RIGHT_COMPLICATION_ID, BOTTOM_COMPLICATION_ID)

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

private const val INTERACTIVE_UPDATE_RATE_MS = 1000

private const val MSG_UPDATE_TIME = 0 // Handler message id for updating the time periodically in interactive mode

private const val HOUR_STROKE_WIDTH = 5f
private const val MINUTE_STROKE_WIDTH = 3f
private const val SECOND_TICK_STROKE_WIDTH = 2f

private const val CENTER_GAP_AND_CIRCLE_RADIUS = 4f

/**
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules.
 */
class Chronowear : CanvasWatchFaceService() {

    internal companion object {
        @SuppressLint("StaticFieldLeak")
        var nonNullPreferences: Preferences? = null
    }

    override fun onCreateEngine(): Engine {
        nonNullPreferences = Preferences(applicationContext)
        return Engine()
    }

    private class EngineHandler(reference: Chronowear.Engine) : Handler() {
        private val weakReference: WeakReference<Chronowear.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = weakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var calendar: Calendar

        private var registeredTimeZoneReceiver = false
        private var muteMode: Boolean = false
        private var centerX: Float = 0F
        private var centerY: Float = 0F

        private var secondHandLength: Float = 0F
        private var minuteHandLength: Float = 0F
        private var hourHandLength: Float = 0F

        private var watchHandColor: Int = 0
        private var watchHandHighlightColor: Int = 0

        private lateinit var hourPaint: Paint
        private lateinit var minutePaint: Paint
        private lateinit var secondPaint: Paint
        private lateinit var tickAndCirclePaint: Paint

        private lateinit var backgroundPaint: Paint

        private var ambient: Boolean = false
        private var lowBitAmbient: Boolean = false
        private var burnInProtection: Boolean = false

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private var activeComplicationDataSparseArray = SparseArray<ComplicationData>(COMPLICATION_IDS.size)

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private var complicationDrawableSparseArray = SparseArray<ComplicationDrawable>(COMPLICATION_IDS.size)

        /* Handler to update the time once a second in interactive mode. */
        private val updateTimeHandler = EngineHandler(this)

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@Chronowear)
                    .setAcceptsTapEvents(true)
                    .build())

            calendar = Calendar.getInstance()

            initializeComplications()
            initializeWatchFace()
        }

        fun initializeWatchFace() {
            initializeBackground()
            initializePaints()
        }

        private fun initializeBackground() {
            backgroundPaint = Paint().apply {
                color = preferences.background.colour
            }
        }

        private fun initializeComplications() {
            COMPLICATION_IDS.forEach { complicationDrawableSparseArray.put(it, ComplicationDrawable(this@Chronowear)) }
            setActiveComplications(*COMPLICATION_IDS)
        }

        private fun initializePaints() {
            watchHandColor = Color.WHITE
            watchHandHighlightColor = preferences.accent.colour

            hourPaint = Paint().apply {
                color = watchHandColor
                strokeWidth = HOUR_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            minutePaint = Paint().apply {
                color = watchHandColor
                strokeWidth = MINUTE_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            secondPaint = Paint().apply {
                color = watchHandHighlightColor
                strokeWidth = SECOND_TICK_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            tickAndCirclePaint = Paint().apply {
                color = watchHandColor
                strokeWidth = SECOND_TICK_STROKE_WIDTH
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
        }

        override fun onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            lowBitAmbient = properties.getBoolean(
                    WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            burnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)

            COMPLICATION_IDS.forEach {
                complicationDrawableSparseArray[it].run {
                    setLowBitAmbient(lowBitAmbient)
                    setBurnInProtection(burnInProtection)
                }
            }
        }

        override fun onComplicationDataUpdate(complicationId: Int, complicationData: ComplicationData?) {
            super.onComplicationDataUpdate(complicationId, complicationData)

            activeComplicationDataSparseArray.put(complicationId, complicationData)

            complicationDrawableSparseArray[complicationId].setComplicationData(complicationData)

            invalidate()
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            ambient = inAmbientMode

            updateWatchHandStyle()

            COMPLICATION_IDS.forEach { complicationDrawableSparseArray[it].setInAmbientMode(ambient) }

            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        private fun updateWatchHandStyle() {
            if (ambient) {
                hourPaint.color = Color.WHITE
                minutePaint.color = Color.WHITE
                secondPaint.color = Color.WHITE
                tickAndCirclePaint.color = Color.WHITE

                if (lowBitAmbient) {
                    hourPaint.isAntiAlias = false
                    minutePaint.isAntiAlias = false
                    secondPaint.isAntiAlias = false
                    tickAndCirclePaint.isAntiAlias = false
                }
            } else {
                hourPaint.color = watchHandColor
                minutePaint.color = watchHandColor
                secondPaint.color = watchHandHighlightColor
                tickAndCirclePaint.color = watchHandColor

                if (lowBitAmbient) {
                    hourPaint.isAntiAlias = true
                    minutePaint.isAntiAlias = true
                    secondPaint.isAntiAlias = true
                    tickAndCirclePaint.isAntiAlias = true
                }
            }
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            if (muteMode != inMuteMode) {
                muteMode = inMuteMode
                hourPaint.alpha = if (inMuteMode) 100 else 255
                minutePaint.alpha = if (inMuteMode) 100 else 255
                secondPaint.alpha = if (inMuteMode) 80 else 255
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            centerX = width / 2f
            centerY = height / 2f

            secondHandLength = (centerX * 0.875).toFloat()
            minuteHandLength = (centerX * 0.75).toFloat()
            hourHandLength = (centerX * 0.5).toFloat()

            val center = width / 2
            val left = width / 4
            val top = width / 4
            val right = width * 3 / 4
            val bottom = width * 3 / 4
            val halfAComplication = width / 8

            fun complicationRect(horizontal: Int, vertical: Int) = Rect(
                    horizontal - halfAComplication,
                    vertical - halfAComplication,
                    horizontal + halfAComplication,
                    vertical + halfAComplication
            )

            complicationDrawableSparseArray[LEFT_COMPLICATION_ID].bounds = complicationRect(left, center)
            complicationDrawableSparseArray[TOP_COMPLICATION_ID].bounds = complicationRect(center, top)
            complicationDrawableSparseArray[RIGHT_COMPLICATION_ID].bounds = complicationRect(right, center)
            complicationDrawableSparseArray[BOTTOM_COMPLICATION_ID].bounds = complicationRect(center, bottom)
        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    println("TAP_TYPE_TOUCH")
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    println("TAP_TYPE_TOUCH_CANCEL")
                }
                WatchFaceService.TAP_TYPE_TAP -> {
                    println("TAP_TYPE_TAP")
                    COMPLICATION_IDS.forEach {
                        if (complicationDrawableSparseArray[it].bounds.contains(x, y)) {
                            if (activeComplicationDataSparseArray[it] == null) {
                                startActivity(ComplicationHelperActivity.createProviderChooserHelperIntent(this@Chronowear, ComponentName(this@Chronowear, Chronowear::class.java), it, *COMPLICATION_SUPPORTED_TYPES[it]))
                            } else {
                                if (activeComplicationDataSparseArray[it].type == ComplicationData.TYPE_NO_PERMISSION) {
                                    startActivity(ComplicationHelperActivity.createPermissionRequestHelperIntent(this@Chronowear, ComponentName(this@Chronowear, Chronowear::class.java)))
                                } else {
                                    activeComplicationDataSparseArray[it].tapAction?.send()
                                }
                            }
                        }
                    }
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now

            drawBackground(canvas)
            drawComplications(canvas, now)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(if (ambient) Color.BLACK else backgroundPaint.color)
        }

        private fun drawComplications(canvas: Canvas, now: Long) {
            COMPLICATION_IDS.forEach { complicationDrawableSparseArray[it].draw(canvas, now) }
        }

        private fun drawWatchFace(canvas: Canvas) {
            val innerTickRadius = centerX - 10
            val outerTickRadius = centerX
            for (tickIndex in 0..11) {
                val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 12).toFloat()
                val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, tickAndCirclePaint)
            }

            val seconds =
                    calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000f
            val secondsRotation = seconds * 6f

            val minutesRotation = calendar.get(Calendar.MINUTE) * 6f

            val hourHandOffset = calendar.get(Calendar.MINUTE) / 2f
            val hoursRotation = calendar.get(Calendar.HOUR) * 30 + hourHandOffset

            canvas.save()

            canvas.rotate(hoursRotation, centerX, centerY)
            canvas.drawLine(
                    centerX,
                    centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    centerX,
                    centerY - hourHandLength,
                    hourPaint)

            canvas.rotate(minutesRotation - hoursRotation, centerX, centerY)
            canvas.drawLine(
                    centerX,
                    centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    centerX,
                    centerY - minuteHandLength,
                    minutePaint)

            if (!ambient) {
                canvas.rotate(secondsRotation - minutesRotation, centerX, centerY)
                canvas.drawLine(
                        centerX,
                        centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        centerX,
                        centerY - secondHandLength,
                        secondPaint)

            }
            canvas.drawCircle(
                    centerX,
                    centerY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    tickAndCirclePaint)

            canvas.restore()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                calendar.timeZone = TimeZone.getDefault()
                initializeWatchFace()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@Chronowear.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = false
            this@Chronowear.unregisterReceiver(timeZoneReceiver)
        }

        /**
         * Starts/stops the [.updateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.updateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !ambient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}
