package com.fishlog.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LakeBlue,
    secondary = DarkSteel,
    tertiary = Aqua,
    background = DeepOcean,
    surface = NightBlue,
    onPrimary = SurfaceWhite,
    onSecondary = SurfaceWhite,
    onTertiary = SurfaceWhite,
    onBackground = OffWhite,
    onSurface = OffWhite,
    primaryContainer = DeepLakeBlue,
    onPrimaryContainer = SurfaceWhite
)

private val LightColorScheme = lightColorScheme(
    primary = DeepLakeBlue,
    secondary = LakeBlue,
    tertiary = Aqua,
    background = OffWhite,
    surface = SurfaceWhite,
    surfaceVariant = Mist,
    onPrimary = SurfaceWhite,
    onSecondary = SurfaceWhite,
    onTertiary = SurfaceWhite,
    onBackground = DeepLakeBlue,
    onSurface = DeepLakeBlue,
    primaryContainer = SkyBlue,
    onPrimaryContainer = DeepLakeBlue
)

@Composable
fun FishLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

