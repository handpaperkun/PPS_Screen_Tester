package com.example.colortestapp.ui.signal

import android.hardware.DataSpace
import android.hardware.HardwareBuffer
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.graphics.lowlatency.BufferInfo
import androidx.graphics.opengl.FrameBuffer
import androidx.graphics.opengl.GLFrameBufferRenderer
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.surface.SurfaceControlCompat
import androidx.hardware.SyncFenceCompat
import kotlin.math.pow

/**
 * HDR 纯色渲染控制器。
 * Google 官方路径：GLFrameBufferRenderer → HardwareBuffer → SurfaceControl + RANGE_EXTENDED.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class HdrController(private val surfaceView: SurfaceView) {

    @Volatile var r: Float = 1f
    @Volatile var g: Float = 1f
    @Volatile var b: Float = 1f

    private val glRenderer = GLRenderer().apply { start() }
    private var fbRenderer: GLFrameBufferRenderer? = null

    private val callback = object : GLFrameBufferRenderer.Callback {
        override fun onDrawFrame(
            eglManager: EGLManager, width: Int, height: Int,
            bufferInfo: BufferInfo, transform: FloatArray
        ) {
            val rr = r; val gg = g; val bb = b
            GLES20.glViewport(0, 0, bufferInfo.width, bufferInfo.height)
            // 裸码值直写 — 测试 HDR 管线 EOTF，不预编码
            GLES20.glClearColor(rr, gg, bb, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }

        override fun onDrawComplete(
            targetSurfaceControl: SurfaceControlCompat,
            transaction: SurfaceControlCompat.Transaction,
            frameBuffer: FrameBuffer,
            syncFence: SyncFenceCompat?
        ) {
            // BT.2020 PQ dataspace — 原生 GENERIC_HDR 通路
            transaction.setDataSpace(targetSurfaceControl, DataSpace.pack(
                DataSpace.STANDARD_BT2020, DataSpace.TRANSFER_ST2084, DataSpace.RANGE_FULL))
        }
    }

    init {
        fbRenderer = try {
            // RGBA_1010102: 10-bit 格式，硬件 overlay 支持，避免 GPU 软件 tone-map
            GLFrameBufferRenderer.Builder(surfaceView, callback)
                .setGLRenderer(glRenderer)
                .setBufferFormat(HardwareBuffer.RGBA_1010102)
                .build()
        } catch (e: Throwable) {
            Log.w("HdrCtrl", "1010102 不支持，回退 FP16: ${e.message}")
            GLFrameBufferRenderer.Builder(surfaceView, callback)
                .setGLRenderer(glRenderer)
                .setBufferFormat(HardwareBuffer.RGBA_FP16)
                .build()
        }
    }

    fun updateColor(rr: Float, gg: Float, bb: Float) {
        r = rr; g = gg; b = bb
        fbRenderer?.render()
    }

    fun onResume() = fbRenderer?.render()

    fun release() {
        fbRenderer?.release(true)
        fbRenderer = null
        glRenderer.stop(true)
    }

    companion object {
        /** SMPTE ST 2084 PQ OETF。L 为归一化亮度[0,1]（1.0=10000nit），返回 PQ 码值[0,1]。 */
        fun pqEncode(l: Float): Float {
            val m1 = 0.1593017578125
            val m2 = 78.84375
            val c1 = 0.8359375
            val c2 = 18.8515625
            val c3 = 18.6875
            val lp = l.coerceIn(0f, 1f).toDouble().pow(m1)
            return ((c1 + c2 * lp) / (1.0 + c3 * lp)).pow(m2).toFloat()
        }
    }
}
