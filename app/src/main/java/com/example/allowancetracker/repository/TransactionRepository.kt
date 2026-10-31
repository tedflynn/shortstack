package com.example.allowancetracker.repository

import androidx.room.withTransaction
import com.example.allowancetracker.data.backup.BackupCodec
import com.example.allowancetracker.data.local.AppDatabase
import com.example.allowancetracker.data.local.CategorySum
import com.example.allowancetracker.data.model.CategoryEntity
import com.example.allowancetracker.data.model.DefaultCategories
import com.example.allowancetracker.data.model.KidProfile
import com.example.allowancetracker.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class TransactionRepository(
    private val database: AppDatabase
) {
    private val transactionDao = database.transactionDao()

    val kidNames: Flow<List<String>> = transactionDao.getAllKidNames()

    val categories: Flow<List<String>> = transactionDao.getAllCategoryNames()

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsForKid(kidName: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForKid(kidName)
    }

    fun getRunningBalanceForKid(kidName: String): Flow<Long?> {
        return transactionDao.getRunningBalanceForKid(kidName)
    }

    fun getCreditTotalsByCategoryForKidInRange(
        kidName: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<CategorySum>> {
        return transactionDao.getCreditTotalsByCategoryForKidInRange(
            kidName = kidName,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )
    }

    fun getDebitTotalsByCategoryForKidInRange(
        kidName: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<CategorySum>> {
        return transactionDao.getDebitTotalsByCategoryForKidInRange(
            kidName = kidName,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )
    }

    suspend fun seedDefaultsIfNeeded() {
        withContext(Dispatchers.IO) {
            if (transactionDao.getAllCategoryNames().first().isEmpty()) {
                DefaultCategories.DEFAULT_CATEGORIES.forEach { name ->
                    transactionDao.insertCategory(CategoryEntity(name = name))
                }
            }
        }
    }

    suspend fun insertKid(name: String) {
        withContext(Dispatchers.IO) {
            transactionDao.insertKid(KidProfile(name = name.trim()))
        }
    }

    suspend fun deleteUnusedKid(name: String): Boolean {
        return withContext(Dispatchers.IO) {
            transactionDao.deleteUnusedKid(name) > 0
        }
    }

    suspend fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        withContext(Dispatchers.IO) {
            transactionDao.insertCategory(CategoryEntity(name = trimmed))
        }
    }

    suspend fun deleteUnusedCategory(name: String): Boolean {
        return withContext(Dispatchers.IO) {
            val deletedRows = transactionDao.deleteUnusedCategory(name.trim())
            deletedRows > 0
        }
    }

    suspend fun insertTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                transactionDao.insertKid(KidProfile(name = transaction.kidName))
                transactionDao.insertCategory(CategoryEntity(name = transaction.category))
                transactionDao.insert(transaction)
            }
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            transactionDao.delete(transaction)
        }
    }

    /** Writes every kid, category and transaction. Closes [outputStream]. */
    suspend fun exportToJson(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                val payload = database.withTransaction {
                    val transactions = transactionDao.getAllTransactions().first()
                    BackupCodec.encode(
                        kids = transactionDao.getAllKidsOnce(),
                        categories = transactionDao.getAllCategoryNamesOnce(),
                        transactions = transactions
                    ) to transactions.size
                }
                writer.write(payload.first)
                payload.second
            }
        }
    }

    /**
     * Restores a backup. The file is fully validated before the database is
     * touched, and the replace runs in one database transaction, so a failure
     * leaves existing data exactly as it was. Closes [inputStream].
     */
    suspend fun importFromJson(
        inputStream: InputStream,
        replaceExisting: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val backup = BackupCodec.decode(rawJson)
            database.withTransaction {
                if (replaceExisting) {
                    transactionDao.deleteAll()
                }
                backup.kids.forEach { transactionDao.insertKid(it) }
                backup.categories.forEach { transactionDao.insertCategory(CategoryEntity(name = it)) }
                transactionDao.insertAll(backup.transactions)
            }
            backup.transactions.size
        }
    }
}
