'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import {
  Hexagon,
  ArrowRight,
  Shield,
  Zap,
  Database,
  Network,
  Cpu,
  BarChart3,
  Server,
  Layers,
  GitMerge,
  BookOpen,
  Sparkles,
  CheckCircle2,
} from 'lucide-react';
import { AnimatedBackground } from '@/components/ui/animated-background';

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.5 },
};

const staggerContainer = {
  animate: {
    transition: {
      staggerChildren: 0.08,
    },
  },
};

export default function LandingPage() {
  return (
    <div className="relative min-h-screen bg-[var(--surface-0)] text-white overflow-hidden font-sans selection:bg-emerald-500/30 selection:text-emerald-300">
      {/* Interactive canvas background */}
      <AnimatedBackground />

      {/* Header / Navbar */}
      <header className="sticky top-0 z-50 border-b border-[oklch(1_0_0/5%)] bg-[var(--surface-0)]/70 backdrop-blur-2xl px-6 lg:px-12 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 via-teal-500 to-cyan-600 shadow-lg shadow-emerald-500/20 ring-1 ring-white/20">
              <Hexagon className="h-4.5 w-4.5 text-white" strokeWidth={2.5} />
            </div>
            <span className="text-sm font-bold tracking-tight text-white/90">
              AtlasKV
            </span>
          </div>

          <div className="hidden md:flex items-center gap-8">
            <a href="#features" className="text-xs text-[oklch(1_0_0/40%)] hover:text-white transition-colors font-medium">Features</a>
            <a href="#architecture" className="text-xs text-[oklch(1_0_0/40%)] hover:text-white transition-colors font-medium">Architecture</a>
            <a href="#studio" className="text-xs text-[oklch(1_0_0/40%)] hover:text-white transition-colors font-medium">Console</a>
          </div>

          <motion.div whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.97 }}>
            <Link
              href="/dashboard"
              className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 px-4 py-2 text-xs font-semibold text-white shadow-lg shadow-emerald-500/20 transition-all"
            >
              Launch Console
              <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </motion.div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative px-6 py-24 lg:py-36 max-w-7xl mx-auto text-center z-10">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
          className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-[11px] font-medium mb-8"
        >
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
          Enterprise Distributed Consensus Key-Value Engine
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
          className="text-4xl sm:text-6xl lg:text-7xl font-bold tracking-tight max-w-5xl mx-auto leading-[1.08] text-transparent bg-clip-text bg-gradient-to-b from-white via-white/90 to-white/50"
        >
          Distributed Consensus, <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 via-teal-400 to-cyan-400">Engineered for Perfection.</span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="mt-6 text-sm sm:text-base text-[oklch(1_0_0/45%)] max-w-2xl mx-auto leading-relaxed font-normal"
        >
          AtlasKV is a fault-tolerant replicated Key-Value store implementing the Raft algorithm in pure Java. 
          Featuring a high-performance gRPC network layer, custom Write-Ahead Log (WAL) persistence, 
          and Joint Consensus membership transitions.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }} className="w-full sm:w-auto">
            <Link
              href="/dashboard"
              className="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-sm font-semibold text-white shadow-xl shadow-emerald-500/25 transition-all flex items-center justify-center gap-2"
            >
              Enter Studio Console
              <ArrowRight className="h-4 w-4" />
            </Link>
          </motion.div>
          <a
            href="#features"
            className="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-[oklch(1_0_0/4%)] border border-[oklch(1_0_0/8%)] hover:bg-[oklch(1_0_0/8%)] hover:border-[oklch(1_0_0/12%)] text-sm font-semibold transition-all text-center"
          >
            Explore Features
          </a>
        </motion.div>
      </section>

      {/* Feature Grid Section */}
      <section id="features" className="py-24 px-6 max-w-7xl mx-auto z-10 relative">
        <div className="text-center mb-16 space-y-2">
          <h2 className="text-2xl sm:text-4xl font-bold tracking-tight text-white/90">
            Engineered For Absolute Correctness
          </h2>
          <p className="text-xs sm:text-sm text-[oklch(1_0_0/40%)]">
            A deep-dive into the architectural pillars that secure consensus in AtlasKV.
          </p>
        </div>

        <motion.div
          variants={staggerContainer}
          initial="initial"
          whileInView="animate"
          viewport={{ once: true, margin: "-100px" }}
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
        >
          {/* Card 1 */}
          <motion.div
            variants={fadeInUp}
            className="glass-card p-6 rounded-2xl group hover:border-emerald-500/20 transition-all"
          >
            <div className="h-10 w-10 rounded-xl bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <Cpu className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">Custom Raft Engine</h3>
            <p className="mt-2 text-xs text-[oklch(1_0_0/40%)] leading-relaxed">
              Zero external dependencies in the consensus core. Built with a single-threaded event loop to prevent race conditions and synchronize state mutations deterministically.
            </p>
          </motion.div>

          {/* Card 2 */}
          <motion.div
            variants={fadeInUp}
            className="glass-card p-6 rounded-2xl group hover:border-emerald-500/20 transition-all"
          >
            <div className="h-10 w-10 rounded-xl bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <Network className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">gRPC Peer Transport</h3>
            <p className="mt-2 text-xs text-[oklch(1_0_0/40%)] leading-relaxed">
              Inter-node communication operates over high-performance gRPC channels with Protocol Buffers serialization, bypassing typical REST latencies.
            </p>
          </motion.div>

          {/* Card 3 */}
          <motion.div
            variants={fadeInUp}
            className="glass-card p-6 rounded-2xl group hover:border-emerald-500/20 transition-all"
          >
            <div className="h-10 w-10 rounded-xl bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <Database className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">WAL-Based Persistence</h3>
            <p className="mt-2 text-xs text-[oklch(1_0_0/40%)] leading-relaxed">
              Strict durability compliance with custom Write-Ahead Logs (WAL) and metadata serialization, ensuring safety and fast recovery across node crashes.
            </p>
          </motion.div>

          {/* Card 4 */}
          <motion.div
            variants={fadeInUp}
            className="glass-card p-6 rounded-2xl group hover:border-emerald-500/20 transition-all"
          >
            <div className="h-10 w-10 rounded-xl bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <GitMerge className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">Joint Consensus Scaling</h3>
            <p className="mt-2 text-xs text-[oklch(1_0_0/40%)] leading-relaxed">
              Safe cluster reconfiguration through two-phase membership shifts (Cold to Cold,New to Cnew), protecting quorums from concurrent candidate elections.
            </p>
          </motion.div>

          {/* Card 5 */}
          <motion.div
            variants={fadeInUp}
            className="glass-card p-6 rounded-2xl group hover:border-emerald-500/20 transition-all"
          >
            <div className="h-10 w-10 rounded-xl bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <Shield className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">Linearizable Reads</h3>
            <p className="mt-2 text-xs text-[oklch(1_0_0/40%)] leading-relaxed">
              Guarantees fresh data using the ReadIndex optimization, verifying leadership status with heartbeats before replying to bypass write log overhead.
            </p>
          </motion.div>

          {/* Card 6 */}
          <motion.div
            variants={fadeInUp}
            className="glass-card p-6 rounded-2xl group hover:border-emerald-500/20 transition-all"
          >
            <div className="h-10 w-10 rounded-xl bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <BarChart3 className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">Next-Gen Observability</h3>
            <p className="mt-2 text-xs text-[oklch(1_0_0/40%)] leading-relaxed">
              AtlasKV Studio provides visual topology graphs, terminal simulations, log length analysis, and metrics charting directly from the cluster nodes.
            </p>
          </motion.div>
        </motion.div>
      </section>

      {/* Footer */}
      <footer className="border-t border-[oklch(1_0_0/5%)] py-8 px-6 text-center text-[oklch(1_0_0/25%)] text-[11px] relative z-10 bg-[var(--surface-0)] font-mono">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="flex h-5 w-5 items-center justify-center rounded bg-gradient-to-br from-emerald-400 to-teal-600">
              <Hexagon className="h-3 w-3 text-white" strokeWidth={2.5} />
            </div>
            <span className="font-semibold text-white/60">AtlasKV</span>
          </div>
          <p>© {new Date().getFullYear()} AtlasKV Project. Licensed under MIT.</p>
        </div>
      </footer>
    </div>
  );
}
