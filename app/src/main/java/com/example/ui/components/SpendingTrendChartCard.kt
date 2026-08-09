package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.util.CurrencyFormatter
import java.util.Locale

data class MonthlySpending(
    val monthLabel: String,
    val fullMonthYear: String,
    val totalExpense: Double
)

private fun formatCompactRupiah(amount: Double): String {
    if (amount <= 0) return "0"
    return when {
        amount >= 1_000_000_000 -> String.format(Locale("in", "ID"), "%.1fM", amount / 1_000_000_000.0)
        amount >= 1_000_000 -> {
            val formatted = String.format(Locale("in", "ID"), "%.1f", amount / 1_000_000.0)
            "${formatted.removeSuffix(".0")}jt"
        }
        amount >= 1_000 -> {
            val formatted = String.format(Locale("in", "ID"), "%.0f", amount / 1_000.0)
            "${formatted}rb"
        }
        else -> amount.toLong().toString()
    }
}

@Composable
fun SpendingTrendChartCard(
    monthlySpendings: List<MonthlySpending>,
    modifier: Modifier = Modifier
) {
    val total6MonthsExpense = monthlySpendings.sumOf { it.totalExpense }
    val maxExpense = (monthlySpendings.maxOfOrNull { it.totalExpense } ?: 0.0).coerceAtLeast(100_000.0)
    val avgMonthlyExpense = if (monthlySpendings.isNotEmpty()) total6MonthsExpense / monthlySpendings.size else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_trend_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ExpenseRed.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Tren Pengeluaran",
                                tint = ExpenseRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Tren Pengeluaran 6 Bulan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Grafik pengeluaran bulanan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Rata-rata/Bln",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(avgMonthlyExpense),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = ExpenseRed,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (total6MonthsExpense == 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada riwayat pengeluaran 6 bulan terakhir.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val lineColor = ExpenseRed
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                val dotBorderColor = MaterialTheme.colorScheme.surface

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    val sidePaddingDp = 24.dp
                    val topPaddingDp = 32.dp
                    val bottomPaddingDp = 16.dp

                    // Chart Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("spending_trend_canvas")
                    ) {
                        val widthPx = size.width
                        val heightPx = size.height

                        val sidePadding = sidePaddingDp.toPx()
                        val topPadding = topPaddingDp.toPx()
                        val bottomPadding = bottomPaddingDp.toPx()

                        val chartWidth = widthPx - (sidePadding * 2)
                        val chartHeight = heightPx - topPadding - bottomPadding

                        val count = monthlySpendings.size
                        if (count < 2) return@Canvas

                        val stepX = chartWidth / (count - 1)

                        // Draw Grid Lines (3 horizontal lines)
                        val gridLineCount = 3
                        for (i in 0..gridLineCount) {
                            val y = topPadding + (chartHeight / gridLineCount) * i
                            drawLine(
                                color = gridColor,
                                start = Offset(sidePadding, y),
                                end = Offset(widthPx - sidePadding, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Calculate points
                        val points = monthlySpendings.mapIndexed { index, spending ->
                            val x = sidePadding + index * stepX
                            val fraction = (spending.totalExpense / maxExpense).toFloat().coerceIn(0f, 1f)
                            val y = topPadding + chartHeight * (1f - fraction)
                            Offset(x, y)
                        }

                        // Build Line Path
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlPoint1 = Offset(p1.x + stepX / 2f, p1.y)
                                val controlPoint2 = Offset(p2.x - stepX / 2f, p2.y)
                                cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                            }
                        }

                        // Build Gradient Fill Path
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(points.last().x, topPadding + chartHeight)
                            lineTo(points.first().x, topPadding + chartHeight)
                            close()
                        }

                        // Draw Gradient Fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.35f),
                                    lineColor.copy(alpha = 0.02f)
                                ),
                                startY = topPadding,
                                endY = topPadding + chartHeight
                            )
                        )

                        // Draw Line Stroke
                        drawPath(
                            path = strokePath,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Draw Point Circles
                        points.forEach { pt ->
                            // Outer circle (White/Surface)
                            drawCircle(
                                color = dotBorderColor,
                                radius = 6.dp.toPx(),
                                center = pt
                            )
                            // Inner circle (ExpenseRed)
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                        }
                    }

                    // Value labels overlay positioned above dots
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = sidePaddingDp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        monthlySpendings.forEach { item ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                val labelText = formatCompactRupiah(item.totalExpense)
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (item.totalExpense > 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // X-Axis Month Labels Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    monthlySpendings.forEach { item ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.monthLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
