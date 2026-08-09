package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionCategory
import com.example.util.CurrencyFormatter

data class QuickTemplate(
    val title: String,
    val amount: Double,
    val category: TransactionCategory
)

val defaultQuickTemplates = listOf(
    QuickTemplate("Kopi Pagi", 18000.0, TransactionCategory.MAKANAN),
    QuickTemplate("Makan Siang", 25000.0, TransactionCategory.MAKANAN),
    QuickTemplate("Bensin Motor", 35000.0, TransactionCategory.TRANSPORT),
    QuickTemplate("Jajan Anak", 15000.0, TransactionCategory.JAJAN),
    QuickTemplate("Belanja Dapur", 50000.0, TransactionCategory.KEBUTUHAN_DAPUR)
)

@Composable
fun QuickTemplateRow(
    onTemplateClick: (String, Double, TransactionCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Template 1-Tap Cepat",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_template_row")
        ) {
            items(defaultQuickTemplates) { item ->
                OutlinedCard(
                    onClick = { onTemplateClick(item.title, item.amount, item.category) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.category.getIcon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.title} (${CurrencyFormatter.formatRupiah(item.amount)})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}
