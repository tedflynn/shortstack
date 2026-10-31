package com.example.allowancetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.allowancetracker.data.model.CategoryEntity
import com.example.allowancetracker.data.model.KidProfile
import com.example.allowancetracker.data.model.Transaction
import com.example.allowancetracker.data.model.TransactionType

class TransactionTypeConverters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): String =
        TransactionType.valueOf(value).name
}

@Database(
    entities = [Transaction::class, KidProfile::class, CategoryEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(TransactionTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DATABASE_NAME = "allowance_tracker_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
