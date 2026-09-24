package com.professor1416.phittoos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val AvatarColors = listOf(
    Color(0xFF0F766E), // Teal 700
    Color(0xFF1D4ED8), // Blue 700
    Color(0xFF6D28D9), // Purple 700
    Color(0xFFBE185D), // Pink 700
    Color(0xFFC2410C), // Orange 700
    Color(0xFF047857), // Green 700
    Color(0xFF4338CA), // Indigo 700
    Color(0xFF0369A1)  // Sky 700
)

@Composable
fun AvatarInitial(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val initial = name.trim().take(1).uppercase().ifEmpty { "?" }
    val colorIndex = abs(name.hashCode()) % AvatarColors.size
    val bgColor = AvatarColors[colorIndex]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}
