package com.example.colortestapp.ui.uniformity

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.colortestapp.model.UniformityConfig
import com.example.colortestapp.ui.common.OptionButton
import com.example.colortestapp.ui.common.Md3BottomSheet
import com.example.colortestapp.ui.theme.PurpleAccent
import com.example.colortestapp.ui.theme.YellowAccent

@Composable
fun UniformityScreen(
    viewModel: UniformityViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    var showOptions by remember { mutableStateOf(false) }
    val grayLevels = listOf(255, 38, 17, 10)
    var currentIndex by remember { mutableIntStateOf(grayLevels.indexOf(config.grayLevel)) }
    var dragHandled by remember { mutableStateOf(false) }

    val grayColor = Color(
        red = config.grayLevel / 255f,
        green = config.grayLevel / 255f,
        blue = config.grayLevel / 255f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(grayColor)
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
                            if (dragAmount < 0 && currentIndex < grayLevels.size - 1) {
                                currentIndex++
                                viewModel.updateConfig(UniformityConfig(grayLevel = grayLevels[currentIndex]))
                            } else if (dragAmount > 0 && currentIndex > 0) {
                                currentIndex--
                                viewModel.updateConfig(UniformityConfig(grayLevel = grayLevels[currentIndex]))
                            }
                        }
                    }
                )
            }
    ) {
        Md3BottomSheet(
            visible = showOptions,
            onDismiss = { showOptions = false }
        ) {
            UniformityOptionsContent(
                config = config,
                onConfigChange = { viewModel.updateConfig(it) }
            )
        }
    }
}

@Composable
private fun UniformityOptionsContent(
    config: UniformityConfig,
    onConfigChange: (UniformityConfig) -> Unit
) {
    val grayLevels = listOf(255, 38, 17, 10)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "均匀度测试选项",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "当前灰阶: G${config.grayLevel}",
            color = YellowAccent,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            grayLevels.forEach { gray ->
                OptionButton(
                    selected = config.grayLevel == gray,
                    onClick = { onConfigChange(UniformityConfig(grayLevel = gray)) },
                    accent = PurpleAccent,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "G$gray",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
