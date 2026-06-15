package com.example.colortestapp.ui.ultrahdr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Gainmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.example.colortestapp.model.UltraHdrConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class UltraHdrViewModel : ViewModel() {

    val steps: List<StepDef> = run {
        (0..20).map { i ->
            val code = (i * 255f / 20f).toInt()
            StepDef(index = i, sdrCode = code, label = when {
                i == 0 -> "黑 G0"; i == 10 -> "灰 G128"; i == 20 -> "白 G255"; else -> "G$code"
            })
        }
    }

    private val _config = MutableStateFlow(UltraHdrConfig())
    val config: StateFlow<UltraHdrConfig> = _config

    fun updateConfig(newConfig: UltraHdrConfig) { _config.value = newConfig }
    fun currentStepDef(): StepDef = steps[_config.value.stepIndex]

    /**
     * 生成指定灰阶的 Ultra HDR Bitmap。
     * 底图: sRGB 编码 (JPEG 默认 EOTF)。
     * 增益图: 4x 全白，gamma 1.0 线性。
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun generateBitmap(width: Int, height: Int, code: Int = currentStepDef().sdrCode): Bitmap {
        val boost = 4.0f

        val base = Bitmap.createBitmap(
            width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888
        )
        Canvas(base).drawColor(android.graphics.Color.rgb(code, code, code))
        val gmBmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            .also { it.setPixel(0, 0, android.graphics.Color.WHITE) }
        base.setGainmap(Gainmap(gmBmp).apply {
            setRatioMin(boost, boost, boost); setRatioMax(boost, boost, boost)
            setGamma(1f, 1f, 1f)
            displayRatioForFullHdr = boost
            minDisplayRatioForHdrTransition = 1.0f
        })
        return base
    }

    /**
     * 保存当前灰阶为 Ultra HDR JPEG。
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun saveStep(width: Int, height: Int): File? {
        return try {
            val code = currentStepDef().sdrCode
            val file = fileForCode(code)
            encodeToFile(width, height, code, file)
            Log.d("UHD", "saved ${file.name} (${file.length()} bytes)")
            file
        } catch (e: Exception) {
            Log.e("UHD", "saveStep error", e)
            null
        }
    }

    /**
     * 保存全部 21 级灰阶到相册。
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun saveAllSteps(width: Int, height: Int): List<File> {
        val saved = mutableListOf<File>()
        for (step in steps) {
            try {
                val file = fileForCode(step.sdrCode)
                encodeToFile(width, height, step.sdrCode, file)
                saved.add(file)
            } catch (e: Exception) {
                Log.e("UHD", "saveAll fail at G${step.sdrCode}", e)
            }
        }
        Log.d("UHD", "saveAll done: ${saved.size}/${steps.size} files")
        return saved
    }

    private fun fileForCode(code: Int): File {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File(dir, "PPSTest_UHD_G$code.jpg")
    }

    private fun encodeToFile(width: Int, height: Int, code: Int, file: File) {
        val bitmap = generateBitmap(width, height, code)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        bitmap.recycle()
    }
}

data class StepDef(val index: Int, val sdrCode: Int, val label: String)
