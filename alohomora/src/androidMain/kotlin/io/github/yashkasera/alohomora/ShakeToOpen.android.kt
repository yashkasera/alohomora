package io.github.yashkasera.alohomora

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.github.yashkasera.alohomora.ShakeDetector.Companion.SHAKE_THRESHOLD_G
import kotlin.math.sqrt

/**
 * No-op on Android: the common `initInternal` path holds no `Context`, so shake detection is wired
 * from [AlohomoraInitializer] via [AndroidShakeToOpen.install] instead. See [installShakeToOpen].
 */
internal actual fun installShakeToOpen() = Unit

/**
 * Registers a process-wide accelerometer listener that launches [DevToolsActivity] on a shake.
 *
 * Installed once from [AlohomoraInitializer], after [ActivityTracker] is attached so a shake can
 * open the console over whatever Activity is in the foreground.
 */
internal object AndroidShakeToOpen {

    private var registered = false

    fun install(context: Context) {
        if (registered) return
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        // No accelerometer (rare, some emulators): shake is simply unavailable, not an error.
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        val detector = ShakeDetector { launchConsole(context) }
        // SENSOR_DELAY_UI is fast enough to feel a shake without the battery cost of the game rate.
        sensorManager.registerListener(detector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        registered = true
    }

    private fun launchConsole(context: Context) {
        if (!ShakeToOpenState.enabled) return
        val activity = ActivityTracker.currentActivity
        // Already looking at the console — a shake here would just relaunch it onto itself.
        if (activity is DevToolsActivity) return

        // DevToolsActivity is a singleTask Activity in its own task affinity; NEW_TASK routes the
        // shake into that task rather than stacking the console on top of the host app, and is
        // mandatory when launching from the application context (no Activity in the foreground).
        val launcher = activity ?: context.applicationContext
        val intent = Intent(launcher, DevToolsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { launcher.startActivity(intent) }
    }
}

/**
 * Detects a shake from raw accelerometer samples: a spike in total acceleration past
 * [SHAKE_THRESHOLD_G] gravities, debounced so one physical shake fires [onShake] once.
 */
private class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private var lastShakeMillis = 0L

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (gForce <= SHAKE_THRESHOLD_G) return

        val nowMillis = event.timestamp / 1_000_000 // event.timestamp is nanoseconds
        if (nowMillis - lastShakeMillis < SHAKE_DEBOUNCE_MILLIS) return
        lastShakeMillis = nowMillis
        onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val SHAKE_THRESHOLD_G = 2.7f
        const val SHAKE_DEBOUNCE_MILLIS = 1_000L
    }
}
