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
} from 'lucide-react';

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.5 },
};

const staggerContainer = {
  animate: {
    transition: {
      staggerChildren: 0.1,
    },
  },
};

export default function LandingPage() {
  return (
    <div className="relative min-h-screen bg-[#070708] text-white overflow-hidden font-sans selection:bg-emerald-500/30 selection:text-emerald-300">
      {/* Decorative ambient background glows */}
      <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] rounded-full bg-emerald-500/5 blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] rounded-full bg-teal-500/5 blur-[120px] pointer-events-none" />
      <div className="absolute top-[30%] right-[15%] w-[300px] h-[300px] rounded-full bg-blue-500/5 blur-[100px] pointer-events-none" />

      {/* Header / Navbar */}
      <header className="sticky top-0 z-50 border-b border-white/[0.04] bg-[#070708]/75 backdrop-blur-md px-6 lg:px-12 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 shadow-md shadow-emerald-500/10">
              <Hexagon className="h-4 w-4 text-white" strokeWidth={2.5} />
            </div>
            <span className="text-sm font-semibold tracking-tight text-white/90">
              AtlasKV
            </span>
          </div>

          <div className="hidden md:flex items-center gap-8">
            <a href="#features" className="text-xs text-white/40 hover:text-white transition-colors">Features</a>
            <a href="#architecture" className="text-xs text-white/40 hover:text-white transition-colors">Architecture</a>
            <a href="#studio" className="text-xs text-white/40 hover:text-white transition-colors">Console</a>
          </div>

          <Link
            href="/dashboard"
            className="flex items-center gap-1.5 rounded-full bg-white/[0.05] border border-white/[0.08] hover:bg-white/[0.1] px-4 py-1.5 text-xs font-medium text-white/90 transition-all hover:scale-105"
          >
            Launch Console
            <ArrowRight className="h-3 w-3" />
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative px-6 py-20 lg:py-32 max-w-7xl mx-auto text-center z-10">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
          className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-[11px] font-medium mb-6"
        >
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
          Production-Inspired Distributed Consensus Store
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
          className="text-4xl sm:text-6xl font-bold tracking-tight max-w-4xl mx-auto leading-[1.1] text-transparent bg-clip-text bg-gradient-to-b from-white via-white/90 to-white/60"
        >
          Distributed Consensus, <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 via-teal-400 to-emerald-500">Engineered from Scratch.</span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="mt-6 text-sm sm:text-base text-white/50 max-w-2xl mx-auto leading-relaxed"
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
          <Link
            href="/dashboard"
            className="w-full sm:w-auto px-8 py-3 rounded-lg bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-sm font-semibold shadow-lg shadow-emerald-500/25 hover:shadow-emerald-500/35 transition-all duration-200 hover:scale-[1.02] flex items-center justify-center gap-2"
          >
            Enter Studio Console
            <ArrowRight className="h-4 w-4" />
          </Link>
          <a
            href="#features"
            className="w-full sm:w-auto px-8 py-3 rounded-lg bg-white/[0.04] border border-white/[0.06] hover:bg-white/[0.08] hover:border-white/[0.1] text-sm font-semibold transition-all duration-200"
          >
            Explore Features
          </a>
        </motion.div>
      </section>

      {/* Feature Grid Section */}
      <section id="features" className="py-20 px-6 max-w-7xl mx-auto z-10 relative">
        <div className="text-center mb-16">
          <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white/90">
            Engineered For Absolute Correctness
          </h2>
          <p className="mt-2 text-xs sm:text-sm text-white/40">
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
            className="p-6 rounded-xl border border-white/[0.05] bg-white/[0.02] backdrop-blur-sm hover:border-emerald-500/20 hover:bg-white/[0.03] transition-all group"
          >
            <div className="h-10 w-10 rounded-lg bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <Cpu className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">Custom Raft Engine</h3>
            <p className="mt-2 text-xs text-white/40 leading-relaxed">
              Zero external dependencies in the consensus core. Built with a single-threaded event loop to prevent race conditions and synchronize state mutations deterministically.
            </p>
          </motion.div>

          {/* Card 2 */}
          <motion.div
            variants={fadeInUp}
            className="p-6 rounded-xl border border-white/[0.05] bg-white/[0.02] backdrop-blur-sm hover:border-emerald-500/20 hover:bg-white/[0.03] transition-all group"
          >
            <div className="h-10 w-10 rounded-lg bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <Network className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">gRPC Peer Transport</h3>
            <p className="mt-2 text-xs text-white/40 leading-relaxed">
              Inter-node communication operates over high-performance gRPC channels with Protocol Buffers serialization, bypassing typical REST latencies.
            </p>
          </motion.div>

          {/* Card 3 */}
          <motion.div
            variants={fadeInUp}
            className="p-6 rounded-xl border border-white/[0.05] bg-white/[0.02] backdrop-blur-sm hover:border-emerald-500/20 hover:bg-white/[0.03] transition-all group"
          >
            <div className="h-10 w-10 rounded-lg bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <Database className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">WAL-Based Persistence</h3>
            <p className="mt-2 text-xs text-white/40 leading-relaxed">
              Strict durability compliance with custom Write-Ahead Logs (WAL) and metadata serialization, ensuring safety and fast recovery across node crashes.
            </p>
          </motion.div>

          {/* Card 4 */}
          <motion.div
            variants={fadeInUp}
            className="p-6 rounded-xl border border-white/[0.05] bg-white/[0.02] backdrop-blur-sm hover:border-emerald-500/20 hover:bg-white/[0.03] transition-all group"
          >
            <div className="h-10 w-10 rounded-lg bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <GitMerge className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">Joint Consensus Scaling</h3>
            <p className="mt-2 text-xs text-white/40 leading-relaxed">
              Safe cluster reconfiguration through two-phase membership shifts (Cold to Cold,New to Cnew), protecting quorums from concurrent candidate elections.
            </p>
          </motion.div>

          {/* Card 5 */}
          <motion.div
            variants={fadeInUp}
            className="p-6 rounded-xl border border-white/[0.05] bg-white/[0.02] backdrop-blur-sm hover:border-emerald-500/20 hover:bg-white/[0.03] transition-all group"
          >
            <div className="h-10 w-10 rounded-lg bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <Shield className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">Linearizable Reads</h3>
            <p className="mt-2 text-xs text-white/40 leading-relaxed">
              Guarantees fresh data using the ReadIndex optimization, verifying leadership status with heartbeats before replying to bypass write log overhead.
            </p>
          </motion.div>

          {/* Card 6 */}
          <motion.div
            variants={fadeInUp}
            className="p-6 rounded-xl border border-white/[0.05] bg-white/[0.02] backdrop-blur-sm hover:border-emerald-500/20 hover:bg-white/[0.03] transition-all group"
          >
            <div className="h-10 w-10 rounded-lg bg-emerald-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <BarChart3 className="h-5 w-5 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-white/90">Next-Gen Observability</h3>
            <p className="mt-2 text-xs text-white/40 leading-relaxed">
              AtlasKV Studio provides visual topology graphs, terminal simulations, log length analysis, and metrics charting directly from the cluster nodes.
            </p>
          </motion.div>
        </motion.div>
      </section>

      {/* Interactive/Visual Architecture Diagram */}
      <section id="architecture" className="py-20 px-6 max-w-7xl mx-auto z-10 relative border-t border-white/[0.04]">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          <div className="lg:col-span-5 space-y-6">
            <div className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-400 uppercase tracking-widest">
              <Layers className="h-3.5 w-3.5" />
              Decoupled Architecture
            </div>
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white/90">
              Clean Separation of Concerns
            </h2>
            <p className="text-xs sm:text-sm text-white/40 leading-relaxed">
              AtlasKV segregates core system components to achieve maximum code modularity and deterministic testability.
            </p>

            <div className="space-y-4">
              <div className="flex gap-3">
                <div className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-bold">1</div>
                <div>
                  <h4 className="text-xs font-semibold text-white/80">Application Shell (Spring Boot)</h4>
                  <p className="text-[11px] text-white/40 mt-0.5">Provides client REST endpoint routers, JSON serialization, and cluster monitoring APIs.</p>
                </div>
              </div>
              <div className="flex gap-3">
                <div className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-bold">2</div>
                <div>
                  <h4 className="text-xs font-semibold text-white/80">Consensus Engine (Pure Java)</h4>
                  <p className="text-[11px] text-white/40 mt-0.5">Contains zero external dependencies. Houses node event-loop queue logic, states, and heartbeats.</p>
                </div>
              </div>
              <div className="flex gap-3">
                <div className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-bold">3</div>
                <div>
                  <h4 className="text-xs font-semibold text-white/80">Transport & Storage Interface Layers</h4>
                  <p className="text-[11px] text-white/40 mt-0.5">Abstract layers implementation for WAL saving and inter-node gRPC network calls.</p>
                </div>
              </div>
            </div>
          </div>

          <div className="lg:col-span-7 bg-white/[0.01] border border-white/[0.04] p-6 rounded-2xl relative overflow-hidden backdrop-blur-md">
            <div className="absolute top-0 right-0 w-[200px] h-[200px] bg-emerald-500/5 blur-[50px] pointer-events-none" />
            <div className="flex items-center justify-between border-b border-white/[0.04] pb-4 mb-6">
              <div className="flex items-center gap-2">
                <Server className="h-4 w-4 text-emerald-400" />
                <span className="text-[11px] font-semibold text-white/70 uppercase tracking-widest">Multi-Tier Architecture Diagram</span>
              </div>
              <span className="text-[10px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">Active</span>
            </div>

            {/* Architecture Stack Drawing */}
            <div className="space-y-4 font-mono text-[10px]">
              {/* L4 */}
              <div className="p-4 rounded-xl border border-white/[0.08] bg-white/[0.03] relative group">
                <div className="absolute top-2 right-3 text-[9px] text-white/30">Level 4</div>
                <div className="text-white/80 font-bold mb-1">Application Container (Spring Boot)</div>
                <div className="text-white/45">REST API · Observability · Client Interfaces · Metrics</div>
              </div>

              {/* Connector */}
              <div className="h-6 flex items-center justify-center">
                <div className="w-0.5 h-full bg-gradient-to-b from-emerald-500/40 to-teal-500/40" />
              </div>

              {/* L3 */}
              <div className="p-4 rounded-xl border border-emerald-500/25 bg-emerald-500/5 relative group">
                <div className="absolute top-2 right-3 text-[9px] text-emerald-400/50">Level 3</div>
                <div className="text-emerald-400 font-bold mb-1">Raft Consensus Core (Pure Java)</div>
                <div className="text-white/45">RaftNode Event Loop · Election Tick · Log Replication Manager</div>
              </div>

              {/* Connector */}
              <div className="h-6 flex items-center justify-center">
                <div className="w-6 h-0.5 bg-gradient-to-r from-emerald-500/20 to-teal-500/20" />
              </div>

              {/* L2 & L1 split */}
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 rounded-xl border border-white/[0.08] bg-white/[0.03] relative">
                  <div className="text-white/80 font-bold mb-1">Layer 2: Transport</div>
                  <div className="text-white/45">gRPC Network Layer · Protobuf RPCs · Peer-to-Peer Calls</div>
                </div>
                <div className="p-4 rounded-xl border border-white/[0.08] bg-white/[0.03] relative">
                  <div className="text-white/80 font-bold mb-1">Layer 1: Storage</div>
                  <div className="text-white/45">Disk WAL · Metadata File · Log Compaction Snapshots</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Visual Call To Action Section */}
      <section id="studio" className="py-20 px-6 max-w-7xl mx-auto z-10 relative border-t border-white/[0.04]">
        <div className="rounded-3xl bg-gradient-to-br from-emerald-500/[0.05] via-transparent to-teal-500/[0.05] border border-white/[0.04] p-8 lg:p-16 flex flex-col lg:flex-row items-center justify-between gap-12 overflow-hidden relative">
          <div className="absolute inset-0 bg-[#09090b]/40 z-0 pointer-events-none" />
          
          <div className="space-y-6 lg:max-w-xl z-10">
            <span className="text-[10px] uppercase tracking-widest font-semibold text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
              AtlasKV Studio Dashboard
            </span>
            <h2 className="text-2xl sm:text-4xl font-bold tracking-tight text-white/95 leading-tight">
              Ready to Monitor Your Cluster in Real Time?
            </h2>
            <p className="text-xs sm:text-sm text-white/40 leading-relaxed">
              Launch the developer console to visualize node state machines, examine client traffic, run key-value explorer lookups, and perform dynamic node scaling transitions.
            </p>
            <div className="flex flex-wrap gap-4 pt-2">
              <Link
                href="/dashboard"
                className="px-6 py-2.5 rounded-lg bg-white text-[#070708] hover:bg-white/90 text-xs font-semibold transition-all duration-200 hover:scale-[1.02] flex items-center gap-1.5 shadow-md shadow-white/5"
              >
                Launch Studio
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
              <Link
                href="/about"
                className="px-6 py-2.5 rounded-lg bg-white/[0.04] border border-white/[0.08] hover:bg-white/[0.08] hover:border-white/[0.12] text-xs font-semibold transition-all duration-200 flex items-center gap-1.5"
              >
                <BookOpen className="h-3.5 w-3.5" />
                Read Overview
              </Link>
            </div>
          </div>

          {/* Minimalist Dashboard Preview Graphics */}
          <div className="w-full lg:w-[480px] bg-[#09090b]/80 border border-white/[0.06] rounded-2xl p-4 shadow-2xl relative z-10 backdrop-blur-md">
            <div className="flex items-center gap-1.5 pb-3 border-b border-white/[0.04]">
              <div className="h-2 w-2 rounded-full bg-red-500/60" />
              <div className="h-2 w-2 rounded-full bg-yellow-500/60" />
              <div className="h-2 w-2 rounded-full bg-emerald-500/60" />
              <span className="text-[9px] text-white/30 ml-2 font-mono">localhost:3000/dashboard</span>
            </div>
            
            <div className="pt-4 space-y-3">
              <div className="flex items-center justify-between bg-white/[0.02] border border-white/[0.04] p-3 rounded-xl">
                <div className="space-y-1">
                  <div className="text-[9px] text-white/40">CLUSTER STATUS</div>
                  <div className="text-[11px] font-semibold text-emerald-400 flex items-center gap-1">
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    HEALTHY (3 Nodes)
                  </div>
                </div>
                <div className="text-[10px] font-mono text-white/60 bg-white/[0.04] px-2 py-0.5 rounded border border-white/[0.04]">Term 16</div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="bg-white/[0.02] border border-white/[0.04] p-3 rounded-xl space-y-1">
                  <div className="text-[9px] text-white/40">COMMIT INDEX</div>
                  <div className="text-xs font-bold font-mono">11</div>
                </div>
                <div className="bg-white/[0.02] border border-white/[0.04] p-3 rounded-xl space-y-1">
                  <div className="text-[9px] text-white/40">LOG LENGTH</div>
                  <div className="text-xs font-bold font-mono">11</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-white/[0.04] py-8 px-6 text-center text-white/20 text-[10px] relative z-10 bg-[#070708]">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="flex h-5 w-5 items-center justify-center rounded bg-gradient-to-br from-emerald-500 to-teal-600">
              <Hexagon className="h-2.5 w-2.5 text-white" strokeWidth={2.5} />
            </div>
            <span className="font-semibold text-white/60">AtlasKV</span>
          </div>
          <p>© {new Date().getFullYear()} AtlasKV Project. Licensed under MIT.</p>
        </div>
      </footer>
    </div>
  );
}
