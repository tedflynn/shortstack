package com.vivificavit.shortstack.data

import com.vivificavit.shortstack.data.export.CsvExport
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class CsvExportTest {
    private val zone = ZoneOffset.UTC

    private fun entry(
        title: String = "Lawn",
        kidName: String = "Sam",
        amountCents: Long = 1250,
        type: TransactionType = TransactionType.CREDIT,
        date: LocalDate = LocalDate.of(2025, 10, 30),
        id: Long = 0
    ) = Transaction(
        id = id,
        kidName = kidName,
        title = title,
        amountCents = amountCents,
        type = type,
        category = "Job",
        timestamp = date.atStartOfDay(zone).toInstant().toEpochMilli()
    )

    /** The rows after the header, without the byte order mark or line endings. */
    private fun rows(vararg entries: Transaction): List<String> =
        CsvExport.encode(entries.toList(), zone).removePrefix("\uFEFF").split("\r\n").drop(1).dropLast(1)

    @Test
    fun writesHeaderAndSignedAmounts() {
        val csv = CsvExport.encode(
            listOf(entry(), entry(title = "Comic", amountCents = 300, type = TransactionType.DEBIT)),
            zone
        )
        assertTrue(csv.startsWith("\uFEFFKid,Date,Note,Category,Type,Amount\r\n"))
        assertEquals(
            listOf("Sam,2025-10-30,Lawn,Job,Income,12.50", "Sam,2025-10-30,Comic,Job,Expense,-3.00"),
            rows(entry(), entry(title = "Comic", amountCents = 300, type = TransactionType.DEBIT, id = 1))
        )
    }

    @Test
    fun sortsByKidThenOldestFirst() {
        val result = rows(
            entry(kidName = "Sam", title = "Newer", date = LocalDate.of(2025, 11, 2)),
            entry(kidName = "ada", title = "Ada's"),
            entry(kidName = "Sam", title = "Older", date = LocalDate.of(2025, 10, 1))
        )
        assertEquals(listOf("Ada's", "Older", "Newer"), result.map { it.split(",")[2] })
    }

    @Test
    fun quotesCommasQuotesAndNewlines() {
        assertEquals(
            listOf(
                "Sam,2025-10-30,\"Front, back\",Job,Income,12.50",
                "Sam,2025-10-30,\"The \"\"big\"\" job\",Job,Income,12.50",
                "Sam,2025-10-30,\"Two\nlines\",Job,Income,12.50"
            ),
            rows(
                entry(title = "Front, back", id = 1),
                entry(title = "The \"big\" job", id = 2),
                entry(title = "Two\nlines", id = 3)
            )
        )
    }

    @Test
    fun defusesNotesThatWouldRunAsFormulas() {
        val notes = rows(
            entry(title = "=HYPERLINK(\"http://x\")", id = 1),
            entry(title = "+1", id = 2),
            entry(title = "-5 for snacks", id = 3),
            entry(title = "@SUM(A1)", id = 4)
        )
        assertEquals("\"'=HYPERLINK(\"\"http://x\"\")\"", notes[0].removePrefix("Sam,2025-10-30,").substringBefore(",Job"))
        assertTrue(notes[1].contains(",'+1,"))
        assertTrue(notes[2].contains(",'-5 for snacks,"))
        assertTrue(notes[3].contains(",'@SUM(A1),"))
        assertTrue(rows(entry(kidName = "=Sam")).single().startsWith("'=Sam,"))
    }
}
