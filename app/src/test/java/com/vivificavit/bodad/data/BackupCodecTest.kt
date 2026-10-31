package com.vivificavit.bodad.data

import com.vivificavit.bodad.data.backup.BackupCodec
import com.vivificavit.bodad.data.model.KidProfile
import com.vivificavit.bodad.data.model.Transaction
import com.vivificavit.bodad.data.model.TransactionType
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
            """{"formatVersion":99}"""
        )
        bad.forEach { raw ->
            val failed = runCatching { BackupCodec.decode(raw) }.isFailure
            assertTrue("expected failure for $raw", failed)
        }
    }
}
