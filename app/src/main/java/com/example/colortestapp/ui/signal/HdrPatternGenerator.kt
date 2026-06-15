package com.example.colortestapp.ui.signal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Gainmap
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.colortestapp.model.SignalData

/**
 * 生成 Ultra HDR (gainmap JPEG) 测试图案。
 * 系统合成器自动应用增益图 → 真实 HDR EOTF 输出。
 */
object HdrPatternGenerator {

    /**
     * 根据 CalMAN XML 数据生成 Ultra HDR Bitmap。
     *
     * @param signalData  解析后的 XML 数据（可为 null → 全白画面）
     * @param width       屏幕像素宽
     * @param height      屏幕像素高
     * @param hdrBoost    HDR 亮度倍率（相对 SDR 白，默认 4.0）
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun generate(
        signalData: SignalData?,
        width: Int,
        height: Int,
        hdrBoost: Float = 4.0f
    ): Bitmap {
        val bg = signalData?.background
        val win = signalData?.window

        val bgR = bg?.color?.get(0) ?: 255
        val bgG = bg?.color?.get(1) ?: 255
        val bgB = bg?.color?.get(2) ?: 255

        val winR = win?.color?.get(0) ?: 255
        val winG = win?.color?.get(1) ?: 255
        val winB = win?.color?.get(2) ?: 255

        val posX = win?.position?.get(0) ?: 0f
        val posY = win?.position?.get(1) ?: 0f
        val sizeX = win?.size?.get(0) ?: 1f
        val sizeY = win?.size?.get(1) ?: 1f

        // ── 1. SDR 基底 ──────────────────────────────────
        val base = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(base)
        canvas.drawColor(android.graphics.Color.rgb(bgR, bgG, bgB))

        // 窗口矩形
        val paint = Paint().apply {
            color = android.graphics.Color.rgb(winR, winG, winB)
        }
        canvas.drawRect(
            posX * width, posY * height,
            (posX + sizeX) * width, (posY + sizeY) * height,
            paint
        )

        // ── 2. 增益图（1/4 分辨率）────────────────────────
        val gmW = (width / 4).coerceAtLeast(1)
        val gmH = (height / 4).coerceAtLeast(1)
        val gmBmp = Bitmap.createBitmap(gmW, gmH, Bitmap.Config.ARGB_8888)
        val gmCanvas = Canvas(gmBmp)

        // 背景区域：G=0 → 不增强
        gmCanvas.drawColor(android.graphics.Color.BLACK)

        // 窗口区域：G=1 → 满增强
        val gmPaint = Paint().apply { color = android.graphics.Color.WHITE }
        gmCanvas.drawRect(
            posX * gmW, posY * gmH,
            (posX + sizeX) * gmW, (posY + sizeY) * gmH,
            gmPaint
        )

        // ── 3. 附加增益图 ─────────────────────────────────
        val gainmap = Gainmap(gmBmp).apply {
            setRatioMin(1f, 1f, 1f)
            setRatioMax(hdrBoost, hdrBoost, hdrBoost)
            setGamma(1f, 1f, 1f)
            setEpsilonSdr(0.003f, 0.003f, 0.003f)
            setEpsilonHdr(0.003f, 0.003f, 0.003f)
            displayRatioForFullHdr = hdrBoost
            minDisplayRatioForHdrTransition = 1.0f
        }
        base.setGainmap(gainmap)
        return base
    }
}
