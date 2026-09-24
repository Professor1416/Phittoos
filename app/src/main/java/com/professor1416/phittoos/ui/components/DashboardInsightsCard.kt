package com.professor1416.phittoos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professor1416.phittoos.domain.DashboardInsights
import com.professor1416.phittoos.domain.SpendingCategory
import com.professor1416.phittoos.domain.SpendingCategoryBreakdown
import com.professor1416.phittoos.domain.WeeklySpendingTrend
import com.professor1416.phittoos.ui.theme.CoralOrange
import com.professor1416.phittoos.ui.theme.CoralOrangeDark
import com.professor1416.phittoos.ui.theme.CoralOrangeSurface
import com.professor1416.phittoos.ui.theme.EmeraldGreen
import com.professor1416.phittoos.ui.theme.EmeraldGreenDark
import com.professor1416.phittoos.ui.theme.EmeraldGreenSurface
import com.professor1416.phittoos.ui.theme.Slate100
import com.professor1416.phittoos.ui.theme.Slate200
import com.professor1416.phittoos.ui.theme.Slate400
import com.professor1416.phittoos.ui.theme.Slate600
import com.professor1416.phittoos.ui.theme.Slate800
import com.professor1416.phittoos.ui.util.Formatters

@Composable
fun DashboardInsightsCard(
    insights: DashboardInsights,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedWeekIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("dashboard_insights_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = EmeraldGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Dashboard Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "30-day trends & breakdown",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "30 Days",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse insights" else "Expand insights",
                            tint = Slate600
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // 1. Proportional Comparison Gauge: Lent vs Borrowed
                    LentVsBorrowedProportionSection(insights)

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. 30-Day Grouped Bar Chart (Recharts-Style)
                    Text(
                        text = "30-DAY VOLUME TRENDS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    WeeklyTrendsChart(
                        weeklyTrends = insights.weeklyTrends,
                        selectedWeekIndex = selectedWeekIndex,
                        onSelectWeek = { weekIdx ->
                            selectedWeekIndex = if (selectedWeekIndex == weekIdx) null else weekIdx
                        }
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // 3. Spending Trends Categorization
                    Text(
                        text = "SPENDING TRENDS BY CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (insights.categoryBreakdowns.isNotEmpty()) {
                        CategoryBreakdownList(insights.categoryBreakdowns)
                    } else {
                        EmptyCategoryPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun LentVsBorrowedProportionSection(insights: DashboardInsights) {
    val lentAmount = insights.totalLentLast30Days
    val borrowedAmount = insights.totalBorrowedLast30Days
    val totalVolume = lentAmount + borrowedAmount

    val lentPercent = if (totalVolume > 0) ((lentAmount / totalVolume) * 100).toInt() else 50
    val borrowedPercent = if (totalVolume > 0) ((borrowedAmount / totalVolume) * 100).toInt() else 50

    val animatedLentRatio by animateFloatAsState(
        targetValue = if (totalVolume > 0) (lentAmount / totalVolume).toFloat() else 0.5f,
        animationSpec = tween(durationMillis = 600),
        label = "lent_ratio_anim"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lent Stat
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lent",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "($lentPercent%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreenDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = Formatters.formatCurrency(lentAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenDark
                    )
                }

                // Borrowed Stat
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "($borrowedPercent%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoralOrangeDark,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Borrowed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CoralOrange)
                        )
                    }
                    Text(
                        text = Formatters.formatCurrency(borrowedAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoralOrangeDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Proportional Bi-Color Ratio Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Slate200)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedLentRatio)
                            .height(10.dp)
                            .background(EmeraldGreen)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .height(10.dp)
                            .background(CoralOrange)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Net 30-Day Flow Summary Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val netFlow = insights.netFlowLast30Days
                val isNetPositive = netFlow >= 0

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isNetPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isNetPositive) EmeraldGreenDark else CoralOrangeDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isNetPositive) "Net Outflow (You gave more)" else "Net Inflow (You received more)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )
                }

                Text(
                    text = "${if (netFlow >= 0) "+" else ""}${Formatters.formatCurrency(netFlow)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isNetPositive) EmeraldGreenDark else CoralOrangeDark
                )
            }
        }
    }
}

@Composable
private fun WeeklyTrendsChart(
    weeklyTrends: List<WeeklySpendingTrend>,
    selectedWeekIndex: Int?,
    onSelectWeek: (Int) -> Unit
) {
    val maxWeeklyVal = (weeklyTrends.maxOfOrNull { maxOf(it.lentAmount, it.borrowedAmount) } ?: 1000.0).coerceAtLeast(100.0)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Interactive Selected Tooltip Banner
            val activeTrend = weeklyTrends.find { it.weekIndex == selectedWeekIndex }
            if (activeTrend != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${activeTrend.weekLabel} (${activeTrend.dateRangeLabel})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Lent: ${Formatters.formatCurrency(activeTrend.lentAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreenDark,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Borrowed: ${Formatters.formatCurrency(activeTrend.borrowedAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CoralOrangeDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val emeraldColor = EmeraldGreen
                val coralColor = CoralOrange
                val gridColor = Slate200

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 24.dp.toPx()
                    val chartH = h - bottomPadding

                    // 1. Draw horizontal reference grid lines
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = (chartH / gridSteps) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // 2. Draw Grouped Bars for each week
                    val groupCount = weeklyTrends.size.coerceAtLeast(1)
                    val groupWidth = w / groupCount
                    val barWidth = 12.dp.toPx()
                    val barSpacing = 4.dp.toPx()

                    weeklyTrends.forEachIndexed { idx, trend ->
                        val groupCenterX = (idx * groupWidth) + (groupWidth / 2f)

                        // Lent Bar (Left)
                        val lentH = ((trend.lentAmount / maxWeeklyVal) * (chartH * 0.9f)).toFloat().coerceAtLeast(3.dp.toPx())
                        val lentLeft = groupCenterX - barWidth - (barSpacing / 2f)
                        val lentTop = chartH - lentH

                        drawRoundRect(
                            color = emeraldColor,
                            topLeft = Offset(lentLeft, lentTop),
                            size = Size(barWidth, lentH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Borrowed Bar (Right)
                        val borrowedH = ((trend.borrowedAmount / maxWeeklyVal) * (chartH * 0.9f)).toFloat().coerceAtLeast(3.dp.toPx())
                        val borrowedLeft = groupCenterX + (barSpacing / 2f)
                        val borrowedTop = chartH - borrowedH

                        drawRoundRect(
                            color = coralColor,
                            topLeft = Offset(borrowedLeft, borrowedTop),
                            size = Size(barWidth, borrowedH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                // Week Labels Overlay at Bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weeklyTrends.forEach { trend ->
                        val isSelected = trend.weekIndex == selectedWeekIndex
                        Text(
                            text = trend.weekLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) EmeraldGreenDark else Slate600,
                            modifier = Modifier
                                .clickable { onSelectWeek(trend.weekIndex) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lent Volume", style = MaterialTheme.typography.labelSmall, color = Slate600)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CoralOrange))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Borrowed Volume", style = MaterialTheme.typography.labelSmall, color = Slate600)
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownList(breakdowns: List<SpendingCategoryBreakdown>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        breakdowns.take(5).forEach { item ->
            CategoryBreakdownRow(item)
        }
    }
}

@Composable
private fun CategoryBreakdownRow(item: SpendingCategoryBreakdown) {
    val (icon, tintColor, bgColor) = getCategoryVisuals(item.category)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.transactionCount} transaction${if (item.transactionCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate600
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(item.totalAmount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.percentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tintColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Percentage Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Slate200)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.percentage / 100f)
                        .height(6.dp)
                        .background(tintColor)
                )
            }
        }
    }
}

@Composable
private fun EmptyCategoryPlaceholder() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Add transaction notes (e.g. 'chai', 'rent', 'uber') to automatically categorize spending.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600,
                lineHeight = 18.sp
            )
        }
    }
}

private fun getCategoryVisuals(category: SpendingCategory): Triple<ImageVector, Color, Color> {
    return when (category) {
        SpendingCategory.FOOD -> Triple(Icons.Default.Fastfood, Color(0xFFF97316), Color(0xFFFFEDD5))
        SpendingCategory.TRAVEL -> Triple(Icons.Default.DirectionsCar, Color(0xFF0284C7), Color(0xFFE0F2FE))
        SpendingCategory.BILLS -> Triple(Icons.Default.ReceiptLong, Color(0xFF7C3AED), Color(0xFFEDE9FE))
        SpendingCategory.SHOPPING -> Triple(Icons.Default.ShoppingBag, Color(0xFFDB2777), Color(0xFFFCE7F3))
        SpendingCategory.HEALTH -> Triple(Icons.Default.LocalHospital, Color(0xFFDC2626), Color(0xFFFEE2E2))
        SpendingCategory.GENERAL -> Triple(Icons.Default.Category, Slate800, Slate100)
    }
}
