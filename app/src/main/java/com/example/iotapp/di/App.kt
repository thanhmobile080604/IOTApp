package com.example.iotapp.di

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp


@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@HiltAndroidApp
class App : Application(), Application.ActivityLifecycleCallbacks,
    LifecycleEventObserver {
    var isShowTrendingDetail = false

    companion object {
        @SuppressLint("StaticFieldLeak")
        @JvmStatic
        lateinit var instance: App
            private set
    }

    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) return
        instance = this
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun isMainProcess(): Boolean {
        return try {
            val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getProcessName()
            } else {
                val pid = android.os.Process.myPid()
                val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
                manager?.runningAppProcesses?.find { it.pid == pid }?.processName
            }
            processName == packageName
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d(javaClass.name, "onActivityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d(javaClass.name, "onActivityStarted")
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
    }


    private val handler by lazy { Handler(Looper.getMainLooper()) }
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_START) {
        }
    }


}