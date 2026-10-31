import React, { useEffect, useRef } from 'react';
import { Check } from 'lucide-react';
import {
  CategorySum,
  isCategorySelectedInSet,
} from '../utils/ledgerLogic';

export const M3_CATEGORY_COLORS: Record<string, string> = {
  Allowance: '#2E7D32',
  Gift: '#00897B',
  Job: '#0277BD',
  'Video Game': '#3949AB',
  Toy: '#6A1B9A',
  Book: '#1565C0',
  Snack: '#E65100',
  Other: '#546E7A',
};

const FALLBACK_COLORS = ['#006A6A', '#4A6363', '#4B607C', '#BA1A1A', '#6750A4'];

export function getCategoryColor(category: string): string {
  if (M3_CATEGORY_COLORS[category]) {
    return M3_CATEGORY_COLORS[category];
  }
  let hash = 0;
  for (let i = 0; i < category.length; i++) {
    hash = (hash * 31 + category.charCodeAt(i)) | 0;
  }
  return FALLBACK_COLORS[Math.abs(hash) % FALLBACK_COLORS.length];
}

interface CategoryDonutChartProps {
  categorySpending: CategorySum[];
  totalLabel?: string;
  emptyLabel?: string;
  size?: number;
  strokeWidth?: number;
}

export const CategoryDonutChart: React.FC<CategoryDonutChartProps> = ({
  categorySpending,
  totalLabel = 'Total Spent',
  emptyLabel = 'No Spends',
  size = 148,
  strokeWidth = 22,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const totalAmount = categorySpending.reduce((acc, item) => acc + item.total, 0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    canvas.width = size * dpr;
    canvas.height = size * dpr;
    ctx.scale(dpr, dpr);

    ctx.clearRect(0, 0, size, size);

    const center = size / 2;
    const radius = (size - strokeWidth) / 2;

    if (totalAmount <= 0 || categorySpending.length === 0) {
      ctx.beginPath();
      ctx.arc(center, center, radius, 0, Math.PI * 2);
      ctx.strokeStyle = '#E1E3E5';
      ctx.lineWidth = strokeWidth;
      ctx.lineCap = 'round';
      ctx.stroke();
      return;
    }

    let currentAngle = -Math.PI / 2;
    const gapRadians = categorySpending.length > 1 ? (2.5 * Math.PI) / 180 : 0;

    for (const item of categorySpending) {
      const proportion = item.total / totalAmount;
      const rawSweep = proportion * Math.PI * 2;
      const sweepAngle = Math.max(0.02, rawSweep - gapRadians);
      const startAngle = currentAngle + gapRadians / 2;

      ctx.beginPath();
      ctx.arc(center, center, radius, startAngle, startAngle + sweepAngle);
      ctx.strokeStyle = getCategoryColor(item.category);
      ctx.lineWidth = strokeWidth;
      ctx.lineCap = 'butt';
      ctx.stroke();

      currentAngle += rawSweep;
    }
  }, [categorySpending, size, strokeWidth, totalAmount]);

  const formattedTotal = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(totalAmount);

  return (
    <div
      className="relative flex items-center justify-center"
      style={{ width: size, height: size }}
      data-testid="category-donut-chart"
    >
      <canvas
        ref={canvasRef}
        style={{ width: size, height: size }}
        aria-label={
          totalAmount > 0
            ? `${totalLabel} donut chart, ${formattedTotal}`
            : `Empty ${emptyLabel} donut chart`
        }
      />
      <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center px-3 text-center">
        <span className="text-[10px] font-medium text-slate-500">
          {totalAmount <= 0 ? emptyLabel : totalLabel}
        </span>
        <span className="text-sm font-bold text-slate-900">{formattedTotal}</span>
      </div>
    </div>
  );
};

interface CategoryLegendProps {
  categorySpending: CategorySum[];
}

export const CategoryLegend: React.FC<CategoryLegendProps> = ({ categorySpending }) => {
  const totalAmount = categorySpending.reduce((acc, item) => acc + item.total, 0);

  if (categorySpending.length === 0 || totalAmount <= 0) {
    return (
      <p className="mt-2 text-center text-[11px] text-slate-400">
        No active categories in range
      </p>
    );
  }

  const currencyFormatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  });

  return (
    <div
      className="mt-2.5 flex flex-col gap-1 w-full px-1"
      data-testid="category-legend"
    >
      {categorySpending.map((item) => {
        const pct = Math.round((item.total / totalAmount) * 100);
        const dotColor = getCategoryColor(item.category);
        return (
          <div
            key={item.category}
            className="flex items-center justify-between gap-1 text-[11px] text-slate-700"
          >
            <div className="flex min-w-0 items-center gap-1.5">
              <span
                className="inline-block h-2 w-2 shrink-0 rounded-full"
                style={{ backgroundColor: dotColor }}
                aria-hidden="true"
              />
              <span className="truncate font-semibold text-slate-800">
                {item.category}
              </span>
            </div>
            <span className="shrink-0 text-slate-600">
              {currencyFormatter.format(item.total)} ({pct}%)
            </span>
          </div>
        );
      })}
    </div>
  );
};

interface MatchingCategoriesChipGroupProps {
  title: string;
  accentColorClass?: string;
  matchingCategories: string[];
  selectedCategories: Set<string>;
  onToggleCategory: (category: string) => void;
  emptyMessage?: string;
}

export const MatchingCategoriesChipGroup: React.FC<
  MatchingCategoriesChipGroupProps
> = ({
  title,
  accentColorClass = 'text-slate-800',
  matchingCategories,
  selectedCategories,
  onToggleCategory,
  emptyMessage = 'No matching categories in this date range',
}) => {
  return (
    <div data-testid="matching-categories-section">
      <h4 className={`mb-2.5 text-xs font-bold ${accentColorClass}`}>
        {title}
      </h4>
      {matchingCategories.length === 0 ? (
        <p className="text-xs text-slate-400">{emptyMessage}</p>
      ) : (
        <div
          className="flex flex-wrap gap-2"
          role="group"
          aria-label={`${title} Filter Chips`}
        >
          {matchingCategories.map((category) => {
            const isSelected = isCategorySelectedInSet(
              selectedCategories,
              category
            );
            const chipColor = getCategoryColor(category);

            return (
              <button
                key={`matching-chip-${category}`}
                type="button"
                aria-pressed={isSelected}
                onClick={() => onToggleCategory(category)}
                style={
                  isSelected
                    ? {
                        backgroundColor: chipColor,
                        borderColor: chipColor,
                        color: '#FFFFFF',
                      }
                    : {
                        backgroundColor: 'transparent',
                        borderColor: chipColor,
                        color: chipColor,
                      }
                }
                className="inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-semibold shadow-2xs transition-opacity hover:opacity-90"
              >
                {isSelected && (
                  <Check
                    className="h-3.5 w-3.5 shrink-0 text-white"
                    aria-hidden="true"
                  />
                )}
                <span>{category}</span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};
