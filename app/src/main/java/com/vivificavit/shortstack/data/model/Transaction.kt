package com.vivificavit.shortstack.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    CREDIT,
    DEBIT
}

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

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    /** The icon key the user picked (see EntryIcons); null picks one automatically. */
    val icon: String? = null
) {
    init {
        require(name.isNotBlank()) { "Category name cannot be blank." }
    }
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val kidName: String,
    val title: String,
    /** Always positive, in whole cents; [type] decides whether it adds or subtracts. */
    val amountCents: Long,
    val type: TransactionType,
    val category: String = DefaultCategories.DEFAULT_CATEGORIES.first(),
    val timestamp: Long = System.currentTimeMillis(),
    /** The [RecurringEntry] that added this entry; null when it was entered by hand. */
    val recurringId: Long? = null
) {
    init {
        require(amountCents > 0L) { "Transaction amount must always be positive." }
        require(title.isNotBlank()) { "Transaction title cannot be blank." }
        require(kidName.isNotBlank()) { "Kid name cannot be blank." }
    }
}

/** How often a [RecurringEntry] comes due. */
@Serializable
enum class Frequency { WEEKLY, EVERY_2_WEEKS, MONTHLY }

/**
 * An entry that repeats, like an allowance or a subscription. Each time it comes
 * due, the app adds a [Transaction] for it (see RecurringMath).
 */
@Entity(
    tableName = "recurring_entries",
    indices = [Index(value = ["kidName"])]
)
data class RecurringEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val kidName: String,
    val title: String,
    val amountCents: Long,
    val type: TransactionType,
    val category: String,
    val frequency: Frequency,
    /** When the first entry was dated; later ones repeat from it, so monthly dates don't drift. */
    val startTimestamp: Long,
    /** How many entries it has added so far; the next one is number [occurrences] (from 0). */
    val occurrences: Int = 0,
    /** For income: the goal that gets [goalCents] of each entry, while it's still being saved for. */
    val goalId: Long? = null,
    val goalCents: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(amountCents > 0L) { "Recurring amount must be positive." }
        require(title.isNotBlank()) { "Recurring title cannot be blank." }
        require(kidName.isNotBlank()) { "Kid name cannot be blank." }
        require(occurrences >= 0) { "Occurrences cannot be negative." }
    }
}

/** Something a kid is saving up for. Money set aside for it is recorded as [GoalMove]s. */
@Entity(
    tableName = "goals",
    indices = [Index(value = ["kidName"])]
)
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val kidName: String,
    val name: String,
    /** What it costs, in whole cents. */
    val targetCents: Long,
    /** The icon key the user picked (see EntryIcons); null picks one from the name. */
    val icon: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** When it was bought; null while the kid is still saving. */
    val completedAt: Long? = null,
    /** The day the kid wants to buy it by, as LocalDate.toEpochDay(); null for no date. */
    val targetDay: Long? = null
) {
    init {
        require(targetCents > 0L) { "Goal target must always be positive." }
        require(name.isNotBlank()) { "Goal name cannot be blank." }
        require(kidName.isNotBlank()) { "Kid name cannot be blank." }
    }
}

/** Why money went into or out of a goal. */
@Serializable
enum class GoalMoveReason {
    /** Moved by hand, to or from pocket money or another goal. */
    MANUAL,

    /** Part of an income entry, set aside when the entry was saved. */
    SPLIT,

    /** Everything saved, let go when the goal was bought. */
    PURCHASE,

    /** Taken to pay for spending that pocket money couldn't cover. */
    COVER,

    /** A correction that keeps a goal from holding less than nothing. */
    ADJUST
}

/**
 * Money set aside for a goal (positive) or taken back out of it (negative).
 * It never leaves the kid's balance; it only stops counting as pocket money.
 */
@Entity(
    tableName = "goal_moves",
    indices = [Index(value = ["kidName"]), Index(value = ["goalId"]), Index(value = ["transactionId"])]
)
data class GoalMove(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val kidName: String,
    val goalId: Long,
    /** Signed, in whole cents; never zero. */
    val amountCents: Long,
    val reason: GoalMoveReason,
    val timestamp: Long = System.currentTimeMillis(),
    /** The entry this move belongs to (its split, purchase or cover), if any. */
    val transactionId: Long? = null
) {
    init {
        require(amountCents != 0L) { "A goal move must move some money." }
        require(kidName.isNotBlank()) { "Kid name cannot be blank." }
    }
}

/** Part of an income entry to set aside for one goal. */
data class GoalSplit(val goalId: Long, val amountCents: Long)

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
