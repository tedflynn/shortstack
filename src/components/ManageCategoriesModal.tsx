import React, { useState } from 'react';
import { Plus, Trash2, X } from 'lucide-react';
import {
  Transaction,
  countCategoryUsage,
} from '../utils/ledgerLogic';

interface ManageCategoriesModalProps {
  categories: string[];
  allTransactions: Transaction[];
  onAddCategory: (name: string) => void;
  onDeleteUnusedCategory: (name: string) => void;
  onClose: () => void;
}

export const ManageCategoriesModal: React.FC<ManageCategoriesModalProps> = ({
  categories,
  allTransactions,
  onAddCategory,
  onDeleteUnusedCategory,
  onClose,
}) => {
  const [newCategoryName, setNewCategoryName] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleAdd = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = newCategoryName.trim();
    if (!trimmed) {
      setError('Please enter a category name.');
      return;
    }
    if (
      categories.some(
        (cat) => cat.toLowerCase() === trimmed.toLowerCase()
      )
    ) {
      setError(`"${trimmed}" already exists.`);
      return;
    }
    onAddCategory(trimmed);
    setNewCategoryName('');
    setError(null);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="manage-categories-title"
    >
      <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h3
              id="manage-categories-title"
              className="text-base font-bold text-slate-900"
            >
              Manage Categories ({categories.length})
            </h3>
            <p className="text-xs text-slate-500">
              Add custom categories to the pool or remove categories that aren&apos;t used
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Manage Categories"
            className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Add Category Form */}
        <form onSubmit={handleAdd} className="mt-3">
          <label
            htmlFor="new-category-input"
            className="mb-1 block text-xs font-semibold text-slate-700"
          >
            Add Category
          </label>
          <div className="flex gap-2">
            <input
              id="new-category-input"
              type="text"
              placeholder="e.g., Chore Bonus, Snack, Craft Supplies..."
              value={newCategoryName}
              onChange={(e) => {
                setNewCategoryName(e.target.value);
                setError(null);
              }}
              className="flex-1 rounded-xl border border-slate-300 px-3.5 py-2 text-xs font-medium text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20"
            />
            <button
              type="submit"
              className="flex items-center gap-1 rounded-xl bg-teal-800 px-3.5 py-2 text-xs font-bold text-white shadow-xs hover:bg-teal-900"
            >
              <Plus className="h-3.5 w-3.5" />
              Add
            </button>
          </div>
          {error && (
            <p role="alert" className="mt-1.5 text-xs font-medium text-red-700">
              {error}
            </p>
          )}
        </form>

        {/* Category List with Unused Delete Option */}
        <div className="mt-4 max-h-60 divide-y divide-slate-100 overflow-y-auto rounded-2xl border border-slate-200 bg-slate-50/60 px-3.5 py-1">
          {categories.map((catName) => {
            const usageCount = countCategoryUsage(allTransactions, catName);
            const isUnused = usageCount === 0;

            return (
              <div
                key={catName}
                className="flex items-center justify-between py-2.5 text-xs"
              >
                <div>
                  <p className="font-semibold text-slate-900">{catName}</p>
                  <p className="text-[11px] text-slate-500">
                    {isUnused
                      ? 'Unused · Can be deleted'
                      : `Used in ${usageCount} ${
                          usageCount === 1 ? 'transaction' : 'transactions'
                        }`}
                  </p>
                </div>

                <button
                  type="button"
                  disabled={!isUnused}
                  onClick={() => onDeleteUnusedCategory(catName)}
                  aria-label={`Delete category ${catName}`}
                  className={`flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs font-semibold transition-colors ${
                    isUnused
                      ? 'text-red-700 hover:bg-red-50'
                      : 'cursor-not-allowed text-slate-300'
                  }`}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  <span>{isUnused ? 'Delete' : 'In use'}</span>
                </button>
              </div>
            );
          })}
        </div>

        <div className="mt-4 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white hover:bg-slate-800"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
