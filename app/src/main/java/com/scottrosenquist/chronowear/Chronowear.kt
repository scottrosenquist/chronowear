package com.scottrosenquist.chronowear

import android.annotation.SuppressLint
import android.content.*
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
import android.support.wearable.complications.ComplicationData

val preferences: Preferences by lazy {
    Chronowear.nonNullPreferences!!
}

private const val MSG_UPDATE_TIME = 0 // Handler message id for updating the time periodically in interactive mode

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

        private var hands = Hands()

        private var ticks = Ticks()

        private var complications = Complications()

        private var registeredTimeZoneReceiver = false
        private var registeredBatteryReceiver = false
        private var registeredNetworkStatusReceiver = false
        private var muteMode: Boolean = false
        private var isCharging: Boolean = false
        private var isConnected: Boolean = false
        private var chargingLevel: Int = 100
        private var centerX: Float = 0F
        private var centerY: Float = 0F

        private var watchHandColor: Int = 0
        private var watchHandHighlightColor: Int = 0

        private lateinit var backgroundPaint: Paint

        private var useChronowearStatusBar: Boolean = true
        private var statusIconSize: Int = 0
        private var statusIconY: Float = 0f
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

        private var useChronowearNotificationIndicator: Boolean = true
        private var notificationIndicatorSize: Float = 0f
        private var notificationIndicatorY: Float = 0f
        private lateinit var notificationIndicatorPaint: Paint

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

            buildWatchFaceStyle()

            calendar = Calendar.getInstance()

            complications.context = this@Chronowear

            setActiveComplications(*COMPLICATION_IDS)

            updatePaints()
        }

        fun buildWatchFaceStyle() {
            setWatchFaceStyle(WatchFaceStyle.Builder(this@Chronowear)
                    .setAcceptsTapEvents(true)
                    .setHideStatusBar(useChronowearStatusBar)
                    .setHideNotificationIndicator(useChronowearNotificationIndicator)
                    .build())
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

        private fun updatePaints() {
            watchHandColor = Color.WHITE
            watchHandHighlightColor = preferences.accent.colour

            backgroundPaint = Paint().apply {
                color = preferences.background.colour
            }

            notificationIndicatorPaint = Paint().apply {
                color = preferences.accent.colour
                isAntiAlias = true
            }

            hands.apply {
                primaryColour = watchHandColor
                accentColour = watchHandHighlightColor
                antiAlias = true
            }

            ticks.apply {
                colour = watchHandColor
                antiAlias = true
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

            complications.lowBitAmbient = lowBitAmbient
            complications.burnInProtection = burnInProtection
        }

        override fun onComplicationDataUpdate(complicationId: Int, complicationData: ComplicationData) {
            super.onComplicationDataUpdate(complicationId, complicationData)

            complications.updateComplicationData(complicationId, complicationData)

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

            complications.ambient = inAmbientMode

            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        private fun updateWatchHandStyle() {
            if (ambient) {
                hands.ambientColour = Color.WHITE
                ticks.colour = Color.WHITE

                if (lowBitAmbient) {
                    hands.antiAlias = false
                    ticks.antiAlias = false
                    notificationIndicatorPaint.isAntiAlias = false
                }
            } else {
                hands.primaryColour = watchHandColor
                hands.accentColour = watchHandHighlightColor
                ticks.colour = watchHandColor

                if (lowBitAmbient) {
                    hands.antiAlias = true
                    ticks.antiAlias = true
                    notificationIndicatorPaint.isAntiAlias = true
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

            hands.watchFaceRadius = width / 2f
            ticks.watchFaceRadius = width / 2f
            complications.watchFaceRadius = width / 2f

            if (useChronowearStatusBar) {
                statusIconSize = (centerX * 0.11f).roundToInt()
                statusIconY = centerY * 0.275f

                initializeStatusIcons()
            }

            if (useChronowearNotificationIndicator) {
                notificationIndicatorSize = centerX * 0.035f
                notificationIndicatorY = centerY * (2f - 0.275f)
            }
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
                    complications.tap(x, y)
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now

            drawBackground(canvas)
            drawTicks(canvas)
            complications.draw(canvas, now)
            if (useChronowearStatusBar && !(preferences.ambientFullMute && ambient && muteMode)) drawStatusIcons(canvas)
            if (useChronowearNotificationIndicator && !(preferences.ambientFullMute && muteMode)) drawNotificationIndicator(canvas)
            drawHands(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(if (ambient) Color.BLACK else backgroundPaint.color)
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
                val statusIconSpacing = statusIconSize + 5f;
                var adjustedX = x - statusIconSpacing / 2f * (this.size - 1f)
                val iterator = this.listIterator()
                while(iterator.hasNext()) {
                    canvas.drawStatusIconDrawable(iterator.next(), adjustedX, y, color)
                    adjustedX += statusIconSpacing
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
            statusIconsToDraw.drawOnCanvas(canvas, centerX, statusIconY, Color.WHITE)
        }

        private fun drawNotificationIndicator(canvas: Canvas) {
            if (unreadCount > 0) {
                canvas.drawCircle(centerX, notificationIndicatorY, notificationIndicatorSize, notificationIndicatorPaint)
            }
        }

        private fun drawTicks(canvas: Canvas) {
            ticks.draw(canvas)
        }

        private fun drawHands(canvas: Canvas) {
//            calendar.setTimeInMillis(54569000); // Screen Shot Time
//            calendar.setTimeInMillis(61200000); // Midnight (Hand Alignment)

            val seconds = calendar.get(Calendar.SECOND)
            val beatsPerSecond = 1
            val millisecondsNormalizer = 1000 / beatsPerSecond
            val milliseconds = calendar.get(Calendar.MILLISECOND) / millisecondsNormalizer * millisecondsNormalizer

            val secondsRotation = if (!ambient) ( seconds + milliseconds / 1000f ) * 6f else null
            val previousSecondsRotation = if (!ambient) ( seconds + ( milliseconds - millisecondsNormalizer ) / 1000f ) * 6f else null

            val minuteHandOffset = calendar.get(Calendar.SECOND) / 10f;
            val minutesRotation = calendar.get(Calendar.MINUTE) * 6f + minuteHandOffset

            val hourHandOffset = calendar.get(Calendar.MINUTE) / 2f
            val hoursRotation = calendar.get(Calendar.HOUR) * 30 + hourHandOffset

            hands.draw(canvas, hoursRotation, minutesRotation, secondsRotation, previousSecondsRotation)
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
                updatePaints()
                invalidate()
            } else {
                unregisterTimeZoneReceiver()
                unregisterBatteryReceiver()
                unregisterNetworkStatusReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        override fun onNotificationCountChanged(count: Int) {
            super.onNotificationCountChanged(count)
            invalidate()
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
                val framesPerSecond = 60
                val interactiveUpdateRateMs = 1000 / framesPerSecond
                val timeMs = System.currentTimeMillis()
                val delayMs = interactiveUpdateRateMs - timeMs % interactiveUpdateRateMs
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}
