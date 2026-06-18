package com.example.colortestapp.ui.signal

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.colortestapp.model.ConnectionState
import com.example.colortestapp.model.SignalData
import com.example.colortestapp.ui.common.Md3BottomSheet
import com.example.colortestapp.ui.theme.BlueAccent
import com.example.colortestapp.ui.theme.RedAccent
import kotlinx.coroutines.delay

@Composable
fun SignalScreen(
    viewModel: SignalViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val activity = LocalContext.current as android.app.Activity

    val connectionState by viewModel.connectionState.collectAsState()
    val signalData by viewModel.signalData.collectAsState()
    val config by viewModel.config.collectAsState()
    var showOptions by remember { mutableStateOf(false) }
    var gamut by remember { mutableIntStateOf(0) }
    var showIndicators by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            activity.window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
    }
    LaunchedEffect(config.hdrEnabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            activity.window.colorMode = if (config.hdrEnabled) ActivityInfo.COLOR_MODE_HDR else ActivityInfo.COLOR_MODE_DEFAULT
    }
    LaunchedEffect(gamut) {
        if (!config.hdrEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            activity.window.colorMode = when (gamut) { 1 -> ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT else -> ActivityInfo.COLOR_MODE_DEFAULT }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                activity.window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }
    }

    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures(onDoubleTap = { showOptions = true }, onTap = { if (showOptions) showOptions = false })
    }) {
        SignalCanvas(signalData = signalData, hdrEnabled = config.hdrEnabled, gamut = gamut, showIndicators = showIndicators)

        if (showIndicators) {
            Box(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                ConnectionStatusIndicator(state = connectionState)
            }
        }

        Md3BottomSheet(visible = showOptions, onDismiss = { showOptions = false }) {
            SignalOptionsContent(
                config = config, connectionState = connectionState,
                gamut = gamut, showIndicators = showIndicators,
                onConfigChange = { viewModel.updateConfig(it) },
                onGamutChange = { gamut = it },
                onShowIndicatorsChange = { showIndicators = it },
                onConnect = { viewModel.connect() },
                onDisconnect = { viewModel.disconnect() })
        }
    }
}

// ── SDR 渲染 ──────────────────────────────────────────

@Composable
private fun SdrSignalCanvas(signalData: SignalData?, screenWidth: Float, screenHeight: Float, gamut: Int) {
    val sw = screenWidth.toInt().coerceAtLeast(1); val sh = screenHeight.toInt().coerceAtLeast(1)
    if (gamut == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val ctx = LocalContext.current; val glView = remember { P3GlView(ctx) }
        LaunchedEffect(signalData) {
            val bg = signalData?.background; val win = signalData?.window
            glView.renderData.set(P3GlView.RenderData(
                r = ((win?.color?.get(0) ?: 255) / 255f), g = ((win?.color?.get(1) ?: 255) / 255f), b = ((win?.color?.get(2) ?: 255) / 255f),
                bgR = ((bg?.color?.get(0) ?: 0) / 255f), bgG = ((bg?.color?.get(1) ?: 0) / 255f), bgB = ((bg?.color?.get(2) ?: 0) / 255f),
                wx = win?.position?.get(0) ?: 0f, wy = win?.position?.get(1) ?: 0f,
                ww = win?.size?.get(0) ?: 1f, wh = win?.size?.get(1) ?: 1f))
            glView.requestRender()
        }
        AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())
    } else {
        Canvas(Modifier.fillMaxSize()) {
            if (signalData != null) {
                val bg = signalData.background; val win = signalData.window
                drawRect(Color(bg.color[0]/255f, bg.color[1]/255f, bg.color[2]/255f), Offset.Zero, Size(sw.toFloat(), sh.toFloat()))
                val wx = win.position[0]*sw; val wy = win.position[1]*sh; val ww = win.size[0]*sw; val wh = win.size[1]*sh
                drawRect(Color(win.color[0]/255f, win.color[1]/255f, win.color[2]/255f), Offset(wx, wy), Size(ww, wh))
            } else drawRect(Color.White, Offset.Zero, Size(sw.toFloat(), sh.toFloat()))
        }
    }
}

// ── HDR 渲染 ──────────────────────────────────────────

@Composable
private fun HdrSignalCanvas(signalData: SignalData?, screenWidth: Float, screenHeight: Float, showIndicators: Boolean) {
    val ctx = LocalContext.current; val surfaceView = remember { SurfaceView(ctx) }
    val pipeline = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Hdr10VideoPipeline() else null }
    var ready by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (pipeline == null) "需要 Android 13+" else "等待…") }

    DisposableEffect(Unit) {
        var lastW = 0; var lastH = 0
        val cb = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
                if (w == lastW && h == lastH) return   // 尺寸未变不重启
                lastW = w; lastH = h
                status = "启动…"
                pipeline?.start(holder.surface, w, h)
                ready = pipeline != null
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                lastW = 0; lastH = 0; ready = false; pipeline?.stop()
            }
        }
        surfaceView.holder.addCallback(cb)
        onDispose { surfaceView.holder.removeCallback(cb); pipeline?.stop() }
    }

    LaunchedEffect(signalData, ready) {
        if (!ready) return@LaunchedEffect
        val win = signalData?.window; val bg = signalData?.background
        pipeline?.render(
            winR = win?.color?.get(0) ?: 255, winG = win?.color?.get(1) ?: 255, winB = win?.color?.get(2) ?: 255,
            bgR = bg?.color?.get(0) ?: 0, bgG = bg?.color?.get(1) ?: 0, bgB = bg?.color?.get(2) ?: 0,
            posX = win?.position?.get(0) ?: 0f, posY = win?.position?.get(1) ?: 0f,
            sizeW = win?.size?.get(0) ?: 1f, sizeH = win?.size?.get(1) ?: 1f)
    }

    LaunchedEffect(Unit) { while (true) { pipeline?.statusText?.let { status = it }; delay(500) } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { surfaceView }, modifier = Modifier.fillMaxSize())
        if (showIndicators) {
            Text("HDR: $status", color = Color(0xFFFF5252), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    .background(Color(0xAA000000)).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

// ── 路由 ──────────────────────────────────────────────

@Composable
private fun SignalCanvas(signalData: SignalData?, hdrEnabled: Boolean, gamut: Int, showIndicators: Boolean) {
    val configuration = LocalConfiguration.current; val density = LocalDensity.current
    val sw = with(density) { configuration.screenWidthDp.dp.toPx() }; val sh = with(density) { configuration.screenHeightDp.dp.toPx() }
    if (hdrEnabled) HdrSignalCanvas(signalData, sw, sh, showIndicators)
    else SdrSignalCanvas(signalData, sw, sh, gamut)
}

// ── 连接状态 ──────────────────────────────────────────

@Composable
private fun ConnectionStatusIndicator(state: ConnectionState) {
    val (color, text) = when (state) {
        ConnectionState.CONNECTED -> BlueAccent to "已连接"
        ConnectionState.CONNECTING -> Color(0xFFFFC107) to "连接中..."
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline to "未连接"
        ConnectionState.ERROR -> RedAccent to "错误"
    }
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), MaterialTheme.shapes.large)
            .border(1.dp, color.copy(alpha = 0.5f), MaterialTheme.shapes.large).padding(horizontal = 12.dp, vertical = 6.dp)
    ) { Box(Modifier.size(10.dp).background(color, CircleShape)); Spacer(Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge) }
}

// ── 选项面板 ──────────────────────────────────────────

@Composable
private fun SignalOptionsContent(
    config: com.example.colortestapp.model.SignalConfig, connectionState: ConnectionState,
    gamut: Int, showIndicators: Boolean,
    onConfigChange: (com.example.colortestapp.model.SignalConfig) -> Unit,
    onGamutChange: (Int) -> Unit, onShowIndicatorsChange: (Boolean) -> Unit,
    onConnect: () -> Unit, onDisconnect: () -> Unit
) {
    val connected = connectionState == ConnectionState.CONNECTED
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text("画面信号发生器", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 4.dp))
        Text("兼容 CM/CS Resolve XML 协议", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
        OutlinedTextField(value = config.ipAddress, onValueChange = { onConfigChange(config.copy(ipAddress = it)) },
            label = { Text("服务器IP地址", color = MaterialTheme.colorScheme.onSurfaceVariant) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = BlueAccent, unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedLabelColor = BlueAccent, unfocusedLabelColor = MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text(if (config.hdrEnabled) "色域（HDR 固定 BT.2020）" else "色域",
            style = MaterialTheme.typography.bodyLarge, color = if (config.hdrEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("sRGB" to 0, "P3" to 1).forEach { (label, idx) ->
                Button(onClick = { if (!config.hdrEnabled) onGamutChange(idx) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (gamut == idx) BlueAccent else MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.small, enabled = !config.hdrEnabled, modifier = Modifier.weight(1f)
                ) { Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("显示状态指示器", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = showIndicators, onCheckedChange = onShowIndicatorsChange,
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = BlueAccent, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedTrackColor = MaterialTheme.colorScheme.outline))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("HDR模式", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = config.hdrEnabled, onCheckedChange = { onConfigChange(config.copy(hdrEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = BlueAccent, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedTrackColor = MaterialTheme.colorScheme.outline))
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { if (connected) onDisconnect() else onConnect() },
            colors = ButtonDefaults.buttonColors(containerColor = if (connected) RedAccent else BlueAccent, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = MaterialTheme.shapes.large, contentPadding = PaddingValues(vertical = 14.dp), modifier = Modifier.fillMaxWidth()
        ) { Text(if (connected) "断开连接" else "连接服务器", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) }
    }
}
