package com.example.colortestapp.ui.ultrahdr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Gainmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.example.colortestapp.model.UltraHdrConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow

class UltraHdrViewModel : ViewModel() {

    // 21 点 SDR 灰阶（码值 0~255），均匀分布，附加均匀 gainmap
    val steps: List<StepDef> = run {
        (0..20).map { i ->
            val code = (i * 255f / 20f).toInt()
            StepDef(
                index = i,
                sdrCode = code,
                label = when {
                    i == 0 -> "黑 G0"
                    i == 10 -> "灰 G128"
                    i == 20 -> "白 G255"
                    else -> "G$code"
                }
            )
        }
    }

    private val _config = MutableStateFlow(UltraHdrConfig())
    val config: StateFlow<UltraHdrConfig> = _config

    fun updateConfig(newConfig: UltraHdrConfig) {
        _config.value = newConfig
    }

    fun currentStepDef(): StepDef = steps[_config.value.stepIndex]

    /**
     * 生成 Ultra HDR 灰阶测试图。
     * SDR 基底 = sRGB 编码的灰阶码值 + 1×1 全白增益图（G=1.0，满增强）。
     * 增益倍率 4.0× = SDR 白 → 812 nit（10000 nit PQ 参考域）。
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun generateBitmap(width: Int, height: Int): Bitmap {
        val code = currentStepDef().sdrCode
        val boost = 4.0f

        // SDR 基底：sRGB 灰阶
        val base = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(base).drawColor(android.graphics.Color.rgb(code, code, code))

        // 1×1 全白增益图（G=1.0 → 满增强）
        val gmBmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).also {
            it.setPixel(0, 0, android.graphics.Color.WHITE)
        }

        val gainmap = Gainmap(gmBmp).apply {
            setRatioMin(boost, boost, boost)
            setRatioMax(boost, boost, boost)
            setGamma(1f, 1f, 1f)
            setEpsilonSdr(0.003f, 0.003f, 0.003f)
            setEpsilonHdr(0.003f, 0.003f, 0.003f)
            displayRatioForFullHdr = boost
            minDisplayRatioForHdrTransition = 1.0f
        }
        base.setGainmap(gainmap)
        return base
    }

    companion object {
        fun sdrCodeToLinear(code: Int): Float {
            val f = code / 255f
            return if (f <= 0.04045f) f / 12.92f
            else ((f + 0.055f) / 1.055f).pow(2.4f)
        }
    }
}

data class StepDef(
    val index: Int,
    val sdrCode: Int,
    val label: String
)
