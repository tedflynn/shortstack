package com.vivificavit.shortstack.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivificavit.shortstack.data.local.AppDatabase
import com.vivificavit.shortstack.data.model.Frequency
import com.vivificavit.shortstack.data.model.Goal
import com.vivificavit.shortstack.data.model.GoalMoveReason
import com.vivificavit.shortstack.data.model.RecurringEntry
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import com.vivificavit.shortstack.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RecurringEntriesTest {
    private val zone = ZoneOffset.UTC
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = TransactionRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun at(date: String) = LocalDateTime.parse("${date}T09:00").toInstant(zone).toEpochMilli()

    private fun allowance(start: String, goalId: Long? = null, goalCents: Long? = null) = RecurringEntry(
        kidName = "Sam", title = "Weekly allowance", amountCents = 1_000, type = TransactionType.CREDIT,
        category = "Allowance", frequency = Frequency.WEEKLY, startTimestamp = at(start),
        goalId = goalId, goalCents = goalCents
    )

    private suspend fun entries() = repository.getMoneyForKid("Sam").first().transactions

    @Test
    fun addsEachDueEntryExactlyOnce() = runBlocking {
        assertEquals(2, repository.addRecurring(allowance("2026-10-30"), now = at("2026-11-06"), zone = zone))
        // Opening the app again the same day, or twice at once, adds nothing more.
        assertEquals(emptyList<Any>(), repository.addDueRecurring(now = at("2026-11-06"), zone = zone))
        val nextWeek = repository.addDueRecurring(now = at("2026-11-13"), zone = zone)
        assertEquals(listOf(1), nextWeek.map { it.count })

        val added = entries()
        assertEquals(3, added.size)
        val scheduleId = repository.getRecurringForKid("Sam").first().single().id
        assertTrue(added.all { it.recurringId == scheduleId && it.amountCents == 1_000L })
        assertEquals(3, repository.getRecurringForKid("Sam").first().single().occurrences)
    }

    @Test
    fun setAsideStopsAtWhatTheGoalStillNeedsAndOnceItsBought() = runBlocking {
        val bikeId = repository.saveGoal(Goal(kidName = "Sam", name = "Bike", targetCents = 500, createdAt = 1))
        // $3 of each allowance: $3, then only the $2 still needed, then nothing.
        repository.addRecurring(allowance("2026-10-23", goalId = bikeId, goalCents = 300), now = at("2026-11-06"), zone = zone)
        val splits = repository.getMoneyForKid("Sam").first().moves
            .filter { it.reason == GoalMoveReason.SPLIT }
            .sortedBy { it.timestamp }
            .map { it.amountCents }
        assertEquals(listOf(300L, 200L), splits)

        val bike = repository.getGoalsForKid("Sam").first().single()
        repository.buyGoal(bike, Transaction(kidName = "Sam", title = "Bike", amountCents = 500, type = TransactionType.DEBIT))
        repository.addDueRecurring(now = at("2026-11-13"), zone = zone)
        val money = repository.getMoneyForKid("Sam").first()
        assertEquals(4, money.transactions.count { it.recurringId != null })
        assertEquals(2, money.moves.count { it.reason == GoalMoveReason.SPLIT })
    }

    @Test
    fun stoppingKeepsWhatWasAddedAndUndoPicksUpWhereItLeftOff() = runBlocking {
        repository.addRecurring(allowance("2026-11-06"), now = at("2026-11-06"), zone = zone)
        val schedule = repository.getRecurringForKid("Sam").first().single()
        val stopped = repository.deleteRecurring(schedule)
        assertEquals(emptyList<Any>(), repository.addDueRecurring(now = at("2026-11-13"), zone = zone))
        assertEquals(1, entries().size)

        repository.restoreRecurring(stopped)
        assertEquals(listOf(1), repository.addDueRecurring(now = at("2026-11-13"), zone = zone).map { it.count })
        assertEquals(2, entries().size)
    }

    @Test
    fun editingAppliesFromTheNextEntryOn() = runBlocking {
        repository.addRecurring(allowance("2026-11-06"), now = at("2026-11-06"), zone = zone)
        val schedule = repository.getRecurringForKid("Sam").first().single()
        // An edit made from a stale copy keeps the count the database has.
        repository.saveRecurring(schedule.copy(amountCents = 1_500, occurrences = 0))
        repository.addDueRecurring(now = at("2026-11-13"), zone = zone)
        assertEquals(listOf(1_500L, 1_000L), entries().map { it.amountCents })
    }

    @Test
    fun survivesBackupAndRestoreAndGoesWithItsKid() = runBlocking {
        val bikeId = repository.saveGoal(Goal(kidName = "Sam", name = "Bike", targetCents = 5_000, createdAt = 1))
        repository.addRecurring(allowance("2026-11-06", goalId = bikeId, goalCents = 200), now = at("2026-11-06"), zone = zone)

        val file = java.io.ByteArrayOutputStream()
        assertTrue(repository.exportToJson { file }.isSuccess)
        repository.deleteKidAndEntries("Sam")
        assertEquals(emptyList<RecurringEntry>(), repository.getRecurringForKid("Sam").first())

        assertTrue(repository.importFromJson({ file.toByteArray().inputStream() }, replaceExisting = true).isSuccess)
        val schedule = repository.getRecurringForKid("Sam").first().single()
        val goal = repository.getGoalsForKid("Sam").first().single()
        assertEquals(goal.id, schedule.goalId)
        assertEquals(1, schedule.occurrences)
        assertEquals(schedule.id, entries().single().recurringId)
    }
}
