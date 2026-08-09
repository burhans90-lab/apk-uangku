package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("in", "ID"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("in", "ID"))
    private val printDateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("in", "ID"))

    fun generateCsvContent(periodTitle: String, transactions: List<TransactionEntity>): String {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        val sb = StringBuilder()
        // UTF-8 BOM byte for Excel & mobile office app compatibility
        sb.append("\uFEFF")
        // Explicit delimiter directive for Microsoft Excel & CSV viewers
        sb.append("sep=;\n")

        // Report Title & Metadata
        sb.append("LAPORAN REKAPITULASI KEUANGAN - UANGKU\n")
        sb.append("Periode Laporan;\"").append(periodTitle).append("\"\n")
        sb.append("Waktu Ekspor;\"").append(printDateFormat.format(Date())).append("\"\n")
        sb.append("Total Pemasukan;\"Rp ").append(CurrencyFormatter.formatDigitsWithDots(totalIncome.toLong().toString())).append("\"\n")
        sb.append("Total Pengeluaran;\"Rp ").append(CurrencyFormatter.formatDigitsWithDots(totalExpense.toLong().toString())).append("\"\n")
        sb.append("Saldo Net Kas;\"Rp ").append(CurrencyFormatter.formatDigitsWithDots(netBalance.toLong().toString())).append("\"\n")
        sb.append("Total Transaksi;\"").append(transactions.size).append(" data\"\n\n")

        // Column Table Header
        sb.append("No;Tanggal;Waktu;Jenis Transaksi;Kategori;Metode Pembayaran;Pemasukan (Rp);Pengeluaran (Rp);Keterangan\n")

        // Rows
        transactions.forEachIndexed { index, tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val timeStr = timeFormat.format(Date(tx.timestamp))
            val typeStr = if (tx.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
            val categoryStr = tx.category.displayName
            val paymentStr = tx.paymentMethod.displayName
            val incomeAmount = if (tx.type == TransactionType.INCOME) tx.amount.toLong() else 0
            val expenseAmount = if (tx.type == TransactionType.EXPENSE) tx.amount.toLong() else 0
            val noteEscaped = tx.note.replace("\"", "\"\"").replace("\n", " ")

            sb.append("${index + 1};")
                .append("\"$dateStr\";")
                .append("\"$timeStr\";")
                .append("\"$typeStr\";")
                .append("\"$categoryStr\";")
                .append("\"$paymentStr\";")
                .append("$incomeAmount;")
                .append("$expenseAmount;")
                .append("\"$noteEscaped\"\n")
        }

        // CSV Total Row
        sb.append(";;;;;\"TOTAL SUM\";${totalIncome.toLong()};${totalExpense.toLong()};\"\"\n")

        return sb.toString()
    }

    fun exportAndShareReport(context: Context, periodTitle: String, transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "Tidak ada data transaksi untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanPeriod = periodTitle.replace(" ", "_").replace("/", "-")
        val fileName = "Laporan_Keuangan_Uangku_${cleanPeriod}.csv"
        val csvData = generateCsvContent(periodTitle, transactions)

        try {
            // Save to internal cache for sharing via Intent
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val cacheFile = File(reportsDir, fileName)
            FileOutputStream(cacheFile).use { fos ->
                fos.write(csvData.toByteArray(Charsets.UTF_8))
            }

            // Also try to save to public Downloads folder for user backup
            var savedToDownloads = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(csvData.toByteArray(Charsets.UTF_8))
                    }
                    savedToDownloads = true
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                    val publicFile = File(downloadsDir, fileName)
                    FileOutputStream(publicFile).use { fos ->
                        fos.write(csvData.toByteArray(Charsets.UTF_8))
                    }
                    savedToDownloads = true
                }
            }

            if (savedToDownloads) {
                Toast.makeText(context, "✅ Laporan Excel (.csv) tersimpan di folder Download!", Toast.LENGTH_LONG).show()
            }

            // Trigger Share/Open Intent
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan Uangku - $periodTitle")
                putExtra(Intent.EXTRA_TEXT, "Berikut rekapitulasi laporan keuangan bulanan Uangku ($periodTitle) dalam format CSV.")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Unduh / Buka Laporan CSV ($fileName)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengekspor laporan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

