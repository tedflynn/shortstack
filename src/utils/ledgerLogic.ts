export enum TransactionType {
  CREDIT = 'CREDIT',
  DEBIT = 'DEBIT',
}

export const DEFAULT_KID_NAME = 'Maya';

export const DEFAULT_CATEGORIES = [
  'Allowance',
  'Gift',
  'Job',
  'Video Game',
  'Toy',
  'Book',
] as const;

// Kept for backward compatibility if referenced
export const DEFAULT_INCOME_CATEGORIES = DEFAULT_CATEGORIES;
export const DEFAULT_SPENDING_CATEGORIES = DEFAULT_CATEGORIES;

export interface Transaction {
  id: number;
  kidName: string;
  title: string;
  amount: number;
  type: TransactionType;
  category: string;
  timestamp: number;
}

export interface CategorySum {
  category: string;
  total: number;
}

export interface CategoryBreakdownRow {
  category: string;
  total: number;
  percentage: number;
}

export interface MonthlyMetrics {
  monthlyIncome: number;
  monthlyExpense: number;
}

/**
 * Pure function mirroring Room DAO's getRunningBalanceForKid() SQL query:
 * COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0.0 END), 0.0) -
 * COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0.0 END), 0.0)
 */
export function computeRunningBalance(transactions: Transaction[]): number {
  let credits = 0;
  let debits = 0;

  for (const tx of transactions) {
    if (tx.type === TransactionType.CREDIT) {
      credits += tx.amount;
    } else if (tx.type === TransactionType.DEBIT) {
      debits += tx.amount;
    }
  }

  return Math.round((credits - debits) * 100) / 100;
}

/**
 * Filters the full transaction list down to a single kid's ledger.
 */
export function filterTransactionsByKid(
  transactions: Transaction[],
  kidName: string
): Transaction[] {
  const normalizedTarget = kidName.trim().toLowerCase();
  return transactions.filter(
    (tx) => tx.kidName.trim().toLowerCase() === normalizedTarget
  );
}

/**
 * Builds the horizontal filter bar list starting with "All", followed by
 * unique categories from the unified category pool in insertion order.
 */
export function buildFilterBarCategories(
  categories: string[],
  extraCategories: string[] = []
): string[] {
  const seen = new Set<string>();
  const result: string[] = ['All'];

  for (const cat of [...categories, ...extraCategories]) {
    const trimmed = cat.trim();
    const key = trimmed.toLowerCase();
    if (trimmed && key !== 'all' && !seen.has(key)) {
      seen.add(key);
      result.push(trimmed);
    }
  }

  return result;
}

/**
 * Counts how many transactions across all kids use the given category.
 * If `type` is provided, restricts the count to that TransactionType;
 * otherwise counts across the unified category pool.
 */
export function countCategoryUsage(
  transactions: Transaction[],
  categoryName: string,
  type?: TransactionType
): number {
  const target = categoryName.trim().toLowerCase();
  return transactions.filter(
    (tx) =>
      (type === undefined || tx.type === type) &&
      tx.category.trim().toLowerCase() === target
  ).length;
}

/**
 * Returns true if a category has 0 transactions and can be safely deleted.
 */
export function canDeleteCategory(
  transactions: Transaction[],
  categoryName: string,
  type?: TransactionType
): boolean {
  return countCategoryUsage(transactions, categoryName, type) === 0;
}

/**
 * Validates and appends a user-entered custom category to the category pool.
 * If the category already exists (case-insensitively), returns the existing category
 * without duplicating it.
 */
export function appendCustomCategory(
  categories: readonly string[],
  rawCategoryName: string
):
  | { valid: true; updatedCategories: string[]; selectedCategory: string }
  | { valid: false; error: string } {
  const trimmed = rawCategoryName.trim();
  if (!trimmed) {
    return {
      valid: false,
      error: 'Please enter a category name.',
    };
  }

  const existing = categories.find(
    (cat) => cat.trim().toLowerCase() === trimmed.toLowerCase()
  );
  if (existing) {
    return {
      valid: true,
      updatedCategories: [...categories],
      selectedCategory: existing,
    };
  }

  return {
    valid: true,
    updatedCategories: [...categories, trimmed],
    selectedCategory: trimmed,
  };
}

/**
 * Converts a YYYY-MM-DD date input string into local start-of-day or end-of-day epoch milliseconds.
 */
export function parseDateInputBoundary(
  dateStr: string | null | undefined,
  boundary: 'start' | 'end'
): number | null {
  if (!dateStr || !dateStr.trim()) {
    return null;
  }
  const parts = dateStr.trim().split('-').map(Number);
  if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) {
    return null;
  }
  const [year, month, day] = parts;
  if (boundary === 'start') {
    return new Date(year, month - 1, day, 0, 0, 0, 0).getTime();
  }
  return new Date(year, month - 1, day, 23, 59, 59, 999).getTime();
}

/**
 * Filters transactions that fall within the optional [startDateStr, endDateStr] inclusive range.
 */
export function filterTransactionsByDateRange(
  transactions: Transaction[],
  startDateStr?: string | null,
  endDateStr?: string | null
): Transaction[] {
  const startMs = parseDateInputBoundary(startDateStr, 'start');
  const endMs = parseDateInputBoundary(endDateStr, 'end');

  const lowerBound =
    startMs !== null && endMs !== null ? Math.min(startMs, endMs) : startMs;
  const upperBound =
    startMs !== null && endMs !== null ? Math.max(startMs, endMs) : endMs;

  return transactions.filter((tx) => {
    if (lowerBound !== null && tx.timestamp < lowerBound) {
      return false;
    }
    if (upperBound !== null && tx.timestamp > upperBound) {
      return false;
    }
    return true;
  });
}

/**
 * Pure function aggregating totals by category for a given TransactionType:
 * SELECT category, SUM(amount) AS total
 * FROM transactions
 * WHERE type = :type AND timestamp BETWEEN :startTimestamp AND :endTimestamp
 * GROUP BY category
 * HAVING total > 0
 * ORDER BY total DESC
 */
export function computeTotalsByCategoryForType(
  transactions: Transaction[],
  type: TransactionType,
  startDateStr?: string | null,
  endDateStr?: string | null
): CategorySum[] {
  const inRange = filterTransactionsByDateRange(
    transactions,
    startDateStr,
    endDateStr
  );
  const totalsByCategory = new Map<string, number>();

  for (const tx of inRange) {
    if (tx.type === type && tx.amount > 0) {
      const current = totalsByCategory.get(tx.category) ?? 0;
      totalsByCategory.set(tx.category, current + tx.amount);
    }
  }

  return Array.from(totalsByCategory.entries())
    .map(([category, total]) => ({
      category,
      total: Math.round(total * 100) / 100,
    }))
    .filter((entry) => entry.total > 0)
    .sort((a, b) => b.total - a.total || a.category.localeCompare(b.category));
}

/**
 * Pure function mirroring Room DAO's getCreditTotalsByCategoryForKidInRange() SQL query:
 * Aggregates CREDIT (Income) transactions by category within the optional date range.
 */
export function computeCreditTotalsByCategory(
  transactions: Transaction[],
  startDateStr?: string | null,
  endDateStr?: string | null
): CategorySum[] {
  return computeTotalsByCategoryForType(
    transactions,
    TransactionType.CREDIT,
    startDateStr,
    endDateStr
  );
}

/**
 * Pure function mirroring Room DAO's getDebitTotalsByCategoryForKidInRange() SQL query:
 * Aggregates DEBIT (Expense) transactions by category within the optional date range.
 */
export function computeDebitTotalsByCategory(
  transactions: Transaction[],
  startDateStr?: string | null,
  endDateStr?: string | null
): CategorySum[] {
  return computeTotalsByCategoryForType(
    transactions,
    TransactionType.DEBIT,
    startDateStr,
    endDateStr
  );
}

/**
 * Extracts the list of category names that have matching entries (total > 0)
 * from the given CategorySum list, preserving the descending total order.
 */
export function extractMatchingCategories(categorySums: CategorySum[]): string[] {
  return categorySums
    .filter((item) => item.total > 0)
    .map((item) => item.category);
}

/**
 * Toggles a category name inside a Set<string> (case-insensitive match) and returns
 * a new Set<string> so multiple categories can be toggled on or off independently.
 */
export function toggleCategoryInSet(
  selectedCategories: ReadonlySet<string>,
  category: string
): Set<string> {
  const trimmed = category.trim();
  const next = new Set<string>(selectedCategories);
  if (!trimmed) {
    return next;
  }

  const existingMatch = Array.from(next).find(
    (item) => item.toLowerCase() === trimmed.toLowerCase()
  );

  if (existingMatch !== undefined) {
    next.delete(existingMatch);
  } else {
    next.add(trimmed);
  }

  return next;
}

/**
 * Checks whether a category is currently active inside `selectedCategories`.
 */
export function isCategorySelectedInSet(
  selectedCategories: ReadonlySet<string>,
  category: string
): boolean {
  const target = category.trim().toLowerCase();
  for (const item of selectedCategories) {
    if (item.trim().toLowerCase() === target) {
      return true;
    }
  }
  return false;
}

/**
 * Filters a CategorySum[] series so only categories present in `selectedCategories`
 * are drawn on the graph.
 */
export function filterCategorySumsBySelection(
  categorySums: CategorySum[],
  selectedCategories: ReadonlySet<string>
): CategorySum[] {
  return categorySums.filter((entry) =>
    isCategorySelectedInSet(selectedCategories, entry.category)
  );
}

/**
 * Builds the complete list of category rows for a given category pool,
 * combining active totals with 0-amount categories so all categories can be listed.
 */
export function buildCategoryBreakdownRows(
  definedCategories: string[],
  categorySums: CategorySum[]
): CategoryBreakdownRow[] {
  const totalAmount = categorySums.reduce((acc, item) => acc + item.total, 0);
  const sumMap = new Map<string, { display: string; total: number }>();

  for (const item of categorySums) {
    sumMap.set(item.category.trim().toLowerCase(), {
      display: item.category.trim(),
      total: item.total,
    });
  }

  const seen = new Set<string>();
  const rows: CategoryBreakdownRow[] = [];

  // First include all categories with positive amounts in descending order
  for (const item of categorySums) {
    const key = item.category.trim().toLowerCase();
    if (!seen.has(key)) {
      seen.add(key);
      rows.push({
        category: item.category,
        total: item.total,
        percentage:
          totalAmount > 0 ? Math.round((item.total / totalAmount) * 100) : 0,
      });
    }
  }

  // Then include any remaining defined categories with $0.00
  for (const cat of definedCategories) {
    const key = cat.trim().toLowerCase();
    if (key && !seen.has(key)) {
      seen.add(key);
      const matched = sumMap.get(key);
      const total = matched ? matched.total : 0;
      rows.push({
        category: cat.trim(),
        total,
        percentage:
          totalAmount > 0 && total > 0
            ? Math.round((total / totalAmount) * 100)
            : 0,
      });
    }
  }

  return rows;
}

export function filterTransactionsByTag(
  transactions: Transaction[],
  tag: string | null
): Transaction[] {
  if (!tag || tag.trim() === '' || tag.toLowerCase() === 'all') {
    return [...transactions].sort(
      (a, b) => b.timestamp - a.timestamp || b.id - a.id
    );
  }
  const normalized = tag.toLowerCase();
  return transactions
    .filter((tx) => tx.category.toLowerCase() === normalized)
    .sort((a, b) => b.timestamp - a.timestamp || b.id - a.id);
}

export function computeMonthlyMetrics(
  transactions: Transaction[],
  referenceTimestamp: number = Date.now()
): MonthlyMetrics {
  const refDate = new Date(referenceTimestamp);
  const refYear = refDate.getFullYear();
  const refMonth = refDate.getMonth();

  let monthlyIncome = 0;
  let monthlyExpense = 0;

  for (const tx of transactions) {
    const txDate = new Date(tx.timestamp);
    if (txDate.getFullYear() === refYear && txDate.getMonth() === refMonth) {
      if (tx.type === TransactionType.CREDIT) {
        monthlyIncome += tx.amount;
      } else {
        monthlyExpense += tx.amount;
      }
    }
  }

  return {
    monthlyIncome: Math.round(monthlyIncome * 100) / 100,
    monthlyExpense: Math.round(monthlyExpense * 100) / 100,
  };
}

export function validateTransactionInput(
  title: string,
  amountStr: string
):
  | { valid: true; parsedAmount: number; trimmedTitle: string }
  | { valid: false; error: string } {
  const trimmedTitle = title.trim();
  const parsedAmount = Number(amountStr);

  if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
    return {
      valid: false,
      error: 'Please enter a valid positive dollar amount.',
    };
  }

  if (!trimmedTitle) {
    return {
      valid: false,
      error: 'Please enter a note for this transaction.',
    };
  }

  return {
    valid: true,
    parsedAmount: Math.round(parsedAmount * 100) / 100,
    trimmedTitle,
  };
}

export function serializeTransactionsToJson(
  transactions: Transaction[]
): string {
  return JSON.stringify(transactions, null, 2);
}

export function deserializeTransactionsFromJson(rawJson: string): Transaction[] {
  const parsed = JSON.parse(rawJson);
  if (!Array.isArray(parsed)) {
    throw new Error('Backup JSON must be an array of transactions.');
  }

  return parsed.map((item, idx) => {
    if (
      typeof item !== 'object' ||
      item === null ||
      typeof item.title !== 'string' ||
      !item.title.trim() ||
      typeof item.amount !== 'number' ||
      item.amount <= 0 ||
      (item.type !== TransactionType.CREDIT &&
        item.type !== TransactionType.DEBIT)
    ) {
      throw new Error(`Invalid transaction record at index ${idx}.`);
    }

    const kidName =
      typeof item.kidName === 'string' && item.kidName.trim()
        ? item.kidName.trim()
        : DEFAULT_KID_NAME;

    return {
      id: typeof item.id === 'number' ? item.id : idx + 1,
      kidName,
      title: item.title.trim(),
      amount: Math.round(item.amount * 100) / 100,
      type: item.type as TransactionType,
      category:
        typeof item.category === 'string' && item.category.trim()
          ? item.category.trim()
          : DEFAULT_CATEGORIES[0],
      timestamp:
        typeof item.timestamp === 'number' ? item.timestamp : Date.now(),
    };
  });
}
