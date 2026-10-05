package com.golfgod.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

val GolfGreen = Color(0xFF2ECC71)
val GolfGold = Color(0xFFD4A017)
val DarkBg = Color(0xFF0A0E27)
val SurfaceDark = Color(0xFF161B40)
val ErrorRed = Color(0xFFE74C3C)
val TextMuted = Color(0xFFB0B8D1)

val WearColorPalette: Colors = Colors(
    primary = GolfGold,
    primaryVariant = Color(0xFFB3860F),
    secondary = GolfGreen,
    secondaryVariant = Color(0xFF27AE60),
    error = ErrorRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onError = Color.White,
    background = DarkBg,
    surface = SurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White
)
