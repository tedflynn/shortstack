package com.example.allowancetracker.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.allowancetracker.R
import com.example.allowancetracker.data.local.AppDatabase
import com.example.allowancetracker.data.local.SelectedKidStore
import com.example.allowancetracker.data.model.Transaction
import com.example.allowancetracker.data.model.TransactionType
import com.example.allowancetracker.repository.TransactionRepository
import com.example.allowancetracker.ui.viewmodel.TrackerViewModel
import com.example.allowancetracker.ui.viewmodel.UiMessage
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

        vm.removeKidIfUnused("Sam")
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

        vm.categorySpending.awaitValue { sums -> sums.map { it.category } == listOf("Snacks") }
        vm.selectedCategories.awaitValue { it == setOf("Snacks") }

        vm.toggleCategorySelection("snacks")
        vm.categorySpending.awaitValue { it.isEmpty() }
        vm.toggleCategorySelection("Snacks")
        vm.categorySpending.awaitValue { it.size == 1 }
        Unit
    }

    @Test
    fun kidWithEntriesIsNotRemoved() = runBlocking {
        repository.insertTransaction(
            Transaction(kidName = "Sam", title = "Allowance", amountCents = 500, type = TransactionType.CREDIT)
        )
        val vm = TrackerViewModel(repository, FakeStore())
        vm.kids.awaitValue { it == listOf("Sam") }

        // Subscribe before acting: uiMessages doesn't replay past messages.
        val message = async(start = CoroutineStart.UNDISPATCHED) {
            vm.uiMessages.awaitValue { true }
        }
        vm.removeKidIfUnused("Sam")
        assertEquals(UiMessage(R.string.message_kid_has_entries, listOf("Sam")), message.await())
        assertEquals(listOf("Sam"), vm.kids.value)
    }
}
