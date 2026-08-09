package com.example.ui.components

import androidx.compose.foundation.layout.*
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
import com.example.util.CurrencyFormatter

@Composable
fun SetBudgetDialog(
    currentDailyBudget: Double,
    currentMonthlyBudget: Double,
    currentMinBalance: Double = 300000.0,
    currentSavingsTarget: Double = 1000000.0,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, Double) -> Unit
) {
    var dailyInput by remember {
        val txt = CurrencyFormatter.formatDigitsWithDots(currentDailyBudget.toLong().toString())
        mutableStateOf(TextFieldValue(text = txt, selection = TextRange(txt.length)))
    }
    var monthlyInput by remember {
        val txt = CurrencyFormatter.formatDigitsWithDots(currentMonthlyBudget.toLong().toString())
        mutableStateOf(TextFieldValue(text = txt, selection = TextRange(txt.length)))
    }
    var minBalanceInput by remember {
        val txt = CurrencyFormatter.formatDigitsWithDots(currentMinBalance.toLong().toString())
        mutableStateOf(TextFieldValue(text = txt, selection = TextRange(txt.length)))
    }
    var savingsTargetInput by remember {
        val txt = CurrencyFormatter.formatDigitsWithDots(currentSavingsTarget.toLong().toString())
        mutableStateOf(TextFieldValue(text = txt, selection = TextRange(txt.length)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Atur Anggaran & Target Tabungan",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dailyInput,
                    onValueChange = { input ->
                        val digits = input.text.filter { it.isDigit() }
                        val formatted = CurrencyFormatter.formatDigitsWithDots(digits)
                        dailyInput = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                    },
                    label = { Text("Batas Anggaran Harian") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.testTag("input_daily_budget")
                )

                OutlinedTextField(
                    value = monthlyInput,
                    onValueChange = { input ->
                        val digits = input.text.filter { it.isDigit() }
                        val formatted = CurrencyFormatter.formatDigitsWithDots(digits)
                        monthlyInput = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                    },
                    label = { Text("Batas Anggaran Bulanan") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.testTag("input_monthly_budget")
                )

                OutlinedTextField(
                    value = minBalanceInput,
                    onValueChange = { input ->
                        val digits = input.text.filter { it.isDigit() }
                        val formatted = CurrencyFormatter.formatDigitsWithDots(digits)
                        minBalanceInput = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                    },
                    label = { Text("Batas Minimum Saldo Kas Warning") },
                    prefix = { Text("Rp ") },
                    supportingText = { Text("Peringatan muncul jika kas di bawah angka ini") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.testTag("input_min_balance")
                )

                OutlinedTextField(
                    value = savingsTargetInput,
                    onValueChange = { input ->
                        val digits = input.text.filter { it.isDigit() }
                        val formatted = CurrencyFormatter.formatDigitsWithDots(digits)
                        savingsTargetInput = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                    },
                    label = { Text("Target Tabungan Bulanan") },
                    prefix = { Text("Rp ") },
                    supportingText = { Text("Target sisa kas/tabungan bersih per bulan") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.testTag("input_savings_target")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dailyVal = dailyInput.text.filter { it.isDigit() }.toDoubleOrNull() ?: 100000.0
                    val monthlyVal = monthlyInput.text.filter { it.isDigit() }.toDoubleOrNull() ?: 3000000.0
                    val minBalVal = minBalanceInput.text.filter { it.isDigit() }.toDoubleOrNull() ?: 300000.0
                    val savingsTargetVal = savingsTargetInput.text.filter { it.isDigit() }.toDoubleOrNull() ?: 1000000.0
                    onSave(dailyVal, monthlyVal, minBalVal, savingsTargetVal)
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_save_budget")
            ) {
                Text(text = "Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal")
            }
        }
    )
}
