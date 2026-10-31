package com.example.allowancetracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.allowancetracker.data.model.CategoryEntity
import com.example.allowancetracker.data.model.KidProfile
import com.example.allowancetracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow

data class CategorySum(
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "total")
    val total: Double
)

@Dao
interface TransactionDao {

    @Query("SELECT name FROM kids ORDER BY createdAt ASC, id ASC")
    fun getAllKidNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKid(kid: KidProfile): Long

    @Query("SELECT name FROM categories ORDER BY id ASC")
    fun getAllCategoryNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE LOWER(category) = LOWER(:name)
        """
    )
    suspend fun countTransactionsUsingCategory(name: String): Int

    @Query(
        """
        DELETE FROM categories
        WHERE LOWER(name) = LOWER(:name)
          AND NOT EXISTS (
              SELECT 1 FROM transactions
              WHERE LOWER(category) = LOWER(:name)
          )
        """
    )
    suspend fun deleteUnusedCategory(name: String): Int

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE kidName = :kidName ORDER BY timestamp DESC, id DESC")
    fun getTransactionsForKid(kidName: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query(
        """
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0.0 END), 0.0) -
            COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0.0 END), 0.0)
        FROM transactions
        """
    )
    fun getRunningBalance(): Flow<Double?>

    @Query(
        """
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0.0 END), 0.0) -
            COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0.0 END), 0.0)
        FROM transactions
        WHERE kidName = :kidName
        """
    )
    fun getRunningBalanceForKid(kidName: String): Flow<Double?>

    @Query(
        """
        SELECT category, SUM(amount) AS total
        FROM transactions
        WHERE type = 'DEBIT'
        GROUP BY category
        HAVING total > 0
        ORDER BY total DESC
        """
    )
    fun getDebitTotalsByCategory(): Flow<List<CategorySum>>

    @Query(
        """
        SELECT category, SUM(amount) AS total
        FROM transactions
        WHERE type = 'CREDIT'
          AND kidName = :kidName
          AND timestamp >= :startTimestamp
          AND timestamp <= :endTimestamp
        GROUP BY category
        HAVING total > 0
        ORDER BY total DESC
        """
    )
    fun getCreditTotalsByCategoryForKidInRange(
        kidName: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<CategorySum>>

    @Query(
        """
        SELECT category, SUM(amount) AS total
        FROM transactions
        WHERE type = 'DEBIT'
          AND kidName = :kidName
          AND timestamp >= :startTimestamp
          AND timestamp <= :endTimestamp
        GROUP BY category
        HAVING total > 0
        ORDER BY total DESC
        """
    )
    fun getDebitTotalsByCategoryForKidInRange(
        kidName: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<CategorySum>>
}
