'use client';

import { cn } from '@/lib/utils';

interface ShimmerSkeletonProps {
  className?: string;
  rounded?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
}

const roundedMap = {
  sm: 'rounded-sm',
  md: 'rounded-md',
  lg: 'rounded-lg',
  xl: 'rounded-xl',
  full: 'rounded-full',
};

export function ShimmerSkeleton({
  className,
  rounded = 'lg',
}: ShimmerSkeletonProps) {
  return (
    <div
      className={cn(
        'skeleton',
        roundedMap[rounded],
        className
      )}
    />
  );
}

export function SkeletonCard() {
  return (
    <div className="glass-card rounded-xl p-5 space-y-3">
      <div className="flex items-start justify-between">
        <div className="space-y-2">
          <ShimmerSkeleton className="h-3 w-20" />
          <ShimmerSkeleton className="h-7 w-16" />
        </div>
        <ShimmerSkeleton className="h-9 w-9 rounded-lg" />
      </div>
      <ShimmerSkeleton className="h-2 w-24" />
    </div>
  );
}

export function SkeletonRow() {
  return (
    <div className="flex items-center gap-4 p-4">
      <ShimmerSkeleton className="h-4 w-4 rounded-full" />
      <ShimmerSkeleton className="h-4 w-32" />
      <ShimmerSkeleton className="h-4 w-48 flex-1" />
      <ShimmerSkeleton className="h-4 w-12" />
      <ShimmerSkeleton className="h-4 w-20" />
    </div>
  );
}

export function SkeletonChart() {
  return (
    <div className="glass-card rounded-xl p-5 space-y-4">
      <div className="flex items-center justify-between">
        <ShimmerSkeleton className="h-4 w-32" />
        <ShimmerSkeleton className="h-4 w-20" />
      </div>
      <ShimmerSkeleton className="h-[200px] w-full rounded-lg" />
    </div>
  );
}
