package com.example.colortestapp.ui.apl

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.colortestapp.model.AplConfig
import com.example.colortestapp.ui.common.OptionButton
import com.example.colortestapp.ui.common.Md3BottomSheet
import com.example.colortestapp.ui.theme.OrangeAccent
import com.example.colortestapp.ui.theme.YellowAccent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream

@Composable
fun AplScreen(
    viewModel: AplViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    var showOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val aplValues = listOf(100, 90, 80, 70, 60, 50, 40, 30, 20, 10)
    var currentIndex by remember { mutableIntStateOf(aplValues.indexOf(config.aplValue)) }
    var dragHandled by remember { mutableStateOf(false) }

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
                        if (!dragHandled && kotlin.math.abs(dragAmount) > 80) {
                            dragHandled = true
                            if (dragAmount < 0 && currentIndex < aplValues.size - 1) {
                                currentIndex++
                                viewModel.updateConfig(AplConfig(aplValue = aplValues[currentIndex]))
                            } else if (dragAmount > 0 && currentIndex > 0) {
                                currentIndex--
                                viewModel.updateConfig(AplConfig(aplValue = aplValues[currentIndex]))
                            }
                        }
                    }
                )
            }
    ) {
        AplCanvas(aplValue = config.aplValue)

        Md3BottomSheet(
            visible = showOptions,
            onDismiss = { showOptions = false }
        ) {
            AplOptionsContent(
                config = config,
                onConfigChange = { viewModel.updateConfig(it) },
                onSaveAllScreenshots = {
                    val screenWidth = configuration.screenWidthDp
                    val screenHeight = configuration.screenHeightDp
                    val widthPx = with(density) { screenWidth.dp.toPx().toInt() }
                    val heightPx = with(density) { screenHeight.dp.toPx().toInt() }
                    saveAllAplScreenshots(context, widthPx, heightPx)
                }
            )
        }
    }
}

@Composable
private fun AplCanvas(aplValue: Int) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val scale = kotlin.math.sqrt(aplValue / 100f)
    val windowWidth: Float; val windowHeight: Float
    if (aplValue < 50) {
        // 小 APL：1:1 正方形，面积 = APL% × 总像素
        val side = kotlin.math.sqrt(aplValue / 100f * screenWidth * screenHeight)
        windowWidth = side; windowHeight = side
    } else {
        windowWidth = scale * screenWidth
        windowHeight = scale * screenHeight
    }
    val windowX = (screenWidth - windowWidth) / 2f
    val windowY = (screenHeight - windowHeight) / 2f

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            color = Color.Black,
            topLeft = Offset(0f, 0f),
            size = Size(screenWidth, screenHeight)
        )

        drawRect(
            color = Color.White,
            topLeft = Offset(windowX, windowY),
            size = Size(windowWidth, windowHeight)
        )
    }
}

@Composable
private fun AplOptionsContent(
    config: AplConfig,
    onConfigChange: (AplConfig) -> Unit,
    onSaveAllScreenshots: () -> Unit
) {
    val aplValues = listOf(100, 90, 80, 70, 60, 50, 40, 30, 20, 10)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "APL测试选项",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "当前APL: ${config.aplValue}",
            color = YellowAccent,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 14.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(aplValues) { apl ->
                OptionButton(
                    selected = config.aplValue == apl,
                    onClick = { onConfigChange(AplConfig(aplValue = apl)) },
                    accent = OrangeAccent
                ) {
                    Text(
                        text = "$apl",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSaveAllScreenshots,
            colors = ButtonDefaults.buttonColors(
                containerColor = OrangeAccent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "一键保存全部APL截图",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

private fun saveAllAplScreenshots(context: Context, width: Int, height: Int) {
    val aplValues = listOf(100, 90, 80, 70, 60, 50, 40, 30, 20, 10)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            aplValues.forEach { aplValue ->
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint()

                canvas.drawColor(android.graphics.Color.BLACK)

                val scale = kotlin.math.sqrt(aplValue / 100f)
                val windowWidth: Int; val windowHeight: Int
                if (aplValue < 50) {
                    val side = kotlin.math.sqrt(aplValue / 100f * width * height).toInt()
                    windowWidth = side; windowHeight = side
                } else {
                    windowWidth = (scale * width).toInt()
                    windowHeight = (scale * height).toInt()
                }
                val windowX = (width - windowWidth) / 2
                val windowY = (height - windowHeight) / 2

                paint.color = android.graphics.Color.WHITE
                canvas.drawRect(
                    windowX.toFloat(), windowY.toFloat(),
                    (windowX + windowWidth).toFloat(), (windowY + windowHeight).toFloat(),
                    paint
                )

                val filename = "APL_$aplValue.png"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream: OutputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }

                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
