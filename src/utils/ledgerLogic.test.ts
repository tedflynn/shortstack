import test from 'node:test';
import assert from 'node:assert/strict';
import {
  DEFAULT_CATEGORIES,
  TransactionType,
  Transaction,
  buildFilterBarCategories,
  buildCategoryBreakdownRows,
  canDeleteCategory,
  countCategoryUsage,
  computeRunningBalance,
  computeCreditTotalsByCategory,
  computeDebitTotalsByCategory,
  extractMatchingCategories,
  filterCategorySumsBySelection,
  filterTransactionsByDateRange,
  filterTransactionsByKid,
  filterTransactionsByTag,
  isCategorySelectedInSet,
  toggleCategoryInSet,
  computeMonthlyMetrics,
  validateTransactionInput,
  serializeTransactionsToJson,
  deserializeTransactionsFromJson,
} from './ledgerLogic';

const sampleTransactions: Transaction[] = [
  {
    id: 1,
    kidName: 'Maya',
    title: 'Weekly Allowance',
    amount: 20.0,
    type: TransactionType.CREDIT,
    category: 'Allowance',
    timestamp: new Date(2026, 9, 5, 10, 0, 0).getTime(),
  },
  {
    id: 2,
    kidName: 'Maya',
    title: 'Birthday Gift from Grandma',
    amount: 35.0,
    type: TransactionType.CREDIT,
    category: 'Gift',
    timestamp: new Date(2026, 9, 12, 12, 0, 0).getTime(),
  },
  {
    id: 3,
    kidName: 'Maya',
    title: 'Lego Space Shuttle',
    amount: 18.5,
    type: TransactionType.DEBIT,
    category: 'Toy',
    timestamp: new Date(2026, 9, 15, 15, 30, 0).getTime(),
  },
  {
    id: 4,
    kidName: 'Maya',
    title: 'Science Graphic Novel',
    amount: 11.5,
    type: TransactionType.DEBIT,
    category: 'Book',
    timestamp: new Date(2026, 9, 18, 16, 0, 0).getTime(),
  },
  {
    id: 5,
    kidName: 'Maya',
    title: 'Racing Video Game',
    amount: 5.0,
    type: TransactionType.DEBIT,
    category: 'Video Game',
    timestamp: new Date(2026, 9, 20, 14, 0, 0).getTime(),
  },
];

test('computeRunningBalance calculates sum of CREDITs minus sum of DEBITs including negative balances', () => {
  assert.equal(computeRunningBalance(sampleTransactions), 20.0);
  assert.equal(computeRunningBalance([]), 0.0);

  const negativeLedger: Transaction[] = [
    {
      id: 10,
      kidName: 'Maya',
      title: 'Small Allowance',
      amount: 5.0,
      type: TransactionType.CREDIT,
      category: 'Allowance',
      timestamp: Date.now(),
    },
    {
      id: 11,
      kidName: 'Maya',
      title: 'Action Figure',
      amount: 12.75,
      type: TransactionType.DEBIT,
      category: 'Toy',
      timestamp: Date.now(),
    },
  ];
  assert.equal(computeRunningBalance(negativeLedger), -7.75);
});

test('unified DEFAULT_CATEGORIES pool matches presets and builds filter bar cleanly', () => {
  assert.deepEqual([...DEFAULT_CATEGORIES], [
    'Allowance',
    'Gift',
    'Job',
    'Video Game',
    'Toy',
    'Book',
  ]);

  const filterBar = buildFilterBarCategories([...DEFAULT_CATEGORIES]);
  assert.deepEqual(filterBar, [
    'All',
    'Allowance',
    'Gift',
    'Job',
    'Video Game',
    'Toy',
    'Book',
  ]);
});

test('canDeleteCategory allows deleting unused categories and protects used categories in unified pool', () => {
  // 'Job' has 0 transactions across all entries -> can delete
  assert.equal(canDeleteCategory(sampleTransactions, 'Job'), true);
  assert.equal(countCategoryUsage(sampleTransactions, 'Job'), 0);

  // 'Allowance' is used in 1 transaction -> cannot delete
  assert.equal(canDeleteCategory(sampleTransactions, 'Allowance'), false);
  assert.equal(countCategoryUsage(sampleTransactions, 'Allowance'), 1);

  // 'Gift' is used in 1 transaction -> cannot delete from unified pool
  assert.equal(canDeleteCategory(sampleTransactions, 'Gift'), false);
  assert.equal(countCategoryUsage(sampleTransactions, 'Gift'), 1);
});

test('computeCreditTotalsByCategory, computeDebitTotalsByCategory, and extractMatchingCategories return only matching categories per tab', () => {
  const incomeBreakdown = computeCreditTotalsByCategory(sampleTransactions);
  assert.equal(incomeBreakdown.length, 2);
  assert.deepEqual(incomeBreakdown[0], { category: 'Gift', total: 35.0 });
  assert.deepEqual(incomeBreakdown[1], { category: 'Allowance', total: 20.0 });
  assert.deepEqual(extractMatchingCategories(incomeBreakdown), [
    'Gift',
    'Allowance',
  ]);

  const expenseBreakdown = computeDebitTotalsByCategory(sampleTransactions);
  assert.equal(expenseBreakdown.length, 3);
  assert.deepEqual(expenseBreakdown[0], { category: 'Toy', total: 18.5 });
  assert.deepEqual(expenseBreakdown[1], { category: 'Book', total: 11.5 });
  assert.deepEqual(expenseBreakdown[2], { category: 'Video Game', total: 5.0 });
  assert.deepEqual(extractMatchingCategories(expenseBreakdown), [
    'Toy',
    'Book',
    'Video Game',
  ]);

  const incomeRows = buildCategoryBreakdownRows(
    ['Allowance', 'Gift', 'Job'],
    incomeBreakdown
  );
  assert.deepEqual(incomeRows, [
    { category: 'Gift', total: 35.0, percentage: 64 },
    { category: 'Allowance', total: 20.0, percentage: 36 },
    { category: 'Job', total: 0, percentage: 0 },
  ]);
});

test('computeDebitTotalsByCategory filters by start and end date range accurately', () => {
  const midRangeBreakdown = computeDebitTotalsByCategory(
    sampleTransactions,
    '2026-10-16',
    '2026-10-19'
  );
  assert.equal(midRangeBreakdown.length, 1);
  assert.deepEqual(midRangeBreakdown[0], { category: 'Book', total: 11.5 });

  const fromOct18 = computeDebitTotalsByCategory(
    sampleTransactions,
    '2026-10-18',
    null
  );
  assert.equal(fromOct18.length, 2);
  assert.deepEqual(fromOct18[0], { category: 'Book', total: 11.5 });
  assert.deepEqual(fromOct18[1], { category: 'Video Game', total: 5.0 });

  const inRangeList = filterTransactionsByDateRange(
    sampleTransactions,
    '2026-10-15',
    '2026-10-18'
  );
  assert.equal(inRangeList.length, 2);
});

test('filterTransactionsByKid isolates each kids ledger and balance accurately', () => {
  const multiKidLedger: Transaction[] = [
    ...sampleTransactions,
    {
      id: 6,
      kidName: 'Leo',
      title: 'Lawn Mowing Job',
      amount: 12.0,
      type: TransactionType.CREDIT,
      category: 'Job',
      timestamp: new Date(2026, 9, 21, 10, 0, 0).getTime(),
    },
    {
      id: 7,
      kidName: 'Leo',
      title: 'Hot Wheels Car',
      amount: 3.5,
      type: TransactionType.DEBIT,
      category: 'Toy',
      timestamp: new Date(2026, 9, 22, 11, 0, 0).getTime(),
    },
  ];

  const mayaTx = filterTransactionsByKid(multiKidLedger, 'Maya');
  const leoTx = filterTransactionsByKid(multiKidLedger, 'Leo');
  const newKidTx = filterTransactionsByKid(multiKidLedger, 'Zoe');

  assert.equal(mayaTx.length, 5);
  assert.equal(computeRunningBalance(mayaTx), 20.0);

  assert.equal(leoTx.length, 2);
  assert.equal(computeRunningBalance(leoTx), 8.5);

  assert.equal(newKidTx.length, 0);
  assert.equal(computeRunningBalance(newKidTx), 0.0);
});

test('filterTransactionsByTag filters by category tag and returns all when null or All', () => {
  assert.equal(filterTransactionsByTag(sampleTransactions, null).length, 5);
  assert.equal(filterTransactionsByTag(sampleTransactions, 'All').length, 5);
  const toys = filterTransactionsByTag(sampleTransactions, 'Toy');
  assert.equal(toys.length, 1);
  assert.equal(toys[0].title, 'Lego Space Shuttle');
});

test('computeMonthlyMetrics computes current month credit and debit sums accurately', () => {
  const octRef = new Date(2026, 9, 25, 10, 0, 0).getTime();
  const metrics = computeMonthlyMetrics(sampleTransactions, octRef);
  assert.equal(metrics.monthlyIncome, 55.0);
  assert.equal(metrics.monthlyExpense, 35.0);
});

test('validateTransactionInput rejects non-positive amounts and blank notes', () => {
  const badAmount = validateTransactionInput('Ice Cream', '0');
  assert.equal(badAmount.valid, false);

  const badNote = validateTransactionInput('   ', '4.50');
  assert.equal(badNote.valid, false);

  const valid = validateTransactionInput(' Comic Book ', '6.25');
  assert.equal(valid.valid, true);
  if (valid.valid) {
    assert.equal(valid.parsedAmount, 6.25);
    assert.equal(valid.trimmedTitle, 'Comic Book');
  }
});

test('JSON serialization and deserialization round-trips transactions faithfully', () => {
  const json = serializeTransactionsToJson(sampleTransactions);
  const restored = deserializeTransactionsFromJson(json);
  assert.equal(restored.length, sampleTransactions.length);
  assert.equal(restored[0].title, 'Weekly Allowance');
  assert.equal(restored[0].kidName, 'Maya');
  assert.equal(restored[2].type, TransactionType.DEBIT);
});

test('toggleCategoryInSet and filterCategorySumsBySelection toggle independent categories and filter graph series', () => {
  const initialSet = new Set<string>([...DEFAULT_CATEGORIES]);
  assert.equal(isCategorySelectedInSet(initialSet, 'Toy'), true);

  // Toggle 'Toy' off
  const withoutToy = toggleCategoryInSet(initialSet, 'Toy');
  assert.equal(isCategorySelectedInSet(withoutToy, 'Toy'), false);
  assert.equal(isCategorySelectedInSet(withoutToy, 'Book'), true);

  const rawDebits = computeDebitTotalsByCategory(sampleTransactions);
  const filteredDebits = filterCategorySumsBySelection(rawDebits, withoutToy);
  assert.deepEqual(filteredDebits, [
    { category: 'Book', total: 11.5 },
    { category: 'Video Game', total: 5.0 },
  ]);

  // Toggle 'Toy' back on
  const restoredSet = toggleCategoryInSet(withoutToy, 'Toy');
  assert.equal(isCategorySelectedInSet(restoredSet, 'Toy'), true);
  assert.equal(filterCategorySumsBySelection(rawDebits, restoredSet).length, 3);
});

test('appendCustomCategory validates input, appends new categories, and avoids case-insensitive duplicates', async () => {
  const { appendCustomCategory } = await import('./ledgerLogic');
  const base = [...DEFAULT_CATEGORIES];

  const emptyResult = appendCustomCategory(base, '   ');
  assert.equal(emptyResult.valid, false);

  const addedResult = appendCustomCategory(base, '  Craft Supplies  ');
  assert.equal(addedResult.valid, true);
  if (addedResult.valid) {
    assert.equal(addedResult.selectedCategory, 'Craft Supplies');
    assert.equal(addedResult.updatedCategories.at(-1), 'Craft Supplies');
    assert.equal(addedResult.updatedCategories.length, base.length + 1);
  }

  const duplicateResult = appendCustomCategory(base, 'allowance');
  assert.equal(duplicateResult.valid, true);
  if (duplicateResult.valid) {
    assert.equal(duplicateResult.selectedCategory, 'Allowance');
    assert.equal(duplicateResult.updatedCategories.length, base.length);
  }
});

