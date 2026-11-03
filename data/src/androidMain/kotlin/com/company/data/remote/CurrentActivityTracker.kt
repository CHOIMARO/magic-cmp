package com.company.data.remote

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Keeps the activity on screen. The login SDKs need an activity to show their screens.
 *
 * Create it when the app starts, before the first activity. Koin creates it at the start (createdAtStart).
 *
 * @param context Context of the app.
 */
internal class CurrentActivityTracker(context: Context) {
    @Volatile
    private var current: WeakReference<Activity>? = null

    /** Activity on screen, or null when the app has no activity. */
    val activity: Activity?
        get() = current?.get()

    init {
        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = keep(activity)
            override fun onActivityStarted(activity: Activity) = keep(activity)
            override fun onActivityResumed(activity: Activity) = keep(activity)
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) {
                if (current?.get() === activity) current = null
            }
        })
    }

    private fun keep(activity: Activity) {
        current = WeakReference(activity)
    }
}
