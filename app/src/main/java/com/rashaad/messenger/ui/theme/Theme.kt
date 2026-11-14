package com.rashaad.messenger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Teal40,
    primaryContainer = TealContainer40,
    secondary = Slate40,
    tertiary = Amber40,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    primaryContainer = TealContainer80,
    secondary = Slate80,
    tertiary = Amber80,
)

/**
 * App theme. Uses Material You dynamic colour on Android 12+ (colours from the wallpaper)
 * and follows the system light/dark setting.
 */
@Composable
fun MessengerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    // Typography uses sp units throughout, so it scales with the user's font-size setting.
    MaterialTheme(colorScheme = colorScheme, typography = MessengerTypography, content = content)
}
