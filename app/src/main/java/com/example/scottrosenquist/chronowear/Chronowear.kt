package com.example.scottrosenquist.chronowear

import android.content.*
import android.content.pm.ProviderInfo
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.annotation.Nullable
import android.support.v4.widget.TextViewCompat
import android.support.wearable.complications.*
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import android.widget.Toast

import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.Executors
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderInfoRetriever
import android.support.wearable.complications.rendering.TextRenderer
import android.text.TextPaint
import android.widget.TextView
import org.w3c.dom.Text
import java.text.DateFormat
import java.text.SimpleDateFormat


private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

private const val NOTIFICATION_COMPLICATION_ID = 0

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

    override fun onCreateEngine(): Engine {
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
        private var faceWidth = 0
        private var faceHeight = 0
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

        private var shouldShowNotification = true
        private var notificationComplicationData: ComplicationData? = null
        private lateinit var notificationTimePaint: TextPaint
        private lateinit var notificationTitlePaint: TextPaint
        private lateinit var notificationTextPaint: TextPaint
        private lateinit var notificationTimeRenderer: TextRenderer
        private lateinit var notificationTitleRenderer: TextRenderer
        private lateinit var notificationTextRenderer: TextRenderer
        private lateinit var notificationTimeBounds: Rect
        private lateinit var notificationTitleBounds: Rect
        private lateinit var notificationTextBounds: Rect
        private lateinit var notificationTimeFormat: SimpleDateFormat
        private lateinit var notificationTimeTextView: TextView
//        private lateinit var notificationComplicationDrawable: ComplicationDrawable

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
            println("new change added 000")

            setWatchFaceStyle(WatchFaceStyle.Builder(this@Chronowear)
                    .setAcceptsTapEvents(true)
                    .setHideStatusBar(true)
                    .build())

            calendar = Calendar.getInstance()

            initializeBackground()
            initializeNotificationComplication()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            backgroundPaint = Paint().apply {
                color = Color.BLACK
            }
        }

        private fun initializeNotificationComplication() {
//            notificationComplicationDrawable = getDrawable(R.drawable.notification_complication_style) as ComplicationDrawable
//            notificationComplicationDrawable = ComplicationDrawable(this@Chronowear)
//            notificationComplicationDrawable.
//            notificationComplicationDrawable.setNoDataText("<No Data>")
//            notificationComplicationDrawable.setComplicationData(ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT).build())


//            val providerInfoRetriever = ProviderInfoRetriever(this@Chronowear, Executors.newCachedThreadPool())
//            providerInfoRetriever.init()
//            providerInfoRetriever.retrieveProviderInfo(
//                    ProviderInfoRetriever.OnProviderInfoReceivedCallback {
//                        @Override fun onProviderReceived() {}
//                    },
//                    ComponentName(this@Chronowear, Chronowear::class.java),
//                    NOTIFICATION_COMPLICATION_ID)
//            providerInfoRetriever.retrieveProviderInfo(
//                    object: ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
//                        override fun onProviderInfoReceived(
//                                watchFaceComplicationId: Int,
//                                @Nullable complicationProviderInfo: ComplicationProviderInfo?) {
//
//                            println("onProviderInfoReceived: " + complicationProviderInfo!!)
//
//                            updateComplicationViews(watchFaceComplicationId, complicationProviderInfo)
//                        }
//                    },
//                    ComponentName(this@Chronowear, Chronowear::class.java),
//                    NOTIFICATION_COMPLICATION_ID)









//            notificationComplicationDrawable.
//            setDefaultSystemComplicationProvider(NOTIFICATION_COMPLICATION_ID, SystemProviders.NEXT_EVENT, ComplicationData.TYPE_LONG_TEXT)
//            setDefaultComplicationProvider(NOTIFICATION_COMPLICATION_ID, ComponentName(this@Chronowear, Chronowear::class.java), ComplicationData.TYPE_LONG_TEXT)
//            ComponentName()
            setActiveComplications(NOTIFICATION_COMPLICATION_ID)
//            notificationComplicationDrawable.setContext(this@Chronowear)
        }

        private fun initializeWatchFace() {
            println("initializeWatchFace()")
            watchHandColor = Color.WHITE
            watchHandHighlightColor = Color.RED

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
                style = Paint.Style.STROKE
            }

            tickAndCirclePaint = Paint().apply {
                color = watchHandColor
                strokeWidth = SECOND_TICK_STROKE_WIDTH
                isAntiAlias = true
                style = Paint.Style.STROKE
            }

            notificationTimePaint = TextPaint().apply {
                color = watchHandColor
                isAntiAlias = true
                textSize = 48f
            }

            notificationTitlePaint = TextPaint().apply {
                color = watchHandColor
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                textSize = 48f
            }

            notificationTextPaint = TextPaint().apply {
                color = watchHandColor
                isAntiAlias = true
                textSize = 36f
            }

            notificationTimeRenderer = TextRenderer()
            notificationTimeRenderer.setPaint(notificationTimePaint)

            notificationTitleRenderer = TextRenderer()
            notificationTitleRenderer.setPaint(notificationTitlePaint)
            notificationTitleRenderer.setMinimumCharactersShown(999)

            notificationTextRenderer = TextRenderer()
            notificationTextRenderer.setPaint(notificationTextPaint)
            notificationTextRenderer.setMinimumCharactersShown(999)

//            notificationTimeFormat = SimpleDateFormat("h:mm a")
            notificationTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT) as SimpleDateFormat

            notificationTimeTextView = TextView(this@Chronowear)
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
        }

        override fun onComplicationDataUpdate(complicationId: Int, complicationData: ComplicationData?) {
            super.onComplicationDataUpdate(complicationId, complicationData)
            if (complicationData != null) {
//                println("contentDescription: "+complicationData.contentDescription)
//                println("imageContentDescription: "+complicationData.imageContentDescription)
                println("longText: "+complicationData.longText?.getText(this@Chronowear, 0))
                println("longTitle: "+complicationData.longTitle?.getText(this@Chronowear, 0))
            }



            notificationComplicationData = complicationData
//            notificationComplicationDrawable.setComplicationData(complicationData)

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
            updateNotificationText()

//            notificationComplicationDrawable.setInAmbientMode(ambient)

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

        private fun updateNotificationText() {
            notificationTimeRenderer.setInAmbientMode(ambient)
            notificationTitleRenderer.setInAmbientMode(ambient)
            notificationTextRenderer.setInAmbientMode(ambient)
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

            faceWidth = width
            faceHeight = height

            centerX = width / 2f
            centerY = height / 2f

            secondHandLength = (centerX * 0.875).toFloat()
            minuteHandLength = (centerX * 0.75).toFloat()
            hourHandLength = (centerX * 0.5).toFloat()

//            setNotificationComplicationBounds()
            setNotificationBounds()
        }

        fun setNotificationBounds() {
            println("setNotificationBounds()")
            notificationTimeBounds = Rect(0, 0, faceWidth, faceHeight / 4)
            notificationTitleBounds = Rect(faceWidth / 4, faceHeight / 4, faceWidth * 3 / 4, faceHeight / 2)
            notificationTextBounds = Rect(0, faceHeight / 2, faceWidth, faceHeight)
//            notificationTimePaint.textSize = notificationTimeBounds.height().toFloat()

//            var test: TextView = TextViewCompat()
//            var test = TextView("test")

        }

        fun setNotificationComplicationBounds() {
            // For most Wear devices, width and height are the same, so we just chose one (width).
            val complicationWidth = centerX * 4f / 3f
            val complicationHeight = centerX / 2f
            val midpointOfScreen = centerX

            val horizontalOffset = midpointOfScreen - complicationWidth / 2
            val verticalOffset = (midpointOfScreen - complicationHeight) / 2

            val bounds =
            // Left, Top, Right, Bottom
                    Rect(
                            horizontalOffset.toInt(),
                            verticalOffset.toInt(),
                            (horizontalOffset + complicationWidth).toInt(),
                            (verticalOffset + complicationHeight).toInt())
            val fullScreenBounds = Rect(0, 0, (centerX * 2f).toInt(), (centerY * 2f).toInt())

//            notificationComplicationDrawable.setBounds(fullScreenBounds)
        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                }
                WatchFaceService.TAP_TYPE_TAP -> {
                    if (notificationComplicationData != null) {
                        println(notificationComplicationData?.type)
                        when (notificationComplicationData?.type) {
                            ComplicationData.TYPE_NOT_CONFIGURED -> {
                                println("not configured")
                            }
                            ComplicationData.TYPE_NO_PERMISSION -> {
                                println("no permission")
                                startActivity(ComplicationHelperActivity.createPermissionRequestHelperIntent(this@Chronowear, ComponentName(this@Chronowear, Chronowear::class.java)))
                            }
                            ComplicationData.TYPE_EMPTY -> {
                                println("empty")
                            }
                            ComplicationData.TYPE_NO_DATA -> {
                                println("no data")
                            }
                            else -> {
                                println("else")
//                            notificationComplicationData?.tapAction?.send()
//                                shouldShowNotification = !shouldShowNotification
                                startActivity(ComplicationHelperActivity.createProviderChooserHelperIntent(this@Chronowear, ComponentName(this@Chronowear, Chronowear::class.java), NOTIFICATION_COMPLICATION_ID, ComplicationData.TYPE_LONG_TEXT))
                            }
                        }
                    } else {
                        println("notificationComplicationData is null")
                        startActivity(ComplicationHelperActivity.createProviderChooserHelperIntent(this@Chronowear, ComponentName(this@Chronowear, Chronowear::class.java), NOTIFICATION_COMPLICATION_ID, ComplicationData.TYPE_LONG_TEXT))
                    }

                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now

            drawBackground(canvas)

            if (shouldShowNotification) {
                drawNotificationComplication(canvas)
            } else {
                drawWatchFace(canvas)
            }
        }

        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
        }

        private fun drawNotificationComplication(canvas: Canvas) {
            canvas.drawRect(notificationTimeBounds, secondPaint)
            canvas.drawRect(notificationTitleBounds, secondPaint)
            canvas.drawRect(notificationTextBounds, secondPaint)
//            notificationTimeRenderer.draw(canvas)
//            val now = System.currentTimeMillis()
//            val calendar = Calendar.getInstance()
            notificationTimeRenderer.setText(notificationTimeFormat.format(calendar.timeInMillis))
            notificationTimeRenderer.setText("12:59 PM")
            notificationTimeRenderer.draw(canvas, notificationTimeBounds)

            notificationTitleRenderer.setText(notificationComplicationData?.longTitle?.getText(this@Chronowear, calendar.timeInMillis))
            notificationTitleRenderer.draw(canvas, notificationTitleBounds)

//            notificationTextRenderer.setText(notificationComplicationData?.longText?.getText(this@Chronowear, calendar.timeInMillis))
//            notificationTextRenderer.draw(canvas, notificationTextBounds)
            var test = TextView(this@Chronowear)
            test.text = notificationComplicationData?.longTitle?.getText(this@Chronowear, calendar.timeInMillis)
//            test.size
            TextViewCompat.setAutoSizeTextTypeWithDefaults(test, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            test.draw(canvas)

//            notificationComplicationDrawable.draw(canvas, currentTimeMillis)
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


