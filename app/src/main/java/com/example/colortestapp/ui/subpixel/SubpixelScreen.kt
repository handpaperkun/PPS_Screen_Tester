package com.example.colortestapp.ui.subpixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val PIXEL_FONT: Map<Char, List<Pair<Int, Int>>> = mapOf(
    'P' to listOf(0 to 0,1 to 0,2 to 0, 0 to 1,3 to 1, 0 to 2,3 to 2, 0 to 3,1 to 3,2 to 3, 0 to 4, 0 to 5, 0 to 6),
    'S' to listOf(1 to 0,2 to 0, 0 to 1,3 to 1, 1 to 2,2 to 2, 3 to 3, 0 to 4, 1 to 5,2 to 5),
    'D' to listOf(0 to 0,1 to 0,2 to 0, 0 to 1,3 to 1, 0 to 2,3 to 2, 0 to 3,3 to 3, 0 to 4,1 to 4,2 to 4),
    'A' to listOf(1 to 0,2 to 0, 0 to 1,3 to 1, 0 to 2,1 to 2,2 to 2,3 to 2, 0 to 3,3 to 3, 0 to 4,3 to 4, 0 to 5,3 to 5, 0 to 6,3 to 6),
    'H' to listOf(0 to 0,3 to 0, 0 to 1,3 to 1, 0 to 2,3 to 2, 0 to 3,1 to 3,2 to 3,3 to 3, 0 to 4,3 to 4, 0 to 5,3 to 5, 0 to 6,3 to 6),
    'I' to listOf(0 to 0,1 to 0,2 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4, 1 to 5, 0 to 6,1 to 6,2 to 6),
)

@Composable
fun SubpixelScreen(onNavigateBack: () -> Unit) {
    val density = LocalDensity.current.density

    fun pxToDp(px: Float): Dp = (px / density).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LineGroups()
                Spacer(Modifier.height(pxToDp(3f)))
                StarTriple()
                Spacer(Modifier.height(pxToDp(3f)))
                PixelText("PPS DASHI")
            }
        }
    }
}

// ── 第一排: 竖3 / 横3 / 斜3 ────────────────────────────

@Composable
private fun LineGroups() {
    val density = LocalDensity.current.density
    val SP = 3   // 3px 间距

    // 总宽(px): 竖3条 + 横3条 + 斜3条, 各占一列区域
    val totalPx = (SP * 2) + SP + (SP * 2) + SP + (SP * 3)
    val maxH = 8  // 像素高度

    Canvas(Modifier.size((totalPx.toFloat() / density).dp, (maxH.toFloat() / density).dp)) {
        val c = Color.White

        fun vLine(x: Float) = drawLine(c, Offset(x, 0.5f), Offset(x, maxH - 0.5f), 1f)
        fun hLine(x0: Float, y: Float, len: Float = 5f) =
            drawLine(c, Offset(x0, y), Offset(x0 + len, y), 1f)
        fun dLine(x0: Float, y0: Float, len: Float = 5f) =
            drawLine(c, Offset(x0, y0), Offset(x0 + len, y0 + len), 1f)

        // 像素中心对齐
        fun px(v: Int) = v + 0.5f

        var x = 0f

        // 竖线组: 3 条竖线, 水平排列, 间距 3px
        for (i in 0..2) { vLine(px(i * SP)); x = (i * SP).toFloat() + 1f }
        x = 3f * SP + SP  // 跳过竖线组 (3*3+3=12px) → 下一组起始

        // 横线组: 3 条横线, 竖直排列, 间距 3px
        for (i in 0..2) { hLine(px(x.toInt()), px(i * SP)) }
        x += SP * 2 + SP  // 跳过横线组宽度

        // 斜线组: 3 条斜线, 水平偏移, 间距 3px
        for (i in 0..2) { dLine(px(x.toInt() + i * SP), 0f, 5f) }
    }
}

// ── 第二排: 3 个米字 ──────────────────────────────────

@Composable
private fun StarTriple() {
    val density = LocalDensity.current.density
    val r = 4f; val dia = r * 2 + 1; val gap = 3f
    val totalPx = dia * 3 + gap * 2 + 2

    Canvas(Modifier.size((totalPx / density).dp, (dia / density).dp)) {
        fun star(cx: Int, cy: Int) {
            for (i in 0..7) {
                val dx = cos(i * Math.PI / 4.0)
                val dy = sin(i * Math.PI / 4.0)
                for (step in 1..4) {
                    val x = (cx + step * dx).roundToInt()
                    val y = (cy + step * dy).roundToInt()
                    drawRect(Color.White, Offset(x.toFloat(), y.toFloat()), Size(1f, 1f))
                }
            }
        }
        val c0 = (r + 1f).roundToInt()
        val offset = (dia + gap).roundToInt()
        star(c0, c0)
        star(c0 + offset, c0)
        star(c0 + offset * 2, c0)
    }
}

// ── 第三排: 像素文字 "PPS DASHI" ──────────────────────

@Composable
private fun PixelText(text: String) {
    val charW = 5; val charH = 7; val charGap = 2; val spaceGap = 3
    val density = LocalDensity.current.density

    var totalW = 0
    for (ch in text) {
        if (ch == ' ') totalW += spaceGap else totalW += charW + charGap
    }
    totalW = totalW - charGap + 2  // 末尾无尾随间距 + padding
    val totalH = charH + 2

    Canvas(Modifier.size((totalW.toFloat() / density).dp, (totalH.toFloat() / density).dp)) {
        var x = 1
        for (ch in text) {
            if (ch == ' ') { x += spaceGap; continue }
            for ((gx, gy) in (PIXEL_FONT[ch] ?: emptyList())) {
                drawRect(Color.White, Offset((x + gx).toFloat(), gy.toFloat()), Size(1f, 1f))
            }
            x += charW + charGap
        }
    }
}
