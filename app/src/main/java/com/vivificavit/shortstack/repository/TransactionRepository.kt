package com.vivificavit.shortstack.repository

import androidx.room.withTransaction
import com.vivificavit.shortstack.data.backup.BackupCodec
import com.vivificavit.shortstack.data.export.CsvExport
import com.vivificavit.shortstack.data.local.AppDatabase
import com.vivificavit.shortstack.data.local.CategorySum
import com.vivificavit.shortstack.data.model.CategoryEntity
import com.vivificavit.shortstack.data.model.DefaultCategories
import com.vivificavit.shortstack.data.model.KidProfile
import com.vivificavit.shortstack.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
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

    /** Permanently deletes a kid and all of their entries, together or not at all. */
    suspend fun deleteKidAndEntries(name: String) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                transactionDao.deleteTransactionsForKid(name)
                transactionDao.deleteKid(name)
            }
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
    suspend fun exportToJson(openStream: () -> OutputStream?): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            open(openStream).bufferedWriter(Charsets.UTF_8).use { writer ->
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

    /** Writes every entry as a spreadsheet to the stream [openStream] gives, and closes it. */
    suspend fun exportToCsv(openStream: () -> OutputStream?): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val transactions = transactionDao.getAllTransactions().first()
            open(openStream).bufferedWriter(Charsets.UTF_8).use { it.write(CsvExport.encode(transactions)) }
            transactions.size
        }
    }

    /**
     * Restores a backup. The file is fully validated before the database is
     * touched, and the replace runs in one database transaction, so a failure
     * leaves existing data exactly as it was. Closes the stream [openStream] gives.
     */
    suspend fun importFromJson(
        openStream: () -> InputStream?,
        replaceExisting: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = open(openStream).use { readAtMost(it, MAX_BACKUP_BYTES) }.toString(Charsets.UTF_8)
            val backup = BackupCodec.decode(rawJson)
            database.withTransaction {
                if (replaceExisting) {
                    // Everything goes, so kids and categories missing from the backup don't linger.
                    transactionDao.deleteAll()
                    transactionDao.deleteAllKids()
                    transactionDao.deleteAllCategories()
                }
                backup.kids.forEach { transactionDao.insertKid(it) }
                backup.categories.forEach { transactionDao.insertCategory(CategoryEntity(name = it)) }
                transactionDao.insertAll(backup.transactions)
            }
            backup.transactions.size
        }
    }

    companion object {
        /** Real backups are a few hundred KB at most; this stops a wrong pick from exhausting memory. */
        const val MAX_BACKUP_BYTES = 10 * 1024 * 1024

        /**
         * Opens a picked file here, on the IO thread and inside the caller's runCatching,
         * so a provider that fails (Drive offline, file gone) reports an error, not a crash.
         */
        private fun <T> open(openStream: () -> T?): T =
            openStream() ?: throw IOException("Couldn't open the file.")

        private fun readAtMost(input: InputStream, limit: Int): ByteArray {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                require(out.size() <= limit) { "The file is too large to be a backup." }
            }
            return out.toByteArray()
        }
    }
}
