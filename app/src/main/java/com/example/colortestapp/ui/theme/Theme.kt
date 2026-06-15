package com.example.colortestapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = White,
    secondary = GreenAccent,
    onSecondary = White,
    tertiary = OrangeAccent,
    onTertiary = White,
    background = NearBlack,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = Gray400,
    outline = Outline,
    error = RedAccent,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = BlueAccent,
    onPrimary = White,
    secondary = GreenAccent,
    onSecondary = White,
    tertiary = OrangeAccent,
    onTertiary = White,
    background = NearBlack,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = Gray400,
    outline = Outline,
    error = RedAccent,
    onError = White
)

@Composable
fun ColorTESTappTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
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
        content = content
    )
}
