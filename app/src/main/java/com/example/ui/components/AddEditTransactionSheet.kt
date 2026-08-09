package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    onDismiss: () -> Unit,
    initialTransaction: TransactionEntity? = null,
    onSave: (String, Double, TransactionType, TransactionCategory, PaymentMethod, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTransaction?.title ?: "") }
    var amountFieldValue by remember { 
        val initialText = if (initialTransaction != null) CurrencyFormatter.formatDigitsWithDots(initialTransaction.amount.toLong().toString()) else ""
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(initialText.length)))
    }
    var selectedType by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    
    // Income type options
    var selectedIncomeType by remember { 
        mutableStateOf(
            if (initialTransaction?.type == TransactionType.INCOME) {
                when (initialTransaction.category) {
                    TransactionCategory.GAJI -> "Gaji"
                    TransactionCategory.TPP -> "TPP"
                    TransactionCategory.SERTIFIKASI -> "Sertifikasi"
                    TransactionCategory.UANG_SAKU -> "Uang Saku"
                    else -> "Lainnya"
                }
            } else "Gaji"
        ) 
    }

    // Expense categories
    val expenseCategories = remember {
        listOf(
            TransactionCategory.MAKANAN,
            TransactionCategory.JAJAN,
            TransactionCategory.KEBUTUHAN_DAPUR,
            TransactionCategory.TRANSPORT,
            TransactionCategory.BELANJA,
            TransactionCategory.NAFKAH_KELUARGA,
            TransactionCategory.KESEHATAN,
            TransactionCategory.PERAWATAN_KENDARAAN,
            TransactionCategory.PENDIDIKAN,
            TransactionCategory.TAGIHAN,
            TransactionCategory.HIBURAN,
            TransactionCategory.INVESTASI,
            TransactionCategory.LAINNYA
        )
    }

    var selectedCategory by remember { mutableStateOf(initialTransaction?.category ?: TransactionCategory.MAKANAN) }
    var selectedPaymentMethod by remember { mutableStateOf(initialTransaction?.paymentMethod ?: PaymentMethod.TUNAI) }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }

    val incomeTypes = listOf("Gaji", "TPP", "Sertifikasi", "Uang Saku", "Lainnya")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { sheetValue ->
                if (sheetValue == SheetValue.Hidden) false else true
            }
        ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .testTag("add_transaction_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTransaction != null) "Edit Catatan Transaksi" else "Tambah Catatan Transaksi",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Form",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income / Expense Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        if (selectedCategory !in expenseCategories) {
                            selectedCategory = TransactionCategory.MAKANAN
                        }
                    },
                    label = { Text("Pengeluaran", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        selectedCategory = TransactionCategory.GAJI
                    },
                    label = { Text("Pemasukan", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedType == TransactionType.INCOME) {
                // Income specific selector: Jenis Pemasukan
                Text(text = "Jenis Pemasukan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(incomeTypes) { item ->
                        val isSelected = selectedIncomeType == item
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedIncomeType = item
                                selectedCategory = when (item) {
                                    "Gaji" -> TransactionCategory.GAJI
                                    "TPP" -> TransactionCategory.TPP
                                    "Sertifikasi" -> TransactionCategory.SERTIFIKASI
                                    "Uang Saku" -> TransactionCategory.UANG_SAKU
                                    else -> TransactionCategory.LAINNYA
                                }
                                if (title.isBlank() || incomeTypes.contains(title)) {
                                    title = item
                                }
                            },
                            label = { Text(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul Transaksi") },
                placeholder = {
                    Text(if (selectedType == TransactionType.INCOME) "mis. Gaji Bulan Ini" else "mis. Beli Token Listrik")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tx_title"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountFieldValue,
                onValueChange = { input ->
                    if (input.text == amountFieldValue.text) {
                        amountFieldValue = input
                    } else {
                        val cursorInInput = input.selection.start
                        val digitsBeforeCursor = input.text.take(cursorInInput).count { it.isDigit() }
                        val digits = input.text.filter { it.isDigit() }
                        val formatted = CurrencyFormatter.formatDigitsWithDots(digits)

                        var newCursorPos = formatted.length
                        if (digitsBeforeCursor == 0) {
                            newCursorPos = 0
                        } else {
                            var digitCount = 0
                            for (i in formatted.indices) {
                                if (formatted[i].isDigit()) {
                                    digitCount++
                                    if (digitCount == digitsBeforeCursor) {
                                        newCursorPos = i + 1
                                        break
                                    }
                                }
                            }
                        }

                        amountFieldValue = TextFieldValue(
                            text = formatted,
                            selection = TextRange(newCursorPos)
                        )
                    }
                },
                label = { Text("Jumlah Nominal") },
                prefix = { Text("Rp ") },
                placeholder = { Text("50.000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tx_amount"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedType == TransactionType.EXPENSE) {
                Text(text = "Kategori", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenseCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName) },
                            leadingIcon = {
                                Icon(imageVector = cat.getIcon(), contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Metode Pembayaran", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PaymentMethod.entries.toTypedArray()) { pm ->
                        FilterChip(
                            selected = selectedPaymentMethod == pm,
                            onClick = { selectedPaymentMethod = pm },
                            label = { Text(pm.displayName) },
                            leadingIcon = {
                                Icon(imageVector = pm.getIcon(), contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan Tambahan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val amountVal = amountFieldValue.text.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            val effectiveTitle = if (title.isBlank() && selectedType == TransactionType.INCOME) selectedIncomeType else title
            val isValid = effectiveTitle.isNotBlank() && amountVal > 0

            Button(
                onClick = {
                    if (isValid) {
                        val finalCategory = if (selectedType == TransactionType.INCOME) {
                            when (selectedIncomeType) {
                                "Gaji" -> TransactionCategory.GAJI
                                "TPP" -> TransactionCategory.TPP
                                "Sertifikasi" -> TransactionCategory.SERTIFIKASI
                                "Uang Saku" -> TransactionCategory.UANG_SAKU
                                else -> TransactionCategory.LAINNYA
                            }
                        } else {
                            selectedCategory
                        }
                        val finalPaymentMethod = if (selectedType == TransactionType.INCOME) PaymentMethod.TRANSFER_BANK else selectedPaymentMethod

                        onSave(effectiveTitle, amountVal, selectedType, finalCategory, finalPaymentMethod, note)
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_tx"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (initialTransaction != null) "Simpan Perubahan" else "Simpan Catatan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
