package com.vivificavit.shortstack.data.backup

import com.vivificavit.shortstack.data.model.DefaultCategories
import com.vivificavit.shortstack.data.model.KidProfile
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import com.vivificavit.shortstack.util.Money
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

@Serializable
data class BackupFile(
    val formatVersion: Int = BackupCodec.CURRENT_FORMAT_VERSION,
    val kids: List<BackupKid> = emptyList(),
    val categories: List<String> = emptyList(),
    val transactions: List<BackupTransaction> = emptyList()
)

@Serializable
data class BackupKid(
    val name: String,
    val createdAt: Long = 0L
)

@Serializable
data class BackupTransaction(
    val kidName: String,
    val title: String,
    /** Present in format version 2+. */
    val amountCents: Long? = null,
    /** Legacy (format version 1) amount in dollars. */
    val amount: Double? = null,
    val type: TransactionType,
    val category: String = DefaultCategories.DEFAULT_CATEGORIES.first(),
    val timestamp: Long
)

/** Fully validated backup contents, ready to write to the database. */
data class BackupContents(
    val kids: List<KidProfile>,
    val categories: List<String>,
    val transactions: List<Transaction>
)

/**
 * Format 1 (original): a bare JSON array of transactions with dollar amounts.
 * Format 2: an object with kids, categories and transactions in cents.
 */
object BackupCodec {
    const val CURRENT_FORMAT_VERSION = 2

    /** Entries dated after this (2100-01-01 UTC) are treated as corrupt. */
    private const val MAX_TIMESTAMP = 4_102_444_800_000L

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(
        kids: List<KidProfile>,
        categories: List<String>,
        transactions: List<Transaction>
    ): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            kids = kids.map { BackupKid(name = it.name, createdAt = it.createdAt) },
            categories = categories,
            transactions = transactions.map {
                BackupTransaction(
                    kidName = it.kidName,
                    title = it.title,
                    amountCents = it.amountCents,
                    type = it.type,
                    category = it.category,
                    timestamp = it.timestamp
                )
            }
        )
    )

    /**
     * Parses and validates the whole file up front, so a bad backup throws
     * here before anything in the database is touched.
     */
    fun decode(raw: String): BackupContents {
        val element = json.parseToJsonElement(raw)
        val file = if (element is JsonArray) {
            BackupFile(
                formatVersion = 1,
                transactions = json.decodeFromJsonElement(
                    ListSerializer(BackupTransaction.serializer()),
                    element
                )
            )
        } else {
            json.decodeFromJsonElement(BackupFile.serializer(), element)
        }
        require(file.formatVersion <= CURRENT_FORMAT_VERSION) {
            "This backup was made by a newer version of the app."
        }

        // Hold restored entries to the same rules as the entry form, so a
        // hand-edited or damaged file can't bring in values the app never makes.
        val transactions = file.transactions.mapIndexed { index, tx ->
            val entry = "Entry ${index + 1}"
            val cents = tx.amountCents
                ?: tx.amount?.let(Money::dollarsToCents)
                ?: throw IllegalArgumentException("$entry has no amount.")
            require(cents in 1..Money.MAX_ENTRY_CENTS) {
                "$entry has an amount outside ${Money.format(1L)} to ${Money.format(Money.MAX_ENTRY_CENTS)}."
            }
            require(tx.kidName.isNotBlank()) { "$entry has no kid name." }
            require(tx.title.isNotBlank()) { "$entry has no note." }
            require(tx.category.isNotBlank()) { "$entry has no category." }
            require(tx.timestamp in 0..MAX_TIMESTAMP) { "$entry has an invalid date." }
            Transaction(
                kidName = tx.kidName.trim(),
                title = tx.title.trim(),
                amountCents = cents,
                type = tx.type,
                category = tx.category.trim(),
                timestamp = tx.timestamp
            )
        }
        require(file.kids.none { it.name.isBlank() }) { "The backup has a kid with no name." }
        val kids = (file.kids.map { KidProfile(name = it.name.trim(), createdAt = it.createdAt) } +
            transactions.map { KidProfile(name = it.kidName) })
            .distinctBy { it.name }
        val categories = (file.categories.map { it.trim() } + transactions.map { it.category })
            .filter { it.isNotBlank() }
            .distinct()
        return BackupContents(kids = kids, categories = categories, transactions = transactions)
    }
}
