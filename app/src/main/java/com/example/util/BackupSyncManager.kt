package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.BudgetEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringRuleEntity
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val exportDate: Long,
    val budget: BudgetEntity?,
    val transactions: List<TransactionEntity>,
    val recurringRules: List<RecurringRuleEntity>
)

object BackupSyncManager {

    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
    val displayDateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("in", "ID"))

    fun generateBackupJson(
        transactions: List<TransactionEntity>,
        budget: BudgetEntity?,
        recurringRules: List<RecurringRuleEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "UANGKU")
        root.put("version", 1)
        root.put("exportTimestamp", System.currentTimeMillis())

        // Budget JSON
        if (budget != null) {
            val budgetObj = JSONObject()
            budgetObj.put("dailyLimit", budget.dailyLimit)
            budgetObj.put("monthlyLimit", budget.monthlyLimit)
            budgetObj.put("minBalanceThreshold", budget.minBalanceThreshold)
            budgetObj.put("monthlySavingsTarget", budget.monthlySavingsTarget)
            root.put("budget", budgetObj)
        }

        // Transactions JSON
        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("title", tx.title)
            obj.put("amount", tx.amount)
            obj.put("type", tx.type.name)
            obj.put("category", tx.category.name)
            obj.put("paymentMethod", tx.paymentMethod.name)
            obj.put("timestamp", tx.timestamp)
            obj.put("note", tx.note)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // Recurring Rules JSON
        val rulesArray = JSONArray()
        recurringRules.forEach { rule ->
            val obj = JSONObject()
            obj.put("id", rule.id)
            obj.put("title", rule.title)
            obj.put("amount", rule.amount)
            obj.put("type", rule.type.name)
            obj.put("category", rule.category.name)
            obj.put("paymentMethod", rule.paymentMethod.name)
            obj.put("frequency", rule.frequency.name)
            obj.put("lastExecutedTimestamp", rule.lastExecutedTimestamp)
            rulesArray.put(obj)
        }
        root.put("recurringRules", rulesArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupData? {
        return try {
            val root = JSONObject(jsonString)

            val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

            // Parse Budget
            var budget: BudgetEntity? = null
            if (root.has("budget") && !root.isNull("budget")) {
                val bObj = root.getJSONObject("budget")
                budget = BudgetEntity(
                    id = 1,
                    dailyLimit = bObj.optDouble("dailyLimit", 100000.0),
                    monthlyLimit = bObj.optDouble("monthlyLimit", 3000000.0),
                    minBalanceThreshold = bObj.optDouble("minBalanceThreshold", 300000.0),
                    monthlySavingsTarget = bObj.optDouble("monthlySavingsTarget", 1000000.0)
                )
            }

            // Parse Transactions
            val txList = mutableListOf<TransactionEntity>()
            if (root.has("transactions")) {
                val arr = root.getJSONArray("transactions")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val typeStr = obj.optString("type", "EXPENSE")
                    val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }

                    val catStr = obj.optString("category", "LAINNYA")
                    val category = try { TransactionCategory.valueOf(catStr) } catch (e: Exception) { TransactionCategory.LAINNYA }

                    val payStr = obj.optString("paymentMethod", "TUNAI")
                    val paymentMethod = try { PaymentMethod.valueOf(payStr) } catch (e: Exception) { PaymentMethod.TUNAI }

                    txList.add(
                        TransactionEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", "Transaksi"),
                            amount = obj.optDouble("amount", 0.0),
                            type = type,
                            category = category,
                            paymentMethod = paymentMethod,
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }

            // Parse Recurring Rules
            val ruleList = mutableListOf<RecurringRuleEntity>()
            if (root.has("recurringRules")) {
                val arr = root.getJSONArray("recurringRules")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val typeStr = obj.optString("type", "EXPENSE")
                    val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }

                    val catStr = obj.optString("category", "LAINNYA")
                    val category = try { TransactionCategory.valueOf(catStr) } catch (e: Exception) { TransactionCategory.LAINNYA }

                    val payStr = obj.optString("paymentMethod", "TUNAI")
                    val paymentMethod = try { PaymentMethod.valueOf(payStr) } catch (e: Exception) { PaymentMethod.TUNAI }

                    val freqStr = obj.optString("frequency", "DAILY")
                    val frequency = try { RecurringFrequency.valueOf(freqStr) } catch (e: Exception) { RecurringFrequency.DAILY }

                    ruleList.add(
                        RecurringRuleEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", "Aturan"),
                            amount = obj.optDouble("amount", 0.0),
                            type = type,
                            category = category,
                            paymentMethod = paymentMethod,
                            frequency = frequency,
                            lastExecutedTimestamp = obj.optLong("lastExecutedTimestamp", 0L)
                        )
                    )
                }
            }

            BackupData(
                exportDate = exportTimestamp,
                budget = budget,
                transactions = txList,
                recurringRules = ruleList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun readJsonFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getSuggestedFileName(): String {
        val dateStr = fileDateFormat.format(Date())
        return "UANGKU_Backup_$dateStr.json"
    }

    fun shareToGoogleDrive(context: Context, jsonString: String) {
        try {
            val fileName = getSuggestedFileName()
            val cacheFile = java.io.File(context.cacheDir, fileName)
            java.io.FileOutputStream(cacheFile).use { os ->
                os.write(jsonString.toByteArray(Charsets.UTF_8))
            }

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "Cadangan Keuangan UANGKU")
                putExtra(Intent.EXTRA_TEXT, "File cadangan catatan keuangan UANGKU untuk disimpan ke Google Drive.")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Prefer Google Drive docs package if available
                setPackage("com.google.android.apps.docs")
            }

            try {
                context.startActivity(Intent.createChooser(shareIntent, "Simpan Cadangan ke Google Drive"))
            } catch (e: Exception) {
                // Fallback to standard chooser if drive package explicit intent fails
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "Cadangan Keuangan UANGKU")
                    putExtra(Intent.EXTRA_TEXT, "File cadangan catatan keuangan UANGKU.")
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(genericIntent, "Simpan ke Google Drive / Bagikan"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal menyiapkan file cadangan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
