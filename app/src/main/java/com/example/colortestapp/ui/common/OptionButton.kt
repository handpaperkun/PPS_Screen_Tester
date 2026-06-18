package com.example.colortestapp.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MD3 FilterChip 封装，保持与原 OptionButton 相同的调用签名。
 * 用于单选选项场景（灰阶选择、APL 值、速度选择等）。
 */
@Composable
fun OptionButton(
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    height: Dp = 40.dp,
    cornerRadius: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(height),
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = accent.copy(alpha = 0.18f),
            selectedLabelColor = selectedContentColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = accent.copy(alpha = 0.5f),
            enabled = true,
            selected = selected
        ),
        label = content
    )
}
