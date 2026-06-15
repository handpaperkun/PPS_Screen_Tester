package com.example.colortestapp.ui.hdrscan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Gainmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HdrScanViewModel : ViewModel() {

    data class ScanPoint(val index: Int, val brightness: Float, val hdrRatio: Float)

    data class ScanState(
        val running: Boolean = false,
        val currentStep: Int = 0,
        val totalSteps: Int = 20,
        val completed: Boolean = false,
        val points: List<ScanPoint> = emptyList(),
        val currentBrightness: Float = 0f
    )

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state

    fun startScan() {
        _state.value = ScanState(running = true, totalSteps = 20, currentBrightness = 0.02f)
    }

    /**
     * @param hdrRatio 系统 API 实测的 HDR/SDR 倍率
     */
    fun advance(hdrRatio: Float = 4.0f): Boolean {
        val s = _state.value
        if (!s.running) return false
        val step = s.currentStep + 1
        if (step >= 20) {
            val lastPt = ScanPoint(step, s.currentBrightness, hdrRatio)
            _state.value = s.copy(currentStep = step, currentBrightness = 1.0f,
                points = s.points + lastPt, running = false, completed = true)
            return false
        }
        val b = (step + 1) / 20f * 0.98f + 0.02f  // 2% -> 100%
        val pt = ScanPoint(step, s.currentBrightness, hdrRatio)
        val pts = s.points + pt
        _state.value = s.copy(currentStep = step, currentBrightness = b, points = pts)
        return true
    }

    fun getCurrentBrightness(): Float = _state.value.currentBrightness
    fun isRunning(): Boolean = _state.value.running
    fun getPoints(): List<ScanPoint> = _state.value.points
    fun getTotalSteps(): Int = _state.value.totalSteps
    fun isCompleted(): Boolean = _state.value.completed

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun generateUltraHdrBitmap(width: Int, height: Int): Bitmap {
        val side = kotlin.math.sqrt(0.1f)
        val pos = (1f - side) / 2f
        val w = width.coerceAtLeast(1); val h = height.coerceAtLeast(1)

        // 底图：10% APL 方块，左半边白色，右半边 RGB 三横条
        val base = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(base)
        cv.drawColor(android.graphics.Color.BLACK)
        val blockL = (pos * w).toInt();      val blockR = ((pos + side) * w).toInt()
        val blockT = (pos * h).toInt();      val blockB = ((pos + side) * h).toInt()
        val midX = (blockL + blockR) / 2

        val p = android.graphics.Paint()
        // 左半边: 白
        p.color = android.graphics.Color.WHITE
        cv.drawRect(blockL.toFloat(), blockT.toFloat(), midX.toFloat(), blockB.toFloat(), p)
        // 右半边: R / G / B 三横条
        val barH = (blockB - blockT) / 3f
        p.color = android.graphics.Color.rgb(255, 0, 0);   cv.drawRect(midX.toFloat(), blockT.toFloat(), blockR.toFloat(), blockT + barH, p)
        p.color = android.graphics.Color.rgb(0, 255, 0);   cv.drawRect(midX.toFloat(), blockT + barH,    blockR.toFloat(), blockT + barH * 2, p)
        p.color = android.graphics.Color.rgb(0, 0, 255);   cv.drawRect(midX.toFloat(), blockT + barH * 2, blockR.toFloat(), blockB.toFloat(), p)

        // 增益图: 同行数同宽，左半边 4X 4X 4X，右半边 1X 2X 3X
        val gmBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        // 左半边: 全白 → max 4X on all channels
        gmBmp.setPixels(
            IntArray((midX - blockL) * (blockB - blockT)) { android.graphics.Color.WHITE },
            0, midX - blockL,
            blockL, blockT, midX - blockL, blockB - blockT
        )
        // 右半边: R=0 (1X), G=85 (2X), B=170 (3X)
        val rightPixels = IntArray((blockR - midX) * (blockB - blockT)) {
            android.graphics.Color.rgb(0, 85, 170)  // R=0→1X, G=85→2X, B=170→3X
        }
        gmBmp.setPixels(
            rightPixels, 0, blockR - midX,
            midX, blockT, blockR - midX, blockB - blockT
        )

        val gainmap = Gainmap(gmBmp).apply {
            setRatioMin(1.0f, 1.0f, 1.0f)
            setRatioMax(4.0f, 4.0f, 4.0f)
            setGamma(1f, 1f, 1f)
            displayRatioForFullHdr = 4.0f
            minDisplayRatioForHdrTransition = 1.0f
        }
        base.setGainmap(gainmap)
        return base
    }

    fun reset() {
        _state.value = ScanState()
    }
}
