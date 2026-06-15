package com.example.colortestapp.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun OptionsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val density = LocalDensity.current
    val cornerShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + slideInVertically { -it },
            exit = fadeOut(tween(200)) + slideOutVertically { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onDismiss)
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(350)) + slideInVertically { -it },
            exit = fadeOut(tween(250)) + slideOutVertically { -it }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.systemBars))
                    .heightIn(max = screenHeightDp * 0.55f)
                    .graphicsLayer {
                        shape = cornerShape
                        clip = true
                        shadowElevation = with(density) { 24.dp.toPx() }
                    }
                    .border(1.dp, Color.White.copy(alpha = 0.08f), cornerShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xE61A1A1A),
                                Color(0xF20D0D0D)
                            )
                        ),
                        cornerShape
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffset < -120f) {
                                    onDismiss()
                                }
                                dragOffset = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragOffset += dragAmount
                            }
                        )
                    }
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .width(48.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )
                content()
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
