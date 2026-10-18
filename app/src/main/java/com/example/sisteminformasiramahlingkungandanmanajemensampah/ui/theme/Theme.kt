package com.example.sisteminformasiramahlingkungandanmanajemensampah.ui.theme

import android.app.Activity
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

private val LightColorScheme = lightColorScheme(
    primary = SiramButterYellow,
    onPrimary = SiramButterYellowOn,

    primaryContainer = SiramButterYellowLight,
    onPrimaryContainer = SiramButterYellowOn,

    secondary = SiramGreen,
    onSecondary = Color.White,

    secondaryContainer = SiramGreenLight,
    onSecondaryContainer = SiramGreenDark,

    tertiary = SiramPastelCoral,
    onTertiary = SiramPastelCoralOn,

    background = SiramMintBackground,
    onBackground = SiramTextPrimary,

    surface = SiramSurfaceWhite,
    onSurface = SiramTextPrimary,

    surfaceVariant = SiramMintBackgroundLight,
    onSurfaceVariant = SiramTextSecondary,

    error = SiramError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(

    primary = SiramDarkGreen,
    onPrimary = Color(0xFF003912),

    primaryContainer = SiramGreenDark,
    onPrimaryContainer = SiramGreenLight,

    secondary = SiramMint,
    onSecondary = Color(0xFF003915),

    background = SiramDarkBackground,
    onBackground = Color(0xFFE2E3E0),

    surface = SiramDarkSurface,
    onSurface = Color(0xFFE2E3E0),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun SistemInformasiRamahLingkunganDanManajemenSampahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Bisa diset secara dinamis dari MainActivity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // ... sisa kode di bawahnya tetap sama

    val colorScheme = when {

        dynamicColor &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {

            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
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