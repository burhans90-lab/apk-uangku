package com.example.util

import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType

data class ParsedTransaction(
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory
)

object QuickTextParser {
    fun parse(input: String): ParsedTransaction? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val amountRegex = Regex("""(\d+[\d.,]*)\s*(k|rb|ribu|jt|juta)?""", RegexOption.IGNORE_CASE)
        val match = amountRegex.find(trimmed) ?: return null

        val rawNumStr = match.groupValues[1].replace(".", "").replace(",", ".")
        val num = rawNumStr.toDoubleOrNull() ?: return null
        val suffix = match.groupValues[2].lowercase()

        val multiplier = when (suffix) {
            "k", "rb", "ribu" -> 1000.0
            "jt", "juta" -> 1000000.0
            else -> 1.0
        }

        val amount = num * multiplier
        if (amount <= 0) return null

        val titleCandidate = trimmed.removeRange(match.range).trim()
            .replace(Regex("""^\s*[-+]|\s*[-+]\s*$"""), "")
            .ifEmpty { "Pencatatan Cepat" }

        val lowerText = trimmed.lowercase()

        val type = if (lowerText.contains("gaji") || lowerText.contains("tpp") || lowerText.contains("serti") || lowerText.contains("saku") || lowerText.contains("pemasukan") || lowerText.contains("dapat") || lowerText.contains("bonus") || lowerText.contains("transfer masuk")) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        val category = when {
            lowerText.contains("jajan") -> TransactionCategory.JAJAN
            lowerText.contains("bumbu") || lowerText.contains("minyak") || lowerText.contains("gas") || lowerText.contains("galon") || lowerText.contains("lauk") || lowerText.contains("buah") || lowerText.contains("beras") || lowerText.contains("dapur") -> TransactionCategory.KEBUTUHAN_DAPUR
            lowerText.contains("makan") || lowerText.contains("kopi") || lowerText.contains("minum") || lowerText.contains("bakso") || lowerText.contains("nasi") -> TransactionCategory.MAKANAN
            lowerText.contains("bensin") || lowerText.contains("gojek") || lowerText.contains("grab") || lowerText.contains("parkir") || lowerText.contains("angkot") -> TransactionCategory.TRANSPORT
            lowerText.contains("obat") || lowerText.contains("sakit") || lowerText.contains("dokter") || lowerText.contains("berobat") || lowerText.contains("rs") || lowerText.contains("puskesmas") || lowerText.contains("klinik") || lowerText.contains("apotek") || lowerText.contains("vitamin") -> TransactionCategory.KESEHATAN
            lowerText.contains("bengkel") || lowerText.contains("servis") || lowerText.contains("service") || lowerText.contains("motor") || lowerText.contains("mobil") || lowerText.contains("oli") || lowerText.contains("ban") || lowerText.contains("perbaikan") || lowerText.contains("onderdil") || lowerText.contains("tambal") -> TransactionCategory.PERAWATAN_KENDARAAN
            lowerText.contains("nafkah") || lowerText.contains("istri") || lowerText.contains("keluarga") || lowerText.contains("pempers") || lowerText.contains("popok") || lowerText.contains("susu") || lowerText.contains("gula") || lowerText.contains("sayur") || lowerText.contains("telur") -> TransactionCategory.NAFKAH_KELUARGA
            lowerText.contains("spp") || lowerText.contains("sekolah") || lowerText.contains("kuliah") || lowerText.contains("pendidikan") || lowerText.contains("les") || lowerText.contains("buku") -> TransactionCategory.PENDIDIKAN
            lowerText.contains("baju") || lowerText.contains("sepatu") || lowerText.contains("mart") || lowerText.contains("indomaret") || lowerText.contains("alfamart") || lowerText.contains("belanja") -> TransactionCategory.BELANJA
            lowerText.contains("listrik") || lowerText.contains("air") || lowerText.contains("wifi") || lowerText.contains("pulsa") || lowerText.contains("kos") || lowerText.contains("tagihan") -> TransactionCategory.TAGIHAN
            lowerText.contains("nonton") || lowerText.contains("game") || lowerText.contains("bioskop") || lowerText.contains("gym") -> TransactionCategory.HIBURAN
            lowerText.contains("tpp") -> TransactionCategory.TPP
            lowerText.contains("serti") -> TransactionCategory.SERTIFIKASI
            lowerText.contains("saku") -> TransactionCategory.UANG_SAKU
            lowerText.contains("gaji") || lowerText.contains("bonus") -> TransactionCategory.GAJI
            lowerText.contains("saham") || lowerText.contains("reksa") || lowerText.contains("crypto") -> TransactionCategory.INVESTASI
            else -> TransactionCategory.LAINNYA
        }

        return ParsedTransaction(
            title = titleCandidate.replaceFirstChar { it.uppercase() },
            amount = amount,
            type = type,
            category = category
        )
    }
}
