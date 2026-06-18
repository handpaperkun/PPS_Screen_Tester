package com.example.colortestapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = White,
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = GreenAccent,
    onSecondary = White,
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFB8F0BA),
    tertiary = OrangeAccent,
    onTertiary = White,
    tertiaryContainer = Color(0xFFE65100),
    onTertiaryContainer = Color(0xFFFFDCC2),
    error = RedAccent,
    onError = White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF050505),
    surfaceContainerLow = Color(0xFF0F0F0F),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF282828),
    surfaceDim = Color(0xFF0A0A0A),
    surfaceBright = Color(0xFF363636),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFF4A6FA5),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

private val LightColorScheme = lightColorScheme(
    primary = BlueAccent,
    onPrimary = White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = GreenAccent,
    onSecondary = White,
    secondaryContainer = Color(0xFFB8F0BA),
    onSecondaryContainer = Color(0xFF002106),
    tertiary = OrangeAccent,
    onTertiary = White,
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF2B1700),
    error = RedAccent,
    onError = White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3EDF7),
    surfaceContainer = Color(0xFFECE6F0),
    surfaceContainerHigh = Color(0xFFE6E0E9),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    surfaceDim = Color(0xFFDDD8E0),
    surfaceBright = Color(0xFFFFFBFE),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFA0CAFD),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

@Composable
fun ColorTESTappTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
