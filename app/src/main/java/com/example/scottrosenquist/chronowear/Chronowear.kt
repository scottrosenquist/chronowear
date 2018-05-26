package com.example.scottrosenquist.chronowear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.BatteryManager
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
import kotlin.math.roundToInt

private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

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
        private var registeredBatteryReceiver = false
        private var registeredNetworkStatusReceiver = false
        private var muteMode: Boolean = false
        private var isCharging: Boolean = false
        private var isConnected: Boolean = false
        private var chargingLevel: Int = 100
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

        private var statusIconSize: Int = 1
        private lateinit var muteIcon: Drawable
        private lateinit var muteAmbientIcon: Drawable
        private lateinit var chargingIconFull: Drawable
        private lateinit var chargingIcon90: Drawable
        private lateinit var chargingIcon80: Drawable
        private lateinit var chargingIcon60: Drawable
        private lateinit var chargingIcon50: Drawable
        private lateinit var chargingIcon30: Drawable
        private lateinit var chargingIcon20: Drawable
        private lateinit var noConnectionIcon: Drawable

        private var ambient: Boolean = false
        private var lowBitAmbient: Boolean = false
        private var burnInProtection: Boolean = false

        /* Handler to update the time once a second in interactive mode. */
        private val updateTimeHandler = EngineHandler(this)

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateTimeZone()
            }
        }

        private fun updateTimeZone() {
            calendar.timeZone = TimeZone.getDefault()
            invalidate()
        }

        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateBatteryInfo(intent)
                invalidate()
            }
        }

        private fun updateBatteryInfo(intent: Intent) {
            val chargingStatus = intent.getIntExtra("status", -1)

            isCharging = when (chargingStatus) {
                BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> true
                else -> false
            }

            chargingLevel = intent.getIntExtra("level", 100)
        }

        private val networkStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateNetworkStatus()
                invalidate()
            }
        }

        private fun updateNetworkStatus() {
            isConnected = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.isConnected == true
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@Chronowear)
                    .setAcceptsTapEvents(true)
                    .build())

            calendar = Calendar.getInstance()

            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            backgroundPaint = Paint().apply {
                color = Color.BLACK
            }
        }

        private fun initializeStatusIcons() {
            fun Drawable.setDefaultBounds(): Drawable {
                this.setBounds(0, 0, statusIconSize, statusIconSize)
                return this
            }

            fun getAndBoundDrawable(drawableId: Int) = getDrawable(drawableId).setDefaultBounds()

            muteIcon = getAndBoundDrawable(R.drawable.ic_stat_notify_mute)
            muteAmbientIcon = getAndBoundDrawable(R.drawable.ic_stat_notify_mute_ambient)

            chargingIconFull = getAndBoundDrawable(R.drawable.ic_stat_notify_charging_full)
            chargingIcon90 = getAndBoundDrawable(R.drawable.ic_stat_notify_charging_90)
            chargingIcon80 = getAndBoundDrawable(R.drawable.ic_stat_notify_charging_80)
            chargingIcon60 = getAndBoundDrawable(R.drawable.ic_stat_notify_charging_60)
            chargingIcon50 = getAndBoundDrawable(R.drawable.ic_stat_notify_charging_50)
            chargingIcon30 = getAndBoundDrawable(R.drawable.ic_stat_notify_charging_30)
            chargingIcon20 = getAndBoundDrawable(R.drawable.ic_stat_notify_charging_20)

            noConnectionIcon = getAndBoundDrawable(R.drawable.ic_stat_notify_no_connection)
        }

        private fun initializeWatchFace() {
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
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            ambient = inAmbientMode

            updateWatchHandStyle()

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
            muteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_PRIORITY
            invalidate()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            centerX = width / 2f
            centerY = height / 2f

            secondHandLength = (centerX * 0.875).toFloat()
            minuteHandLength = (centerX * 0.75).toFloat()
            hourHandLength = (centerX * 0.5).toFloat()

            statusIconSize = (centerX * 0.1f).roundToInt()
            initializeStatusIcons()
        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                }
                WatchFaceService.TAP_TYPE_TAP -> {

                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now

            drawBackground(canvas)
            drawStatusIcons(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
        }

        private fun drawStatusIcons(canvas: Canvas) {
            fun Canvas.drawStatusIconDrawable(drawable: Drawable, x: Float, y: Float, color: Int) {
                this.save()
                this.translate(x - drawable.bounds.right / 2f, y - drawable.bounds.bottom / 2f)
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                drawable.draw(this)
                this.restore()
            }

            fun MutableList<Drawable>.drawOnCanvas(canvas: Canvas, x: Float, y: Float, color: Int) {
                var adjustedX = x - statusIconSize / 2 * (this.size - 1)
                val iterator = this.listIterator()
                while(iterator.hasNext()) {
                    canvas.drawStatusIconDrawable(iterator.next(), adjustedX, y, color)
                    adjustedX += statusIconSize
                }
            }

            fun determineChargingIcon(): Drawable {
                return chargingLevel.let {
                    when {
                        it == 100 -> chargingIconFull
                        it > 90 -> chargingIcon90
                        it > 80 -> chargingIcon80
                        it > 60 -> chargingIcon60
                        it > 50 -> chargingIcon50
                        it > 30 -> chargingIcon30
                        else -> chargingIcon20
                    }
                }
            }

            val statusIconsToDraw: MutableList<Drawable> = ArrayList() // todo: make this work with mutableListOf<Drawable>()
            if (muteMode) statusIconsToDraw.add(if (ambient) muteAmbientIcon else muteIcon)
            if (isCharging) statusIconsToDraw.add(determineChargingIcon())
            if (!isConnected) statusIconsToDraw.add(noConnectionIcon)
            statusIconsToDraw.drawOnCanvas(canvas, centerX, centerY / 4f, Color.WHITE)
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
                registerTimeZoneReceiver()
                registerBatteryReceiver()
                registerNetworkStatusReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                updateTimeZone()
                updateBatteryInfo(this@Chronowear.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
                updateNetworkStatus()
            } else {
                unregisterTimeZoneReceiver()
                unregisterBatteryReceiver()
                unregisterNetworkStatusReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerTimeZoneReceiver() {
            if (registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@Chronowear.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterTimeZoneReceiver() {
            if (!registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = false
            this@Chronowear.unregisterReceiver(timeZoneReceiver)
        }

        private fun registerBatteryReceiver() {
            if (registeredBatteryReceiver) {
                return
            }
            registeredBatteryReceiver = true
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            this@Chronowear.registerReceiver(batteryReceiver, filter)
        }

        private fun unregisterBatteryReceiver() {
            if (!registeredBatteryReceiver) {
                return
            }
            registeredBatteryReceiver = false
            this@Chronowear.unregisterReceiver(batteryReceiver)
        }

        private fun registerNetworkStatusReceiver() {
            if (registeredNetworkStatusReceiver) {
                return
            }
            registeredNetworkStatusReceiver = true
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            this@Chronowear.registerReceiver(networkStatusReceiver, filter)
        }

        private fun unregisterNetworkStatusReceiver() {
            if (!registeredNetworkStatusReceiver) {
                return
            }
            registeredNetworkStatusReceiver = false
            this@Chronowear.unregisterReceiver(networkStatusReceiver)
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


