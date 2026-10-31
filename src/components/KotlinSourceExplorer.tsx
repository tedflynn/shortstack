import React, { useState } from 'react';
import { Check, Copy, Download, FileCode } from 'lucide-react';
import { KOTLIN_SOURCE_FILES, KotlinSourceFile } from '../data/kotlinFiles';

export const KotlinSourceExplorer: React.FC = () => {
  const [selectedPath, setSelectedPath] = useState<string>(
    KOTLIN_SOURCE_FILES[0].path
  );
  const [copiedPath, setCopiedPath] = useState<string | null>(null);

  const activeFile: KotlinSourceFile =
    KOTLIN_SOURCE_FILES.find((f) => f.path === selectedPath) ??
    KOTLIN_SOURCE_FILES[0];

  const handleCopy = async (file: KotlinSourceFile) => {
    try {
      await navigator.clipboard.writeText(file.code);
      setCopiedPath(file.path);
      setTimeout(() => setCopiedPath(null), 2000);
    } catch {
      // Clipboard fallback
    }
  };

  const handleDownloadFile = (file: KotlinSourceFile) => {
    const blob = new Blob([file.code], { type: 'text/x-kotlin;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = file.path.split('/').pop() || 'Source.kt';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="flex flex-col rounded-3xl border border-slate-200 bg-white shadow-sm">
      {/* File Tabs Header */}
      <div className="border-b border-slate-200 px-5 py-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="text-base font-bold text-slate-900">
              Native Kotlin Source Tree
            </h2>
            <p className="text-xs text-slate-500">
              Namespace: com.example.allowancetracker · Jetpack Compose M3 + Room +
              kotlinx.serialization
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => handleCopy(activeFile)}
              className="flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition-colors hover:bg-slate-50"
            >
              {copiedPath === activeFile.path ? (
                <>
                  <Check className="h-3.5 w-3.5 text-emerald-600" />
                  Copied
                </>
              ) : (
                <>
                  <Copy className="h-3.5 w-3.5" />
                  Copy File
                </>
              )}
            </button>
            <button
              type="button"
              onClick={() => handleDownloadFile(activeFile)}
              className="flex items-center gap-1.5 rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-slate-800"
            >
              <Download className="h-3.5 w-3.5" />
              Download .kt
            </button>
          </div>
        </div>

        {/* File selector bar */}
        <div
          role="tablist"
          aria-label="Kotlin source files"
          className="mt-3.5 flex flex-wrap gap-1 rounded-xl bg-slate-100 p-1"
        >
          {KOTLIN_SOURCE_FILES.map((file) => {
            const isSelected = file.path === activeFile.path;
            return (
              <button
                key={file.path}
                type="button"
                role="tab"
                aria-selected={isSelected}
                onClick={() => setSelectedPath(file.path)}
                className={`flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors ${
                  isSelected
                    ? 'bg-white font-semibold text-slate-900 shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <FileCode className="h-3.5 w-3.5 shrink-0 text-teal-700" />
                <span>{file.path}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Active File Summary */}
      <div className="border-b border-slate-100 bg-slate-50 px-5 py-2.5 text-xs text-slate-600">
        <span className="font-semibold text-slate-900">{activeFile.path}</span>
        <span aria-hidden="true" className="mx-2 text-slate-400">
          ·
        </span>
        <span>{activeFile.summary}</span>
      </div>

      {/* Code Viewer */}
      <div className="max-h-[680px] overflow-auto rounded-b-3xl bg-slate-950 p-5 text-xs leading-relaxed text-slate-100">
        <pre className="font-mono whitespace-pre">
          <code>{activeFile.code}</code>
        </pre>
      </div>
    </div>
  );
};
