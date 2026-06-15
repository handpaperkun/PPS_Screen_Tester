package com.example.colortestapp.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.colortestapp.ui.theme.ElevatedSurface
import com.example.colortestapp.ui.theme.Outline
import com.example.colortestapp.ui.theme.White

@Composable
fun OptionButton(
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    selectedContentColor: Color = White,
    height: Dp = 44.dp,
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.then(Modifier.height(height)),
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accent else ElevatedSurface,
            contentColor = if (selected) selectedContentColor else White
        ),
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.8f) else Outline
        ),
        contentPadding = contentPadding
    ) {
        content()
    }
}
