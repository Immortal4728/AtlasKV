export default function Loading() {
  return (
    <div className="space-y-6 animate-pulse p-2">
      {/* Header Skeleton */}
      <div className="space-y-2">
        <div className="h-6 w-48 bg-zinc-800/80 rounded-md" />
        <div className="h-4 w-80 bg-zinc-800/50 rounded-md" />
      </div>

      {/* Primary Banner Skeleton */}
      <div className="h-24 w-full bg-zinc-900/60 border border-white/5 rounded-xl" />

      {/* Grid Skeleton Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-28 p-5 bg-zinc-900/40 border border-white/5 rounded-xl space-y-3">
            <div className="h-4 w-24 bg-zinc-800/60 rounded" />
            <div className="h-7 w-32 bg-zinc-800/80 rounded" />
          </div>
        ))}
      </div>

      {/* Table Skeleton */}
      <div className="h-64 w-full bg-zinc-900/40 border border-white/5 rounded-xl p-4 space-y-3">
        <div className="h-8 w-full bg-zinc-800/50 rounded" />
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-10 w-full bg-zinc-800/30 rounded" />
        ))}
      </div>
    </div>
  );
}
