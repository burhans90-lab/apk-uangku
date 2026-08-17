package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.UangkuViewModel
import com.example.util.BackupData
import com.example.util.BackupSyncManager
import java.util.Date

@Composable
fun GoogleDriveSyncDialog(
    viewModel: UangkuViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lastSyncTs by viewModel.lastSyncTimestamp.collectAsState()
    val syncErrorMessage by viewModel.syncErrorMessage.collectAsState()
    val syncSuccessMessage by viewModel.syncSuccessMessage.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val recurringRules by viewModel.recurringRules.collectAsState()

    var pendingRestoreData by remember { mutableStateOf<BackupData?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var lastFailedOperation by remember { mutableStateOf<String?>(null) } // "backup" or "restore"

    // SAF Document Creator for direct Google Drive / Storage saving
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = viewModel.generateBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }
                viewModel.updateLastSyncTimestamp()
                lastFailedOperation = null
                Toast.makeText(context, "✅ Berhasil mencadangkan data ke Google Drive!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                lastFailedOperation = "backup"
                viewModel.setSyncError("Gagal menyimpan cadangan ke Google Drive: ${e.localizedMessage}")
            }
        } else {
            lastFailedOperation = "backup"
            viewModel.setSyncError("Proses pencadangan dibatalkan atau izin folder Google Drive tidak diberikan.")
        }
    }

    // SAF Document Picker for Google Drive / Local JSON restoring
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val jsonString = BackupSyncManager.readJsonFromUri(context, uri)
            if (jsonString != null) {
                val parsed = BackupSyncManager.parseBackupJson(jsonString)
                if (parsed != null) {
                    pendingRestoreData = parsed
                    viewModel.clearSyncStatus()
                    lastFailedOperation = null
                } else {
                    lastFailedOperation = "restore"
                    viewModel.setSyncError("Format file cadangan Google Drive tidak valid atau rusak!")
                }
            } else {
                lastFailedOperation = "restore"
                viewModel.setSyncError("Gagal membaca file dari Google Drive. Pastikan akun & koneksi aktif.")
            }
        } else {
            lastFailedOperation = "restore"
            viewModel.setSyncError("Proses pemulihan dibatalkan: Tidak ada file cadangan yang dipilih.")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (syncErrorMessage != null) Icons.Default.CloudOff else Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (syncErrorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Sinkronisasi Google Drive",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Amankan catatan keuangan Anda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sync Status Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (syncErrorMessage != null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (syncErrorMessage != null) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (syncErrorMessage != null) MaterialTheme.colorScheme.error else IncomeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (syncErrorMessage != null) "Status: Gagal Sinkronisasi" else "Status: Google Drive Siap",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (syncErrorMessage != null) MaterialTheme.colorScheme.error else IncomeGreen
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                        Text(
                            text = if (lastSyncTs != null && lastSyncTs!! > 0) {
                                "Terakhir disinkronkan:\n${BackupSyncManager.displayDateFormat.format(Date(lastSyncTs!!))}"
                            } else {
                                "Terakhir disinkronkan: Belum Pernah"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Data lokal: ${transactions.size} transaksi, ${recurringRules.size} aturan otomatis.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Error Notification Banner with Retry
                if (syncErrorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sinkronisasi Terkendala",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = syncErrorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.clearSyncStatus() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                ) {
                                    Text("Abaikan")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        viewModel.clearSyncStatus()
                                        if (lastFailedOperation == "restore") {
                                            openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                                        } else {
                                            val suggestedName = BackupSyncManager.getSuggestedFileName()
                                            createDocumentLauncher.launch(suggestedName)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Coba Lagi", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                // Success Message Banner if recently synced
                if (syncSuccessMessage != null && syncErrorMessage == null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = IncomeGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = syncSuccessMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        }
                    }
                }

                // Info Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhonelinkSetup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Catatan keuangan tidak akan hilang saat Anda berganti HP atau melakukan reset.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Sync Action Buttons
                Button(
                    onClick = {
                        viewModel.clearSyncStatus()
                        val suggestedName = BackupSyncManager.getSuggestedFileName()
                        createDocumentLauncher.launch(suggestedName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_drive_backup_save"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Cadangan ke Google Drive")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.clearSyncStatus()
                        openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_drive_restore_open"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pulihkan Data dari Google Drive")
                }

                // Share directly to Google Drive option
                TextButton(
                    onClick = {
                        val json = viewModel.generateBackupJson()
                        BackupSyncManager.shareToGoogleDrive(context, json)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Opsi Berbagi / Kirim Cadangan", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )

    // Restore Options Dialog
    pendingRestoreData?.let { data ->
        val exportDateStr = BackupSyncManager.displayDateFormat.format(Date(data.exportDate))
        AlertDialog(
            onDismissRequest = { pendingRestoreData = null },
            title = {
                Text(
                    text = "Pulihkan Catatan Keuangan?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ditemukan file cadangan tertanggal:\n$exportDateStr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• ${data.transactions.size} transaksi\n• ${data.recurringRules.size} aturan otomatis\n• Pengaturan Anggaran",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Pilih metode pemulihan data:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = {
                            isRestoring = true
                            viewModel.restoreBackup(data, mergeMode = true) { success, count ->
                                isRestoring = false
                                pendingRestoreData = null
                                if (success) {
                                    Toast.makeText(context, "✅ $count transaksi baru berhasil digabungkan!", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.setSyncError("Gagal menggabungkan data cadangan.")
                                }
                            }
                        },
                        enabled = !isRestoring,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Gabungkan dengan Data Saat Ini")
                    }

                    OutlinedButton(
                        onClick = {
                            isRestoring = true
                            viewModel.restoreBackup(data, mergeMode = false) { success, count ->
                                isRestoring = false
                                pendingRestoreData = null
                                if (success) {
                                    Toast.makeText(context, "✅ Berhasil memulihkan $count transaksi!", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.setSyncError("Gagal menggantikan data cadangan.")
                                }
                            }
                        },
                        enabled = !isRestoring,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Gantikan Seluruh Data Saat Ini")
                    }

                    TextButton(
                        onClick = { pendingRestoreData = null },
                        enabled = !isRestoring,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Batal")
                    }
                }
            },
            confirmButton = {}
        )
    }
}
