package com.vivificavit.shortstack.data.backup

import com.vivificavit.shortstack.data.model.DefaultCategories
import com.vivificavit.shortstack.data.model.Goal
import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.GoalMoveReason
import com.vivificavit.shortstack.data.model.KidProfile
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import com.vivificavit.shortstack.util.Money
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlin.math.abs

@Serializable
data class BackupFile(
    val formatVersion: Int = BackupCodec.CURRENT_FORMAT_VERSION,
    val kids: List<BackupKid> = emptyList(),
    val categories: List<String> = emptyList(),
    val transactions: List<BackupTransaction> = emptyList(),
    /** Category name -> icon key, for categories with an icon the user picked. Optional. */
    val categoryIcons: Map<String, String> = emptyMap(),
    /** Present in format version 3+. */
    val goals: List<BackupGoal> = emptyList(),
    /** Present in format version 3+. */
    val goalMoves: List<BackupGoalMove> = emptyList()
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
    val timestamp: Long,
    /** The entry's id when it was backed up, so goal moves can point to it. Format version 3+. */
    val ref: Long? = null
)

@Serializable
data class BackupGoal(
    /** The goal's id when it was backed up, so goal moves can point to it. */
    val ref: Long,
    val kidName: String,
    val name: String,
    val targetCents: Long,
    val icon: String? = null,
    val createdAt: Long = 0L,
    val completedAt: Long? = null
)

@Serializable
data class BackupGoalMove(
    val goalRef: Long,
    val amountCents: Long,
    val reason: GoalMoveReason,
    val timestamp: Long,
    val transactionRef: Long? = null
)

/** Fully validated backup contents, ready to write to the database. */
data class BackupContents(
    val kids: List<KidProfile>,
    val categories: List<String>,
    val transactions: List<Transaction>,
    /** Lower-cased category name -> icon key. */
    val categoryIcons: Map<String, String> = emptyMap(),
    /** Each transaction's ref (or null), in the same order as [transactions]. */
    val transactionRefs: List<Long?> = emptyList(),
    /** Goals by their ref; their ids are left for the database to assign. */
    val goals: Map<Long, Goal> = emptyMap(),
    /** Moves whose goal and entry are still refs; see [BackupCodec.decode]. */
    val goalMoves: List<BackupGoalMove> = emptyList()
)

/**
 * Format 1 (original): a bare JSON array of transactions with dollar amounts.
 * Format 2: an object with kids, categories and transactions in cents.
 * Format 3: adds goals and the money moved into and out of them.
 */
object BackupCodec {
    const val CURRENT_FORMAT_VERSION = 3

    /** Entries dated after this (2100-01-01 UTC) are treated as corrupt. */
    private const val MAX_TIMESTAMP = 4_102_444_800_000L

    private const val MAX_ICON_KEY_LENGTH = 40

    /**
     * Largest single goal move accepted. A purchase or correction can add up many
     * entries, so this is well above [Money.MAX_ENTRY_CENTS]; it only stops sums overflowing.
     */
    private const val MAX_MOVE_CENTS = 1_000_000_000_000L

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(
        kids: List<KidProfile>,
        categories: List<String>,
        transactions: List<Transaction>,
        categoryIcons: Map<String, String> = emptyMap(),
        goals: List<Goal> = emptyList(),
        goalMoves: List<GoalMove> = emptyList()
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
                    timestamp = it.timestamp,
                    ref = it.id
                )
            },
            categoryIcons = categoryIcons,
            goals = goals.map {
                BackupGoal(
                    ref = it.id,
                    kidName = it.kidName,
                    name = it.name,
                    targetCents = it.targetCents,
                    icon = it.icon,
                    createdAt = it.createdAt,
                    completedAt = it.completedAt
                )
            },
            goalMoves = goalMoves.map {
                BackupGoalMove(
                    goalRef = it.goalId,
                    amountCents = it.amountCents,
                    reason = it.reason,
                    timestamp = it.timestamp,
                    transactionRef = it.transactionId
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
        val transactionRefs = file.transactions.map { it.ref }

        val goals = file.goals.mapIndexed { index, goal ->
            val label = "Goal ${index + 1}"
            require(goal.kidName.isNotBlank()) { "$label has no kid name." }
            require(goal.name.isNotBlank()) { "$label has no name." }
            require(goal.targetCents in 1..Money.MAX_ENTRY_CENTS) {
                "$label costs outside ${Money.format(1L)} to ${Money.format(Money.MAX_ENTRY_CENTS)}."
            }
            require(goal.createdAt in 0..MAX_TIMESTAMP && (goal.completedAt ?: 0L) in 0..MAX_TIMESTAMP) {
                "$label has an invalid date."
            }
            goal.ref to Goal(
                kidName = goal.kidName.trim(),
                name = goal.name.trim(),
                targetCents = goal.targetCents,
                // Like category icons, a malformed one just falls back to an automatic icon.
                icon = goal.icon?.takeIf { it.isNotBlank() && it.length <= MAX_ICON_KEY_LENGTH },
                createdAt = goal.createdAt,
                completedAt = goal.completedAt
            )
        }
        require(goals.map { it.first }.toSet().size == goals.size) { "The backup has two goals with the same id." }
        val goalsByRef = goals.toMap()

        val knownRefs = transactionRefs.filterNotNull().toSet()
        val goalMoves = file.goalMoves.mapIndexed { index, move ->
            val label = "Goal move ${index + 1}"
            require(move.goalRef in goalsByRef) { "$label is for a goal that isn't in the backup." }
            require(move.amountCents != 0L && abs(move.amountCents) <= MAX_MOVE_CENTS) { "$label has an invalid amount." }
            require(move.timestamp in 0..MAX_TIMESTAMP) { "$label has an invalid date." }
            // A link to an entry that isn't here only loses the note on that entry.
            move.copy(transactionRef = move.transactionRef?.takeIf { it in knownRefs })
        }

        require(file.kids.none { it.name.isBlank() }) { "The backup has a kid with no name." }
        val kids = (file.kids.map { KidProfile(name = it.name.trim(), createdAt = it.createdAt) } +
            transactions.map { KidProfile(name = it.kidName) } +
            goalsByRef.values.map { KidProfile(name = it.kidName) })
            .distinctBy { it.name }
        val categories = (file.categories.map { it.trim() } + transactions.map { it.category })
            .filter { it.isNotBlank() }
            .distinct()
        // Icons are cosmetic: one that's malformed is dropped rather than failing the restore,
        // and a key this version doesn't know just falls back to an automatic icon.
        val categoryIcons = file.categoryIcons
            .filter { (name, icon) -> name.isNotBlank() && icon.isNotBlank() && icon.length <= MAX_ICON_KEY_LENGTH }
            .mapKeys { (name, _) -> name.trim().lowercase() }
        return BackupContents(
            kids = kids,
            categories = categories,
            transactions = transactions,
            categoryIcons = categoryIcons,
            transactionRefs = transactionRefs,
            goals = goalsByRef,
            goalMoves = goalMoves
        )
    }
}
