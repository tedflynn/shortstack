import React, { useMemo, useState } from 'react';
import { Code2, Smartphone } from 'lucide-react';
import {
  DEFAULT_CATEGORIES,
  DEFAULT_KID_NAME,
  Transaction,
  TransactionType,
  buildFilterBarCategories,
  canDeleteCategory,
  computeMonthlyMetrics,
  computeRunningBalance,
  computeCreditTotalsByCategory,
  computeDebitTotalsByCategory,
  deserializeTransactionsFromJson,
  extractMatchingCategories,
  filterCategorySumsBySelection,
  filterTransactionsByKid,
  filterTransactionsByTag,
  serializeTransactionsToJson,
  toggleCategoryInSet,
} from './utils/ledgerLogic';
import { AndroidSimulator } from './components/AndroidSimulator';
import { KotlinSourceExplorer } from './components/KotlinSourceExplorer';

const INITIAL_TRANSACTIONS: Transaction[] = [
  {
    id: 1,
    kidName: 'Maya',
    title: 'Weekly Allowance',
    amount: 20.0,
    type: TransactionType.CREDIT,
    category: 'Allowance',
    timestamp: Date.now() - 1000 * 60 * 60 * 24 * 6,
  },
  {
    id: 2,
    kidName: 'Maya',
    title: 'Birthday Gift from Grandma',
    amount: 35.0,
    type: TransactionType.CREDIT,
    category: 'Gift',
    timestamp: Date.now() - 1000 * 60 * 60 * 24 * 5,
  },
  {
    id: 3,
    kidName: 'Maya',
    title: 'Lego Space Shuttle',
    amount: 18.5,
    type: TransactionType.DEBIT,
    category: 'Toy',
    timestamp: Date.now() - 1000 * 60 * 60 * 24 * 3,
  },
  {
    id: 4,
    kidName: 'Maya',
    title: 'Science Graphic Novel',
    amount: 11.5,
    type: TransactionType.DEBIT,
    category: 'Book',
    timestamp: Date.now() - 1000 * 60 * 60 * 24 * 2,
  },
  {
    id: 5,
    kidName: 'Maya',
    title: 'Racing Video Game',
    amount: 6.75,
    type: TransactionType.DEBIT,
    category: 'Video Game',
    timestamp: Date.now() - 1000 * 60 * 60 * 14,
  },
];

export function App() {
  const [kids, setKids] = useState<string[]>([DEFAULT_KID_NAME]);
  const [selectedKid, setSelectedKid] = useState<string>(DEFAULT_KID_NAME);
  const [categories, setCategories] = useState<string[]>([
    ...DEFAULT_CATEGORIES,
  ]);
  const [selectedCategories, setSelectedCategories] = useState<Set<string>>(
    () => new Set<string>([...DEFAULT_CATEGORIES])
  );
  const [transactions, setTransactions] =
    useState<Transaction[]>(INITIAL_TRANSACTIONS);
  const [selectedFilterTag, setSelectedFilterTag] = useState<string | null>(
    null
  );
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [statusBanner, setStatusBanner] = useState<string | null>(null);
  const [workspaceView, setWorkspaceView] = useState<'split' | 'code'>('split');

  const showTemporaryStatus = (message: string) => {
    setStatusBanner(message);
    window.setTimeout(() => {
      setStatusBanner((current) => (current === message ? null : current));
    }, 3500);
  };

  const filterBarCategories = useMemo(
    () => buildFilterBarCategories(categories),
    [categories]
  );

  // Filter all transactions down to the currently selected kid
  const kidTransactions = useMemo(
    () => filterTransactionsByKid(transactions, selectedKid),
    [transactions, selectedKid]
  );

  const balance = useMemo(
    () => computeRunningBalance(kidTransactions),
    [kidTransactions]
  );

  const { monthlyIncome, monthlyExpense } = useMemo(
    () => computeMonthlyMetrics(kidTransactions),
    [kidTransactions]
  );

  const filteredTransactions = useMemo(
    () => filterTransactionsByTag(kidTransactions, selectedFilterTag),
    [kidTransactions, selectedFilterTag]
  );

  const rawCategoryIncome = useMemo(
    () => computeCreditTotalsByCategory(kidTransactions, startDate, endDate),
    [kidTransactions, startDate, endDate]
  );

  const matchingIncomeCategories = useMemo(
    () => extractMatchingCategories(rawCategoryIncome),
    [rawCategoryIncome]
  );

  const categoryIncome = useMemo(
    () => filterCategorySumsBySelection(rawCategoryIncome, selectedCategories),
    [rawCategoryIncome, selectedCategories]
  );

  const rawCategorySpending = useMemo(
    () => computeDebitTotalsByCategory(kidTransactions, startDate, endDate),
    [kidTransactions, startDate, endDate]
  );

  const matchingExpenseCategories = useMemo(
    () => extractMatchingCategories(rawCategorySpending),
    [rawCategorySpending]
  );

  const categorySpending = useMemo(
    () => filterCategorySumsBySelection(rawCategorySpending, selectedCategories),
    [rawCategorySpending, selectedCategories]
  );

  const handleToggleCategorySelection = (category: string) => {
    setSelectedCategories((prev) => toggleCategoryInSet(prev, category));
  };

  const handleSelectKid = (kidName: string) => {
    setSelectedKid(kidName);
  };

  const handleAddKid = (kidName: string) => {
    const trimmed = kidName.trim();
    if (!trimmed) return;
    const existing = kids.find(
      (k) => k.toLowerCase() === trimmed.toLowerCase()
    );
    if (existing) {
      setSelectedKid(existing);
      showTemporaryStatus(`Switched to ${existing}'s ledger`);
      return;
    }
    setKids((prev) => [...prev, trimmed]);
    setSelectedKid(trimmed);
    showTemporaryStatus(`Added ${trimmed}'s ledger`);
  };

  const handleAddCategory = (name: string) => {
    const trimmed = name.trim();
    if (!trimmed) return;
    setSelectedCategories((prev) => new Set(prev).add(trimmed));
    setCategories((prev) =>
      prev.some((c) => c.toLowerCase() === trimmed.toLowerCase())
        ? prev
        : [...prev, trimmed]
    );
    showTemporaryStatus(`Added "${trimmed}" to categories`);
  };

  const handleDeleteUnusedCategory = (name: string) => {
    if (!canDeleteCategory(transactions, name)) {
      showTemporaryStatus(
        `Cannot delete "${name}" because it is used in transactions.`
      );
      return;
    }

    setCategories((prev) =>
      prev.filter((c) => c.toLowerCase() !== name.toLowerCase())
    );
    setSelectedCategories((prev) => {
      const next = new Set(prev);
      for (const item of next) {
        if (item.toLowerCase() === name.toLowerCase()) {
          next.delete(item);
        }
      }
      return next;
    });

    if (
      selectedFilterTag &&
      selectedFilterTag.toLowerCase() === name.toLowerCase()
    ) {
      setSelectedFilterTag(null);
    }

    showTemporaryStatus(`Removed unused category "${name}"`);
  };

  const handleAddTransaction = (
    title: string,
    amount: number,
    type: TransactionType,
    category: string
  ) => {
    const nextId =
      transactions.reduce((max, item) => Math.max(max, item.id), 0) + 1;
    const newTx: Transaction = {
      id: nextId,
      kidName: selectedKid,
      title,
      amount,
      type,
      category,
      timestamp: Date.now(),
    };
    setTransactions((prev) => [newTx, ...prev]);
    setCategories((prev) =>
      prev.some((c) => c.toLowerCase() === category.toLowerCase())
        ? prev
        : [...prev, category]
    );
    setSelectedCategories((prev) => new Set(prev).add(category));
    showTemporaryStatus(
      `${type === TransactionType.CREDIT ? 'Added' : 'Recorded'} $${amount.toFixed(
        2
      )} (${title}) for ${selectedKid}`
    );
  };

  const handleDeleteTransaction = (tx: Transaction) => {
    setTransactions((prev) => prev.filter((item) => item.id !== tx.id));
    showTemporaryStatus(`Deleted "${tx.title}"`);
  };

  const handleSetDateRange = (nextStart: string, nextEnd: string) => {
    setStartDate(nextStart);
    setEndDate(nextEnd);
  };

  const handleExportBackup = () => {
    const jsonPayload = serializeTransactionsToJson(transactions);
    const blob = new Blob([jsonPayload], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `allowance_backup_${new Date()
      .toISOString()
      .slice(0, 10)}.json`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
    showTemporaryStatus(
      `Exported ${transactions.length} transactions to JSON backup`
    );
  };

  const handleImportBackupFile = async (file: File) => {
    try {
      const rawText = await file.text();
      const imported = deserializeTransactionsFromJson(rawText);
      setTransactions(imported);
      const importedKids = Array.from(
        new Set(imported.map((tx) => tx.kidName))
      );
      if (importedKids.length > 0) {
        setKids((prev) => Array.from(new Set([...prev, ...importedKids])));
        if (!importedKids.includes(selectedKid)) {
          setSelectedKid(importedKids[0]);
        }
      }
      // Ensure any custom categories in imported transactions are registered
      const importedCats = Array.from(
        new Set(imported.map((tx) => tx.category.trim()).filter(Boolean))
      );
      if (importedCats.length > 0) {
        setCategories((prev) => {
          const next = [...prev];
          for (const cat of importedCats) {
            if (!next.some((c) => c.toLowerCase() === cat.toLowerCase())) {
              next.push(cat);
            }
          }
          return next;
        });
        setSelectedCategories((prev) => {
          const next = new Set(prev);
          for (const cat of importedCats) {
            next.add(cat);
          }
          return next;
        });
      }
      showTemporaryStatus(
        `Restored ${imported.length} transactions from backup`
      );
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Invalid backup JSON file.';
      showTemporaryStatus(`Restore failed: ${message}`);
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      {/* Top Workspace Header */}
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-3.5 sm:px-6">
          <div>
            <h1 className="text-base font-bold tracking-tight text-slate-900 sm:text-lg">
              Kids Allowance &amp; Spending Tracker
            </h1>
            <p className="text-xs text-slate-500">
              Native Android (Kotlin · Jetpack Compose · Material 3 · Room
              SQLite · kotlinx.serialization)
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setWorkspaceView('split')}
              className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition-colors ${
                workspaceView === 'split'
                  ? 'bg-teal-900 text-white'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
              }`}
            >
              <Smartphone className="h-3.5 w-3.5" />
              Simulator + Code
            </button>
            <button
              type="button"
              onClick={() => setWorkspaceView('code')}
              className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition-colors ${
                workspaceView === 'code'
                  ? 'bg-teal-900 text-white'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
              }`}
            >
              <Code2 className="h-3.5 w-3.5" />
              Full Kotlin Source
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
        {workspaceView === 'split' ? (
          <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
            <div className="lg:col-span-5">
              <AndroidSimulator
                kids={kids}
                selectedKid={selectedKid}
                onSelectKid={handleSelectKid}
                onAddKid={handleAddKid}
                balance={balance}
                monthlyIncome={monthlyIncome}
                monthlyExpense={monthlyExpense}
                transactions={filteredTransactions}
                allTransactions={transactions}
                categories={categories}
                filterBarCategories={filterBarCategories}
                matchingIncomeCategories={matchingIncomeCategories}
                matchingExpenseCategories={matchingExpenseCategories}
                selectedCategories={selectedCategories}
                onToggleCategorySelection={handleToggleCategorySelection}
                onAddCategory={handleAddCategory}
                onDeleteUnusedCategory={handleDeleteUnusedCategory}
                categoryIncome={categoryIncome}
                categorySpending={categorySpending}
                selectedFilterTag={selectedFilterTag}
                startDate={startDate}
                endDate={endDate}
                statusBanner={statusBanner}
                onSetFilterTag={setSelectedFilterTag}
                onSetDateRange={handleSetDateRange}
                onAddTransaction={handleAddTransaction}
                onDeleteTransaction={handleDeleteTransaction}
                onExportBackup={handleExportBackup}
                onImportBackupFile={handleImportBackupFile}
              />
            </div>

            <div className="lg:col-span-7">
              <KotlinSourceExplorer />
            </div>
          </div>
        ) : (
          <KotlinSourceExplorer />
        )}
      </main>
    </div>
  );
}

export default App;
