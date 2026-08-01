package io.github.yashkasera.alohomora

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

internal data class ActivityEvent(
    val activityName: String,
    val timestamp: Long,
    val state: ActivityState,
    val intentSnapshot: IntentSnapshot? = null,
    val taskId: Int? = null,
)

internal enum class ActivityState {
    CREATED,
    STARTED,
    RESUMED,
    PAUSED,
    STOPPED,
    DESTROYED
}

internal data class IntentSnapshot(
    val action: String?,
    val data: String?,
    val categories: Set<String>?,
    val flags: Int,
    val extras: Map<String, String>,
)

internal object ActivityTracker : Application.ActivityLifecycleCallbacks {

    private val _events = CopyOnWriteArrayList<ActivityEvent>()
    val events: List<ActivityEvent>
        get() = _events.toList()

    /**
     * The Activity currently in the foreground, or null when the app is backgrounded.
     *
     * Weak so the tracker never keeps a destroyed Activity (and its whole view tree) alive. Used
     * to overlay the connection-request sheet on whatever the developer is looking at; when this
     * is null there is nothing to overlay and Android 10+ forbids starting an Activity from the
     * background, so the caller must fall back to a notification.
     */
    @Volatile
    private var currentActivityRef: WeakReference<Activity>? = null

    val currentActivity: Activity?
        get() = currentActivityRef?.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    /**
     * Application context, captured at startup.
     *
     * Needed for the notification fallback, which by definition runs when there is no Activity
     * to borrow a context from.
     */
    @Volatile
    var applicationContext: Context? = null
        private set

    fun attach(application: Application) {
        applicationContext = application.applicationContext
        application.registerActivityLifecycleCallbacks(this)
    }

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
        currentActivityRef = WeakReference(activity)
        record(activity, ActivityState.RESUMED)
    }

    override fun onActivityPaused(activity: Activity) {
        // Cleared only if this is still the tracked Activity: during A -> B the ordering is
        // B.onResume then A.onPause, so clearing unconditionally would drop the reference to B.
        if (currentActivity === activity) currentActivityRef = null
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

private fun Intent.toSnapshot(): IntentSnapshot =
    IntentSnapshot(
        action = action,
        data = dataString,
        categories = categories,
        flags = flags,
        extras = extras?.keySet()?.associateWith { key ->
            extras?.get(key)?.let { it::class.java.simpleName } ?: "null"
        } ?: emptyMap(),
    )
