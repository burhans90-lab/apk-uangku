package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.RecurringDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.RecurringRuleEntity
import com.example.data.model.TransactionEntity

@Database(
    entities = [TransactionEntity::class, RecurringRuleEntity::class, BudgetEntity::class],
    version = 3,
    exportSchema = false
)
abstract class UangkuDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        @Volatile
        private var INSTANCE: UangkuDatabase? = null

        fun getDatabase(context: Context): UangkuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UangkuDatabase::class.java,
                    "uangku_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
