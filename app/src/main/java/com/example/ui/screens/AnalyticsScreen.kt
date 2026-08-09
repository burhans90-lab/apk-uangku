package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.ui.components.MonthlySpending
import com.example.ui.components.SpendingTrendChartCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.UangkuViewModel
import com.example.util.CurrencyFormatter
import com.example.util.ReportExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CategoryExpenseStat(
    val category: TransactionCategory,
    val totalAmount: Double,
    val percentage: Float
)

private fun formatMonthYear(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
    return sdf.format(Date(timestamp))
}

private fun getCurrentYearMonths(): List<String> {
    val cal = java.util.Calendar.getInstance()
    val year = cal.get(java.util.Calendar.YEAR)
    val sdf = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
    return (0..11).map { monthIdx ->
        val c = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, monthIdx)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        sdf.format(c.time)
    }
}

@Composable
fun AnalyticsScreen(
    viewModel: UangkuViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()

    val currentMonthName = remember { formatMonthYear(System.currentTimeMillis()) }
    var selectedMonth by remember { mutableStateOf(currentMonthName) }

    val availableMonths = remember(transactions, currentMonthName) {
        val monthsFromTx = transactions.map { formatMonthYear(it.timestamp) }.distinct()
        val allYearMonths = getCurrentYearMonths()
        
        val list = mutableListOf<String>()
        list.add("Semua Waktu")
        list.add(currentMonthName) // Bulan berjalan
        
        allYearMonths.forEach { m ->
            if (!list.contains(m)) {
                list.add(m)
            }
        }
        
        monthsFromTx.forEach { m ->
            if (!list.contains(m)) {
                list.add(m)
            }
        }
        
        list
    }

    val filteredTx = remember(transactions, selectedMonth) {
        if (selectedMonth == "Semua Waktu") {
            transactions
        } else {
            transactions.filter { formatMonthYear(it.timestamp) == selectedMonth }
        }
    }

    val totalExpense = filteredTx
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }

    val totalIncome = filteredTx
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }

    // Category breakdown logic
    val categoryStats = remember(filteredTx, totalExpense) {
        val expenseTx = filteredTx.filter { it.type == TransactionType.EXPENSE }
        val grouped = expenseTx.groupBy { it.category }

        grouped.map { (cat, list) ->
            val catTotal = list.sumOf { it.amount }
            val pct = if (totalExpense > 0) (catTotal / totalExpense).toFloat() else 0f
            CategoryExpenseStat(cat, catTotal, pct)
        }.sortedByDescending { it.totalAmount }
    }

    // Spending trend over last 6 months
    val last6MonthsSpending = remember(transactions) {
        val cal = java.util.Calendar.getInstance()
        val months = mutableListOf<MonthlySpending>()
        val labelSdf = SimpleDateFormat("MMM", Locale("in", "ID"))
        val fullSdf = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))

        for (i in 5 downTo 0) {
            val c = (cal.clone() as java.util.Calendar).apply {
                add(java.util.Calendar.MONTH, -i)
            }
            val monthStr = fullSdf.format(c.time)
            val shortLabel = labelSdf.format(c.time)

            val monthExpense = transactions.filter { tx ->
                tx.type == TransactionType.EXPENSE && formatMonthYear(tx.timestamp) == monthStr
            }.sumOf { it.amount }

            months.add(MonthlySpending(shortLabel, monthStr, monthExpense))
        }
        months
    }

    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = ExpenseRed) },
            title = { Text("Amankan & Bersihkan Database?") },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus ${filteredTx.size} data transaksi untuk periode '$selectedMonth'?\n\n" +
                            "💡 Rekomendasi: Pastikan Anda telah mengunduh file Laporan Excel (.csv) terlebih dahulu agar data tetap aman sebelum memori database dibersihkan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = filteredTx.map { it.id }
                        viewModel.deleteTransactions(idsToDelete)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Ya, Bersihkan Memori")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Laporan & Analisis Keuangan",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Month Filter Selector
                Text(
                    text = "Pilih Periode Laporan:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableMonths) { month ->
                        FilterChip(
                            selected = selectedMonth == month,
                            onClick = { selectedMonth = month },
                            label = { Text(month) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // 6-Month Spending Trend Line Chart
        item {
            SpendingTrendChartCard(
                monthlySpendings = last6MonthsSpending
            )
        }

        // Export Excel & Database Backup/Cleanup Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ekspor Laporan & Amankan Database",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Unduh rekap bulanan ke format file Excel (.csv) untuk mengantisipasi memori HP penuh.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                ReportExporter.exportAndShareReport(context, selectedMonth, filteredTx)
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("btn_export_excel"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Unduh Excel",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }

                        if (filteredTx.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_clear_period_db")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Kosongkan DB",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ratio Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ringkasan Pemasukan vs Pengeluaran",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Pemasukan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CurrencyFormatter.formatRupiah(totalIncome), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = IncomeGreen)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Total Pengeluaran", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CurrencyFormatter.formatRupiah(totalExpense), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ExpenseRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val grandTotal = totalIncome + totalExpense
                    val incomeRatio = if (grandTotal > 0) (totalIncome / grandTotal).toFloat() else 0.5f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(incomeRatio.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(IncomeGreen)
                        )
                        Box(
                            modifier = Modifier
                                .weight((1f - incomeRatio).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(ExpenseRed)
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengeluaran Berdasarkan Kategori",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (categoryStats.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Belum ada pengeluaran tercatat untuk dibuat statistik.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(categoryStats) { stat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(stat.category.colorHex).copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = stat.category.getIcon(),
                                        contentDescription = null,
                                        tint = Color(stat.category.colorHex),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = stat.category.displayName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "${(stat.percentage * 100).toInt()}% dari total pengeluaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Text(
                                text = CurrencyFormatter.formatRupiah(stat.totalAmount),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRed
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { stat.percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(stat.category.colorHex),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
