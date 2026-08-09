package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.UangkuDatabase
import com.example.data.model.*
import com.example.data.repository.UangkuRepository
import com.example.util.DateUtils
import com.example.util.ParsedTransaction
import com.example.util.QuickTextParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UangkuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UangkuRepository

    val transactions: StateFlow<List<TransactionEntity>>
    val recurringRules: StateFlow<List<RecurringRuleEntity>>
    val budget: StateFlow<BudgetEntity>

    // Quick text input state
    val quickInputText = MutableStateFlow("")
    val parsedQuickTransaction: StateFlow<ParsedTransaction?> = quickInputText
        .map { QuickTextParser.parse(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoProcessedBannerMessage = MutableStateFlow<String?>(null)

    // Search and Filters
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<TransactionCategory?>(null)
    val selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val startDateFilter = MutableStateFlow<Long?>(null)
    val endDateFilter = MutableStateFlow<Long?>(null)

    val filteredTransactions: StateFlow<List<TransactionEntity>>

    // Calculations
    val totalBalance: StateFlow<Double>
    val monthlyIncome: StateFlow<Double>
    val monthlyExpense: StateFlow<Double>
    val todayExpense: StateFlow<Double>

    val dailyBudgetLimit: StateFlow<Double>
    val monthlyBudgetLimit: StateFlow<Double>
    val minBalanceThreshold: StateFlow<Double>
    val monthlySavingsTarget: StateFlow<Double>

    init {
        val db = UangkuDatabase.getDatabase(application)
        repository = UangkuRepository(db.transactionDao(), db.recurringDao())

        transactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        recurringRules = repository.allRecurringRules
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        budget = repository.budgetFlow
            .map { it ?: BudgetEntity() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetEntity())

        dailyBudgetLimit = budget.map { it.dailyLimit }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100000.0)

        monthlyBudgetLimit = budget.map { it.monthlyLimit }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000000.0)

        minBalanceThreshold = budget.map { it.minBalanceThreshold }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 300000.0)

        monthlySavingsTarget = budget.map { it.monthlySavingsTarget }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1000000.0)

        totalBalance = transactions.map { txs ->
            txs.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        monthlyIncome = transactions.map { txs ->
            txs.filter { it.type == TransactionType.INCOME && DateUtils.isThisMonth(it.timestamp) }
                .sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        monthlyExpense = transactions.map { txs ->
            txs.filter { it.type == TransactionType.EXPENSE && DateUtils.isThisMonth(it.timestamp) }
                .sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        todayExpense = transactions.map { txs ->
            txs.filter { it.type == TransactionType.EXPENSE && DateUtils.isToday(it.timestamp) }
                .sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        val dateRangeFlow = combine(startDateFilter, endDateFilter) { start, end -> Pair(start, end) }

        filteredTransactions = combine(
            transactions,
            searchQuery,
            selectedCategoryFilter,
            selectedTypeFilter,
            dateRangeFlow
        ) { txs, query, cat, type, dateRange ->
            val (startMs, endMs) = dateRange
            txs.filter { tx ->
                val matchesQuery = query.isEmpty() ||
                        tx.title.contains(query, ignoreCase = true) ||
                        tx.note.contains(query, ignoreCase = true) ||
                        tx.category.displayName.contains(query, ignoreCase = true) ||
                        tx.paymentMethod.displayName.contains(query, ignoreCase = true)

                val matchesCat = cat == null || tx.category == cat
                val matchesType = type == null || tx.type == type
                val matchesStart = startMs == null || tx.timestamp >= startMs
                val matchesEnd = endMs == null || tx.timestamp <= endMs

                matchesQuery && matchesCat && matchesType && matchesStart && matchesEnd
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Automatically run recurring checks on startup
        runAutoProcessing()
    }

    fun onQuickInputChange(text: String) {
        quickInputText.value = text
    }

    fun submitQuickInput() {
        val parsed = parsedQuickTransaction.value ?: return
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = parsed.title,
                    amount = parsed.amount,
                    type = parsed.type,
                    category = parsed.category
                )
            )
            quickInputText.value = ""
        }
    }

    fun addQuickTemplate(title: String, amount: Double, category: TransactionCategory) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = category
                )
            )
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        paymentMethod: PaymentMethod,
        note: String
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    paymentMethod = paymentMethod,
                    note = note
                )
            )
        }
    }

    fun updateTransactionEntity(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun deleteTransactions(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteTransactions(ids)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
    }

    fun updateBudgetSettings(daily: Double, monthly: Double, minBalance: Double, savingsTarget: Double = monthlySavingsTarget.value) {
        viewModelScope.launch {
            repository.setBudget(daily = daily, monthly = monthly, minBalance = minBalance, savingsTarget = savingsTarget)
        }
    }

    fun updateMonthlySavingsTarget(target: Double) {
        viewModelScope.launch {
            repository.setBudget(
                daily = dailyBudgetLimit.value,
                monthly = monthlyBudgetLimit.value,
                minBalance = minBalanceThreshold.value,
                savingsTarget = target
            )
        }
    }

    fun updateDailyBudget(amount: Double) {
        viewModelScope.launch {
            repository.setBudget(daily = amount, monthly = monthlyBudgetLimit.value, minBalance = minBalanceThreshold.value)
        }
    }

    fun updateMonthlyBudget(amount: Double) {
        viewModelScope.launch {
            repository.setBudget(daily = dailyBudgetLimit.value, monthly = amount, minBalance = minBalanceThreshold.value)
        }
    }

    fun addRecurringRule(
        title: String,
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        paymentMethod: PaymentMethod,
        frequency: RecurringFrequency
    ) {
        viewModelScope.launch {
            repository.insertRecurringRule(
                RecurringRuleEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    paymentMethod = paymentMethod,
                    frequency = frequency
                )
            )
            runAutoProcessing()
        }
    }

    fun deleteRecurringRule(id: Long) {
        viewModelScope.launch {
            repository.deleteRecurringRule(id)
        }
    }

    fun runAutoProcessing() {
        viewModelScope.launch {
            val rules = repository.getRecurringRulesSync()
            var addedCount = 0

            val now = System.currentTimeMillis()
            for (rule in rules) {
                val shouldExecute = when (rule.frequency) {
                    RecurringFrequency.DAILY -> !DateUtils.isToday(rule.lastExecutedTimestamp)
                    RecurringFrequency.WEEKLY -> (now - rule.lastExecutedTimestamp) >= (7 * 24 * 60 * 60 * 1000L)
                    RecurringFrequency.MONTHLY -> !DateUtils.isThisMonth(rule.lastExecutedTimestamp)
                }

                if (shouldExecute) {
                    repository.insertTransaction(
                        TransactionEntity(
                            title = rule.title + " (Otomatis)",
                            amount = rule.amount,
                            type = rule.type,
                            category = rule.category,
                            paymentMethod = rule.paymentMethod,
                            note = "Dicurahkan secara otomatis oleh aturan rutin."
                        )
                    )
                    repository.updateRecurringRule(rule.copy(lastExecutedTimestamp = now))
                    addedCount++
                }
            }

            if (addedCount > 0) {
                autoProcessedBannerMessage.value = "Terdeteksi $addedCount transaksi rutin harian berhasil dicatat otomatis!"
            }
        }
    }

    fun clearAutoBanner() {
        autoProcessedBannerMessage.value = null
    }
}
