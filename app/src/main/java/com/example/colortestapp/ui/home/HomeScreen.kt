package com.example.colortestapp.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.colortestapp.navigation.Screen
import com.example.colortestapp.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit
) {
    val safeAreaInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val features = listOf(
        FeatureItem(
            screen = Screen.SignalReceiver,
            title = "画面信号接收器",
            description = "监听服务器信号，实时显示测试画面",
            color = MaterialTheme.colorScheme.primary
        ),
        FeatureItem(
            screen = Screen.MotionBlur,
            title = "拖影测试",
            description = "测试异色拖影表现与响应速度",
            color = MaterialTheme.colorScheme.secondary
        ),
        FeatureItem(
            screen = Screen.AplTest,
            title = "APL测试",
            description = "测试画面不同APL，支持保存到本地相册",
            color = MaterialTheme.colorScheme.tertiary
        ),
        FeatureItem(
            screen = Screen.Uniformity,
            title = "均匀度测试",
            description = "全屏纯色灰阶测试，检测屏幕均匀性",
            color = MaterialTheme.colorScheme.primaryContainer
        ),
        FeatureItem(
            screen = Screen.UltraHdrTest,
            title = "UltraHDR灰阶测试",
            description = "21点UltraHDR 灰阶，Base 2.2，权当看个激发吧",
            color = MaterialTheme.colorScheme.secondaryContainer
        ),
        FeatureItem(
            screen = Screen.Subpixel,
            title = "次像素渲染测试",
            description = "测试OLED次像素排列",
            color = MaterialTheme.colorScheme.tertiaryContainer
        ),
        FeatureItem(
            screen = Screen.HdrScan,
            title = "HDR倍率扫描",
            description = "自动扫描HDR倍率",
            color = MaterialTheme.colorScheme.error
        ),
        FeatureItem(
            screen = Screen.RgbTest,
            title = "RGB测试",
            description = "杂项测试工具箱",
            color = MaterialTheme.colorScheme.primary
        ),
        FeatureItem(
            screen = Screen.Motion,
            title = "FPS Flicker",
            description = "刷新率切换时是否存在闪烁   (FPS汇报并不准确只做指示）",
            color = MaterialTheme.colorScheme.secondary
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "穷玩组测试工具箱V1.99",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = safeAreaInsets
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = safeAreaInsets
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(features, key = { it.screen.route }) { feature ->
                FeatureCard(
                    feature = feature,
                    onClick = { onNavigate(feature.screen) }
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureItem,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )
    val density = LocalDensity.current
    val cardShape = MaterialTheme.shapes.extraLarge

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shape = cardShape
                clip = true
            },
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (pressed) 2.dp else 6.dp
        ),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 渐变背景点缀
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                feature.color.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            start = Offset.Zero,
                            end = Offset(800f, 800f)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // MD3 图标容器
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(feature.color.copy(alpha = 0.15f))
                        .border(1.5.dp, feature.color.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    FeatureIcon(feature = feature, size = 36.dp)
                }
            }
        }
    }
}

@Composable
private fun FeatureIcon(
    feature: FeatureItem,
    size: androidx.compose.ui.unit.Dp
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val sz = this.size.minDimension
        val strokeWidth = sz * 0.035f
        when (feature.screen) {
            Screen.SignalReceiver -> {
                val inset = sz * 0.1f
                drawRect(
                    color = feature.color.copy(alpha = 0.7f),
                    topLeft = Offset(inset, inset),
                    size = Size(sz - inset * 2, sz - inset * 2),
                    style = Stroke(width = strokeWidth * 1.2f)
                )
                val block = (sz - inset * 2) / 4.5f
                val colors = listOf(Color(0xFFE53935), Color(0xFF43A047), Color(0xFF1E88E5))
                colors.forEachIndexed { i, c ->
                    drawRect(
                        color = c.copy(alpha = 0.85f),
                        topLeft = Offset(inset + block * 0.5f + i * block * 1.15f, inset + block * 0.3f),
                        size = Size(block * 0.8f, block * 0.8f)
                    )
                }
                val cx = sz / 2; val cy = sz / 2
                val crossR = sz * 0.22f
                drawLine(feature.color, Offset(cx - crossR, cy), Offset(cx + crossR, cy), strokeWidth * 0.7f)
                drawLine(feature.color, Offset(cx, cy - crossR), Offset(cx, cy + crossR), strokeWidth * 0.7f)
            }

            Screen.MotionBlur -> {
                val lineCount = 5
                val gap = sz / (lineCount + 1)
                repeat(lineCount) { i ->
                    val y = gap * (i + 1)
                    val alpha = if (i == 2) 1f else 0.5f
                    val width = if (i == 2) strokeWidth * 1.4f else strokeWidth
                    drawLine(
                        color = feature.color.copy(alpha = alpha),
                        start = Offset(sz * 0.15f, y),
                        end = Offset(sz * 0.85f, y),
                        strokeWidth = width
                    )
                }
            }

            Screen.AplTest -> {
                val inset = sz * 0.12f
                drawRect(
                    color = White.copy(alpha = 0.9f),
                    topLeft = Offset(inset, inset),
                    size = Size(sz - inset * 2, sz - inset * 2)
                )
                val innerInset = sz * 0.32f
                drawRect(
                    color = feature.color.copy(alpha = 0.95f),
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(sz - innerInset * 2, sz - innerInset * 2)
                )
            }

            Screen.Uniformity -> {
                val inset = sz * 0.15f
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(
                            feature.color.copy(alpha = 0.8f),
                            White.copy(alpha = 0.6f)
                        )
                    ),
                    topLeft = Offset(inset, inset),
                    size = Size(sz - inset * 2, sz - inset * 2)
                )
            }

            Screen.UltraHdrTest -> {
                val steps = 4
                val w = sz * 0.6f / steps
                val h = sz * 0.7f
                val yBase = sz * 0.15f
                for (i in 0 until steps) {
                    val x = sz * 0.2f + w * i
                    drawRect(
                        color = White.copy(alpha = 0.35f + i * 0.2f),
                        topLeft = Offset(x, yBase),
                        size = Size(w - 1f, h)
                    )
                }
            }

            Screen.Subpixel -> {
                val g = sz / 8f
                for (row in 0..3) for (col in 0..3) {
                    if ((row + col) % 2 == 0) {
                        drawRect(
                            color = feature.color.copy(alpha = 0.8f),
                            topLeft = Offset(sz * 0.2f + col * g, sz * 0.2f + row * g),
                            size = Size(g - 1f, g - 1f)
                        )
                    }
                }
            }

            Screen.RgbTest -> {
                val barH = sz * 0.18f
                val barW = sz * 0.55f
                val startX = sz * 0.22f
                val colors = listOf(
                    Color(0xFFFF4444), Color(0xFF44FF44), Color(0xFF4444FF)
                )
                colors.forEachIndexed { i, c ->
                    val y = sz * 0.22f + i * barH * 1.5f
                    drawRect(color = c, topLeft = Offset(startX, y), size = Size(barW, barH))
                }
                val markX = startX + barW + sz * 0.06f
                drawRect(
                    color = feature.color.copy(alpha = 0.9f),
                    topLeft = Offset(markX, sz * 0.22f),
                    size = Size(sz * 0.12f, barH * 3.5f)
                )
                drawCircle(feature.color, sz * 0.05f, Offset(markX + sz * 0.06f, sz * 0.22f + barH * 1.75f))
            }

            Screen.Motion -> {
                val inset = sz * 0.15f
                val pts = listOf(0.7f, 0.4f, 0.8f, 0.3f, 0.9f, 0.2f, 1.0f, 0.35f, 0.85f)
                for (i in 0 until pts.size - 1) {
                    val x1 = inset + (sz - inset * 2) * i / (pts.size - 1f)
                    val x2 = inset + (sz - inset * 2) * (i + 1) / (pts.size - 1f)
                    val y1 = sz - inset - (pts[i] * (sz - inset * 2))
                    val y2 = sz - inset - (pts[i + 1] * (sz - inset * 2))
                    drawLine(feature.color, Offset(x1, y1), Offset(x2, y2), strokeWidth * 1.5f)
                }
            }

            else -> { // Screen.HdrScan
                val inset = sz * 0.18f
                val cw = sz - inset * 2; val ch = sz - inset * 2
                drawLine(
                    color = feature.color.copy(alpha = 0.55f),
                    start = Offset(inset, sz - inset),
                    end = Offset(inset + cw, sz - inset),
                    strokeWidth = strokeWidth * 0.8f
                )
                drawLine(
                    color = feature.color.copy(alpha = 0.55f),
                    start = Offset(inset, inset),
                    end = Offset(inset, sz - inset),
                    strokeWidth = strokeWidth * 0.8f
                )
                val barW = cw / 7f
                val gap = barW * 0.6f
                val heights = listOf(0.35f, 0.55f, 0.7f, 0.9f)
                heights.forEachIndexed { i, h ->
                    val barH = ch * h
                    val x = inset + 0.7f * barW + i * (barW + gap)
                    val y = sz - inset - barH
                    drawRect(
                        color = feature.color.copy(alpha = 0.35f + i * 0.15f),
                        topLeft = Offset(x, y),
                        size = Size(barW, barH)
                    )
                }
                val lastX = inset + 0.7f * barW + 3 * (barW + gap) + barW / 2
                val lastY = sz - inset - ch * 0.9f
                drawCircle(
                    color = feature.color.copy(alpha = 0.95f),
                    radius = strokeWidth * 1.5f,
                    center = Offset(lastX, lastY)
                )
            }
        }
    }
}

private data class FeatureItem(
    val screen: Screen,
    val title: String,
    val description: String,
    val color: Color
)
