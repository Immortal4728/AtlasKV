'use client';

import * as React from 'react';
import { Search } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Dialog, DialogContent } from '@/components/ui/dialog';

export function CommandDialog({
  open,
  onOpenChange,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="p-0 overflow-hidden bg-zinc-950 border-white/10 text-zinc-100 max-w-lg shadow-2xl">
        {children}
      </DialogContent>
    </Dialog>
  );
}

export function CommandInput({
  placeholder,
  value,
  onValueChange,
}: {
  placeholder?: string;
  value?: string;
  onValueChange?: (val: string) => void;
}) {
  return (
    <div className="flex items-center px-3 border-b border-white/10 bg-zinc-900/60">
      <Search className="h-4 w-4 mr-2.5 shrink-0 text-zinc-500" />
      <input
        value={value}
        onChange={(e) => onValueChange && onValueChange(e.target.value)}
        placeholder={placeholder}
        className="flex h-11 w-full rounded-md bg-transparent py-3 text-xs text-zinc-100 placeholder:text-zinc-500 focus:outline-none disabled:cursor-not-allowed disabled:opacity-50"
      />
    </div>
  );
}

export function CommandList({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={cn('max-h-[300px] overflow-y-auto p-2', className)}>
      {children}
    </div>
  );
}

export function CommandEmpty({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return <div className={cn('py-6 text-center text-xs text-zinc-500', className)}>{children}</div>;
}

export function CommandGroup({
  heading,
  children,
}: {
  heading: string;
  children: React.ReactNode;
}) {
  return (
    <div className="px-2 py-1.5">
      <div className="px-2 py-1 text-[10px] font-mono uppercase text-zinc-500 font-semibold tracking-wider">
        {heading}
      </div>
      <div className="space-y-0.5">{children}</div>
    </div>
  );
}

export function CommandItem({
  onSelect,
  className,
  children,
}: {
  onSelect?: () => void;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onSelect}
      className={cn(
        'w-full flex items-center px-2.5 py-2 rounded-lg text-xs font-medium text-zinc-300 hover:text-white hover:bg-white/[0.06] transition-colors cursor-pointer text-left',
        className
      )}
    >
      {children}
    </button>
  );
}

export function CommandSeparator({ className }: { className?: string }) {
  return <div className={cn('h-px bg-white/10 my-1', className)} />;
}
