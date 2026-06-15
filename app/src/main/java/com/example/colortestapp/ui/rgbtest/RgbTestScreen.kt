package com.example.colortestapp.ui.rgbtest

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Gainmap
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.colortestapp.ui.common.OptionsPanel
import kotlinx.coroutines.launch

@Composable
fun RgbTestScreen(vm: RgbTestViewModel = viewModel(), onBack: () -> Unit) {
    val act = LocalContext.current as android.app.Activity
    val d = LocalDensity.current; val cfg = LocalConfiguration.current
    val w = with(d) { cfg.screenWidthDp.dp.toPx().toInt() }
    val h = with(d) { cfg.screenHeightDp.dp.toPx().toInt() }
    val hw = w / 2
    val s = rememberCoroutineScope()
    val ctx = LocalContext.current

    var menu by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(RgbTestMode.WIDE_GAMUT) }
    var toggle by remember { mutableStateOf(false) }
    var cmp by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                act.window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }
    }
    LaunchedEffect(mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            act.window.colorMode = when (mode) {
                RgbTestMode.WIDE_GAMUT -> ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
                RgbTestMode.RGB_GAINMAP -> ActivityInfo.COLOR_MODE_HDR
                else -> ActivityInfo.COLOR_MODE_DEFAULT
            }
    }

    val p3Bmp = remember(mode, w, h) { if (mode == RgbTestMode.WIDE_GAMUT) vm.loadAssetBitmap(ctx, "p3_sample.jpg", hw, h) else null }
    val srgbBmp = remember(mode, w, h) { if (mode == RgbTestMode.WIDE_GAMUT) vm.loadAssetBitmap(ctx, "srgb_sample.jpg", hw, h) else null }

    val clipBmp = remember(mode, w, h) {
        if (mode == RgbTestMode.GRAYSCALE_GRADIENT) vm.generateClippingBitmap(w, h) else null
    }

    val hdrBmp = remember(mode, w, h) {
        if (mode == RgbTestMode.RGB_GAINMAP) vm.generateRgbGainmapBitmap(w, h) else null
    }
    val sdrBmp = remember(mode, w, h) {
        if (mode == RgbTestMode.RGB_GAINMAP) vm.generateRgbBaseBitmap(w, h) else null
    }
    val dBmp = when (mode) {
        RgbTestMode.RGB_GAINMAP -> if (cmp && sdrBmp != null) sdrBmp else hdrBmp
        else -> hdrBmp
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // 双图布局 (广色域 / ISO HDR 共用)
        val dual = remember {
            LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL
                addView(ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_XY; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f) })
                addView(ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_XY; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f) }) }
        }
        val showDual = mode == RgbTestMode.WIDE_GAMUT
        AndroidView(factory = { dual }, modifier = Modifier.fillMaxSize(),
            update = { it.visibility = if (showDual) View.VISIBLE else View.GONE
                if (showDual) {
                    val iv1 = it.getChildAt(0) as ImageView
                    val iv2 = it.getChildAt(1) as ImageView
                    when (mode) {
                        RgbTestMode.WIDE_GAMUT -> { iv1.setImageBitmap(p3Bmp); iv2.setImageBitmap(srgbBmp) }
                        else -> {}
                    }
                } })

        if (!showDual) {
            val singleBmp = if (mode == RgbTestMode.GRAYSCALE_GRADIENT) clipBmp else dBmp
            if (singleBmp != null) Image(singleBmp.asImageBitmap(), "", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }

        Box(Modifier.fillMaxSize().pointerInput(mode) {
            detectTapGestures(
                onPress = { if (mode == RgbTestMode.RGB_GAINMAP) { cmp = true; tryAwaitRelease(); cmp = false } },
                onTap = { if (menu) menu = false },
                onDoubleTap = { menu = true }
            )
        })

        Box(Modifier.fillMaxSize().padding(top = 8.dp, end = 8.dp), contentAlignment = Alignment.TopEnd) {
            Text("RGB · " + when (mode) {
                RgbTestMode.WIDE_GAMUT -> "左P3 右sRGB"
                RgbTestMode.RGB_GAINMAP -> if (cmp) "SDR" else "左4X 右1X/2X/3X"
                RgbTestMode.GRAYSCALE_GRADIENT -> "W/R/G/B 0→30"
            }, color = Color(0x66FFFFFF), fontSize = 12.sp)
        }

        OptionsPanel(visible = menu, onDismiss = { menu = false }) {
            Column {
                Text("RGB 测试", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                RgbTestMode.entries.forEach { m ->
                    val sel = m == mode
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Color(0xFF4CAF50).copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { mode = m; vm.currentMode = m; toggle = false; cmp = false; menu = false }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(m.label + if (sel) " ✓" else "",
                            color = if (sel) Color(0xFF4CAF50) else Color(0xBBFFFFFF),
                            fontSize = 16.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        s.launch {
                            when (mode) {
                                RgbTestMode.WIDE_GAMUT -> {
                                    // 保存示例图（屏幕比例拉伸）
                                    p3Bmp?.let { vm.savePng(ctx, hw, h, { _, _ -> it }, "P3示例") }
                                    srgbBmp?.let { vm.savePng(ctx, hw, h, { _, _ -> it }, "sRGB示例") }
                                }
                                RgbTestMode.RGB_GAINMAP -> {
                                    hdrBmp?.let { vm.saveJpegGain(ctx, it, "RGB_GAINMAP_HDR") }
                                    sdrBmp?.let { vm.savePng(ctx, w, h, { _, _ -> it }, "RGB_GAINMAP_SDR") }
                                }
                                RgbTestMode.GRAYSCALE_GRADIENT -> {
                                    clipBmp?.let { vm.saveJpeg(ctx, it, "截断测试") }
                                }
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        modifier = Modifier.height(40.dp)
                    ) { Text("保存", color = Color.White, fontSize = 14.sp) }
                    Button(onClick = { menu = false; onBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.height(40.dp)
                    ) { Text("返回", color = Color.White, fontSize = 14.sp) }
                }
            }
        }
    }
}
