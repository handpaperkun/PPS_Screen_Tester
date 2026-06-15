package com.example.colortestapp.ui.signal

import android.content.Context
import android.opengl.EGL14
import android.opengl.GLES30
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.pow

/**
 * SurfaceView + GL 线程，EGL Surface 标 BT.2020 PQ。
 * 裸码值 → PQ 逆 EOTF 编码 → glClearColor → FP16 framebuffer → 显示。
 */
class HdrGlView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "HdrGlView"
        private const val EGL_GL_COLORSPACE_KHR = 0x309D
        private const val EGL_GL_COLORSPACE_BT2020_PQ_EXT = 0x3340

        // ST.2084 PQ 逆 EOTF 常量
        private const val M1 = 0.1593017578125f
        private const val M2 = 78.84375f
        private const val C1 = 0.8359375f
        private const val C2 = 18.8515625f
        private const val C3 = 18.6875f

        /** 线性亮度 (0..1 = 0..10000 nit) → PQ 编码值 */
        fun pqInverseEotf(linear: Float): Float {
            val y = linear.coerceIn(0f, 1f)
            val ym1 = y.pow(M1)
            return ((C1 + C2 * ym1) / (1f + C3 * ym1)).pow(M2)
        }
    }

    data class RenderData(val r: Float = 1f, val g: Float = 1f, val b: Float = 1f)

    val renderData = AtomicReference(RenderData())
    private var glThread: GlThread? = null
    private val running = AtomicBoolean(false)
    @Volatile private var surfaceW = 1; @Volatile private var surfaceH = 1

    init { holder.addCallback(this) }

    fun requestRender() { synchronized(this) { (this as Object).notifyAll() } }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running.set(true)
        glThread = GlThread(holder, running).apply { start() }
    }
    override fun surfaceChanged(holder: SurfaceHolder, fmt: Int, w: Int, h: Int) {
        surfaceW = w; surfaceH = h
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running.set(false); requestRender(); glThread = null
    }

    private inner class GlThread(
        private val holder: SurfaceHolder,
        private val running: AtomicBoolean
    ) : Thread("HdrGlThread") {

        override fun run() {
            val dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            EGL14.eglInitialize(dpy, IntArray(2), 0, IntArray(2), 1)

            val config = choose(dpy)
            val ctx = EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0)

            val surf = EGL14.eglCreateWindowSurface(dpy, config, holder.surface,
                intArrayOf(EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_PQ_EXT, EGL14.EGL_NONE), 0)
            EGL14.eglMakeCurrent(dpy, surf, surf, ctx)

            while (running.get()) {
                val d = renderData.get()
                GLES30.glViewport(0, 0, surfaceW.coerceAtLeast(1), surfaceH.coerceAtLeast(1))
                // 裸码值 → PQ 编码
                GLES30.glClearColor(pqInverseEotf(d.r), pqInverseEotf(d.g), pqInverseEotf(d.b), 1f)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                EGL14.eglSwapBuffers(dpy, surf)

                synchronized(this@HdrGlView) {
                    try { (this@HdrGlView as Object).wait(33) } catch (_: Exception) {}
                }
            }

            EGL14.eglDestroySurface(dpy, surf)
            EGL14.eglDestroyContext(dpy, ctx)
            EGL14.eglTerminate(dpy)
        }

        private fun choose(dpy: android.opengl.EGLDisplay): android.opengl.EGLConfig {
            // FP16
            val fp16 = intArrayOf(
                EGL14.EGL_RED_SIZE, 16, EGL14.EGL_GREEN_SIZE, 16,
                EGL14.EGL_BLUE_SIZE, 16, EGL14.EGL_ALPHA_SIZE, 16,
                EGL14.EGL_RENDERABLE_TYPE, 0x0040, EGL14.EGL_NONE)
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val num = IntArray(1)
            EGL14.eglChooseConfig(dpy, fp16, 0, configs, 0, 1, num, 0)
            if (num[0] > 0) { Log.d(TAG, "FP16 EGL config"); return configs[0]!! }
            // 回退 RGBA8
            Log.w(TAG, "FP16 not available, RGBA8")
            val rg8 = intArrayOf(EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, 0x0040, EGL14.EGL_NONE)
            EGL14.eglChooseConfig(dpy, rg8, 0, configs, 0, 1, num, 0)
            return configs[0]!!
        }
    }
}
