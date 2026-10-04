package com.fuxizhong.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FuxiColorScheme = lightColorScheme(
    primary = CinnabarRed,
    onPrimary = PaperWhite,
    secondary = InkDeep,
    onSecondary = PaperWhite,
    background = PaperCream,
    onBackground = InkDeep,
    surface = PaperCard,
    onSurface = InkDeep,
    surfaceVariant = PaperWarm,
    onSurfaceVariant = InkMedium,
    outline = BorderWarm
)

@Composable
fun FuxiZhongTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PaperCream.toArgb()
            window.navigationBarColor = PaperCream.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = FuxiColorScheme,
        content = content
    )
}
