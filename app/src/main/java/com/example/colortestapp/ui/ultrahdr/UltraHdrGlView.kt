package com.example.colortestapp.ui.ultrahdr

import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SurfaceView + EGL FP16 — 用 HardwareBuffer RGBA_FP16 渲染 >1.0 HDR 值。
 * Compose Image 只能用 AB24 (8-bit)，值被截断到 [0,1]。
 */
class UltraHdrGlView(
    context: android.content.Context,
    private val onError: (String) -> Unit = {}
) : SurfaceView(context), SurfaceHolder.Callback {

    private var glThread: GlThread? = null
    private val running = AtomicBoolean(false)

    /** 0..1 归一化的 SDR 码值 + gainmap 增强。r>1 表示 HDR 亮度。 */
    var renderR: Float = 0f; var renderG: Float = 0f; var renderB: Float = 0f

    init { holder.addCallback(this) }

    override fun surfaceCreated(h: SurfaceHolder) { running.set(true); glThread = GlThread(h, running).apply { start() } }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) {}
    override fun surfaceDestroyed(h: SurfaceHolder) { running.set(false); glThread = null }

    private inner class GlThread(
        private val holder: SurfaceHolder,
        private val r: AtomicBoolean
    ) : Thread("UltraHdrGL") {

        override fun run() {
            val dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            EGL14.eglInitialize(dpy, IntArray(2), 0, IntArray(2), 1)
            // 请求 FP16 EGL config
            val cfg = choose(dpy)
            val ctx = EGL14.eglCreateContext(dpy, cfg, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0)
            // EGL surface 标 BT2020_PQ_EXT = 0x3340
            val surf = EGL14.eglCreateWindowSurface(dpy, cfg, holder.surface,
                intArrayOf(0x309D /*EGL_GL_COLORSPACE_KHR*/, 0x3340 /*BT2020_PQ_EXT*/, EGL14.EGL_NONE), 0)
            EGL14.eglMakeCurrent(dpy, surf, surf, ctx)

            while (r.get()) {
                // 直接用 glClearColor 渲染 >1.0 的线性值到 FP16 表面
                GLES20.glViewport(0, 0, width, height)
                GLES20.glClearColor(renderR, renderG, renderB, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                EGL14.eglSwapBuffers(dpy, surf)
                try { sleep(33) } catch (_: Exception) {}
            }

            EGL14.eglDestroySurface(dpy, surf)
            EGL14.eglDestroyContext(dpy, ctx)
            EGL14.eglTerminate(dpy)
        }

        private fun choose(dpy: android.opengl.EGLDisplay): android.opengl.EGLConfig {
            val cfgs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val num = IntArray(1)
            // FP16
            EGL14.eglChooseConfig(dpy, intArrayOf(
                EGL14.EGL_RED_SIZE, 16, EGL14.EGL_GREEN_SIZE, 16,
                EGL14.EGL_BLUE_SIZE, 16, EGL14.EGL_ALPHA_SIZE, 16,
                EGL14.EGL_RENDERABLE_TYPE, 0x0040, EGL14.EGL_NONE), 0, cfgs, 0, 1, num, 0)
            if (num[0] > 0) return cfgs[0]!!
            // 10-bit
            EGL14.eglChooseConfig(dpy, intArrayOf(
                EGL14.EGL_RED_SIZE, 10, EGL14.EGL_GREEN_SIZE, 10,
                EGL14.EGL_BLUE_SIZE, 10, EGL14.EGL_ALPHA_SIZE, 2,
                EGL14.EGL_RENDERABLE_TYPE, 0x0040, EGL14.EGL_NONE), 0, cfgs, 0, 1, num, 0)
            if (num[0] > 0) return cfgs[0]!!
            // fallback
            EGL14.eglChooseConfig(dpy, intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, 0x0040, EGL14.EGL_NONE), 0, cfgs, 0, 1, num, 0)
            return cfgs[0]!!
        }
    }
}
