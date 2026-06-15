package com.example.colortestapp.ui.motion

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Display
import android.view.Surface
import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MotionState(
    val currentFps: Float = 60f,
    val requestedRate: Float = 60f,
    val pressing: Boolean = false,
    val deviation: Float = 0f
)

class MotionViewModel : ViewModel() {
    private val _state = MutableStateFlow(MotionState())
    val state: StateFlow<MotionState> = _state

    private var choreographer: Choreographer? = null
    private var frameCallback: Choreographer.FrameCallback? = null
    private var dm: DisplayManager? = null
    private var displayListener: DisplayManager.DisplayListener? = null
    private var anchorView: SurfaceView? = null

    fun init(act: Activity) {
        dm = act.getSystemService(DisplayManager::class.java)
        val d = act.windowManager.defaultDisplay
        // 只用于 max 查询，不显示
        displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) {}
            override fun onDisplayRemoved(id: Int) {}
            override fun onDisplayChanged(id: Int) {}
        }
        dm?.registerDisplayListener(displayListener!!, Handler(Looper.getMainLooper()))
    }

    fun attachSurfaceView(sv: SurfaceView) { anchorView = sv }
    fun detachSurfaceView() { anchorView = null }

    fun startFpsMonitor() {
        choreographer = Choreographer.getInstance()
        var lastNs = 0L; var minInterval = Long.MAX_VALUE; var frameCount = 0; var windowStart = 0L
        val cb = object : Choreographer.FrameCallback {
            override fun doFrame(ns: Long) {
                if (lastNs > 0L) {
                    val interval = ns - lastNs
                    if (interval < minInterval && interval > 0) minInterval = interval
                }
                lastNs = ns
                if (windowStart == 0L) windowStart = ns
                frameCount++
                val elapsed = (ns - windowStart) / 1e9f
                if (elapsed >= 0.25f) {
                    // 用最短帧间隔反算真实刷新率
                    val actualHz = if (minInterval < Long.MAX_VALUE) 1e9f / minInterval else 60f
                    val dev = kotlin.math.abs(actualHz - _state.value.requestedRate)
                    _state.value = _state.value.copy(currentFps = actualHz, deviation = dev)
                    frameCount = 0; windowStart = ns; minInterval = Long.MAX_VALUE
                }
                choreographer?.postFrameCallback(this)
            }
        }
        frameCallback = cb; choreographer?.postFrameCallback(cb)
    }

    fun stopFpsMonitor() {
        frameCallback?.let { choreographer?.removeFrameCallback(it) }; frameCallback = null; choreographer = null
        displayListener?.let { dm?.unregisterDisplayListener(it) }; displayListener = null; dm = null
    }

    fun requestFrameRate(targetHz: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            anchorView?.holder?.surface?.let { if (it.isValid) it.setFrameRate(targetHz, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT, Surface.CHANGE_FRAME_RATE_ALWAYS) }
        }
        _state.value = _state.value.copy(requestedRate = targetHz)
    }

    fun requestMax() {
        val max = dm?.getDisplay(Display.DEFAULT_DISPLAY)?.supportedModes?.maxOfOrNull { it.refreshRate } ?: 120f
        requestFrameRate(max)
    }
    fun request60() { requestFrameRate(60f) }
    fun setPressing(p: Boolean) { _state.value = _state.value.copy(pressing = p) }
}
