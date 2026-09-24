package com.professor1416.phittoos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primary Brand - Slate palette
val Slate950 = Color(0xFF090D16)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

// Green for money owed to the user (LENT / Positive / Settled)
val EmeraldGreen = Color(0xFF10B981)
val EmeraldGreenDark = Color(0xFF047857)
val EmeraldGreenLight = Color(0xFFD1FAE5)
val EmeraldGreenSurface = Color(0xFFF0FDF4)

// Orange/Red for money the user owes (BORROWED / Negative)
val CoralOrange = Color(0xFFF97316)
val CoralOrangeDark = Color(0xFFC2410C)
val CoralOrangeLight = Color(0xFFFFEDD5)
val CoralOrangeSurface = Color(0xFFFFF7ED)

// Red for overdue
val Red600 = Color(0xFFDC2626)
val Red100 = Color(0xFFFEE2E2)
val Red900 = Color(0xFF7F1D1D)
val Red300 = Color(0xFFFCA5A5)

// Amber for partial payments
val Amber700 = Color(0xFFB45309)
val Amber100 = Color(0xFFFEF3C7)
val Amber900 = Color(0xFF78350F)
val Amber300 = Color(0xFFFCD34D)

// Accent / Brand
val BrandTeal = Color(0xFF0D9488)
val BrandTealLight = Color(0xFFCCFBF1)
val BrandIndigo = Color(0xFF4F46E5)

/**
 * Semantic Financial State Colors that adapt smoothly across Light and Dark themes
 */
data class FinancialColorScheme(
    val lentContainer: Color,
    val onLentContainer: Color,
    val lentAccent: Color,
    val borrowedContainer: Color,
    val onBorrowedContainer: Color,
    val borrowedAccent: Color,
    val settledContainer: Color,
    val onSettledContainer: Color,
    val overdueContainer: Color,
    val onOverdueContainer: Color,
    val overdueAccent: Color,
    val partialContainer: Color,
    val onPartialContainer: Color,
    val cardBackground: Color,
    val chipBackground: Color
)

val LightFinancialColors = FinancialColorScheme(
    lentContainer = Color(0xFFDCFCE7),
    onLentContainer = Color(0xFF065F46),
    lentAccent = Color(0xFF047857),
    borrowedContainer = Color(0xFFFFEDD5),
    onBorrowedContainer = Color(0xFF9A3412),
    borrowedAccent = Color(0xFFC2410C),
    settledContainer = Color(0xFFDCFCE7),
    onSettledContainer = Color(0xFF065F46),
    overdueContainer = Color(0xFFFEE2E2),
    onOverdueContainer = Color(0xFF991B1B),
    overdueAccent = Color(0xFFDC2626),
    partialContainer = Color(0xFFFEF3C7),
    onPartialContainer = Color(0xFF92400E),
    cardBackground = Color.White,
    chipBackground = Color(0xFFF1F5F9)
)

val DarkFinancialColors = FinancialColorScheme(
    lentContainer = Color(0xFF064E3B),
    onLentContainer = Color(0xFF6EE7B7),
    lentAccent = Color(0xFF34D399),
    borrowedContainer = Color(0xFF7C2D12),
    onBorrowedContainer = Color(0xFFFDBA74),
    borrowedAccent = Color(0xFFFB923C),
    settledContainer = Color(0xFF064E3B),
    onSettledContainer = Color(0xFF6EE7B7),
    overdueContainer = Color(0xFF7F1D1D),
    onOverdueContainer = Color(0xFFFCA5A5),
    overdueAccent = Color(0xFFF87171),
    partialContainer = Color(0xFF78350F),
    onPartialContainer = Color(0xFFFCD34D),
    cardBackground = Color(0xFF1E293B),
    chipBackground = Color(0xFF334155)
)

val LocalFinancialColors = staticCompositionLocalOf { LightFinancialColors }

object PhittoosColors {
    val financial: FinancialColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalFinancialColors.current
}
