import React, { useRef, useState } from 'react';
import { Check, X } from 'lucide-react';
import {
  TransactionType,
  appendCustomCategory,
  validateTransactionInput,
} from '../utils/ledgerLogic';

interface AddTransactionModalProps {
  kidName: string;
  categories: string[];
  onClose: () => void;
  onAddCategory?: (name: string) => void;
  onSave: (
    title: string,
    amount: number,
    type: TransactionType,
    category: string
  ) => void;
}

export const AddTransactionModal: React.FC<AddTransactionModalProps> = ({
  kidName,
  categories,
  onClose,
  onAddCategory,
  onSave,
}) => {
  const [selectedType, setSelectedType] = useState<TransactionType>(
    TransactionType.CREDIT
  );
  const isCredit = selectedType === TransactionType.CREDIT;

  const [amountInput, setAmountInput] = useState('');
  const [noteInput, setNoteInput] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>(
    categories[0] ?? 'Allowance'
  );
  const [error, setError] = useState<string | null>(null);

  const [showNewCategoryPrompt, setShowNewCategoryPrompt] = useState(false);
  const [newCategoryInput, setNewCategoryInput] = useState('');
  const [newCategoryError, setNewCategoryError] = useState<string | null>(null);
  const newCategoryInputRef = useRef<HTMLInputElement | null>(null);

  const handleSelectSegment = (nextType: TransactionType) => {
    setSelectedType(nextType);
    setError(null);
  };

  const handleOpenNewCategoryPrompt = () => {
    setShowNewCategoryPrompt(true);
    setNewCategoryInput('');
    setNewCategoryError(null);
    window.setTimeout(() => {
      newCategoryInputRef.current?.focus();
    }, 0);
  };

  const handleConfirmNewCategory = () => {
    const result = appendCustomCategory(categories, newCategoryInput);
    if (!result.valid) {
      setNewCategoryError(result.error);
      return;
    }
    onAddCategory?.(result.selectedCategory);
    setSelectedCategory(result.selectedCategory);
    setShowNewCategoryPrompt(false);
    setNewCategoryInput('');
    setNewCategoryError(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const result = validateTransactionInput(noteInput, amountInput);
    if (!result.valid) {
      setError(result.error);
      return;
    }
    onSave(
      result.trimmedTitle,
      result.parsedAmount,
      selectedType,
      selectedCategory
    );
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/45 p-0 sm:items-center sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="bottom-sheet-title"
    >
      <div className="w-full max-w-md rounded-t-3xl bg-white p-6 shadow-2xl sm:rounded-3xl">
        <div className="mx-auto mb-3 h-1.5 w-10 rounded-full bg-slate-300 sm:hidden" />
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2
              id="bottom-sheet-title"
              className="text-lg font-bold text-slate-900"
            >
              New Entry for {kidName}
            </h2>
            <p className="text-xs text-slate-500">
              Select Income or Expense and choose a category
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal"
            className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* SingleChoiceSegmentedButtonRow: Income | Expense */}
        <div
          role="radiogroup"
          aria-label="Transaction Type"
          className="mb-4 grid grid-cols-2 overflow-hidden rounded-full border border-slate-300 bg-slate-50"
        >
          <button
            type="button"
            role="radio"
            aria-checked={isCredit}
            onClick={() => handleSelectSegment(TransactionType.CREDIT)}
            className={`flex items-center justify-center gap-1.5 border-r border-slate-300 py-2.5 text-xs font-bold transition-colors ${
              isCredit
                ? 'bg-emerald-100 text-emerald-950'
                : 'bg-transparent text-slate-700 hover:bg-slate-100'
            }`}
          >
            {isCredit && <Check className="h-3.5 w-3.5 shrink-0" />}
            <span>Income</span>
          </button>

          <button
            type="button"
            role="radio"
            aria-checked={!isCredit}
            onClick={() => handleSelectSegment(TransactionType.DEBIT)}
            className={`flex items-center justify-center gap-1.5 py-2.5 text-xs font-bold transition-colors ${
              !isCredit
                ? 'bg-red-100 text-red-950'
                : 'bg-transparent text-slate-700 hover:bg-slate-100'
            }`}
          >
            {!isCredit && <Check className="h-3.5 w-3.5 shrink-0" />}
            <span>Expense</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label
              htmlFor="transaction-amount"
              className="mb-1 block text-xs font-semibold text-slate-700"
            >
              Amount ($)
            </label>
            <input
              id="transaction-amount"
              type="number"
              inputMode="decimal"
              step="0.01"
              min="0.01"
              placeholder="0.00"
              value={amountInput}
              onChange={(e) => {
                setAmountInput(e.target.value);
                setError(null);
              }}
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-base font-medium text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20"
            />
          </div>

          <div>
            <label
              htmlFor="transaction-note"
              className="mb-1 block text-xs font-semibold text-slate-700"
            >
              Item Note / Description
            </label>
            <input
              id="transaction-note"
              type="text"
              placeholder={
                isCredit
                  ? 'Weekly allowance, Birthday gift, Lawn mowing...'
                  : 'Video game, Lego set, Book...'
              }
              value={noteInput}
              onChange={(e) => {
                setNoteInput(e.target.value);
                setError(null);
              }}
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20"
            />
          </div>

          <div>
            <span className="mb-2 block text-xs font-semibold text-slate-700">
              Category
            </span>
            <div
              className="flex flex-wrap gap-1.5"
              role="group"
              aria-label="Category picker"
            >
              {categories.map((category) => {
                const active = selectedCategory === category;
                return (
                  <button
                    key={category}
                    type="button"
                    aria-pressed={active}
                    onClick={() => setSelectedCategory(category)}
                    className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                      active
                        ? 'bg-teal-800 text-white shadow-xs'
                        : 'border border-slate-300 bg-transparent text-slate-700 hover:bg-slate-100'
                    }`}
                  >
                    {active && <Check className="h-3.5 w-3.5 shrink-0" />}
                    <span>{category}</span>
                  </button>
                );
              })}

              <button
                type="button"
                onClick={handleOpenNewCategoryPrompt}
                data-testid="add-new-category-chip"
                className={`inline-flex items-center gap-1 rounded-lg border px-3 py-1.5 text-xs font-bold transition-colors ${
                  showNewCategoryPrompt
                    ? 'border-teal-800 bg-teal-50 text-teal-900'
                    : 'border-dashed border-teal-700 bg-teal-50/40 text-teal-900 hover:bg-teal-50'
                }`}
              >
                <span>+ New</span>
              </button>
            </div>

            {showNewCategoryPrompt && (
              <div
                className="mt-2.5 rounded-xl border border-teal-200 bg-teal-50/50 p-3"
                data-testid="new-category-prompt"
              >
                <label
                  htmlFor="new-category-name-input"
                  className="mb-1.5 block text-xs font-semibold text-slate-800"
                >
                  New Category Name
                </label>
                <div className="flex items-center gap-2">
                  <input
                    ref={newCategoryInputRef}
                    id="new-category-name-input"
                    type="text"
                    placeholder="e.g., Craft Supplies, Sports..."
                    value={newCategoryInput}
                    onChange={(e) => {
                      setNewCategoryInput(e.target.value);
                      setNewCategoryError(null);
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleConfirmNewCategory();
                      } else if (e.key === 'Escape') {
                        e.preventDefault();
                        setShowNewCategoryPrompt(false);
                        setNewCategoryError(null);
                      }
                    }}
                    className="min-w-0 flex-1 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20"
                  />
                  <button
                    type="button"
                    onClick={handleConfirmNewCategory}
                    className="shrink-0 rounded-lg bg-teal-800 px-3 py-1.5 text-xs font-bold text-white shadow-2xs transition-colors hover:bg-teal-900"
                  >
                    Add
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowNewCategoryPrompt(false);
                      setNewCategoryError(null);
                    }}
                    className="shrink-0 rounded-lg px-2.5 py-1.5 text-xs font-semibold text-slate-600 transition-colors hover:bg-slate-200/70"
                  >
                    Cancel
                  </button>
                </div>
                {newCategoryError && (
                  <p
                    role="alert"
                    className="mt-1.5 text-xs font-medium text-red-700"
                  >
                    {newCategoryError}
                  </p>
                )}
              </div>
            )}
          </div>

          {error && (
            <p role="alert" className="text-xs font-medium text-red-700">
              {error}
            </p>
          )}

          <div className="pt-2">
            <button
              type="submit"
              className={`w-full rounded-xl px-4 py-3 text-sm font-bold text-white shadow-sm transition-colors ${
                isCredit
                  ? 'bg-emerald-700 hover:bg-emerald-800'
                  : 'bg-red-700 hover:bg-red-800'
              }`}
            >
              Save Entry
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
