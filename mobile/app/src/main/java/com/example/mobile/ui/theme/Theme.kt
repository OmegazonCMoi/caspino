package com.example.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.AccentGreen
import com.example.mobile.ui.theme.AccentPurple
import com.example.mobile.ui.theme.AccentRed
import com.example.mobile.ui.theme.DarkBackground
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkBorderVariant
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlue.copy(alpha = 0.2f),
    onPrimaryContainer = AccentBlue,
    
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = AccentPurple.copy(alpha = 0.2f),
    onSecondaryContainer = AccentPurple,
    
    tertiary = AccentGreen,
    onTertiary = Color.White,
    tertiaryContainer = AccentGreen.copy(alpha = 0.2f),
    onTertiaryContainer = AccentGreen,
    
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    
    outline = DarkBorder,
    outlineVariant = DarkBorderVariant,
    
    error = AccentRed,
    onError = Color.White,
    errorContainer = AccentRed.copy(alpha = 0.2f),
    onErrorContainer = AccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun MobileTheme(
    darkTheme: Boolean = true, // Forcé en dark mode pour le style Apple/shadcn
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Désactivé pour utiliser notre thème personnalisé
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