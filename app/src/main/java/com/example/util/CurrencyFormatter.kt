package com.example.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun formatRupiah(amount: Double, showSymbol: Boolean = true): String {
        val localeID = Locale("in", "ID")
        val formatter = NumberFormat.getCurrencyInstance(localeID)
        formatter.maximumFractionDigits = 0

        val formatted = formatter.format(amount)
        // Adjust default "Rp" formatting to clean space if needed
        return if (showSymbol) {
            formatted.replace("Rp", "Rp ")
        } else {
            formatted.replace("Rp", "").trim()
        }
    }

    fun formatDigitsWithDots(digits: String): String {
        val cleanDigits = digits.filter { it.isDigit() }
        if (cleanDigits.isEmpty()) return ""
        val parsed = cleanDigits.toLongOrNull() ?: return cleanDigits
        val localeID = Locale("in", "ID")
        val formatter = NumberFormat.getNumberInstance(localeID)
        return formatter.format(parsed)
    }

    fun formatCompact(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> String.format(Locale("in", "ID"), "%.1f M", amount / 1_000_000_000)
            amount >= 1_000_000 -> String.format(Locale("in", "ID"), "%.1f Jt", amount / 1_000_000)
            amount >= 1_000 -> String.format(Locale("in", "ID"), "%.0f Rb", amount / 1_000)
            else -> amount.toInt().toString()
        }
    }
}
