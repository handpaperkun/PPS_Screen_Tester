package com.example.colortestapp.ui.hdrscan

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.colortestapp.network.HdrScanSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HdrScanScreen(
    viewModel: HdrScanViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as Activity
    var showChart by remember { mutableStateOf(false) }

    // 扫描完成 → 清除亮度覆写 + 跳结果
    LaunchedEffect(state.running, state.completed) {
        if (!state.running && state.completed && state.points.isNotEmpty()) {
            // 清除窗口亮度覆写，恢复系统亮度
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            showChart = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 确保亮度覆写被清除
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                activity.window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (showChart && state.completed) {
        LaunchedEffect(Unit) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                activity.window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }
        ChartResultScreen(state, activity, onChartAction = { action ->
            when (action) {
                "retest" -> { viewModel.reset(); showChart = false }
                "back" -> { viewModel.reset(); onNavigateBack() }
            }
        })
    } else if (state.running) {
        LaunchedEffect(Unit) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                activity.window.colorMode = ActivityInfo.COLOR_MODE_HDR
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        ScanningScreen(state, viewModel, activity)
    } else {
        LaunchedEffect(Unit) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                activity.window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }
        IdleScreen(viewModel = viewModel)
    }
}

@Composable
private fun IdleScreen(viewModel: HdrScanViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
            .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 标题区
        Box(
            Modifier
                .size(72.dp)
                .background(Color(0xFF4CAF50).copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "☀",
                fontSize = 36.sp,
                color = Color(0xFF4CAF50),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "HDR 倍率扫描",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "自动控制亮度 2% → 100%\n系统 API 读取屏幕 HDR/SDR 倍率\n10% APL · 20步 · 每步1.5s · 延迟5s\n左半4X/4X/4X · 右半1X/2X/3X 增益图",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(32.dp))

        // 准备提示
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "测试前准备",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "请先将手机关闭自动亮度、屏幕亮度调至最低（最好为最低亮度），再进入测试。",
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // 步骤指南
        StepItem(number = "1", title = "关闭自动亮度", desc = "避免系统自行改变屏幕亮度")
        Spacer(Modifier.height(12.dp))
        StepItem(number = "2", title = "调至最低亮度", desc = "将亮度滑块拖到最低")
        Spacer(Modifier.height(12.dp))
        StepItem(number = "3", title = "点击开始扫描", desc = "进入测试后系统会自动拉升亮度")

        Spacer(Modifier.height(40.dp))

        // 开始按钮
        Button(
            onClick = { viewModel.startScan() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("开始扫描", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepItem(number: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ScanningScreen(
    state: HdrScanViewModel.ScanState,
    viewModel: HdrScanViewModel,
    activity: Activity
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val wPx = with(density) { config.screenWidthDp.dp.toPx().toInt() }
    val hPx = with(density) { config.screenHeightDp.dp.toPx().toInt() }
    val bitmap = remember(state.currentStep, wPx, hPx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            viewModel.generateUltraHdrBitmap(wPx, hPx) else null
    }

    // 自动扫描亮度 + 读取系统 HDR 倍率
    LaunchedEffect(state.running) {
        if (!state.running) return@LaunchedEffect
        val dm = activity.getSystemService(DisplayManager::class.java)
        val attrs = activity.window.attributes
        attrs.screenBrightness = viewModel.getCurrentBrightness()
        activity.window.attributes = attrs
        delay(5000)  // 等待屏幕亮度响应

        while (viewModel.isRunning()) {
            delay(400)
            // 用系统 API 读取屏幕 HDR/SDR 倍率
            val ratio = queryHdrSdrRatio(dm)
            val hasMore = viewModel.advance(ratio)
            if (!hasMore) break
            attrs.screenBrightness = viewModel.getCurrentBrightness()
            activity.window.attributes = attrs
            delay(1100)
        }
    }

    val progress = (state.currentStep + 1).coerceAtMost(state.totalSteps) / state.totalSteps.toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.weight(1f)) {
            if (bitmap != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "HDR",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
        }

        // 状态面板
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout))
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("扫描进度", color = Color.Gray, fontSize = 13.sp)
                Text(
                    "${state.currentStep + 1} / ${state.totalSteps}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFF333333),
            )

            Spacer(Modifier.height(18.dp))

            Text("当前亮度", color = Color.Gray, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${(state.currentBrightness * 100).toInt()}",
                    color = Color(0xFF4CAF50),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "%",
                    color = Color(0xFF4CAF50),
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 6.dp, start = 3.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFF4CAF50),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "正在采集 HDR/SDR 倍率…",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * 查询系统 API 获取屏幕 HDR/SDR 倍率。
 * API 34+: Display.getHdrSdrRatio()
 * API 30+: Display.HdrCapabilities 计算 maxLumi/minLumi 比率
 * 不支持: 返回 4.0X 作为占位 (Ultra HDR gain)
 */
private fun queryHdrSdrRatio(displayManager: DisplayManager?): Float {
    displayManager ?: return 4.0f
    return try {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+: 直接获取
            val r = display.hdrSdrRatio
            if (r > 1.0f) r else 4.0f
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: 用 HdrCapabilities 估算
            val caps = display.hdrCapabilities
            val maxLumi = caps.desiredMaxLuminance  // cd/m²
            val minLumi = caps.desiredMinLuminance   // cd/m²
            val avgLumi = caps.desiredMaxAverageLuminance // cd/m²
            // SDR 白约 100 nits → HDR/SDR = maxLumi/100
            (maxLumi / 100f).coerceIn(1f, 20f)
        } else {
            4.0f
        }
    } catch (e: Exception) {
        4.0f
    }
}

@Composable
private fun ChartResultScreen(
    state: HdrScanViewModel.ScanState,
    activity: Activity,
    onChartAction: (String) -> Unit
) {
    val points = state.points
    val brightnessPct = points.map { (it.brightness * 100f).coerceIn(0f, 100f) }
    val ratios = points.map { it.hdrRatio }
    val context = activity
    val scope = CoroutineScope(Dispatchers.Main)

    // 发送到上位机
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            HdrScanSender(activity).start(points)
        }
    }

    val maxRatio = (ratios.maxOrNull() ?: 5f) * 1.15f
    val yLabels = (5 downTo 0).map { String.format("%.1fX", maxRatio * it / 5f) }
    val xLabels = listOf("2%", "20%", "40%", "60%", "80%", "100%")

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
            .padding(16.dp)
    ) {
        // 标题区
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "HDR 倍率曲线",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "系统 API 实测 · 亮度 2% → 100% · 10% APL",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 图表卡片
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Text(
                    "HDR 倍率 vs 屏幕亮度 (%)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))

                Row(Modifier.weight(1f).fillMaxWidth()) {
                    // Y 轴标签
                    Column(
                        Modifier
                            .width(46.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        yLabels.forEach { lbl ->
                            Text(
                                lbl,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // 绘图区
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            ChartCanvas(
                                brightnessPct = brightnessPct,
                                ratios = ratios,
                                maxY = maxRatio,
                                modifier = Modifier.fillMaxSize()
                            )
                            DataPointLabels(
                                brightnessPct = brightnessPct,
                                ratios = ratios,
                                maxY = maxRatio,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // X 轴标签
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            xLabels.forEach { lbl ->
                                Text(lbl, color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                        Text(
                            "屏幕亮度 (%)",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 按钮区
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onChartAction("retest") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("重新测试", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        saveChartToGallery(context, brightnessPct, ratios, maxRatio)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text("↓", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(5.dp))
                Text("保存图", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onChartAction("back") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("返回主页", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ChartCanvas(
    brightnessPct: List<Float>,
    ratios: List<Float>,
    maxY: Float,
    modifier: Modifier = Modifier
) {
    if (brightnessPct.isEmpty() || ratios.isEmpty()) return
    Canvas(modifier) {
        val cw = size.width
        val ch = size.height
        val maxX = 100f

        // 网格线
        for (i in 0..5) {
            val y = ch * i / 5f
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(0f, y),
                end = Offset(cw, y),
                strokeWidth = 1f
            )
        }
        for (x in listOf(2f, 20f, 40f, 60f, 80f, 100f)) {
            val px = cw * x / maxX
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(px, 0f),
                end = Offset(px, ch),
                strokeWidth = 1f
            )
        }

        // 坐标轴
        drawLine(
            color = Color(0x55FFFFFF),
            start = Offset(0f, ch),
            end = Offset(cw, ch),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color(0x55FFFFFF),
            start = Offset(0f, 0f),
            end = Offset(0f, ch),
            strokeWidth = 1.5f
        )

        // 填充区域
        val fillPath = Path().apply {
            ratios.forEachIndexed { i, r ->
                val px = cw * brightnessPct[i] / maxX
                val py = ch * (1f - (r / maxY).coerceIn(0f, 1f))
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            lineTo(cw, ch)
            lineTo(0f, ch)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x334CAF50), Color(0x054CAF50)),
                startY = 0f,
                endY = ch
            )
        )

        // 折线
        val linePath = Path()
        ratios.forEachIndexed { i, r ->
            val px = cw * brightnessPct[i] / maxX
            val py = ch * (1f - (r / maxY).coerceIn(0f, 1f))
            if (i == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
        }
        drawPath(
            path = linePath,
            color = Color(0xFF4CAF50),
            style = Stroke(width = 3f)
        )

        // 数据点
        ratios.forEachIndexed { i, r ->
            val px = cw * brightnessPct[i] / maxX
            val py = ch * (1f - (r / maxY).coerceIn(0f, 1f))
            drawCircle(color = Color(0xFF4CAF50), radius = 4f, center = Offset(px, py))
            drawCircle(color = Color.White, radius = 1.8f, center = Offset(px, py))
        }
    }
}

@Composable
private fun DataPointLabels(
    brightnessPct: List<Float>,
    ratios: List<Float>,
    maxY: Float,
    modifier: Modifier = Modifier
) {
    if (ratios.isEmpty()) return
    val peakIndex = ratios.indices.maxByOrNull { ratios[it] } ?: return
    val lastIndex = ratios.lastIndex
    val labeled = listOf(peakIndex, lastIndex).distinct().sorted()

    BoxWithConstraints(modifier) {
        val maxW = maxWidth
        val maxH = maxHeight
        labeled.forEach { i ->
            val x = brightnessPct[i] / 100f
            val y = 1f - (ratios[i] / maxY).coerceIn(0f, 1f)
            val isPeak = i == peakIndex
            Text(
                text = String.format("%.1fX", ratios[i]),
                color = if (isPeak) Color(0xFF81C784) else Color(0xFFB0B0B0),
                fontSize = 9.sp,
                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.offset(
                    x = maxW * x - 12.dp,
                    y = maxH * y - 16.dp
                )
            )
        }
    }
}

// ==================== 保存到相册 ====================
@SuppressLint("NewApi")
private fun saveChartToGallery(
    context: android.content.Context, brightnessPct: List<Float>, ratios: List<Float>, maxY: Float
) {
    if (brightnessPct.isEmpty()) return
    val W = 2560; val H = 1440  // 16:9
    val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(bmp)

    // ---- 全局背景 ----
    cv.drawColor(android.graphics.Color.rgb(26, 26, 26))

    // ---- 标题区 ----
    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 56f; isFakeBoldText = true; isAntiAlias = true
    }
    val subtitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY; textSize = 34f; isAntiAlias = true
    }
    val margin = 60f
    cv.drawText("HDR 倍率曲线", margin, margin + titlePaint.textSize - 8f, titlePaint)
    cv.drawText("系统 API 实测 · 亮度 2% → 100% · 10% APL", margin, margin + titlePaint.textSize + 34f, subtitlePaint)

    val cardTop = margin + titlePaint.textSize + subtitlePaint.textSize + 26f

    // ---- 图表卡片背景 ----
    val cardPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(34, 34, 34)
        style = android.graphics.Paint.Style.FILL; isAntiAlias = true
    }
    val cardPad = 40f
    val cardBottom = H - margin - 48f
    cv.drawRoundRect(margin, cardTop, W - margin, cardBottom, 24f, 24f, cardPaint)

    // 卡片内边距
    val innerL = margin + cardPad; val innerT = cardTop + cardPad
    val innerR = W - margin - cardPad; val innerB = cardBottom - cardPad
    val innerW = innerR - innerL; val innerH = innerB - innerT

    // ---- 卡片内标题 ----
    val cardTitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 36f; isFakeBoldText = true; isAntiAlias = true
    }
    cv.drawText("HDR 倍率", innerL, innerT + cardTitlePaint.textSize, cardTitlePaint)

    val chartTop = innerT + cardTitlePaint.textSize + 10f
    val chartAreaH = innerB - chartTop

    // ---- 画笔 (数据区) ----
    val yLabelW = 160f  // Y 轴标签宽度
    val chartPadL = yLabelW + 16f
    val chartPadB = 70f   // X 轴标签高度
    val chartPadT = 4f
    val chartL = innerL + chartPadL
    val chartR = innerR - 20f
    val chartT = chartTop + chartPadT
    val chartB = innerB - chartPadB
    val chartW = chartR - chartL; val chartH = chartB - chartT
    val maxX = 100f

    val textSmall = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY; textSize = 30f; isAntiAlias = true
    }
    val textLabel = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(200, 176, 176, 176); textSize = 28f; isAntiAlias = true
    }
    val textLabelPeak = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(129, 199, 132); textSize = 32f; isAntiAlias = true; isFakeBoldText = true
    }
    val textLabelValley = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(229, 115, 115); textSize = 32f; isAntiAlias = true; isFakeBoldText = true
    }
    val gridPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(34, 255, 255, 255); strokeWidth = 1.8f
    }
    val axisPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(85, 255, 255, 255); strokeWidth = 3f
    }
    val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(76, 175, 80); strokeWidth = 7f
        style = android.graphics.Paint.Style.STROKE; isAntiAlias = true
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    val dotOuter = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(76, 175, 80); style = android.graphics.Paint.Style.FILL; isAntiAlias = true
    }
    val dotInner = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.FILL; isAntiAlias = true
    }
    val markerPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true
    }
    val textAxisLabel = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY; textSize = 36f; isAntiAlias = true
    }

    // ---- 网格 + Y 轴标签 ----
    for (i in 0..5) {
        val y = chartT + chartH * i / 5f
        cv.drawLine(chartL, y, chartR, y, gridPaint)
        val label = String.format("%.1fX", maxY * (5 - i) / 5f)
        cv.drawText(label, innerL + 8f, y + 11f, textSmall)
    }

    // ---- X 轴标签 + 网格 ----
    for (x in listOf(2f, 20f, 40f, 60f, 80f, 100f)) {
        val px = chartL + chartW * x / maxX
        cv.drawLine(px, chartT, px, chartB, gridPaint)
        cv.drawText("${x.toInt()}%", px - 24f, chartB + 42f, textSmall)
    }
    // 坐标轴
    cv.drawLine(chartL, chartB, chartR, chartB, axisPaint)
    cv.drawLine(chartL, chartT, chartL, chartB, axisPaint)
    cv.drawText("屏幕亮度 (%)", chartL + chartW / 2f - 80f, innerB + 6f, textAxisLabel)

    // ---- 数据区 ----
    if (ratios.isNotEmpty()) {
        val pts = FloatArray(ratios.size * 2)
        ratios.forEachIndexed { i, r ->
            pts[i * 2] = chartL + chartW * brightnessPct[i] / maxX
            pts[i * 2 + 1] = chartT + chartH * (1f - (r / maxY).coerceIn(0f, 1f))
        }

        // 渐变填充
        val fillPaint = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.FILL }
        val fillPath = android.graphics.Path().apply {
            moveTo(pts[0], pts[1])
            for (i in 1 until ratios.size) { lineTo(pts[i * 2], pts[i * 2 + 1]) }
            lineTo(pts[(ratios.size - 1) * 2], chartB)
            lineTo(pts[0], chartB)
            close()
        }
        val gradShader = android.graphics.LinearGradient(
            0f, chartT, 0f, chartB,
            intArrayOf(android.graphics.Color.argb(60, 76, 175, 80), android.graphics.Color.argb(5, 76, 175, 80)),
            null, android.graphics.Shader.TileMode.CLAMP
        )
        fillPaint.shader = gradShader
        cv.drawPath(fillPath, fillPaint)

        // 折线
        for (i in 1 until ratios.size) {
            cv.drawLine(pts[(i - 1) * 2], pts[(i - 1) * 2 + 1], pts[i * 2], pts[i * 2 + 1], linePaint)
        }

        // 数据点 (双圈)
        for (i in ratios.indices) {
            cv.drawCircle(pts[i * 2], pts[i * 2 + 1], 10f, dotOuter)
            cv.drawCircle(pts[i * 2], pts[i * 2 + 1], 4.5f, dotInner)
        }

        // ---- 峰值 / 谷值标注 ----
        val peakIdx = ratios.indices.maxByOrNull { ratios[it] } ?: 0
        val valleyIdx = ratios.indices.minByOrNull { ratios[it] } ?: 0

        for ((idx, isPeak, label) in listOf(
            Triple(peakIdx, true, "峰值"),
            Triple(valleyIdx, false, "谷值")
        )) {
            val px = pts[idx * 2]
            val py = pts[idx * 2 + 1]
            val ratioText = String.format("%.1fX", ratios[idx])
            val fullLabel = "$label $ratioText"
            val lp = if (isPeak) textLabelPeak else textLabelValley
            val lw = lp.measureText(fullLabel)
            val lh = lp.textSize

            val dir = if (isPeak) -1f else 1f
            val lx = (px - lw / 2f).coerceIn(20f, W - lw - 20f)
            val ly = py + dir * (28f + lh)

            // 虚线环
            markerPaint.color = if (isPeak) android.graphics.Color.argb(200, 129, 199, 132)
                                else android.graphics.Color.argb(200, 229, 115, 115)
            markerPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 7f), 0f)
            cv.drawCircle(px, py, 16f, markerPaint)
            markerPaint.pathEffect = null

            // 背景 + 文字
            val bgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(190, 40, 40, 40); style = android.graphics.Paint.Style.FILL; isAntiAlias = true
            }
            cv.drawRoundRect(lx - 8f, ly - lh - 4f, lx + lw + 8f, ly + 8f, 8f, 8f, bgPaint)
            cv.drawText(fullLabel, lx, ly, lp)
        }
    }

    // ---- 保存到 MediaStore ----
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "HDR_Scan_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PPSTest")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(it, values, null, null)
            }
        }
        bmp.recycle()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, "已保存到 Pictures/PPSTest", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        bmp.recycle()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
