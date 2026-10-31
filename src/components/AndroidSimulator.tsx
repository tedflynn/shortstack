import React, { useRef, useState } from 'react';
import {
  Calendar,
  Check,
  ChevronDown,
  Download,
  MoreVertical,
  Plus,
  RotateCcw,
  Trash2,
  Upload,
  User,
  X,
} from 'lucide-react';
import {
  CategorySum,
  Transaction,
  TransactionType,
} from '../utils/ledgerLogic';
import { AddTransactionModal } from './AddTransactionModal';
import { ManageCategoriesModal } from './ManageCategoriesModal';
import {
  CategoryDonutChart,
  CategoryLegend,
  MatchingCategoriesChipGroup,
} from './SpendingCharts';

interface AndroidSimulatorProps {
  kids: string[];
  selectedKid: string;
  onSelectKid: (kidName: string) => void;
  onAddKid: (kidName: string) => void;
  balance: number;
  monthlyIncome: number;
  monthlyExpense: number;
  transactions: Transaction[];
  allTransactions: Transaction[];
  categories: string[];
  filterBarCategories: string[];
  matchingIncomeCategories: string[];
  matchingExpenseCategories: string[];
  selectedCategories: Set<string>;
  onToggleCategorySelection: (category: string) => void;
  onAddCategory: (name: string) => void;
  onDeleteUnusedCategory: (name: string) => void;
  categoryIncome: CategorySum[];
  categorySpending: CategorySum[];
  selectedFilterTag: string | null;
  startDate: string;
  endDate: string;
  statusBanner: string | null;
  onSetFilterTag: (tag: string | null) => void;
  onSetDateRange: (startDate: string, endDate: string) => void;
  onAddTransaction: (
    title: string,
    amount: number,
    type: TransactionType,
    category: string
  ) => void;
  onDeleteTransaction: (tx: Transaction) => void;
  onExportBackup: () => void;
  onImportBackupFile: (file: File) => void;
}

export const AndroidSimulator: React.FC<AndroidSimulatorProps> = ({
  kids,
  selectedKid,
  onSelectKid,
  onAddKid,
  balance,
  transactions,
  allTransactions,
  categories,
  filterBarCategories,
  matchingIncomeCategories,
  matchingExpenseCategories,
  selectedCategories,
  onToggleCategorySelection,
  onAddCategory,
  onDeleteUnusedCategory,
  categoryIncome,
  categorySpending,
  selectedFilterTag,
  startDate,
  endDate,
  statusBanner,
  onSetFilterTag,
  onSetDateRange,
  onAddTransaction,
  onDeleteTransaction,
  onExportBackup,
  onImportBackupFile,
}) => {
  const [activeTab, setActiveTab] = useState<'tally' | 'income' | 'expenses'>(
    'tally'
  );
  const [overflowOpen, setOverflowOpen] = useState(false);
  const [kidMenuOpen, setKidMenuOpen] = useState(false);
  const [addKidModalOpen, setAddKidModalOpen] = useState(false);
  const [manageCategoriesOpen, setManageCategoriesOpen] = useState(false);
  const [newKidNameInput, setNewKidNameInput] = useState('');
  const [newKidError, setNewKidError] = useState<string | null>(null);
  const [showEntrySheet, setShowEntrySheet] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const currencyFormatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  });

  const dateFormatter = new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onImportBackupFile(file);
      e.target.value = '';
    }
  };

  const handleConfirmAddKid = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = newKidNameInput.trim();
    if (!trimmed) {
      setNewKidError("Please enter the kid's name.");
      return;
    }
    onAddKid(trimmed);
    setNewKidNameInput('');
    setNewKidError(null);
    setAddKidModalOpen(false);
  };

  const hasDateRangeFilter = Boolean(startDate || endDate);

  const renderDateRangeSelectorCard = (tabLabel: string) => (
    <section
      aria-label="Date Range Selector"
      className="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-2xs"
    >
      <div className="mb-3 flex items-center justify-between">
        <div>
          <h3 className="text-xs font-bold text-slate-900">
            Summary Date Range
          </h3>
          <p className="text-[11px] text-slate-500">
            Select start and end dates for {selectedKid}&apos;s {tabLabel.toLowerCase()}
          </p>
        </div>
        {hasDateRangeFilter && (
          <button
            type="button"
            onClick={() => onSetDateRange('', '')}
            className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-[11px] font-semibold text-teal-800 hover:bg-teal-50"
          >
            <RotateCcw className="h-3 w-3" />
            Reset All
          </button>
        )}
      </div>

      <div className="grid grid-cols-2 gap-2.5">
        <div>
          <label
            htmlFor="breakdown-start-date"
            className="mb-1 flex items-center gap-1 text-[11px] font-semibold text-slate-600"
          >
            <Calendar className="h-3 w-3 text-teal-700" />
            Start Date
          </label>
          <input
            id="breakdown-start-date"
            type="date"
            value={startDate}
            onChange={(e) => onSetDateRange(e.target.value, endDate)}
            className="w-full rounded-xl border border-slate-300 bg-slate-50/70 px-2.5 py-1.5 text-xs font-medium text-slate-900 focus:border-teal-700 focus:bg-white focus:outline-none"
          />
        </div>
        <div>
          <label
            htmlFor="breakdown-end-date"
            className="mb-1 flex items-center gap-1 text-[11px] font-semibold text-slate-600"
          >
            <Calendar className="h-3 w-3 text-teal-700" />
            End Date
          </label>
          <input
            id="breakdown-end-date"
            type="date"
            value={endDate}
            onChange={(e) => onSetDateRange(startDate, e.target.value)}
            className="w-full rounded-xl border border-slate-300 bg-slate-50/70 px-2.5 py-1.5 text-xs font-medium text-slate-900 focus:border-teal-700 focus:bg-white focus:outline-none"
          />
        </div>
      </div>
    </section>
  );

  return (
    <div
      className="relative mx-auto flex h-[640px] w-full max-w-[430px] flex-col overflow-hidden rounded-[2rem] border border-slate-300/90 bg-slate-50 shadow-lg"
      data-testid="android-simulator-viewport"
    >
      <input
        ref={fileInputRef}
        type="file"
        accept="application/json,.json"
        className="hidden"
        onChange={handleFileChange}
        aria-label="Restore JSON Backup File"
      />

      {/* Material 3 TopAppBar with Top-Left Kid Selector */}
      <header className=" shrink-0 bg-white">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-2.5">
            {/* Top-left Kid Profile Switcher */}
            <div className="relative">
              <button
                type="button"
                onClick={() => {
                  setKidMenuOpen((prev) => !prev);
                  setOverflowOpen(false);
                }}
                aria-label="Select Kid Profile"
                aria-expanded={kidMenuOpen}
                className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-slate-50 px-3 py-1.5 text-xs font-bold text-slate-900 shadow-2xs transition-colors hover:bg-slate-100"
              >
                <User className="h-3.5 w-3.5 text-teal-800" />
                <span>{selectedKid}</span>
                <ChevronDown className="h-3.5 w-3.5 text-slate-500" />
              </button>

              {kidMenuOpen && (
                <div
                  role="menu"
                  aria-label="Kids List"
                  className="absolute left-0 z-40 mt-1.5 w-48 rounded-xl border border-slate-200 bg-white py-1.5 shadow-xl"
                >
                  {kids.map((kidName) => {
                    const isCurrent = kidName === selectedKid;
                    return (
                      <button
                        key={kidName}
                        type="button"
                        role="menuitem"
                        onClick={() => {
                          setKidMenuOpen(false);
                          onSelectKid(kidName);
                        }}
                        className={`flex w-full items-center justify-between px-3.5 py-2 text-left text-xs transition-colors hover:bg-slate-100 ${
                          isCurrent
                            ? 'font-bold text-teal-900'
                            : 'font-medium text-slate-700'
                        }`}
                      >
                        <span>{kidName}</span>
                        {isCurrent && (
                          <Check className="h-3.5 w-3.5 text-teal-700" />
                        )}
                      </button>
                    );
                  })}

                  <div className="my-1 h-px bg-slate-100" />

                  <button
                    type="button"
                    role="menuitem"
                    onClick={() => {
                      setKidMenuOpen(false);
                      setNewKidNameInput('');
                      setNewKidError(null);
                      setAddKidModalOpen(true);
                    }}
                    className="flex w-full items-center gap-1.5 px-3.5 py-2 text-left text-xs font-bold text-teal-800 transition-colors hover:bg-teal-50"
                  >
                    <span>+ Kid</span>
                  </button>
                </div>
              )}
            </div>

            <img
              src="/assets/aistudio/bodad_537ad8c2.png"
              alt="boDad Logo"
              className="h-8 w-auto object-contain"
            />
          </div>

          <div className="relative">
            <button
              type="button"
              onClick={() => {
                setOverflowOpen((prev) => !prev);
                setKidMenuOpen(false);
              }}
              aria-label="Backup Options"
              aria-expanded={overflowOpen}
              className="rounded-lg p-2 text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900"
            >
              <MoreVertical className="h-5 w-5" />
            </button>

            {overflowOpen && (
              <div
                role="menu"
                className="absolute right-0 z-30 mt-1 w-56 rounded-xl border border-slate-200 bg-white py-1.5 shadow-xl"
              >
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setOverflowOpen(false);
                    onExportBackup();
                  }}
                  className="flex w-full items-center gap-2.5 px-4 py-2.5 text-left text-xs font-medium text-slate-800 hover:bg-slate-100"
                >
                  <Download className="h-4 w-4 text-teal-700" />
                  Export Backup to Drive
                </button>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setOverflowOpen(false);
                    fileInputRef.current?.click();
                  }}
                  className="flex w-full items-center gap-2.5 px-4 py-2.5 text-left text-xs font-medium text-slate-800 hover:bg-slate-100"
                >
                  <Upload className="h-4 w-4 text-teal-700" />
                  Restore Backup
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Material 3 Primary TabRow: Ledger | Income | Expenses */}
        <div
          role="tablist"
          aria-label="App Navigation Tabs"
          className="grid grid-cols-3 border-t border-slate-100 px-2"
        >
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'tally'}
            onClick={() => setActiveTab('tally')}
            className={`relative py-3 text-xs font-semibold transition-colors ${
              activeTab === 'tally'
                ? 'text-teal-900'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            Ledger
            {activeTab === 'tally' && (
              <span
                className="absolute inset-x-4 bottom-0 h-0.5 rounded-t-full bg-teal-800"
                aria-hidden="true"
              />
            )}
          </button>

          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'income'}
            onClick={() => setActiveTab('income')}
            className={`relative py-3 text-xs font-semibold transition-colors ${
              activeTab === 'income'
                ? 'text-teal-900'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            Income
            {activeTab === 'income' && (
              <span
                className="absolute inset-x-4 bottom-0 h-0.5 rounded-t-full bg-teal-800"
                aria-hidden="true"
              />
            )}
          </button>

          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'expenses'}
            onClick={() => setActiveTab('expenses')}
            className={`relative py-3 text-xs font-semibold transition-colors ${
              activeTab === 'expenses'
                ? 'text-teal-900'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            Expenses
            {activeTab === 'expenses' && (
              <span
                className="absolute inset-x-4 bottom-0 h-0.5 rounded-t-full bg-teal-800"
                aria-hidden="true"
              />
            )}
          </button>
        </div>
      </header>

      {/* Main Scrollable Content */}
      <div
        className="flex-1 space-y-3.5 overflow-y-auto p-4 pb-24"
        data-testid="simulator-scroll-container"
      >
        {statusBanner && (
          <div
            role="status"
            className="rounded-xl bg-slate-900 px-3.5 py-2 text-xs font-medium text-white shadow-xs"
          >
            {statusBanner}
          </div>
        )}

        {activeTab === 'tally' && (
          <>
            {/* Header Card: Kid's Balance */}
            <section
              aria-label="Balance Summary"
              className="rounded-2xl bg-teal-900 p-5 text-white shadow-xs"
            >
              <p className="text-xs font-medium text-teal-100/90">
                {selectedKid}&apos;s Balance
              </p>
              <p
                className={`mt-1 text-3xl font-extrabold tracking-tight ${
                  balance < 0 ? 'text-red-400' : 'text-white'
                }`}
                data-testid="running-balance-value"
              >
                {currencyFormatter.format(balance)}
              </p>
            </section>

            {/* Transaction List */}
            <div className="space-y-2" data-testid="transaction-list">
              {transactions.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-6 text-center">
                  <p className="text-sm font-semibold text-slate-800">
                    No transactions recorded for {selectedKid} yet
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    Use &quot;+ Entry&quot; below to log an entry.
                  </p>
                </div>
              ) : (
                transactions.map((tx) => {
                  const isCredit = tx.type === TransactionType.CREDIT;
                  const signedAmount = `${isCredit ? '+' : '-'}${currencyFormatter.format(
                    tx.amount
                  )}`;

                  return (
                    <div
                      key={tx.id}
                      className="flex items-center justify-between rounded-2xl border border-slate-200/80 bg-white px-3.5 py-3 shadow-2xs transition-colors hover:border-slate-300"
                    >
                      <div className="flex min-w-0 flex-1 items-center gap-3">
                        <div
                          className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-bold ${
                            isCredit
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-red-100 text-red-800'
                          }`}
                          aria-hidden="true"
                        >
                          {isCredit ? '+' : '-'}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-semibold text-slate-900">
                            {tx.title}
                          </p>
                          <div className="mt-0.5 flex items-center gap-1.5 text-xs text-slate-500">
                            <span className="font-medium text-slate-700">
                              {tx.category}
                            </span>
                            <span aria-hidden="true">·</span>
                            <span>
                              {dateFormatter.format(new Date(tx.timestamp))}
                            </span>
                          </div>
                        </div>
                      </div>

                      <div className="ml-3 flex items-center gap-2">
                        <span
                          className={`text-sm font-bold ${
                            isCredit ? 'text-emerald-700' : 'text-red-700'
                          }`}
                        >
                          {signedAmount}
                        </span>
                        <button
                          type="button"
                          onClick={() => onDeleteTransaction(tx)}
                          aria-label={`Delete ${tx.title}`}
                          className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-red-50 hover:text-red-700"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </>
        )}

        {activeTab === 'income' && (
          <div className="space-y-3.5" data-testid="income-tab-content">
            {renderDateRangeSelectorCard('Income')}

            {/* Matching Categories Section for Income */}
            <section
              aria-label="Income Categories"
              className="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-2xs"
            >
              <MatchingCategoriesChipGroup
                title="Income Categories"
                accentColorClass="text-emerald-800"
                matchingCategories={matchingIncomeCategories}
                selectedCategories={selectedCategories}
                onToggleCategory={onToggleCategorySelection}
                emptyMessage="No matching income categories in this date range"
              />
            </section>

            {/* Income Donut Chart + Legend */}
            <section
              aria-label="Income Breakdown Chart"
              className="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-2xs"
            >
              <div className="mb-3 text-center">
                <h3 className="text-sm font-bold text-slate-900">
                  {selectedKid}&apos;s Income
                </h3>
                <p className="text-[11px] text-slate-500">
                  {hasDateRangeFilter
                    ? `${startDate || 'Beginning'} — ${endDate || 'Today'}`
                    : 'All recorded income'}
                </p>
              </div>

              <div
                className="flex flex-col items-center rounded-xl bg-emerald-50/40 p-4"
                data-testid="income-donut-section"
              >
                <span className="mb-2 text-xs font-bold text-emerald-800">
                  Income
                </span>
                <CategoryDonutChart
                  categorySpending={categoryIncome}
                  totalLabel="Total Income"
                  emptyLabel="No Income"
                  size={156}
                  strokeWidth={22}
                />
                <CategoryLegend categorySpending={categoryIncome} />
              </div>
            </section>
          </div>
        )}

        {activeTab === 'expenses' && (
          <div className="space-y-3.5" data-testid="expenses-tab-content">
            {renderDateRangeSelectorCard('Expenses')}

            {/* Matching Categories Section for Expenses */}
            <section
              aria-label="Expense Categories"
              className="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-2xs"
            >
              <MatchingCategoriesChipGroup
                title="Expense Categories"
                accentColorClass="text-red-800"
                matchingCategories={matchingExpenseCategories}
                selectedCategories={selectedCategories}
                onToggleCategory={onToggleCategorySelection}
                emptyMessage="No matching expense categories in this date range"
              />
            </section>

            {/* Expenses Donut Chart + Legend */}
            <section
              aria-label="Expenses Breakdown Chart"
              className="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-2xs"
            >
              <div className="mb-3 text-center">
                <h3 className="text-sm font-bold text-slate-900">
                  {selectedKid}&apos;s Expenses
                </h3>
                <p className="text-[11px] text-slate-500">
                  {hasDateRangeFilter
                    ? `${startDate || 'Beginning'} — ${endDate || 'Today'}`
                    : 'All recorded expenses'}
                </p>
              </div>

              <div
                className="flex flex-col items-center rounded-xl bg-red-50/40 p-4"
                data-testid="expense-donut-section"
              >
                <span className="mb-2 text-xs font-bold text-red-800">
                  Expenses
                </span>
                <CategoryDonutChart
                  categorySpending={categorySpending}
                  totalLabel="Total Expenses"
                  emptyLabel="No Expenses"
                  size={156}
                  strokeWidth={22}
                />
                <CategoryLegend categorySpending={categorySpending} />
              </div>
            </section>
          </div>
        )}
      </div>

      {/* Persistent Scaffold floatingActionButton slot at bottom-right */}
      <div className="pointer-events-none absolute right-4 bottom-4 z-30">
        <button
          type="button"
          onClick={() => setShowEntrySheet(true)}
          data-testid="extended-fab-entry"
          className="pointer-events-auto inline-flex items-center gap-2 rounded-2xl bg-teal-800 px-5 py-3.5 text-sm font-bold text-white shadow-lg transition-all hover:bg-teal-900 active:scale-[0.99]"
        >
          <Plus className="h-4 w-4 stroke-[2.5]" />
          <span>Entry</span>
        </button>
      </div>

      {/* Add Kid Profile Dialog */}
      {addKidModalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="add-kid-dialog-title"
        >
          <div className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl">
            <div className="mb-3 flex items-center justify-between">
              <h3
                id="add-kid-dialog-title"
                className="text-base font-bold text-slate-900"
              >
                Add Kid Profile
              </h3>
              <button
                type="button"
                onClick={() => setAddKidModalOpen(false)}
                aria-label="Close Add Kid Modal"
                className="rounded-lg p-1 text-slate-500 hover:bg-slate-100"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <p className="mb-4 text-xs text-slate-500">
              Create a separate allowance ledger and breakdown for another child.
            </p>
            <form onSubmit={handleConfirmAddKid} className="space-y-3">
              <div>
                <label
                  htmlFor="kid-name-input"
                  className="mb-1 block text-xs font-semibold text-slate-700"
                >
                  Kid&apos;s Name
                </label>
                <input
                  id="kid-name-input"
                  type="text"
                  placeholder="e.g., Leo, Sam..."
                  value={newKidNameInput}
                  onChange={(e) => {
                    setNewKidNameInput(e.target.value);
                    setNewKidError(null);
                  }}
                  className="w-full rounded-xl border border-slate-300 px-3.5 py-2 text-sm text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20"
                />
              </div>
              {newKidError && (
                <p role="alert" className="text-xs font-medium text-red-700">
                  {newKidError}
                </p>
              )}
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setAddKidModalOpen(false)}
                  className="rounded-xl px-3.5 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-xl bg-teal-800 px-4 py-2 text-xs font-bold text-white shadow-xs hover:bg-teal-900"
                >
                  Add Kid
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Manage Categories (+/-) Modal */}
      {manageCategoriesOpen && (
        <ManageCategoriesModal
          categories={categories}
          allTransactions={allTransactions}
          onAddCategory={onAddCategory}
          onDeleteUnusedCategory={onDeleteUnusedCategory}
          onClose={() => setManageCategoriesOpen(false)}
        />
      )}

      {/* ModalBottomSheet for Transaction Entry */}
      {showEntrySheet && (
        <AddTransactionModal
          kidName={selectedKid}
          categories={categories}
          onClose={() => setShowEntrySheet(false)}
          onAddCategory={onAddCategory}
          onSave={(title, amount, type, category) => {
            onAddTransaction(title, amount, type, category);
            setShowEntrySheet(false);
          }}
        />
      )}
    </div>
  );
};
