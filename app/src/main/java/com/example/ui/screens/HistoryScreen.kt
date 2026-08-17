package com.example.ui.screens

import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.ExpensePieChartCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.UangkuViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.ReportExporter

private enum class DateFilterPreset { ALL, TODAY, SEVEN_DAYS, THIS_MONTH, CUSTOM }

@Composable
fun HistoryScreen(
    viewModel: UangkuViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val startDateFilter by viewModel.startDateFilter.collectAsState()
    val endDateFilter by viewModel.endDateFilter.collectAsState()

    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val periodTransactions by viewModel.periodTransactions.collectAsState()

    val context = LocalContext.current
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    var isSearchActive by remember { mutableStateOf(searchQuery.isNotEmpty()) }

    var selectedPreset by remember { mutableStateOf(DateFilterPreset.ALL) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("in", "ID")) }

    fun showDatePicker(initialMs: Long?, onDateSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply {
            if (initialMs != null) timeInMillis = initialMs
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onDateSelected(selectedCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val filteredIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val filteredExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("history_screen")
    ) {
        // Fixed Top Header & Search Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = {
                        Text(
                            text = "Cari deskripsi, kategori, catatan...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotEmpty()) {
                                viewModel.searchQuery.value = ""
                            } else {
                                isSearchActive = false
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Tutup pencarian"
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Riwayat Transaksi",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "Hasil pencarian: \"$searchQuery\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier.testTag("history_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari Transaksi",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

        item {
            // Date Range Quick Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedPreset == DateFilterPreset.ALL && startDateFilter == null && endDateFilter == null,
                        onClick = {
                            selectedPreset = DateFilterPreset.ALL
                            viewModel.startDateFilter.value = null
                            viewModel.endDateFilter.value = null
                        },
                        label = { Text("Semua Tanggal", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPreset == DateFilterPreset.TODAY,
                        onClick = {
                            selectedPreset = DateFilterPreset.TODAY
                            val now = System.currentTimeMillis()
                            viewModel.startDateFilter.value = DateUtils.getStartOfDay(now)
                            viewModel.endDateFilter.value = DateUtils.getEndOfDay(now)
                        },
                        label = { Text("Hari Ini", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPreset == DateFilterPreset.SEVEN_DAYS,
                        onClick = {
                            selectedPreset = DateFilterPreset.SEVEN_DAYS
                            val now = System.currentTimeMillis()
                            val cal7 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                            viewModel.startDateFilter.value = DateUtils.getStartOfDay(cal7.timeInMillis)
                            viewModel.endDateFilter.value = DateUtils.getEndOfDay(now)
                        },
                        label = { Text("7 Hari", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPreset == DateFilterPreset.THIS_MONTH,
                        onClick = {
                            selectedPreset = DateFilterPreset.THIS_MONTH
                            val now = System.currentTimeMillis()
                            viewModel.startDateFilter.value = DateUtils.getStartOfMonth(now)
                            viewModel.endDateFilter.value = DateUtils.getEndOfDay(now)
                        },
                        label = { Text("Bulan Ini", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        item {
            // Custom Date Pickers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        showDatePicker(startDateFilter) { pickedMs ->
                            selectedPreset = DateFilterPreset.CUSTOM
                            viewModel.startDateFilter.value = DateUtils.getStartOfDay(pickedMs)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (startDateFilter != null) "Dari: ${dateFormatter.format(Date(startDateFilter!!))}" else "Dari Tanggal",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        showDatePicker(endDateFilter) { pickedMs ->
                            selectedPreset = DateFilterPreset.CUSTOM
                            viewModel.endDateFilter.value = DateUtils.getEndOfDay(pickedMs)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (endDateFilter != null) "s/d: ${dateFormatter.format(Date(endDateFilter!!))}" else "Sampai Tanggal",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }

                if (startDateFilter != null || endDateFilter != null) {
                    IconButton(
                        onClick = {
                            selectedPreset = DateFilterPreset.ALL
                            viewModel.startDateFilter.value = null
                            viewModel.endDateFilter.value = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Reset Tanggal",
                            tint = ExpenseRed
                        )
                    }
                }
            }
        }

        item {
            // Filtered Totals Summary Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Pemasukan (Filter)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(CurrencyFormatter.formatRupiah(filteredIncome), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = IncomeGreen)
                    }
                    Box(modifier = Modifier.height(24.dp).width(1.dp).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Pengeluaran (Filter)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(CurrencyFormatter.formatRupiah(filteredExpense), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ExpenseRed)
                    }
                }
            }
        }

        item {
            // Pie Chart Visualisation with Interactive Category Selection
            ExpensePieChartCard(
                transactions = periodTransactions,
                selectedCategory = selectedCategoryFilter,
                onCategorySelected = { cat ->
                    viewModel.selectedCategoryFilter.value = cat
                }
            )
        }

        item {
            // Filter Chips Row (Semua, Pengeluaran, Pemasukan + Active Category Filter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp).size(20.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { viewModel.selectedTypeFilter.value = null },
                            label = { Text("Semua", maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.EXPENSE,
                            onClick = { viewModel.selectedTypeFilter.value = TransactionType.EXPENSE },
                            label = { Text("Pengeluaran", maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.INCOME,
                            onClick = { viewModel.selectedTypeFilter.value = TransactionType.INCOME },
                            label = { Text("Pemasukan", maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                        )
                    }

                    selectedCategoryFilter?.let { cat ->
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.selectedCategoryFilter.value = null },
                                label = {
                                    Text(
                                        text = "Kategori: ${cat.displayName}",
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus filter kategori",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (selectedCategoryFilter != null) {
                                "Tidak ada transaksi untuk kategori \"${selectedCategoryFilter?.displayName}\"."
                            } else {
                                "Tidak ada catatan transaksi ditemukan pada filter ini."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (selectedCategoryFilter != null || selectedTypeFilter != null || startDateFilter != null || endDateFilter != null || searchQuery.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.selectedCategoryFilter.value = null
                                    viewModel.selectedTypeFilter.value = null
                                    viewModel.startDateFilter.value = null
                                    viewModel.endDateFilter.value = null
                                    viewModel.searchQuery.value = ""
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset Semua Filter", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                TransactionItemCard(
                    transaction = tx,
                    onDeleteClick = { _ -> transactionToDelete = tx },
                    onEditClick = { editingTx -> transactionToEdit = editingTx }
                )
            }
        }
    }
    }

    // Confirmation Dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(
                    text = "Hapus Transaksi?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus catatan \"${tx.title}\" sebesar ${CurrencyFormatter.formatRupiah(tx.amount)}?\n\nTindakan ini tidak dapat dibatalkan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.whiteOrOnPrimary())
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { transactionToDelete = null }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Sheet
    transactionToEdit?.let { tx ->
        AddEditTransactionSheet(
            initialTransaction = tx,
            onDismiss = { transactionToEdit = null },
            onSave = { title, amount, type, category, paymentMethod, note ->
                viewModel.updateTransactionEntity(
                    tx.copy(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        paymentMethod = paymentMethod,
                        note = note
                    )
                )
                transactionToEdit = null
            }
        )
    }
}

@Composable
private fun ColorScheme.whiteOrOnPrimary() = androidx.compose.ui.graphics.Color.White
