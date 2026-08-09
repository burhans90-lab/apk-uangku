package com.example.data.repository

import com.example.data.dao.RecurringDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.RecurringRuleEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

class UangkuRepository(
    private val transactionDao: TransactionDao,
    private val recurringDao: RecurringDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val budgetFlow: Flow<BudgetEntity?> = transactionDao.getBudgetFlow()
    val allRecurringRules: Flow<List<RecurringRuleEntity>> = recurringDao.getAllRules()

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun deleteTransactions(ids: List<Long>) {
        transactionDao.deleteTransactionsByIds(ids)
    }

    suspend fun clearAllTransactions() {
        transactionDao.clearAllTransactions()
    }

    suspend fun setBudget(daily: Double, monthly: Double, minBalance: Double = 300000.0, savingsTarget: Double = 1000000.0) {
        transactionDao.setBudget(BudgetEntity(id = 1, dailyLimit = daily, monthlyLimit = monthly, minBalanceThreshold = minBalance, monthlySavingsTarget = savingsTarget))
    }

    suspend fun insertRecurringRule(rule: RecurringRuleEntity): Long {
        return recurringDao.insertRule(rule)
    }

    suspend fun updateRecurringRule(rule: RecurringRuleEntity) {
        recurringDao.updateRule(rule)
    }

    suspend fun deleteRecurringRule(id: Long) {
        recurringDao.deleteRuleById(id)
    }

    suspend fun getRecurringRulesSync(): List<RecurringRuleEntity> {
        return recurringDao.getAllRulesSync()
    }

    suspend fun getBudgetSync(): BudgetEntity? {
        return transactionDao.getBudgetSync()
    }
}
