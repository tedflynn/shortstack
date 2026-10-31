package com.example.allowancetracker.repository

import com.example.allowancetracker.data.local.CategorySum
import com.example.allowancetracker.data.local.TransactionDao
import com.example.allowancetracker.data.model.CategoryEntity
import com.example.allowancetracker.data.model.DefaultCategories
import com.example.allowancetracker.data.model.KidProfile
import com.example.allowancetracker.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    private val jsonSerializer = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val kidNames: Flow<List<String>> = transactionDao.getAllKidNames()

    val categories: Flow<List<String>> = transactionDao.getAllCategoryNames()

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsForKid(kidName: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForKid(kidName)
    }

    fun getRunningBalanceForKid(kidName: String): Flow<Double?> {
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
            transactionDao.insertKid(KidProfile(name = Transaction.DEFAULT_KID_NAME))
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
            transactionDao.insertKid(KidProfile(name = transaction.kidName))
            transactionDao.insertCategory(CategoryEntity(name = transaction.category))
            transactionDao.insert(transaction)
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            transactionDao.delete(transaction)
        }
    }

    suspend fun exportToJson(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTransactions = transactionDao.getAllTransactions().first()
            val jsonPayload = jsonSerializer.encodeToString(currentTransactions)
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }
            currentTransactions.size
        }
    }

    suspend fun importFromJson(
        inputStream: InputStream,
        replaceExisting: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val importedTransactions = jsonSerializer.decodeFromString<List<Transaction>>(rawJson)
            if (replaceExisting) {
                transactionDao.deleteAll()
            }
            val normalizedTransactions = if (replaceExisting) {
                importedTransactions
            } else {
                importedTransactions.map { it.copy(id = 0L) }
            }
            normalizedTransactions.map { it.kidName }.distinct().forEach { kidName ->
                transactionDao.insertKid(KidProfile(name = kidName))
            }
            normalizedTransactions.forEach { tx ->
                transactionDao.insertCategory(CategoryEntity(name = tx.category))
            }
            transactionDao.insertAll(normalizedTransactions)
            normalizedTransactions.size
        }
    }
}
