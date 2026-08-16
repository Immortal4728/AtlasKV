'use client';

import { useState } from 'react';
import { useAuth } from '@/hooks/use-auth';
import { Shield, Lock, Globe, Layers, X, Edit3, Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import { motion, AnimatePresence } from 'framer-motion';

interface NamespaceBadgeProps {
  showSwitcher?: boolean;
  className?: string;
}

export function NamespaceBadge({ showSwitcher = false, className }: NamespaceBadgeProps) {
  const { authInfo, isAdmin, isUser, activeNamespace, adminNamespace, updateAdminNamespace, userId } = useAuth();
  const [editing, setEditing] = useState(false);
  const [targetNs, setTargetNs] = useState(adminNamespace || '');

  const handleSave = () => {
    updateAdminNamespace(targetNs);
    setEditing(false);
  };

  const handleClear = () => {
    setTargetNs('');
    updateAdminNamespace('');
  };

  if (isUser) {
    return (
      <span
        className={cn(
          'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-bold tracking-tight',
          'bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border border-indigo-500/30',
          className
        )}
        title={`User isolated namespace: ${userId}`}
      >
        <Lock className="h-3 w-3 text-indigo-500" />
        <span>ns: {userId}</span>
        <span className="text-[9px] uppercase px-1 py-0.2 rounded bg-indigo-500/20 text-indigo-600 dark:text-indigo-300">
          Isolated
        </span>
      </span>
    );
  }

  if (isAdmin) {
    return (
      <div className={cn('inline-flex items-center gap-1.5 font-mono text-[11px]', className)}>
        {editing ? (
          <div className="inline-flex items-center gap-1 bg-neutral-900 border border-amber-500/40 rounded-lg px-2 py-0.5 shadow-sm">
            <Layers className="h-3 w-3 text-amber-400" />
            <input
              type="text"
              value={targetNs}
              onChange={(e) => setTargetNs(e.target.value)}
              placeholder="tenant-namespace..."
              className="bg-transparent text-[11px] text-white outline-none w-28 font-mono placeholder:text-neutral-500"
              autoFocus
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleSave();
                if (e.key === 'Escape') setEditing(false);
              }}
            />
            <button
              onClick={handleSave}
              className="p-0.5 text-emerald-400 hover:text-emerald-300 rounded cursor-pointer"
              title="Apply namespace filter"
            >
              <Check className="h-3 w-3" />
            </button>
            <button
              onClick={() => setEditing(false)}
              className="p-0.5 text-neutral-400 hover:text-neutral-200 rounded cursor-pointer"
              title="Cancel"
            >
              <X className="h-3 w-3" />
            </button>
          </div>
        ) : (
          <span
            className={cn(
              'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-bold tracking-tight border',
              adminNamespace
                ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30'
                : 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
            )}
          >
            {adminNamespace ? (
              <>
                <Layers className="h-3 w-3 text-amber-500" />
                <span>ns: {adminNamespace}</span>
                <span className="text-[9px] uppercase px-1 py-0.2 rounded bg-amber-500/20 text-amber-600 dark:text-amber-300">
                  Target
                </span>
                {showSwitcher && (
                  <>
                    <button
                      onClick={() => setEditing(true)}
                      className="ml-1 p-0.5 hover:text-amber-300 rounded cursor-pointer"
                      title="Change target namespace"
                    >
                      <Edit3 className="h-2.5 w-2.5" />
                    </button>
                    <button
                      onClick={handleClear}
                      className="p-0.5 hover:text-rose-400 rounded cursor-pointer"
                      title="Reset to global keyspace"
                    >
                      <X className="h-2.5 w-2.5" />
                    </button>
                  </>
                )}
              </>
            ) : (
              <>
                <Globe className="h-3 w-3 text-emerald-500" />
                <span>ns: global</span>
                <span className="text-[9px] uppercase px-1 py-0.2 rounded bg-emerald-500/20 text-emerald-600 dark:text-emerald-300">
                  Root
                </span>
                {showSwitcher && (
                  <button
                    onClick={() => setEditing(true)}
                    className="ml-1 p-0.5 hover:text-emerald-300 rounded cursor-pointer"
                    title="Target a specific tenant namespace"
                  >
                    <Edit3 className="h-2.5 w-2.5" />
                  </button>
                )}
              </>
            )}
          </span>
        )}
      </div>
    );
  }

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-mono text-neutral-500 bg-neutral-100 dark:bg-neutral-800 border border-border',
        className
      )}
    >
      <Globe className="h-3 w-3 text-neutral-400" />
      <span>ns: local-dev</span>
    </span>
  );
}
