package com.vivificavit.shortstack.data

import com.vivificavit.shortstack.data.backup.BackupCodec
import com.vivificavit.shortstack.data.backup.BackupGoalMove
import com.vivificavit.shortstack.data.model.Frequency
import com.vivificavit.shortstack.data.model.Goal
import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.GoalMoveReason
import com.vivificavit.shortstack.data.model.KidProfile
import com.vivificavit.shortstack.data.model.RecurringEntry
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun roundTripsKidsCategoriesAndTransactions() {
        val tx = Transaction(
            id = 7,
            kidName = "Sam",
            title = "Lawn",
            amountCents = 1250,
            type = TransactionType.CREDIT,
            category = "Job",
            timestamp = 1_000L
        )
        val raw = BackupCodec.encode(
            kids = listOf(KidProfile(name = "Sam", createdAt = 5L), KidProfile(name = "Ada", createdAt = 6L)),
            categories = listOf("Job", "Snacks"),
            transactions = listOf(tx)
        )
        val decoded = BackupCodec.decode(raw)

        assertEquals(listOf("Sam", "Ada"), decoded.kids.map { it.name })
        assertEquals(listOf("Job", "Snacks"), decoded.categories)
        assertEquals(listOf(tx.copy(id = 0)), decoded.transactions)
    }

    @Test
    fun readsLegacyArrayBackupsInDollars() {
        val legacy = """
            [{"id":3,"kidName":"Maya","title":"Allowance","amount":19.99,
              "type":"CREDIT","category":"Allowance","timestamp":42}]
        """.trimIndent()
        val decoded = BackupCodec.decode(legacy)

        assertEquals(1999L, decoded.transactions.single().amountCents)
        assertEquals(listOf("Maya"), decoded.kids.map { it.name })
        assertEquals(listOf("Allowance"), decoded.categories)
    }

    @Test
    fun rejectsInvalidBackupsBeforeTouchingData() {
        val bad = listOf(
            "not json",
            """[{"title":"x","type":"CREDIT","timestamp":1}]""",
            """[{"title":"x","amount":-1.0,"type":"CREDIT","timestamp":1}]""",
            """{"formatVersion":99}""",
            entry(amountCents = -500),
            entry(amountCents = 0),
            entry(amountCents = 100_000_001),
            entry(kidName = "   "),
            entry(title = ""),
            entry(category = " "),
            entry(timestamp = -1),
            entry(timestamp = 9_999_999_999_999),
            """{"kids":[{"name":" "}]}"""
        )
        bad.forEach { raw ->
            val failed = runCatching { BackupCodec.decode(raw) }.isFailure
            assertTrue("expected failure for $raw", failed)
        }
    }

    @Test
    fun acceptsTheLargestAllowedEntry() {
        val decoded = BackupCodec.decode(entry(amountCents = 100_000_000))
        assertEquals(100_000_000L, decoded.transactions.single().amountCents)
    }

    @Test
    fun roundTripsChosenCategoryIcons() {
        val raw = BackupCodec.encode(
            kids = listOf(KidProfile(name = "Sam")),
            categories = listOf("Job", "Snacks"),
            transactions = emptyList(),
            categoryIcons = mapOf("Snacks" to "candy")
        )
        assertEquals(mapOf("snacks" to "candy"), BackupCodec.decode(raw).categoryIcons)
    }

    @Test
    fun dropsMalformedIconsWithoutFailingTheRestore() {
        val raw = """{"categories":["Job"],"categoryIcons":{"Job":"work"," ":"star","Snacks":"","Toy":"${"x".repeat(41)}"}}"""
        assertEquals(mapOf("job" to "work"), BackupCodec.decode(raw).categoryIcons)
    }

    @Test
    fun roundTripsGoalsAndTheirMoves() {
        val tx = Transaction(id = 7, kidName = "Sam", title = "Allowance", amountCents = 1_000, type = TransactionType.CREDIT)
        val goal = Goal(id = 3, kidName = "Sam", name = "Game", targetCents = 7_000, icon = "game", createdAt = 2, targetDay = 20_807)
        val moves = listOf(
            GoalMove(id = 1, kidName = "Sam", goalId = 3, amountCents = 200, reason = GoalMoveReason.SPLIT, timestamp = 5, transactionId = 7),
            GoalMove(id = 2, kidName = "Sam", goalId = 3, amountCents = -50, reason = GoalMoveReason.MANUAL, timestamp = 6)
        )
        val decoded = BackupCodec.decode(
            BackupCodec.encode(
                kids = listOf(KidProfile(name = "Sam")),
                categories = emptyList(),
                transactions = listOf(tx),
                goals = listOf(goal),
                goalMoves = moves
            )
        )

        assertEquals(listOf<Long?>(7), decoded.transactionRefs)
        assertEquals(mapOf(3L to goal.copy(id = 0)), decoded.goals)
        assertEquals(
            listOf(
                BackupGoalMove(goalRef = 3, amountCents = 200, reason = GoalMoveReason.SPLIT, timestamp = 5, transactionRef = 7),
                BackupGoalMove(goalRef = 3, amountCents = -50, reason = GoalMoveReason.MANUAL, timestamp = 6)
            ),
            decoded.goalMoves
        )
    }

    @Test
    fun formatTwoBackupsRestoreWithNoGoals() {
        val decoded = BackupCodec.decode(entry())
        assertEquals(emptyMap<Long, Goal>(), decoded.goals)
        assertEquals(emptyList<BackupGoalMove>(), decoded.goalMoves)
        assertEquals(listOf<Long?>(null), decoded.transactionRefs)
    }

    @Test
    fun rejectsBrokenGoals() {
        val goal = """{"ref":1,"kidName":"Sam","name":"Game","targetCents":7000}"""
        val move = """{"goalRef":1,"amountCents":200,"reason":"MANUAL","timestamp":1}"""
        val bad = listOf(
            """{"goals":[{"ref":1,"kidName":"Sam","name":" ","targetCents":7000}]}""",
            """{"goals":[{"ref":1,"kidName":"Sam","name":"Game","targetCents":0}]}""",
            """{"goals":[{"ref":1,"kidName":" ","name":"Game","targetCents":7000}]}""",
            """{"goals":[$goal,$goal]}""",
            """{"goals":[$goal],"goalMoves":[{"goalRef":2,"amountCents":200,"reason":"MANUAL","timestamp":1}]}""",
            """{"goals":[$goal],"goalMoves":[{"goalRef":1,"amountCents":0,"reason":"MANUAL","timestamp":1}]}""",
            """{"goals":[$goal],"goalMoves":[{"goalRef":1,"amountCents":200,"reason":"MANUAL","timestamp":-1}]}"""
        )
        bad.forEach { raw ->
            assertTrue("expected failure for $raw", runCatching { BackupCodec.decode(raw) }.isFailure)
        }
        // A link to an entry that isn't in the file is dropped, not fatal.
        val linked = move.replace("}", ""","transactionRef":99}""")
        val decoded = BackupCodec.decode("""{"goals":[$goal],"goalMoves":[$linked]}""")
        assertEquals(null, decoded.goalMoves.single().transactionRef)
        assertEquals(listOf("Sam"), decoded.kids.map { it.name })

        // So is a buy-by day outside the dates the app accepts.
        val farOff = goal.replace("}", ""","targetDay":99999}""")
        assertEquals(null, BackupCodec.decode("""{"goals":[$farOff]}""").goals.values.single().targetDay)
    }

    @Test
    fun roundTripsRepeatingEntriesAndDropsBrokenLinks() {
        val schedule = RecurringEntry(
            id = 4, kidName = "Sam", title = "Allowance", amountCents = 1_000, type = TransactionType.CREDIT,
            category = "Allowance", frequency = Frequency.WEEKLY, startTimestamp = 5, occurrences = 2,
            goalId = 3, goalCents = 200, createdAt = 6
        )
        val goal = Goal(id = 3, kidName = "Sam", name = "Game", targetCents = 7_000)
        val tx = Transaction(id = 7, kidName = "Sam", title = "Allowance", amountCents = 1_000, type = TransactionType.CREDIT, recurringId = 4)
        val decoded = BackupCodec.decode(
            BackupCodec.encode(
                kids = listOf(KidProfile(name = "Sam")), categories = emptyList(), transactions = listOf(tx),
                goals = listOf(goal), recurring = listOf(schedule)
            )
        )
        assertEquals(mapOf(4L to schedule.copy(id = 0)), decoded.recurring)
        assertEquals(listOf<Long?>(4), decoded.transactionRecurringRefs)

        // A set-aside for a missing goal, or an entry pointing at a missing schedule, loses only the link.
        val orphan = BackupCodec.decode(
            BackupCodec.encode(
                kids = emptyList(), categories = emptyList(), transactions = listOf(tx.copy(recurringId = 9)),
                recurring = listOf(schedule)
            )
        )
        assertEquals(null, orphan.recurring.values.single().goalId)
        assertEquals(null, orphan.recurring.values.single().goalCents)
        assertEquals(listOf<Long?>(null), orphan.transactionRecurringRefs)
        assertTrue(runCatching { BackupCodec.decode("""{"recurring":[{"ref":1,"kidName":"Sam","title":"A","amountCents":0,"type":"CREDIT","category":"A","frequency":"WEEKLY","startTimestamp":1}]}""") }.isFailure)
    }

    /** A one-entry backup that's valid unless a field is overridden. */
    private fun entry(
        amountCents: Long = 500,
        kidName: String = "Sam",
        title: String = "Lawn",
        category: String = "Job",
        timestamp: Long = 1_000
    ): String =
        """{"transactions":[{"kidName":"$kidName","title":"$title","amountCents":$amountCents,""" +
            """"type":"CREDIT","category":"$category","timestamp":$timestamp}]}"""
}
