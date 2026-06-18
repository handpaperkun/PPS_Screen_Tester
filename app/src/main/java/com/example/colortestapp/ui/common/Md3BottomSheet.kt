package com.example.colortestapp.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MD3 ModalBottomSheet 封装，保持与原 OptionsPanel 相同的调用签名。
 *
 * @param visible 控制底部工作表的显示/隐藏
 * @param onDismiss 关闭回调
 * @param content 工作表内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Md3BottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (visible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 0.dp,
            scrimColor = Color.Black.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
