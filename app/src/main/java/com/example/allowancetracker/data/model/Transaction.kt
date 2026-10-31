package com.example.allowancetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    CREDIT,
    DEBIT
}

@Serializable
@Entity(
    tableName = "kids",
    indices = [Index(value = ["name"], unique = true)]
)
data class KidProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(name.isNotBlank()) { "Kid name cannot be blank." }
    }
}

@Serializable
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String
) {
    init {
        require(name.isNotBlank()) { "Category name cannot be blank." }
    }
}

@Serializable
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val kidName: String = DEFAULT_KID_NAME,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String = DefaultCategories.DEFAULT_CATEGORIES.first(),
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(amount > 0.0) { "Transaction amount must always be positive." }
        require(title.isNotBlank()) { "Transaction title cannot be blank." }
        require(kidName.isNotBlank()) { "Kid name cannot be blank." }
    }

    companion object {
        const val DEFAULT_KID_NAME = "Maya"
    }
}

object DefaultCategories {
    val DEFAULT_CATEGORIES: List<String> = listOf(
        "Allowance",
        "Gift",
        "Job",
        "Video Game",
        "Toy",
        "Book"
    )
}
