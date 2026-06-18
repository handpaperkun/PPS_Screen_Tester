package com.example.colortestapp.ui.motion

import android.view.SurfaceView
import android.view.WindowManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.colortestapp.ui.common.Md3BottomSheet

val GRAY_LEVELS = listOf(17, 38, 72, 128, 165)

@Composable
fun MotionScreen(
    vm: MotionViewModel = viewModel(),
    onBack: () -> Unit
) {
    val act = LocalContext.current as android.app.Activity
    val state by vm.state.collectAsState()

    var menu by remember { mutableStateOf(false) }
    var grayIdx by remember { mutableIntStateOf(0) }
    var showOverlay by remember { mutableStateOf(true) }
    val bgColor = Color(GRAY_LEVELS[grayIdx].let { (it shl 16) or (it shl 8) or it or (0xFF shl 24) })

    val sv = remember { SurfaceView(act).apply { setZOrderOnTop(false); visibility = android.view.View.INVISIBLE } }
    DisposableEffect(Unit) {
        act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        vm.init(act); vm.attachSurfaceView(sv)
        vm.startFpsMonitor(); vm.requestMax()
        onDispose { vm.detachSurfaceView(); vm.stopFpsMonitor(); act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Box(Modifier.fillMaxSize().background(bgColor)) {
        if (showOverlay) {
            // ── 中心大号指示器 ──
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.width(320.dp).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 36.dp, vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(88.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (state.pressing) "高刷" else "60Hz",
                                color = MaterialTheme.colorScheme.onSurface, fontSize = 76.sp, fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace, lineHeight = 72.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            String.format("%3d FPS", state.currentFps.toInt()),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), fontSize = 28.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            // ── 右上角小号指示器 ──
            Box(Modifier.fillMaxSize().padding(top = 10.dp, end = 12.dp), contentAlignment = Alignment.TopEnd) {
                Text(
                    String.format("%d FPS", state.currentFps.toInt()),
                    color = if (grayIdx >= 3) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp, fontFamily = FontFamily.Monospace
                )
            }
        }

        // 手势
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(
                onPress = { vm.setPressing(true); vm.requestMax(); tryAwaitRelease(); vm.setPressing(false); vm.request60() },
                onDoubleTap = { menu = true }
            )
        })

        // 菜单
        Md3BottomSheet(visible = menu, onDismiss = { menu = false }) {
            Column {
                Text("FPS Flicker", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("背景灰阶", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GRAY_LEVELS.forEachIndexed { i, gv ->
                        val sel = i == grayIdx
                        val gColor = Color((gv shl 16) or (gv shl 8) or gv or (0xFF shl 24))
                        Box(
                            Modifier.clip(MaterialTheme.shapes.small).background(gColor).background(if (sel) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else Color.Transparent)
                                .clickable { grayIdx = i }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G$gv", color = if (gv > 128) Color.Black else Color.White,
                                style = MaterialTheme.typography.labelLarge, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .clickable { showOverlay = !showOverlay }.padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center) {
                    Text("显示 " + if (showOverlay) "●" else "○", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { menu = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    modifier = Modifier.height(40.dp)
                ) { Text("返回", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
