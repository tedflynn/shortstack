package com.vivificavit.shortstack.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivificavit.shortstack.data.local.AppDatabase
import com.vivificavit.shortstack.data.model.Frequency
import com.vivificavit.shortstack.data.model.Goal
import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.GoalMoveReason
import com.vivificavit.shortstack.data.model.RecurringEntry
import com.vivificavit.shortstack.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Builds a real version-3 database from the exported schema, fills it, then
 * opens it with the current app database. Room validates the migrated schema
 * on open, so a migration that doesn't match the entities fails here.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val dbName = "migration-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    /** Creates the tables exactly as exported for [version], and returns the open database. */
    private fun createDatabase(version: Int): SQLiteDatabase {
        val schema = File("schemas/${AppDatabase::class.java.name}/$version.json").readText()
        val database = Json.parseToJsonElement(schema).jsonObject["database"]!!.jsonObject
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName), null)
        database["entities"]!!.jsonArray.forEach { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            db.execSQL(entity.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity.jsonObject["indices"]?.jsonArray?.forEach { index ->
                db.execSQL(index.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
        }
        database["setupQueries"]!!.jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
        db.version = version
        return db
    }

    private fun createVersion3Database() {
        val db = createDatabase(3)
        db.execSQL("INSERT INTO kids (name, createdAt) VALUES ('Maya', 1)")
        db.execSQL("INSERT INTO categories (name) VALUES ('Allowance'), ('Toy')")
        val rows = listOf(
            Triple(0.1, "CREDIT", "Allowance"),
            Triple(0.2, "CREDIT", "Allowance"),
            Triple(19.99, "CREDIT", "Allowance"),
            Triple(1.15, "DEBIT", "Toy")
        )
        rows.forEachIndexed { i, (amount, type, category) ->
            db.execSQL(
                "INSERT INTO transactions (kidName, title, amount, type, category, timestamp) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any>("Maya", "Entry $i", amount, type, category, 1_000L + i)
            )
        }
        db.close()
    }

    private fun openAppDatabase(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

    @Test
    fun migrate5To6KeepsEntriesAndStartsWithNoGoals() = runTest {
        createDatabase(5).apply {
            execSQL("INSERT INTO kids (name, createdAt) VALUES ('Maya', 1)")
            execSQL(
                "INSERT INTO transactions (kidName, title, amountCents, type, category, timestamp) " +
                    "VALUES ('Maya', 'Allowance', 500, 'CREDIT', 'Allowance', 1)"
            )
            close()
        }

        val appDb = openAppDatabase()
        try {
            assertEquals(500L, appDb.transactionDao().getBalanceForKidOnce("Maya"))
            val goals = appDb.goalDao()
            assertEquals(emptyList<Goal>(), goals.getGoalsForKidOnce("Maya"))
            val goalId = goals.upsertGoal(Goal(kidName = "Maya", name = "Game", targetCents = 7_000, targetDay = 20_807))
            assertEquals(20_807L, goals.getGoal(goalId)?.targetDay)
            goals.insertMoves(
                listOf(GoalMove(kidName = "Maya", goalId = goalId, amountCents = 200, reason = GoalMoveReason.MANUAL))
            )
            assertEquals(GoalMoveReason.MANUAL, goals.getMovesForGoal(goalId).single().reason)
        } finally {
            appDb.close()
        }
    }

    @Test
    fun migrate6To7KeepsEntriesAndStartsWithNothingRepeating() = runTest {
        createDatabase(6).apply {
            execSQL("INSERT INTO kids (name, createdAt) VALUES ('Maya', 1)")
            execSQL(
                "INSERT INTO transactions (kidName, title, amountCents, type, category, timestamp) " +
                    "VALUES ('Maya', 'Allowance', 500, 'CREDIT', 'Allowance', 1)"
            )
            close()
        }

        val appDb = openAppDatabase()
        try {
            assertEquals(null, appDb.transactionDao().getTransactionsForKidOnce("Maya").single().recurringId)
            val recurring = appDb.recurringDao()
            assertEquals(emptyList<RecurringEntry>(), recurring.getAllRecurringOnce())
            val id = recurring.upsertRecurring(
                RecurringEntry(
                    kidName = "Maya", title = "Allowance", amountCents = 500, type = TransactionType.CREDIT,
                    category = "Allowance", frequency = Frequency.MONTHLY, startTimestamp = 1
                )
            )
            assertEquals(Frequency.MONTHLY, recurring.getRecurring(id)?.frequency)
        } finally {
            appDb.close()
        }
    }

    @Test
    fun migrate6To7AddsTheBuyByDayToEarlyV6Goals() = runTest {
        // A v6 database from before goals had a buy-by day: no targetDay column.
        createDatabase(5).apply {
            execSQL(
                "CREATE TABLE `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kidName` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, `targetCents` INTEGER NOT NULL, `icon` TEXT, `createdAt` INTEGER NOT NULL, " +
                    "`completedAt` INTEGER)"
            )
            execSQL("CREATE INDEX `index_goals_kidName` ON `goals` (`kidName`)")
            execSQL(
                "CREATE TABLE `goal_moves` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kidName` TEXT NOT NULL, " +
                    "`goalId` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `reason` TEXT NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, `transactionId` INTEGER)"
            )
            execSQL("CREATE INDEX `index_goal_moves_kidName` ON `goal_moves` (`kidName`)")
            execSQL("CREATE INDEX `index_goal_moves_goalId` ON `goal_moves` (`goalId`)")
            execSQL("CREATE INDEX `index_goal_moves_transactionId` ON `goal_moves` (`transactionId`)")
            execSQL("INSERT INTO goals (kidName, name, targetCents, createdAt) VALUES ('Maya', 'Bike', 9000, 1)")
            execSQL("PRAGMA user_version = 6")
            close()
        }

        val appDb = openAppDatabase()
        try {
            val goal = appDb.goalDao().getGoalsForKidOnce("Maya").single()
            assertEquals("Bike", goal.name)
            assertEquals(null, goal.targetDay)
        } finally {
            appDb.close()
        }
    }

    @Test
    fun migrate4To5KeepsCategoriesWithAutomaticIcons() = runTest {
        createDatabase(4).apply {
            execSQL("INSERT INTO categories (name) VALUES ('Allowance'), ('Snacks')")
            close()
        }

        val appDb = openAppDatabase()
        try {
            val dao = appDb.transactionDao()
            val categories = dao.getAllCategoriesOnce()
            assertEquals(listOf("Allowance", "Snacks"), categories.map { it.name })
            assertEquals(listOf(null, null), categories.map { it.icon })
            dao.setCategoryIcon("snacks", "candy")
            assertEquals("candy", dao.getAllCategoriesOnce().last().icon)
        } finally {
            appDb.close()
        }
    }

    @Test
    fun migrate3To4KeepsEveryTransactionInCents() = runTest {
        createVersion3Database()

        val appDb = openAppDatabase()
        try {
            val dao = appDb.transactionDao()
            val transactions = dao.getAllTransactions().first()
            assertEquals(listOf(115L, 1999L, 20L, 10L), transactions.map { it.amountCents })
            // 0.10 + 0.20 + 19.99 - 1.15 = 19.14
            assertEquals(1914L, dao.getRunningBalanceForKid("Maya").first())
            assertEquals(listOf("Maya"), dao.getAllKidNames().first())
            assertEquals(listOf("Allowance", "Toy"), dao.getAllCategoryNames().first())
        } finally {
            appDb.close()
        }
    }
}
