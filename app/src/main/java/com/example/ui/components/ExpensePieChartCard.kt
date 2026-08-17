package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
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
import java.util.Locale
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
    selectedCategory: TransactionCategory? = null,
    onCategorySelected: ((TransactionCategory?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

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

    // Auto-scroll the list when category is selected via chart tap
    LaunchedEffect(selectedCategory) {
        if (selectedCategory != null && categoryItems.isNotEmpty()) {
            val index = categoryItems.indexOfFirst { it.category == selectedCategory }
            if (index >= 0) {
                listState.animateScrollToItem(index)
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
                        .padding(top = 10.dp)
                ) {
                    if (categoryItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
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

                        // Row containing Donut Canvas & Category Scrollable List
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Donut Canvas Chart
                            Box(
                                modifier = Modifier
                                    .size(135.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .pointerInput(categoryItems, selectedCategory) {
                                            detectTapGestures { offset ->
                                                val center = Offset(size.width / 2f, size.height / 2f)
                                                val dx = offset.x - center.x
                                                val dy = offset.y - center.y
                                                val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                                                val radius = size.width / 2f

                                                // If center hole is clicked when filtered, reset filter
                                                if (distance < radius * 0.42f) {
                                                    if (selectedCategory != null) {
                                                        onCategorySelected?.invoke(null)
                                                    }
                                                    return@detectTapGestures
                                                }

                                                if (distance <= radius * 1.3f) {
                                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                    if (angle < -90f) angle += 360f

                                                    val clicked = categoryItems.find { item ->
                                                        angle >= item.startAngle && angle < (item.startAngle + item.sweepAngle)
                                                    }

                                                    val newCategory = if (clicked?.category == selectedCategory) null else clicked?.category
                                                    onCategorySelected?.invoke(newCategory)
                                                }
                                            }
                                        }
                                ) {
                                    val strokeWidth = 22.dp.toPx()
                                    val chartSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                                    categoryItems.forEach { item ->
                                        val isSelected = item.category == selectedCategory
                                        val width = if (isSelected) strokeWidth * 1.3f else strokeWidth
                                        val color = if (selectedCategory == null || isSelected) item.color else item.color.copy(alpha = 0.30f)

                                        drawArc(
                                            color = color,
                                            startAngle = item.startAngle,
                                            sweepAngle = (item.sweepAngle - 1.5f).coerceAtLeast(0.5f),
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
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            if (selectedCategory != null) {
                                                onCategorySelected?.invoke(null)
                                            }
                                        }
                                        .padding(4.dp)
                                ) {
                                    if (activeItem != null) {
                                        Text(
                                            text = activeItem.category.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", activeItem.percentage)}%",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                            color = activeItem.color
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatRupiah(activeItem.amount),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
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

                            // Category Breakdown Scrollable Legend List (Fixed height ~ 4 items max, scrollable)
                            val listHeight = if (categoryItems.size <= 3) {
                                (categoryItems.size * 36 + (categoryItems.size - 1).coerceAtLeast(0) * 4).dp
                            } else {
                                146.dp
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(listHeight)
                                    .padding(start = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(
                                    items = categoryItems,
                                    key = { _, item -> item.category.name }
                                ) { _, item ->
                                    val isSelected = item.category == selectedCategory
                                    CategoryLegendItemRow(
                                        item = item,
                                        isSelected = isSelected,
                                        onClick = {
                                            val newCat = if (isSelected) null else item.category
                                            onCategorySelected?.invoke(newCat)
                                        }
                                    )
                                }
                            }
                        }

                        // Active filter banner inside card
                        if (selectedCategory != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Memfilter Kategori: ${selectedCategory.displayName}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onCategorySelected?.invoke(null) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus Filter Kategori",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
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

@Composable
private fun CategoryLegendItemRow(
    item: CategoryExpenseItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) item.color.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = if (isSelected) BorderStroke(1.5.dp, item.color) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
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
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", item.percentage)}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = if (isSelected) item.color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = CurrencyFormatter.formatRupiah(item.amount),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
