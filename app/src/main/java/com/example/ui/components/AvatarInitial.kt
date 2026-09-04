package com.example.ui.components

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
    Color(0xFF0D9488), // Teal
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFFF97316), // Orange
    Color(0xFF10B981), // Green
    Color(0xFF6366F1), // Indigo
    Color(0xFF14B8A6)  // Cyan
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
