package com.vivificavit.shortstack.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivificavit.shortstack.R
import com.vivificavit.shortstack.data.local.AppDatabase
import com.vivificavit.shortstack.data.local.SelectedKidStore
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import com.vivificavit.shortstack.repository.TransactionRepository
import com.vivificavit.shortstack.ui.viewmodel.TrackerViewModel
import com.vivificavit.shortstack.ui.viewmodel.UiMessage
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TrackerViewModelTest {
    private class FakeStore(initial: String? = null) : SelectedKidStore {
        private val state = MutableStateFlow(initial)
        override val lastSelectedKid: Flow<String?> = state
        val current: String? get() = state.value
        override suspend fun setLastSelectedKid(name: String) {
            state.value = name
        }
    }

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = TransactionRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun <T> Flow<T>.awaitValue(predicate: (T) -> Boolean): T =
        withTimeout(5_000) { first(predicate) }

    @Test
    fun freshInstallHasNoKidsUntilOneIsAdded() = runBlocking {
        val store = FakeStore()
        val vm = TrackerViewModel(repository, store)

        vm.hasNoKids.awaitValue { it }
        assertEquals(null, vm.selectedKid.value)

        vm.addKid("Sam")
        vm.selectedKid.awaitValue { it == "Sam" }
        assertEquals("Sam", store.current)
        vm.hasNoKids.awaitValue { !it }
        Unit
    }

    @Test
    fun reopensOnLastSelectedKidAndFallsBackWhenRemoved() = runBlocking {
        repository.insertKid("Ada")
        repository.insertKid("Sam")

        val vm = TrackerViewModel(repository, FakeStore(initial = "Sam"))
        vm.selectedKid.awaitValue { it == "Sam" }

        vm.removeKid("Sam")
        vm.selectedKid.awaitValue { it == "Ada" }

        val staleVm = TrackerViewModel(repository, FakeStore(initial = "Ghost"))
        staleVm.selectedKid.awaitValue { it == "Ada" }
        Unit
    }

    @Test
    fun customCategoriesAreShownInChartsByDefault() = runBlocking {
        repository.insertTransaction(
            Transaction(
                kidName = "Sam",
                title = "Chips",
                amountCents = 250,
                type = TransactionType.DEBIT,
                category = "Snacks"
            )
        )
        // A new ViewModel stands in for the app being restarted.
        val vm = TrackerViewModel(repository, FakeStore(initial = "Sam"))

        vm.spendingSums.awaitValue { sums -> sums.map { it.category } == listOf("Snacks") }
        assertEquals(emptySet<String>(), vm.hiddenCategories.value)

        vm.toggleCategorySelection("Snacks")
        vm.hiddenCategories.awaitValue { it == setOf("snacks") }
        vm.toggleCategorySelection("snacks")
        vm.hiddenCategories.awaitValue { it.isEmpty() }
        Unit
    }

    @Test
    fun removingKidDeletesOnlyTheirEntries() = runBlocking {
        repository.insertTransaction(
            Transaction(kidName = "Sam", title = "Allowance", amountCents = 500, type = TransactionType.CREDIT)
        )
        repository.insertTransaction(
            Transaction(kidName = "Ada", title = "Gift", amountCents = 700, type = TransactionType.CREDIT)
        )
        val vm = TrackerViewModel(repository, FakeStore())
        vm.kids.awaitValue { it.toSet() == setOf("Sam", "Ada") }

        // Subscribe before acting: uiMessages doesn't replay past messages.
        val message = async(start = CoroutineStart.UNDISPATCHED) {
            vm.uiMessages.awaitValue { true }
        }
        vm.removeKid("Sam")
        assertEquals(UiMessage(R.string.message_kid_removed, listOf("Sam")), message.await())
        vm.kids.awaitValue { it == listOf("Ada") }
        assertEquals(listOf("Ada"), repository.allTransactions.first().map { it.kidName })
    }

    @Test
    fun suggestsMostRecentCategoryPerType() = runBlocking {
        val vm = TrackerViewModel(repository, FakeStore())
        vm.lastCategoryByType.awaitValue { it.isEmpty() }

        repository.insertTransaction(
            Transaction(kidName = "Sam", title = "Allowance", amountCents = 500, type = TransactionType.CREDIT, category = "Allowance", timestamp = 1_000)
        )
        repository.insertTransaction(
            Transaction(kidName = "Sam", title = "Lego", amountCents = 900, type = TransactionType.DEBIT, category = "Toy", timestamp = 2_000)
        )
        repository.insertTransaction(
            Transaction(kidName = "Ada", title = "Comic", amountCents = 300, type = TransactionType.DEBIT, category = "Book", timestamp = 3_000)
        )

        vm.lastCategoryByType.awaitValue {
            it == mapOf(TransactionType.CREDIT to "Allowance", TransactionType.DEBIT to "Book")
        }
        Unit
    }

    @Test
    fun ledgerIsNullUntilLoadedThenHasBalanceAndEntries() = runBlocking {
        repository.insertTransaction(
            Transaction(kidName = "Sam", title = "Allowance", amountCents = 500, type = TransactionType.CREDIT)
        )
        val vm = TrackerViewModel(repository, FakeStore(initial = "Sam"))
        assertEquals(null, vm.ledger.value)

        val ledger = vm.ledger.awaitValue { it != null }!!
        assertEquals(500L, ledger.balanceCents)
        assertEquals(listOf("Allowance"), ledger.transactions.map { it.title })
    }

    @Test
    fun editingAnEntryKeepsItsIdAndUpdatesBalance() = runBlocking {
        repository.insertTransaction(
            Transaction(kidName = "Sam", title = "Allowance", amountCents = 500, type = TransactionType.CREDIT)
        )
        val vm = TrackerViewModel(repository, FakeStore(initial = "Sam"))
        val original = vm.ledger.awaitValue { it != null }!!.transactions.single()

        vm.updateTransaction(original.copy(title = "Chores", amountCents = 750))
        val edited = vm.ledger.awaitValue { it?.balanceCents == 750L }!!.transactions.single()
        assertEquals(original.id, edited.id)
        assertEquals("Chores", edited.title)
    }

    @Test
    fun undoRestoresDeletedEntry() = runBlocking {
        repository.insertTransaction(
            Transaction(kidName = "Sam", title = "Comic", amountCents = 300, type = TransactionType.DEBIT)
        )
        val vm = TrackerViewModel(repository, FakeStore(initial = "Sam"))
        val entry = vm.ledger.awaitValue { it != null }!!.transactions.single()

        val message = async(start = CoroutineStart.UNDISPATCHED) {
            vm.uiMessages.awaitValue { true }
        }
        vm.deleteTransaction(entry)
        val deleted = message.await()
        assertEquals(R.string.message_entry_deleted, deleted.resId)
        vm.ledger.awaitValue { it?.transactions?.isEmpty() == true }

        deleted.onUndo!!.invoke()
        val restored = vm.ledger.awaitValue { it?.transactions?.size == 1 }!!
        assertEquals(entry, restored.transactions.single())
        assertEquals(-300L, restored.balanceCents)
    }
}
