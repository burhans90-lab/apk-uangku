package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.viewmodel.UangkuViewModel

import com.example.data.model.TransactionEntity
import com.example.ui.theme.ExpenseRed
import com.example.util.CurrencyFormatter

import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning

@Composable
fun HomeScreen(
    viewModel: UangkuViewModel,
    onAdjustBudgetClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val todayExpense by viewModel.todayExpense.collectAsState()
    val dailyBudgetLimit by viewModel.dailyBudgetLimit.collectAsState()
    val minBalanceThreshold by viewModel.minBalanceThreshold.collectAsState()
    val monthlySavingsTarget by viewModel.monthlySavingsTarget.collectAsState()

    val quickText by viewModel.quickInputText.collectAsState()
    val parsedQuickTx by viewModel.parsedQuickTransaction.collectAsState()
    val autoBannerMessage by viewModel.autoProcessedBannerMessage.collectAsState()

    val transactions by viewModel.transactions.collectAsState()

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Summary Card
        item {
            SummaryCard(
                totalBalance = totalBalance,
                monthlyIncome = monthlyIncome,
                monthlyExpense = monthlyExpense,
                todayExpense = todayExpense,
                dailyBudgetLimit = dailyBudgetLimit,
                minBalanceThreshold = minBalanceThreshold,
                onAdjustBudgetClick = onAdjustBudgetClick
            )
        }

        // 2. Target Tabungan Bulanan Card with Visual Progress Bar
        item {
            SavingsTargetCard(
                monthlyIncome = monthlyIncome,
                monthlyExpense = monthlyExpense,
                monthlySavingsTarget = monthlySavingsTarget,
                onEditTargetClick = onAdjustBudgetClick
            )
        }

        // 2. Auto-Processed Banner if triggered
        autoBannerMessage?.let { msg ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearAutoBanner() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }
                }
            }
        }

        // 3. Quick Text Parser Input Bar
        item {
            QuickAddBar(
                text = quickText,
                onTextChanged = { viewModel.onQuickInputChange(it) },
                parsed = parsedQuickTx,
                onSubmit = { viewModel.submitQuickInput() }
            )
        }

        // 4. Quick Template Row (1-Tap)
        item {
            QuickTemplateRow(
                onTemplateClick = { title, amount, cat ->
                    viewModel.addQuickTemplate(title, amount, cat)
                }
            )
        }

        // 5. Recent Transactions Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Catatan Keuangan Terbaru",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(
                    onClick = onSeeAllClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Lihat Semua (${transactions.size})",
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 6. Recent Transactions List
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Catatan Keuangan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Gunakan kolom Catat Cepat di atas untuk mulai mencatat!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            val recentItems = transactions.take(10)
            items(recentItems, key = { it.id }) { tx ->
                TransactionItemCard(
                    transaction = tx,
                    onDeleteClick = { _ -> transactionToDelete = tx },
                    onEditClick = { editingTx -> transactionToEdit = editingTx }
                )
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
