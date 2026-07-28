'use client';

import Link from 'next/link';
import { motion, useScroll, useTransform, useMotionValueEvent } from 'framer-motion';
import { useState, useRef } from 'react';
import { ArrowRight, ArrowUpRight } from 'lucide-react';
import { RaftClusterViz } from '@/components/ui/raft-cluster-viz';
import { ArchitectureFlow } from '@/components/ui/architecture-flow';

const GitHubIcon = ({ className = '' }: { className?: string }) => (
  <svg className={className} viewBox="0 0 16 16" fill="currentColor">
    <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z" />
  </svg>
);

/* ─────────────────────────────────────────────────────────────────────────────
   ANIMATION VARIANTS
   ───────────────────────────────────────────────────────────────────────────── */
const fade = {
  hidden: { opacity: 0, y: 24, filter: 'blur(8px)' },
  visible: (i: number = 0) => ({
    opacity: 1, y: 0, filter: 'blur(0px)',
    transition: { duration: 0.7, delay: i * 0.08, ease: [0.25, 0.46, 0.45, 0.94] as const },
  }),
};

const scaleIn = {
  hidden: { opacity: 0, scale: 0.96 },
  visible: (i: number = 0) => ({
    opacity: 1, scale: 1,
    transition: { duration: 0.6, delay: i * 0.1, ease: [0.25, 0.46, 0.45, 0.94] as const },
  }),
};

/* ─────────────────────────────────────────────────────────────────────────────
   METRICS DATA
   ───────────────────────────────────────────────────────────────────────────── */
const METRICS = [
  { value: '99.99%', label: 'Fault Tolerance', sub: 'Quorum-based replication' },
  { value: '<1ms', label: 'Read Latency', sub: 'ReadIndex optimization' },
  { value: 'Raft', label: 'Consensus', sub: 'Linearizable guarantees' },
  { value: 'gRPC', label: 'Transport', sub: 'Protobuf serialization' },
  { value: 'WAL', label: 'Durability', sub: 'Write-Ahead Logging' },
  { value: 'Snap', label: 'Recovery', sub: 'Periodic snapshots' },
];

/* ─────────────────────────────────────────────────────────────────────────────
   FEATURES DATA
   ───────────────────────────────────────────────────────────────────────────── */
const FEATURES = [
  {
    title: 'Custom Raft Consensus Engine',
    description: 'Zero external dependencies in the consensus core. Single-threaded event loop eliminates race conditions and synchronizes state mutations deterministically. Every state transition is provably correct.',
    detail: 'Built on the Raft paper by Ongaro & Ousterhout with full implementation of leader election, log replication, safety guarantees, and cluster membership changes.',
    code: `// Leader election with randomized timeout
if (elapsedMs > electionTimeout) {
    currentTerm++;
    role = CANDIDATE;
    votedFor = selfId;
    requestVotes(peers);
}`,
    metric: { value: '0', label: 'External Dependencies' },
  },
  {
    title: 'gRPC Network Transport',
    description: 'Inter-node communication operates over high-performance gRPC channels with Protocol Buffers serialization. Bidirectional streaming enables efficient heartbeat and replication pipelines.',
    detail: 'Custom Protobuf schemas for AppendEntries, RequestVote, InstallSnapshot, and AddServer RPCs with built-in retry logic and deadline propagation.',
    code: `service RaftService {
  rpc AppendEntries (AppendReq)
    returns (AppendResp);
  rpc RequestVote (VoteReq)
    returns (VoteResp);
  rpc InstallSnapshot (stream Chunk)
    returns (InstallResp);
}`,
    metric: { value: '< 0.5ms', label: 'RPC Latency (p99)' },
  },
  {
    title: 'Write-Ahead Log Persistence',
    description: 'Strict durability compliance with custom WAL implementation. Every committed entry is persisted to disk before acknowledgement, ensuring safety across node crashes and power failures.',
    detail: 'Segment-based file format with CRC32 checksums, automatic compaction, and efficient sequential I/O patterns optimized for modern SSDs.',
    code: `// WAL append with fsync guarantee
walSegment.append(entry);
walSegment.flush();  // fsync to disk
walSegment.sync();   // durability barrier
replicationAck(entry.index);`,
    metric: { value: '100%', label: 'Durability Guarantee' },
  },
  {
    title: 'Joint Consensus Membership',
    description: 'Safe cluster reconfiguration through two-phase membership transitions. Cold → C(old,new) → Cnew protocol protects quorum invariants during topology changes.',
    detail: 'Servers can be added or removed without downtime. The joint consensus protocol ensures that at no point can two leaders be elected for the same term.',
    code: `// Two-phase membership change
Phase 1: replicate C(old,new) config
  → requires majority of BOTH old and new
Phase 2: replicate C(new) config
  → requires majority of new only`,
    metric: { value: '0', label: 'Downtime for Reconfig' },
  },
];

/* ─────────────────────────────────────────────────────────────────────────────
   NAV LINKS
   ───────────────────────────────────────────────────────────────────────────── */
const NAV_LINKS = [
  { label: 'Features', href: '#features' },
  { label: 'Architecture', href: '#architecture' },
  { label: 'Documentation', href: '#docs' },
  { label: 'Console', href: '/dashboard' },
];

/* ═══════════════════════════════════════════════════════════════════════════════
   HOMEPAGE
   ═══════════════════════════════════════════════════════════════════════════════ */
export default function LandingPage() {
  const [scrolled, setScrolled] = useState(false);
  const heroRef = useRef<HTMLElement>(null);
  const { scrollY } = useScroll();

  useMotionValueEvent(scrollY, 'change', (v) => {
    setScrolled(v > 40);
  });

  const heroOpacity = useTransform(scrollY, [0, 600], [1, 0]);
  const heroScale = useTransform(scrollY, [0, 600], [1, 0.97]);

  return (
    <div className="relative min-h-screen text-white overflow-x-hidden selection:bg-[#14E0C5]/20 selection:text-[#14E0C5]"
      style={{ background: '#050505' }}>

      {/* ═══ FLOATING NAVBAR ═══ */}
      <motion.header
        className="fixed top-0 left-0 right-0 z-[100] flex justify-center"
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.5, delay: 0.2 }}
      >
        <nav
          className="mt-4 mx-4 flex items-center gap-1 px-2 py-1.5 rounded-full transition-all duration-500"
          style={{
            background: scrolled
              ? 'rgba(5, 5, 5, 0.72)'
              : 'rgba(5, 5, 5, 0.3)',
            backdropFilter: scrolled ? 'blur(40px) saturate(1.8)' : 'blur(12px)',
            WebkitBackdropFilter: scrolled ? 'blur(40px) saturate(1.8)' : 'blur(12px)',
            border: `1px solid rgba(255, 255, 255, ${scrolled ? 0.08 : 0.04})`,
            transform: scrolled ? 'scale(0.97)' : 'scale(1)',
            boxShadow: scrolled
              ? '0 8px 40px rgba(0,0,0,0.5), 0 0 0 0.5px rgba(255,255,255,0.06) inset'
              : 'none',
          }}
        >
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2.5 pl-3 pr-4">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M10 1L18.66 6V14L10 19L1.34 14V6L10 1Z" stroke="#14E0C5" strokeWidth="1.5" fill="rgba(20,224,197,0.08)" />
              <circle cx="10" cy="10" r="2.5" fill="#14E0C5" opacity="0.7" />
            </svg>
            <span className="text-[13px] font-semibold tracking-[-0.01em] text-white/90">AtlasKV</span>
          </Link>

          {/* Separator */}
          <div className="w-px h-4 bg-white/[0.07]" />

          {/* Links */}
          <div className="flex items-center">
            {NAV_LINKS.map((link) => (
              <a
                key={link.label}
                href={link.href}
                className="px-3.5 py-1.5 text-[12.5px] font-medium text-white/40 hover:text-white/90 transition-colors duration-200 rounded-full hover:bg-white/[0.04]"
              >
                {link.label}
              </a>
            ))}
            <a
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="px-3 py-1.5 text-[12.5px] font-medium text-white/40 hover:text-white/90 transition-colors duration-200 rounded-full hover:bg-white/[0.04] flex items-center gap-1.5"
            >
              <GitHubIcon className="w-3.5 h-3.5" />
              GitHub
            </a>
          </div>

          {/* Separator */}
          <div className="w-px h-4 bg-white/[0.07]" />

          {/* CTA */}
          <Link
            href="/dashboard"
            className="ml-1 mr-0.5 flex items-center gap-1.5 px-4 py-1.5 rounded-full text-[12.5px] font-semibold transition-all duration-300 hover:brightness-110"
            style={{
              background: 'linear-gradient(135deg, #14E0C5 0%, #0EA5E9 100%)',
              color: '#050505',
            }}
          >
            Launch Studio
            <ArrowRight className="w-3 h-3" />
          </Link>
        </nav>
      </motion.header>


      {/* ═══ HERO SECTION ═══ */}
      <motion.section
        ref={heroRef}
        style={{ opacity: heroOpacity, scale: heroScale }}
        className="relative pt-36 pb-24 lg:pt-44 lg:pb-32 px-6"
      >
        {/* Subtle radial glow behind hero */}
        <div
          className="absolute top-0 left-1/2 -translate-x-1/2 w-[900px] h-[600px] pointer-events-none"
          style={{
            background: 'radial-gradient(ellipse at center, rgba(20,224,197,0.04) 0%, transparent 70%)',
          }}
        />

        <div className="max-w-[1200px] mx-auto relative z-10">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 lg:gap-20 items-center">

            {/* Left: Copy */}
            <div>
              {/* Badge */}
              <motion.div
                variants={fade}
                initial="hidden"
                animate="visible"
                custom={0}
                className="inline-flex items-center gap-2 px-3 py-1 rounded-full mb-8"
                style={{
                  background: 'rgba(20, 224, 197, 0.06)',
                  border: '1px solid rgba(20, 224, 197, 0.12)',
                }}
              >
                <span className="w-1.5 h-1.5 rounded-full bg-[#14E0C5] animate-pulse" />
                <span className="text-[11px] font-medium text-[#14E0C5]/80 tracking-wide uppercase">
                  Distributed Consensus Database
                </span>
              </motion.div>

              {/* Headline */}
              <motion.h1
                variants={fade}
                initial="hidden"
                animate="visible"
                custom={1}
                className="text-[clamp(2.5rem,5.5vw,4.5rem)] font-normal leading-[1.06] tracking-[-0.02em]"
                style={{
                  fontFamily: '"Instrument Serif", Georgia, serif',
                  color: 'white',
                }}
              >
                Distributed Systems,{' '}
                <br />
                <span style={{ color: 'rgba(255,255,255,0.4)', fontStyle: 'italic' }}>
                  Built with Precision.
                </span>
              </motion.h1>

              {/* Tech Stack Pills */}
              <motion.div
                variants={fade}
                initial="hidden"
                animate="visible"
                custom={2}
                className="mt-8 flex flex-wrap gap-2"
              >
                {['Raft Consensus', 'gRPC', 'Write-Ahead Log', 'Snapshots', 'Joint Consensus', 'Java 21'].map((tech) => (
                  <span
                    key={tech}
                    className="px-3 py-1 rounded-full text-[11px] font-medium text-white/35 tracking-wide"
                    style={{
                      background: 'rgba(255,255,255,0.03)',
                      border: '1px solid rgba(255,255,255,0.06)',
                    }}
                  >
                    {tech}
                  </span>
                ))}
              </motion.div>

              {/* Description */}
              <motion.p
                variants={fade}
                initial="hidden"
                animate="visible"
                custom={3}
                className="mt-7 text-[15px] leading-[1.7] text-white/35 max-w-md font-normal"
              >
                A fault-tolerant replicated key-value store implementing the Raft consensus
                algorithm in pure Java. High-performance gRPC transport, custom WAL persistence,
                and joint consensus membership transitions.
              </motion.p>

              {/* CTAs */}
              <motion.div
                variants={fade}
                initial="hidden"
                animate="visible"
                custom={4}
                className="mt-10 flex items-center gap-4"
              >
                <Link
                  href="/dashboard"
                  className="group flex items-center gap-2.5 px-6 py-3 rounded-full text-[13px] font-semibold transition-all duration-300 hover:brightness-110"
                  style={{
                    background: 'linear-gradient(135deg, #14E0C5 0%, #0EA5E9 100%)',
                    color: '#050505',
                  }}
                >
                  Launch Studio
                  <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
                </Link>

                <a
                  href="#architecture"
                  className="flex items-center gap-2 px-5 py-3 rounded-full text-[13px] font-medium text-white/50 transition-all duration-300 hover:text-white/80 hover:bg-white/[0.04]"
                  style={{
                    border: '1px solid rgba(255,255,255,0.08)',
                  }}
                >
                  View Architecture
                  <ArrowUpRight className="w-3.5 h-3.5" />
                </a>
              </motion.div>
            </div>

            {/* Right: Live Raft Cluster Visualization */}
            <motion.div
              variants={scaleIn}
              initial="hidden"
              animate="visible"
              custom={2}
              className="relative"
            >
              <div
                className="relative rounded-2xl overflow-hidden"
                style={{
                  background: 'rgba(255,255,255,0.02)',
                  border: '1px solid rgba(255,255,255,0.06)',
                  aspectRatio: '4 / 3',
                }}
              >
                <RaftClusterViz />
                {/* Corner label */}
                <div className="absolute bottom-3 right-4 flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-[#14E0C5]/60 animate-pulse" />
                  <span className="text-[9px] font-medium text-white/20 tracking-widest uppercase">Live Cluster</span>
                </div>
              </div>
            </motion.div>

          </div>
        </div>
      </motion.section>


      {/* ═══ METRICS BAR ═══ */}
      <section className="relative px-6 pb-28">
        {/* Top border line */}
        <div className="max-w-[1200px] mx-auto">
          <div className="h-px w-full" style={{ background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.06), transparent)' }} />
          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: '-80px' }}
            className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-px pt-12"
            style={{
              background: 'transparent',
            }}
          >
            {METRICS.map((m, i) => (
              <motion.div
                key={m.label}
                variants={fade}
                custom={i}
                className="text-center py-6 px-4"
              >
                <div
                  className="text-[30px] font-normal tracking-[-0.02em] leading-none"
                  style={{ color: '#14E0C5', fontFamily: '"Instrument Serif", Georgia, serif', fontStyle: 'italic' }}
                >
                  {m.value}
                </div>
                <div className="mt-2 text-[12px] font-medium text-white/60 tracking-[-0.01em]">
                  {m.label}
                </div>
                <div className="mt-1 text-[10px] text-white/20 font-normal">
                  {m.sub}
                </div>
              </motion.div>
            ))}
          </motion.div>
          <div className="h-px w-full" style={{ background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.06), transparent)' }} />
        </div>
      </section>


      {/* ═══ ARCHITECTURE SECTION ═══ */}
      <section id="architecture" className="relative px-6 py-28">
        <div className="max-w-[1200px] mx-auto">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-20 items-center">

            {/* Left: Flow visualization */}
            <motion.div
              variants={scaleIn}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: '-100px' }}
              className="relative order-2 lg:order-1"
            >
              <div
                className="rounded-2xl overflow-hidden"
                style={{
                  background: 'rgba(255,255,255,0.015)',
                  border: '1px solid rgba(255,255,255,0.05)',
                  height: '480px',
                }}
              >
                <ArchitectureFlow />
              </div>
            </motion.div>

            {/* Right: Copy */}
            <div className="order-1 lg:order-2">
              <motion.p
                variants={fade}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                className="text-[11px] font-medium text-[#14E0C5]/60 tracking-widest uppercase mb-4"
              >
                Request Lifecycle
              </motion.p>
              <motion.h2
                variants={fade}
                initial="hidden"
                whileInView="visible"
                custom={1}
                viewport={{ once: true }}
                className="text-[clamp(1.75rem,3.5vw,2.75rem)] font-normal leading-[1.12] tracking-[-0.015em]"
                style={{ fontFamily: '"Instrument Serif", Georgia, serif' }}
              >
                Every write travels
                <br />
                <span className="text-white/35" style={{ fontStyle: 'italic' }}>through consensus.</span>
              </motion.h2>
              <motion.p
                variants={fade}
                initial="hidden"
                whileInView="visible"
                custom={2}
                viewport={{ once: true }}
                className="mt-6 text-[14px] leading-[1.8] text-white/30 max-w-md"
              >
                A client request enters through the REST API, is forwarded to the
                cluster leader, appended to the Raft log, replicated to followers,
                committed after quorum acknowledgement, applied to the state machine,
                persisted to storage, and returned to the client. Every step is observable.
              </motion.p>

              <motion.div
                variants={fade}
                initial="hidden"
                whileInView="visible"
                custom={3}
                viewport={{ once: true }}
                className="mt-8 flex flex-col gap-3"
              >
                {[
                  { step: '1', label: 'Client submits a PUT/GET/DELETE via REST' },
                  { step: '2', label: 'Leader appends entry to local log' },
                  { step: '3', label: 'AppendEntries RPC replicates to all followers' },
                  { step: '4', label: 'Quorum confirms — entry is committed' },
                  { step: '5', label: 'State machine applies, storage persists' },
                ].map((item) => (
                  <div key={item.step} className="flex items-start gap-3">
                    <span
                      className="flex-shrink-0 w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-semibold mt-0.5"
                      style={{
                        background: 'rgba(20,224,197,0.08)',
                        color: 'rgba(20,224,197,0.6)',
                        border: '1px solid rgba(20,224,197,0.12)',
                      }}
                    >
                      {item.step}
                    </span>
                    <span className="text-[13px] text-white/40 leading-relaxed">{item.label}</span>
                  </div>
                ))}
              </motion.div>
            </div>
          </div>
        </div>
      </section>


      {/* ═══ FEATURES SECTION ═══ */}
      <section id="features" className="relative px-6 py-28">
        <div className="max-w-[1200px] mx-auto">
          <motion.div
            variants={fade}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            className="text-center mb-20"
          >
            <p className="text-[11px] font-medium text-[#14E0C5]/60 tracking-widest uppercase mb-4">
              Engineering
            </p>
            <h2
              className="text-[clamp(1.75rem,3.5vw,2.75rem)] font-normal leading-[1.12] tracking-[-0.015em]"
              style={{ fontFamily: '"Instrument Serif", Georgia, serif' }}
            >
              Built for correctness,
              <br />
              <span className="text-white/35" style={{ fontStyle: 'italic' }}>optimized for performance.</span>
            </h2>
          </motion.div>

          <div className="flex flex-col gap-32">
            {FEATURES.map((feature, i) => {
              const isReversed = i % 2 === 1;

              return (
                <motion.div
                  key={feature.title}
                  variants={fade}
                  initial="hidden"
                  whileInView="visible"
                  viewport={{ once: true, margin: '-80px' }}
                  className={`grid grid-cols-1 lg:grid-cols-2 gap-16 items-center ${isReversed ? '' : ''}`}
                >
                  {/* Code / Diagram Side */}
                  <div className={isReversed ? 'order-2' : 'order-2 lg:order-1'}>
                    <div
                      className="rounded-xl overflow-hidden p-6"
                      style={{
                        background: 'rgba(255,255,255,0.02)',
                        border: '1px solid rgba(255,255,255,0.05)',
                      }}
                    >
                      {/* Fake window chrome */}
                      <div className="flex items-center gap-1.5 mb-5">
                        <span className="w-2 h-2 rounded-full bg-white/[0.06]" />
                        <span className="w-2 h-2 rounded-full bg-white/[0.06]" />
                        <span className="w-2 h-2 rounded-full bg-white/[0.06]" />
                      </div>
                      <pre className="text-[12px] leading-[1.7] font-mono text-white/40 overflow-x-auto">
                        <code>{feature.code}</code>
                      </pre>

                      {/* Metric inline */}
                      <div className="mt-6 pt-4" style={{ borderTop: '1px solid rgba(255,255,255,0.04)' }}>
                        <div className="flex items-baseline gap-2">
                          <span className="text-[24px] font-normal tracking-tight" style={{ color: '#14E0C5', fontFamily: '"Instrument Serif", Georgia, serif', fontStyle: 'italic' }}>
                            {feature.metric.value}
                          </span>
                          <span className="text-[11px] text-white/25 font-medium">{feature.metric.label}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Text Side */}
                  <div className={isReversed ? 'order-1' : 'order-1 lg:order-2'}>
                    <h3 className="text-[22px] font-normal tracking-[-0.01em] leading-[1.3]" style={{ fontFamily: '"Instrument Serif", Georgia, serif' }}>
                      {feature.title}
                    </h3>
                    <p className="mt-4 text-[14px] leading-[1.8] text-white/35">
                      {feature.description}
                    </p>
                    <p className="mt-3 text-[13px] leading-[1.7] text-white/20">
                      {feature.detail}
                    </p>
                  </div>
                </motion.div>
              );
            })}
          </div>
        </div>
      </section>


      {/* ═══ DOCS / SDK SECTION ═══ */}
      <section id="docs" className="relative px-6 py-28">
        <div className="max-w-[1200px] mx-auto">
          <div className="h-px w-full mb-20" style={{ background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.06), transparent)' }} />

          <motion.div
            variants={fade}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <p className="text-[11px] font-medium text-[#14E0C5]/60 tracking-widest uppercase mb-4">
              Developer Experience
            </p>
            <h2
              className="text-[clamp(1.75rem,3.5vw,2.75rem)] font-normal leading-[1.12] tracking-[-0.015em]"
              style={{ fontFamily: '"Instrument Serif", Georgia, serif' }}
            >
              SDKs, CLI, and a
              <br />
              <span className="text-white/35" style={{ fontStyle: 'italic' }}>visual management studio.</span>
            </h2>
          </motion.div>

          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: '-80px' }}
            className="grid grid-cols-1 md:grid-cols-3 gap-px"
          >
            {[
              {
                title: 'Java SDK',
                description: 'Type-safe client with connection pooling, automatic leader discovery, and retry logic.',
                lang: 'java',
                snippet: `AtlasClient client = AtlasClient
  .builder("localhost:8080")
  .build();

client.put("user:1", userData);
String val = client.get("user:1");`,
              },
              {
                title: 'TypeScript SDK',
                description: 'First-class TypeScript support with full type inference and async/await patterns.',
                lang: 'typescript',
                snippet: `import { Atlas } from '@atlaskv/client';

const db = new Atlas('localhost:8080');

await db.put('user:1', userData);
const val = await db.get('user:1');`,
              },
              {
                title: 'CLI',
                description: 'Interactive command-line interface for cluster management, data operations, and diagnostics.',
                lang: 'bash',
                snippet: `$ atlas cluster status
┌─────────┬────────┬──────┐
│ Node    │ Role   │ Term │
├─────────┼────────┼──────┤
│ node-1  │ Leader │ 5    │
│ node-2  │ Follow │ 5    │
│ node-3  │ Follow │ 5    │
└─────────┴────────┴──────┘`,
              },
            ].map((sdk, i) => (
              <motion.div
                key={sdk.title}
                variants={fade}
                custom={i}
                className="p-8"
                style={{
                  background: 'rgba(255,255,255,0.015)',
                  border: '1px solid rgba(255,255,255,0.04)',
                }}
              >
                <h3 className="text-[16px] font-normal tracking-[-0.01em] mb-2" style={{ fontFamily: '"Instrument Serif", Georgia, serif' }}>{sdk.title}</h3>
                <p className="text-[12px] text-white/30 leading-relaxed mb-6">{sdk.description}</p>
                <pre className="text-[11px] leading-[1.6] font-mono text-white/30 overflow-x-auto">
                  <code>{sdk.snippet}</code>
                </pre>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>


      {/* ═══ CTA SECTION ═══ */}
      <section className="relative px-6 py-32">
        <div className="max-w-[600px] mx-auto text-center">
          <motion.div
            variants={fade}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
          >
            <h2
              className="text-[clamp(1.75rem,3.5vw,2.5rem)] font-normal leading-[1.12] tracking-[-0.015em]"
              style={{ fontFamily: '"Instrument Serif", Georgia, serif' }}
            >
              Ready to deploy
              <br />
              <span className="text-white/35" style={{ fontStyle: 'italic' }}>consensus at scale?</span>
            </h2>
            <p className="mt-5 text-[14px] text-white/30 leading-relaxed">
              Launch AtlasKV Studio to visualize your cluster topology,
              monitor replication, and manage data in real time.
            </p>
            <div className="mt-10 flex items-center justify-center gap-4">
              <Link
                href="/dashboard"
                className="group flex items-center gap-2.5 px-7 py-3.5 rounded-full text-[13px] font-semibold transition-all duration-300 hover:brightness-110"
                style={{
                  background: 'linear-gradient(135deg, #14E0C5 0%, #0EA5E9 100%)',
                  color: '#050505',
                }}
              >
                Launch Studio
                <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
              </Link>
              <a
                href="https://github.com"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-2 px-5 py-3.5 rounded-full text-[13px] font-medium text-white/40 transition-all duration-300 hover:text-white/70 hover:bg-white/[0.04]"
                style={{ border: '1px solid rgba(255,255,255,0.08)' }}
              >
                <GitHubIcon className="w-3.5 h-3.5" />
                Star on GitHub
              </a>
            </div>
          </motion.div>
        </div>
      </section>


      {/* ═══ FOOTER ═══ */}
      <footer className="relative px-6 pb-10 pt-6">
        <div className="max-w-[1200px] mx-auto">
          <div className="h-px w-full mb-8" style={{ background: 'rgba(255,255,255,0.04)' }} />

          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            {/* Logo */}
            <div className="flex items-center gap-2">
              <svg width="16" height="16" viewBox="0 0 20 20" fill="none">
                <path d="M10 1L18.66 6V14L10 19L1.34 14V6L10 1Z" stroke="#14E0C5" strokeWidth="1.5" fill="rgba(20,224,197,0.08)" />
                <circle cx="10" cy="10" r="2" fill="#14E0C5" opacity="0.5" />
              </svg>
              <span className="text-[12px] font-medium text-white/30">AtlasKV</span>
            </div>

            {/* Links */}
            <div className="flex items-center gap-6">
              {['Documentation', 'GitHub', 'License', 'Releases'].map((link) => (
                <a
                  key={link}
                  href="#"
                  className="text-[11px] text-white/20 hover:text-white/50 transition-colors font-medium"
                >
                  {link}
                </a>
              ))}
            </div>

            {/* Copyright */}
            <p className="text-[11px] text-white/15 font-normal">
              © {new Date().getFullYear()} AtlasKV · MIT License
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
