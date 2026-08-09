package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class TransactionCategory(val displayName: String, val colorHex: Long) {
    MAKANAN("Makanan & Minuman", 0xFFFF6B6B),
    JAJAN("Jajan", 0xFFFF70A6),
    KEBUTUHAN_DAPUR("Kebutuhan Dapur", 0xFF80B918),
    TRANSPORT("Transportasi", 0xFF4D96FF),
    BELANJA("Belanja & Kebutuhan", 0xFFFFB84C),
    NAFKAH_KELUARGA("Nafkah Keluarga", 0xFFFF9800),
    KESEHATAN("Kesehatan & Berobat", 0xFFE63946),
    PERAWATAN_KENDARAAN("Perawatan Kendaraan", 0xFFF4A261),
    PENDIDIKAN("Pendidikan", 0xFF4361EE),
    TAGIHAN("Tagihan & Utilitas", 0xFF9966FF),
    HIBURAN("Hiburan & Gaya Hidup", 0xFFFF78C4),
    GAJI("Gaji", 0xFF6BCB77),
    TPP("TPP", 0xFF00B4D8),
    SERTIFIKASI("Sertifikasi", 0xFF2A9D8F),
    UANG_SAKU("Uang Saku", 0xFFFFB703),
    INVESTASI("Investasi", 0xFF00C9A7),
    LAINNYA("Lain-lain", 0xFF8D99AE);

    fun getIcon(): ImageVector {
        return when (this) {
            MAKANAN -> Icons.Default.Fastfood
            JAJAN -> Icons.Default.ShoppingBag
            KEBUTUHAN_DAPUR -> Icons.Default.Home
            TRANSPORT -> Icons.Default.DirectionsBus
            BELANJA -> Icons.Default.ShoppingBag
            NAFKAH_KELUARGA -> Icons.Default.Home
            KESEHATAN -> Icons.Default.LocalHospital
            PERAWATAN_KENDARAAN -> Icons.Default.Build
            PENDIDIKAN -> Icons.Default.School
            TAGIHAN -> Icons.Default.Receipt
            HIBURAN -> Icons.Default.FitnessCenter
            GAJI -> Icons.Default.Work
            TPP -> Icons.Default.Payments
            SERTIFIKASI -> Icons.Default.School
            UANG_SAKU -> Icons.Default.AccountBalanceWallet
            INVESTASI -> Icons.Default.Money
            LAINNYA -> Icons.Default.MoreHoriz
        }
    }
}

enum class PaymentMethod(val displayName: String) {
    TUNAI("Tunai"),
    TRANSFER_BANK("Transfer Bank"),
    E_WALLET("E-Wallet"),
    KARTU_KREDIT("Kartu Kredit");

    fun getIcon(): ImageVector {
        return when (this) {
            TUNAI -> Icons.Default.Money
            TRANSFER_BANK -> Icons.Default.AccountBalanceWallet
            E_WALLET -> Icons.Default.Payment
            KARTU_KREDIT -> Icons.Default.CreditCard
        }
    }
}

enum class RecurringFrequency(val displayName: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan")
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val frequency: RecurringFrequency = RecurringFrequency.DAILY,
    val lastExecutedTimestamp: Long = 0
)

@Entity(tableName = "user_budget")
data class BudgetEntity(
    @PrimaryKey val id: Int = 1,
    val dailyLimit: Double = 100000.0,
    val monthlyLimit: Double = 3000000.0,
    val minBalanceThreshold: Double = 300000.0,
    val monthlySavingsTarget: Double = 1000000.0
)
