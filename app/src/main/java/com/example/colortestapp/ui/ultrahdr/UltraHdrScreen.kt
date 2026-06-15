package com.example.colortestapp.ui.ultrahdr

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.colortestapp.model.UltraHdrConfig
import com.example.colortestapp.ui.common.OptionsPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.colortestapp.ui.theme.ElevatedSurface
import com.example.colortestapp.ui.theme.Gray400
import com.example.colortestapp.ui.theme.PurpleAccent
import com.example.colortestapp.ui.theme.White
import com.example.colortestapp.ui.theme.YellowAccent

@Composable
fun UltraHdrScreen(
    viewModel: UltraHdrViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    var showOptions by remember { mutableStateOf(false) }
    var dragHandled by remember { mutableStateOf(false) }
    val stepDef = viewModel.currentStepDef()

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val widthPx = with(density) { configuration.screenWidthDp.dp.toPx().toInt() }
    val heightPx = with(density) { configuration.screenHeightDp.dp.toPx().toInt() }

    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()

    // 所有灰阶都带 gainmap → 始终 HDR 窗口模式
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        LaunchedEffect(Unit) {
            activity.window.colorMode = ActivityInfo.COLOR_MODE_HDR
        }
        DisposableEffect(Unit) {
            onDispose {
                activity.window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
            }
        }
    }

    // 生成当前灰阶的 Ultra HDR Bitmap
    // 直接用 config.stepIndex 作为 key，确保状态变化时立即重新生成
    val stepIndex = config.stepIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { showOptions = true },
                    onTap = { if (showOptions) showOptions = false }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragHandled = false },
                    onDragEnd = { dragHandled = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (!dragHandled && kotlin.math.abs(dragAmount) > 60) {
                            dragHandled = true
                            val idx = config.stepIndex
                            if (dragAmount < 0 && idx < viewModel.steps.size - 1) {
                                viewModel.updateConfig(UltraHdrConfig(stepIndex = idx + 1))
                            } else if (dragAmount > 0 && idx > 0) {
                                viewModel.updateConfig(UltraHdrConfig(stepIndex = idx - 1))
                            }
                        }
                    }
                )
            }
    ) {
        // 官方 Ultra HDR 显示路径: Gainmap Bitmap → ImageView → hardware Canvas 自动应用
        val bitmap = remember(stepIndex, widthPx, heightPx) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                viewModel.generateBitmap(widthPx, heightPx) else null
        }
        if (bitmap != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.FIT_XY
                        // 强制硬件加速，确保 gainmap 被 Canvas 处理
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    }
                },
                update = { (it as android.widget.ImageView).setImageBitmap(bitmap) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
        }

        // 选项面板
        OptionsPanel(
            visible = showOptions,
            onDismiss = { showOptions = false }
        ) {
            UltraHdrOptionsContent(
                stepDef = stepDef,
                stepCount = viewModel.steps.size,
                onSelectStep = { idx ->
                    viewModel.updateConfig(UltraHdrConfig(stepIndex = idx))
                    showOptions = false
                },
                onSave = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val file = viewModel.saveStep(widthPx, heightPx)
                            withContext(Dispatchers.Main) {
                                if (file != null) {
                                    Toast.makeText(activity, "已保存 ${file.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(activity, "保存失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                onSaveAll = {
                    scope.launch {
                        Toast.makeText(activity, "正在保存 21 张...", Toast.LENGTH_SHORT).show()
                        withContext(Dispatchers.IO) {
                            val files = viewModel.saveAllSteps(widthPx, heightPx)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(activity, "已保存 ${files.size}/21 张到相册", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun UltraHdrOptionsContent(
    stepDef: StepDef,
    stepCount: Int,
    onSelectStep: (Int) -> Unit,
    onSave: () -> Unit,
    onSaveAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "Ultra HDR 灰阶测试",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 当前灰阶信息
        Text(
            text = "当前: #${stepDef.index}  ${stepDef.label}",
            style = MaterialTheme.typography.titleMedium,
            color = YellowAccent,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "SDR码值: ${stepDef.sdrCode} / 255  ·  增益: 4.0×  ·  Ultra HDR",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 快速跳转按钮：每行7个
        val chunked = (0 until stepCount).chunked(7)
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { idx ->
                    val isSelected = idx == stepDef.index
                    Button(
                        onClick = { onSelectStep(idx) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) {
                                PurpleAccent
                            } else {
                                ElevatedSurface
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "$idx",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) White else Gray400
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = YellowAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("保存当前", style = MaterialTheme.typography.labelLarge, color = Color.Black)
            }
            Button(
                onClick = onSaveAll,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("保存全部 21 张", style = MaterialTheme.typography.labelLarge, color = White)
            }
        }
    }
}
