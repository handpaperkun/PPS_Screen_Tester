package com.example.colortestapp.ui.signal

import android.content.Context
import android.opengl.EGL14
import android.opengl.GLES30
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * SurfaceView + GL 线程，将 EGL Surface 标为 DISPLAY_P3_PASSTHROUGH。
 * GPU 驱动直接解释内容为 P3 色彩空间。
 */
class P3GlView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "P3GlView"
        // EGL 扩展常量
        private const val EGL_GL_COLORSPACE_KHR = 0x309D
        private const val EGL_GL_COLORSPACE_DISPLAY_P3_PASSTHROUGH_EXT = 0x3490
    }

    data class RenderData(
        val r: Float = 1f, val g: Float = 1f, val b: Float = 1f,
        val bgR: Float = 0f, val bgG: Float = 0f, val bgB: Float = 0f,
        val wx: Float = 0f, val wy: Float = 0f, val ww: Float = 1f, val wh: Float = 1f
    )

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

    // ── GL 线程 ────────────────────────────────────────

    private inner class GlThread(
        private val holder: SurfaceHolder,
        private val running: AtomicBoolean
    ) : Thread("P3GlThread") {

        override fun run() {
            val dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            EGL14.eglInitialize(dpy, IntArray(2), 0, IntArray(2), 1)

            val config = choose(dpy)
            val ctx = EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0)

            // EGL Surface 标 P3
            val surf = EGL14.eglCreateWindowSurface(dpy, config, holder.surface,
                intArrayOf(EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_DISPLAY_P3_PASSTHROUGH_EXT, EGL14.EGL_NONE), 0)
            EGL14.eglMakeCurrent(dpy, surf, surf, ctx)

            while (running.get()) {
                val d = renderData.get()
                val sw = surfaceW.coerceAtLeast(1); val sh = surfaceH.coerceAtLeast(1)
                GLES30.glViewport(0, 0, sw, sh)
                GLES30.glScissor(0, 0, sw, sh); GLES30.glEnable(GLES30.GL_SCISSOR_TEST)
                // 背景
                GLES30.glClearColor(d.bgR, d.bgG, d.bgB, 1f)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                // 窗口
                val wl = (d.wx * sw).toInt().coerceIn(0, sw)
                val wt = (d.wy * sh).toInt().coerceIn(0, sh)
                val wr = ((d.wx + d.ww) * sw).toInt().coerceIn(wl, sw)
                val wb = ((d.wy + d.wh) * sh).toInt().coerceIn(wt, sh)
                GLES30.glScissor(wl, sh - wb, wr - wl, wb - wt)
                GLES30.glClearColor(d.r, d.g, d.b, 1f)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                GLES30.glDisable(GLES30.GL_SCISSOR_TEST)
                EGL14.eglSwapBuffers(dpy, surf)

                synchronized(this@P3GlView) {
                    try { (this@P3GlView as Object).wait(33) } catch (_: Exception) {}
                }
            }

            EGL14.eglDestroySurface(dpy, surf)
            EGL14.eglDestroyContext(dpy, ctx)
            EGL14.eglTerminate(dpy)
        }

        private fun choose(dpy: android.opengl.EGLDisplay): android.opengl.EGLConfig {
            val attribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, 0x0040,
                EGL14.EGL_NONE)
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val num = IntArray(1)
            EGL14.eglChooseConfig(dpy, attribs, 0, configs, 0, 1, num, 0)
            return configs[0]!!
        }
    }
}
