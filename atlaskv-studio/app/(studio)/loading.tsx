import { SkeletonCard, SkeletonRow, SkeletonChart } from '@/components/ui/shimmer-skeleton';

export default function Loading() {
  return (
    <div className="space-y-6 p-2">
      {/* Header Skeleton */}
      <div className="space-y-2">
        <div className="skeleton h-6 w-48 rounded-lg" />
        <div className="skeleton h-4 w-80 rounded-md" />
      </div>

      {/* Primary Banner Skeleton */}
      <div className="skeleton h-28 w-full rounded-xl" />

      {/* Grid Skeleton Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <SkeletonCard key={i} />
        ))}
      </div>

      {/* Chart & Table Skeleton */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <SkeletonChart />
        <SkeletonChart />
      </div>
    </div>
  );
}
