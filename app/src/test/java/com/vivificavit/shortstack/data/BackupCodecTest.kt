package com.vivificavit.shortstack.data

import com.vivificavit.shortstack.data.backup.BackupCodec
import com.vivificavit.shortstack.data.model.KidProfile
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
