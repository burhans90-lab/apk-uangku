package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.CurrencyFormatter
import kotlin.math.atan2

data class CategoryExpenseItem(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float
)

@Composable
fun ExpensePieChartCard(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }

    // Filter expense transactions only
    val expenseTransactions = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE && it.amount > 0 }
    }

    val totalExpense = remember(expenseTransactions) {
        expenseTransactions.sumOf { it.amount }
    }

    // Group expenses by category
    val categoryItems = remember(expenseTransactions, totalExpense) {
        if (totalExpense <= 0) emptyList()
        else {
            val grouped = expenseTransactions.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }

            var currentAngle = -90f
            grouped.map { (cat, amount) ->
                val pct = (amount / totalExpense).toFloat()
                val sweep = pct * 360f
                val start = currentAngle
                currentAngle += sweep

                CategoryExpenseItem(
                    category = cat,
                    amount = amount,
                    percentage = pct * 100f,
                    color = Color(cat.colorHex),
                    startAngle = start,
                    sweepAngle = sweep
                )
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_pie_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "Diagram Pengeluaran",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ringkasan Grafik Pengeluaran",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Sembunyikan Grafik" else "Tampilkan Grafik",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    if (categoryItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada catatan pengeluaran pada periode ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val activeItem = categoryItems.find { it.category == selectedCategory }

                        // Row containing Donut Canvas & Category Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Donut Canvas Chart
                            Box(
                                modifier = Modifier.size(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .pointerInput(categoryItems) {
                                            detectTapGestures { offset ->
                                                val center = Offset(size.width / 2f, size.height / 2f)
                                                val dx = offset.x - center.x
                                                val dy = offset.y - center.y
                                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                if (angle < 0) angle += 360f

                                                // Find clicked slice (-90 offset normalized)
                                                val clicked = categoryItems.find { item ->
                                                    var start = item.startAngle
                                                    var end = item.startAngle + item.sweepAngle

                                                    // Normalize angles to 0..360 range
                                                    fun norm(a: Float): Float {
                                                        var v = a % 360f
                                                        if (v < 0) v += 360f
                                                        return v
                                                    }

                                                    val normStart = norm(start)
                                                    val normEnd = norm(end)

                                                    if (normStart <= normEnd) {
                                                        angle in normStart..normEnd
                                                    } else {
                                                        angle >= normStart || angle <= normEnd
                                                    }
                                                }

                                                selectedCategory = if (clicked?.category == selectedCategory) null else clicked?.category
                                            }
                                        }
                                ) {
                                    val strokeWidth = 26.dp.toPx()
                                    val chartSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                                    categoryItems.forEach { item ->
                                        val isSelected = item.category == selectedCategory
                                        val width = if (isSelected) strokeWidth * 1.25f else strokeWidth
                                        val color = if (selectedCategory == null || isSelected) item.color else item.color.copy(alpha = 0.35f)

                                        drawArc(
                                            color = color,
                                            startAngle = item.startAngle,
                                            sweepAngle = item.sweepAngle - 1.5f, // subtle gap
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = chartSize,
                                            style = Stroke(width = width)
                                        )
                                    }
                                }

                                // Center Donut Label
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    if (activeItem != null) {
                                        Text(
                                            text = activeItem.category.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${String.format("%.1f", activeItem.percentage)}%",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = activeItem.color
                                        )
                                    } else {
                                        Text(
                                            text = "Total\nPengeluaran",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = CurrencyFormatter.formatRupiah(totalExpense),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            fontSize = 10.5.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Category Breakdown Legend List (Top 4 + Others)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categoryItems.take(4).forEach { item ->
                                    val isSelected = item.category == selectedCategory
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedCategory = if (isSelected) null else item.category
                                            }
                                            .background(
                                                if (isSelected) item.color.copy(alpha = 0.15f) else Color.Transparent
                                            )
                                            .padding(vertical = 3.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(item.color, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.category.displayName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "${String.format("%.0f", item.percentage)}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) item.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }

                                if (categoryItems.size > 4) {
                                    val otherSum = categoryItems.drop(4).sumOf { it.amount }
                                    val otherPct = (otherSum / totalExpense) * 100
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Kategori Lainnya (${categoryItems.size - 4})",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "${String.format("%.0f", otherPct)}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
