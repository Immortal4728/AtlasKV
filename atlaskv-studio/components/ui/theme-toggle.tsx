'use client';

import { useTheme } from '@/components/providers/theme-provider';
import { Sun, Moon, Laptop } from 'lucide-react';
import { motion } from 'framer-motion';

export function ThemeToggle({ className = '' }: { className?: string }) {
  const { theme, setTheme } = useTheme();

  const options: Array<{ id: 'dark' | 'light' | 'system'; icon: typeof Sun; label: string }> = [
    { id: 'light', icon: Sun, label: 'Light mode' },
    { id: 'dark', icon: Moon, label: 'Dark mode' },
    { id: 'system', icon: Laptop, label: 'System theme' },
  ];

  return (
    <div
      className={`inline-flex items-center gap-0.5 p-1 rounded-full border border-[oklch(1_0_0/8%)] dark:border-[oklch(1_0_0/8%)] bg-[var(--surface-1)] shadow-inner ${className}`}
    >
      {options.map((opt) => {
        const Icon = opt.icon;
        const isActive = theme === opt.id;

        return (
          <button
            key={opt.id}
            onClick={() => setTheme(opt.id)}
            title={opt.label}
            className="relative flex items-center justify-center h-6 w-6 rounded-full text-xs font-medium transition-colors focus:outline-none"
          >
            {isActive && (
              <motion.div
                layoutId="theme-active-pill"
                className="absolute inset-0 rounded-full bg-gradient-to-br from-emerald-500 to-teal-600 shadow-sm shadow-emerald-500/20"
                transition={{ type: 'spring', stiffness: 400, damping: 30 }}
              />
            )}
            <Icon
              className={`relative z-10 h-3.5 w-3.5 transition-colors ${
                isActive
                  ? 'text-white'
                  : 'text-neutral-500 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-white'
              }`}
            />
          </button>
        );
      })}
    </div>
  );
}
