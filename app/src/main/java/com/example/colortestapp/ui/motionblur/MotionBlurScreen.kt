package com.example.colortestapp.ui.motionblur

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.colortestapp.model.FontColorMode
import com.example.colortestapp.model.MotionBlurConfig
import com.example.colortestapp.ui.common.OptionButton
import com.example.colortestapp.ui.common.Md3BottomSheet
import com.example.colortestapp.ui.theme.GreenAccent
import kotlinx.coroutines.isActive

@Composable
fun MotionBlurScreen(
    viewModel: MotionBlurViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    var showOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.rgb(config.backgroundGray, config.backgroundGray, config.backgroundGray)))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { showOptions = true },
                    onTap = { if (showOptions) showOptions = false }
                )
            }
    ) {
        ScrollingTexts(config = config)

        Md3BottomSheet(
            visible = showOptions,
            onDismiss = { showOptions = false }
        ) {
            MotionBlurOptionsContent(
                config = config,
                onConfigChange = { viewModel.updateConfig(it) }
            )
        }
    }
}

@Composable
private fun ScrollingTexts(config: MotionBlurConfig) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val shortSidePx = minOf(screenWidthPx, screenHeightPx)

    // 基于 1080p 分辨率的速度倍率：高分辨率屏文字移动更快以保持视觉一致
    val resolutionFactor = shortSidePx / 1080f
    val scrollSpeedPx = config.scrollSpeed * resolutionFactor

    val textSegments: List<Pair<String, Int>> = listOf(
        "【255】穷玩组穷玩组穷玩组穷玩组穷玩组-用于检测显示器拖影表现，通过不同灰度文字的移动轨迹来评估面板响应时间与过冲控制能力。请仔细观察文字移动时的边缘清晰度与拖尾程度，优质面板应保持画面干净利落。" to 255,
        "【200】穷玩组穷玩组穷玩组穷玩组穷玩组-拖影体验——良好的面板应保持文字边缘锐利无重影拖尾现象，文字移动过程中不应出现色彩分离或明显残影。此测试可有效评估显示器运动图像响应时间与像素过冲调校水平。" to 200,
        "【128】穷玩组穷玩组穷玩组穷玩组穷玩组-灰阶128测试中低亮度下的拖影检测更为严苛，注意观察文字边缘是否有明显残影或色彩分离现象产生。低灰阶下的拖影表现直接影响日常使用中网页浏览与文档阅读的实际体验。" to 128
    )

    var contentHeightPx by remember { mutableFloatStateOf(screenHeightPx) }
    val animatedOffset = remember { Animatable(screenHeightPx) }

    LaunchedEffect(contentHeightPx, scrollSpeedPx) {
        if (contentHeightPx > 0f) {
            val totalDistance = screenHeightPx + contentHeightPx
            val durationMs = (totalDistance / scrollSpeedPx * 1000).toInt().coerceAtLeast(1000)
            while (isActive) {
                animatedOffset.snapTo(screenHeightPx)
                animatedOffset.animateTo(
                    targetValue = -contentHeightPx,
                    animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
                )
            }
        }
    }

    val fontColor = when (config.fontColorMode) {
        FontColorMode.WHITE -> null
        FontColorMode.RED -> Color.Red
        FontColorMode.GREEN -> Color.Green
        FontColorMode.BLUE -> Color.Blue
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val h = coords.size.height
                if (h > 0) contentHeightPx = h.toFloat()
            }
            .offset(y = with(density) { animatedOffset.value.toDp() }),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        textSegments.forEach { (text: String, grayValue: Int) ->
            val textColor = if (fontColor != null) {
                fontColor.copy(alpha = grayValue / 255f)
            } else {
                Color(grayValue, grayValue, grayValue)
            }

            Text(
                text = text,
                color = textColor,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun MotionBlurOptionsContent(
    config: MotionBlurConfig,
    onConfigChange: (MotionBlurConfig) -> Unit
) {
    val grayLevels = listOf(10, 17, 38, 54, 255)
    val speeds = listOf(960, 1440, 2560, 3840)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "拖影测试选项",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        PanelLabel("背景灰阶")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            grayLevels.forEach { gray ->
                OptionButton(
                    selected = config.backgroundGray == gray,
                    onClick = { onConfigChange(config.copy(backgroundGray = gray)) },
                    accent = GreenAccent,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("G$gray", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PanelLabel("字体颜色")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FontColorMode.entries.forEach { mode ->
                val modeLabel = when (mode) {
                    FontColorMode.WHITE -> "白"
                    FontColorMode.RED -> "红"
                    FontColorMode.GREEN -> "绿"
                    FontColorMode.BLUE -> "蓝"
                }
                val (selectedColor, contentColor) = when (mode) {
                    FontColorMode.WHITE -> (Color(0xFFE0E0E0) to Color.Black)
                    FontColorMode.RED -> (Color.Red to MaterialTheme.colorScheme.onSurface)
                    FontColorMode.GREEN -> (Color.Green to MaterialTheme.colorScheme.onSurface)
                    FontColorMode.BLUE -> (Color.Blue to MaterialTheme.colorScheme.onSurface)
                }
                OptionButton(
                    selected = config.fontColorMode == mode,
                    onClick = { onConfigChange(config.copy(fontColorMode = mode)) },
                    accent = selectedColor,
                    selectedContentColor = contentColor,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(modeLabel, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PanelLabel("滚动速度 (px/s)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            speeds.forEach { speed ->
                OptionButton(
                    selected = config.scrollSpeed == speed,
                    onClick = { onConfigChange(config.copy(scrollSpeed = speed)) },
                    accent = GreenAccent,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("$speed", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
        // 分辨率倍率提示
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val shortSideDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
        val factor = with(density) { shortSideDp.dp.toPx() } / 1080f
        Text(
            text = "分辨率倍率: ${"%.2f".format(factor)}×  (基准 1080p)",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun PanelLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}
