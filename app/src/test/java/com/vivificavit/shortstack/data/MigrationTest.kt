package com.vivificavit.shortstack.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivificavit.shortstack.data.local.AppDatabase
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

    private fun createVersion3Database() {
        val schema = File("schemas/${AppDatabase::class.java.name}/3.json").readText()
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
        db.version = 3
        db.close()
    }

    @Test
    fun migrate3To4KeepsEveryTransactionInCents() = runTest {
        createVersion3Database()

        val appDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
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
