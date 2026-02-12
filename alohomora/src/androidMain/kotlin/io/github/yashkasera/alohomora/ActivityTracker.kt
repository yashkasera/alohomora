package io.github.yashkasera.alohomora

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable
import java.util.concurrent.CopyOnWriteArrayList

data class ActivityEvent(
    val activityName: String,
    val timestamp: Long,
    val state: ActivityState,
    val intentSnapshot: IntentSnapshot? = null,
    val taskId: Int? = null,
)

enum class ActivityState {
    CREATED,
    STARTED,
    RESUMED,
    PAUSED,
    STOPPED,
    DESTROYED
}

data class IntentSnapshot(
    val action: String?,
    val data: String?,
    val categories: Set<String>?,
    val flags: Int,
    val extras: Map<String, String>,
)

object ActivityTracker : Application.ActivityLifecycleCallbacks {

    private val _events = CopyOnWriteArrayList<ActivityEvent>()
    val events: List<ActivityEvent>
        get() = _events.toList()

    private fun record(
        activity: Activity,
        state: ActivityState,
        includeIntent: Boolean = false,
    ) {
//        if (activity is DevToolsActivity) return

        _events += ActivityEvent(
            activityName = activity::class.java.name,
            timestamp = System.currentTimeMillis(),
            state = state,
            intentSnapshot = if (includeIntent) activity.intent?.toSnapshot() else null,
            taskId = activity.taskId,
        )
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        record(
            activity = activity,
            state = ActivityState.CREATED,
            includeIntent = true,
        )
    }

    override fun onActivityStarted(activity: Activity) {
        record(activity, ActivityState.STARTED)
    }

    override fun onActivityResumed(activity: Activity) {
        record(activity, ActivityState.RESUMED)
    }

    override fun onActivityPaused(activity: Activity) {
        record(activity, ActivityState.PAUSED)
    }

    override fun onActivityStopped(activity: Activity) {
        record(activity, ActivityState.STOPPED)
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {
        // intentionally ignored
    }

    override fun onActivityDestroyed(activity: Activity) {
        record(activity, ActivityState.DESTROYED)
    }

    fun clear() {
        _events.clear()
    }
}

fun Intent.toSnapshot(): IntentSnapshot =
    IntentSnapshot(
        action = action,
        data = dataString,
        categories = categories,
        flags = flags,
        extras = extras?.keySet()?.associateWith { key ->
            extras?.get(key)?.let { it::class.java.simpleName } ?: "null"
        } ?: emptyMap(),
    )
