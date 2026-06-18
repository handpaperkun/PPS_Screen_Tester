package com.example.colortestapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // Chips, snackbars
    small = RoundedCornerShape(8.dp),         // Text fields, menus
    medium = RoundedCornerShape(12.dp),       // Cards
    large = RoundedCornerShape(16.dp),        // FABs, navigation drawer
    extraLarge = RoundedCornerShape(28.dp)    // Dialogs, bottom sheets
)
