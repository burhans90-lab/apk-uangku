package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.AddRecurringRuleDialog
import com.example.ui.components.GoogleDriveSyncDialog
import com.example.ui.components.SetBudgetDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.UangkuTheme
import com.example.ui.viewmodel.UangkuViewModel

enum class NavigationTab(val title: String, val icon: ImageVector, val tag: String) {
    HOME("Beranda", Icons.Default.Home, "tab_home"),
    HISTORY("Riwayat", Icons.Default.ReceiptLong, "tab_history"),
    ANALYTICS("Analisis", Icons.Default.BarChart, "tab_analytics"),
    AUTOMATION("Otomatisasi", Icons.Default.AutoAwesome, "tab_automation")
}

class MainActivity : ComponentActivity() {

    private val viewModel: UangkuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UangkuTheme {
                UangkuMainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UangkuMainApp(viewModel: UangkuViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showDriveSyncDialog by remember { mutableStateOf(false) }

    val dailyBudget by viewModel.dailyBudgetLimit.collectAsState()
    val monthlyBudget by viewModel.monthlyBudgetLimit.collectAsState()
    val minBalanceThreshold by viewModel.minBalanceThreshold.collectAsState()
    val monthlySavingsTarget by viewModel.monthlySavingsTarget.collectAsState()
    val lastSyncTs by viewModel.lastSyncTimestamp.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "UANGKU",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showDriveSyncDialog = true },
                        modifier = Modifier.testTag("topbar_drive_sync_button")
                    ) {
                        Icon(
                            imageVector = if (lastSyncTs != null && lastSyncTs!! > 0) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            contentDescription = "Sinkronisasi Google Drive",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(text = tab.title, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == NavigationTab.HOME) {
                FloatingActionButton(
                    onClick = { showAddTransactionSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Catatan Manual"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MainTabContent(
                currentTab = currentTab,
                viewModel = viewModel,
                onAdjustBudgetClick = { showBudgetDialog = true },
                onSeeAllClick = { currentTab = NavigationTab.HISTORY },
                onAddRuleClick = { showAddRecurringDialog = true },
                onOpenDriveSync = { showDriveSyncDialog = true }
            )
        }
    }

    // Dialogs & Sheets
    if (showDriveSyncDialog) {
        GoogleDriveSyncDialog(
            viewModel = viewModel,
            onDismiss = { showDriveSyncDialog = false }
        )
    }

    if (showAddTransactionSheet) {
        AddEditTransactionSheet(
            onDismiss = { showAddTransactionSheet = false },
            onSave = { title, amount, type, category, paymentMethod, note ->
                viewModel.addTransaction(title, amount, type, category, paymentMethod, note = note)
            }
        )
    }

    if (showBudgetDialog) {
        SetBudgetDialog(
            currentDailyBudget = dailyBudget,
            currentMonthlyBudget = monthlyBudget,
            currentMinBalance = minBalanceThreshold,
            currentSavingsTarget = monthlySavingsTarget,
            onDismiss = { showBudgetDialog = false },
            onSave = { daily, monthly, minBal, savingsTarget ->
                viewModel.updateBudgetSettings(daily, monthly, minBal, savingsTarget)
            }
        )
    }

    if (showAddRecurringDialog) {
        AddRecurringRuleDialog(
            onDismiss = { showAddRecurringDialog = false },
            onSave = { title, amount, type, category, paymentMethod, frequency ->
                viewModel.addRecurringRule(title, amount, type, category, paymentMethod, frequency)
            }
        )
    }
}

@Composable
private fun MainTabContent(
    currentTab: NavigationTab,
    viewModel: UangkuViewModel,
    onAdjustBudgetClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onAddRuleClick: () -> Unit,
    onOpenDriveSync: () -> Unit
) {
    when (currentTab) {
        NavigationTab.HOME -> HomeScreen(
            viewModel = viewModel,
            onAdjustBudgetClick = onAdjustBudgetClick,
            onSeeAllClick = onSeeAllClick
        )

        NavigationTab.HISTORY -> HistoryScreen(
            viewModel = viewModel
        )

        NavigationTab.ANALYTICS -> AnalyticsScreen(
            viewModel = viewModel
        )

        NavigationTab.AUTOMATION -> AutomationScreen(
            viewModel = viewModel,
            onAddRuleClick = onAddRuleClick,
            onOpenDriveSync = onOpenDriveSync
        )
    }
}
