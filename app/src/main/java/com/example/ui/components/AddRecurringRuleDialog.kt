package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringFrequency
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.util.CurrencyFormatter

@Composable
fun AddRecurringRuleDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, TransactionType, TransactionCategory, PaymentMethod, RecurringFrequency) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.TAGIHAN) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.DAILY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Buat Aturan Transaksi Otomatis",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Transaksi Rutin") },
                    placeholder = { Text("mis. Kopi Pagi Harian / Kos Bulanan") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.testTag("input_recurring_title")
                )

                OutlinedTextField(
                    value = amountFieldValue,
                    onValueChange = { input ->
                        val digits = input.text.filter { it.isDigit() }
                        val formatted = CurrencyFormatter.formatDigitsWithDots(digits)
                        amountFieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                    },
                    label = { Text("Jumlah Nominal") },
                    prefix = { Text("Rp ") },
                    placeholder = { Text("50.000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.testTag("input_recurring_amount")
                )

                Text(text = "Frekuensi", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurringFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = selectedFrequency == freq,
                            onClick = { selectedFrequency = freq },
                            label = { Text(freq.displayName) }
                        )
                    }
                }

                Text(text = "Kategori", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TransactionCategory.entries.toTypedArray()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val amountVal = amountFieldValue.text.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            val isValid = title.isNotBlank() && amountVal > 0

            Button(
                onClick = {
                    if (isValid) {
                        onSave(title, amountVal, TransactionType.EXPENSE, selectedCategory, PaymentMethod.TUNAI, selectedFrequency)
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("btn_save_recurring")
            ) {
                Text(text = "Simpan Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal")
            }
        }
    )
}
