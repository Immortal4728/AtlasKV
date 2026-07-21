'use client';

import { motion } from 'framer-motion';
import { Hexagon } from 'lucide-react';

export default function AboutPage() {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="flex flex-col items-center justify-center py-20"
    >
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 mb-6 shadow-xl shadow-emerald-500/20">
        <Hexagon className="h-8 w-8 text-white" strokeWidth={2} />
      </div>
      <h2 className="text-2xl font-bold tracking-tight text-white/90 mb-2">
        AtlasKV Studio
      </h2>
      <p className="text-sm text-white/40 max-w-lg text-center mb-8">
        A modern developer platform for AtlasKV — a fault-tolerant distributed
        key-value store built on the Raft consensus algorithm.
      </p>

      <div className="grid grid-cols-2 gap-x-12 gap-y-3 text-sm">
        <div className="text-white/30 text-right">Version</div>
        <div className="text-white/70 font-mono">v1.0.0</div>
        <div className="text-white/30 text-right">Consensus</div>
        <div className="text-white/70">Raft (Joint Consensus)</div>
        <div className="text-white/30 text-right">Transport</div>
        <div className="text-white/70">gRPC</div>
        <div className="text-white/30 text-right">Storage</div>
        <div className="text-white/70">WAL + Snapshots</div>
        <div className="text-white/30 text-right">API</div>
        <div className="text-white/70">REST + Swagger</div>
        <div className="text-white/30 text-right">Runtime</div>
        <div className="text-white/70">Java 21 + Spring Boot 3</div>
        <div className="text-white/30 text-right">Frontend</div>
        <div className="text-white/70">Next.js + shadcn/ui</div>
        <div className="text-white/30 text-right">License</div>
        <div className="text-white/70">MIT</div>
      </div>

      <div className="mt-10 border-t border-white/[0.06] pt-6">
        <p className="text-xs text-white/20 text-center">
          Built by Rishikesh Suvarna · Sprints 1–10 Complete
        </p>
      </div>
    </motion.div>
  );
}
