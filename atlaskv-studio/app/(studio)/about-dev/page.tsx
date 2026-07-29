'use client';

import { motion } from 'framer-motion';
import Image from 'next/image';
import {
  ExternalLink,
  BookOpen,
  Server,
  Terminal,
  Code2,
  FileCode2,
  Globe,
  Boxes,
  Shield,
  Container,
  Tag,
} from 'lucide-react';

/* ═══════════════════════════════════════════════════════════════════════════
   DATA
   ═══════════════════════════════════════════════════════════════════════════ */

const developers = [
  {
    name: 'K. Rishi Chowdary',
    role: 'Lead Developer',
    photo: '/Rishi.png',
    bio: 'Designed and implemented the AtlasKV distributed database and developer platform.',
    color: 'emerald' as const,
    responsibilities: [
      'Architecture',
      'Raft Consensus',
      'Backend',
      'AtlasKV Studio',
      'Java SDK',
      'TypeScript SDK',
      'CLI',
      'REST API',
      'UI/UX',
      'Documentation',
    ],
    github: 'https://github.com/Immortal4728',
    linkedin: '#',
  },
  {
    name: 'T. Bharath',
    role: 'Deployment & Infrastructure',
    photo: '/bharath.jpeg',
    bio: 'Deployment, infrastructure configuration, cloud setup and deployment validation.',
    color: 'cyan' as const,
    responsibilities: [
      'Cloud Deployment',
      'Infrastructure',
      'Server Configuration',
      'Deployment Testing',
    ],
    github: '#',
    linkedin: '#',
  },
];

const highlights = [
  { label: 'Distributed Nodes', value: '3', icon: Server },
  { label: 'Java SDK', value: '✓', icon: Code2 },
  { label: 'TypeScript SDK', value: '✓', icon: FileCode2 },
  { label: 'CLI', value: '✓', icon: Terminal },
  { label: 'REST API', value: '✓', icon: Globe },
  { label: 'Raft Consensus', value: '✓', icon: Shield },
  { label: 'Docker', value: '✓', icon: Container },
  { label: 'Current Release', value: 'v3.3', icon: Tag },
];

/* ═══════════════════════════════════════════════════════════════════════════
   ANIMATIONS
   ═══════════════════════════════════════════════════════════════════════════ */

const fadeUp = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
};

const stagger = {
  animate: { transition: { staggerChildren: 0.06 } },
};

/* ═══════════════════════════════════════════════════════════════════════════
   PAGE
   ═══════════════════════════════════════════════════════════════════════════ */

export default function AboutDevPage() {
  return (
    <div className="max-w-5xl mx-auto pb-24 pt-6 sm:pt-10 px-2">
      {/* ─── HERO ─── */}
      <motion.section
        initial={{ opacity: 0, y: -12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: [0.25, 0.46, 0.45, 0.94] }}
        className="flex flex-col items-center text-center space-y-6 mb-20"
      >
        {/* Logo */}
        <motion.div
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="relative"
        >
          <div className="h-24 w-24 sm:h-28 sm:w-28 rounded-[1.75rem] bg-white p-3 shadow-xl shadow-neutral-900/5 dark:shadow-black/30 border border-neutral-200/60 dark:border-white/10 overflow-hidden">
            <Image
              src="/atlaskv-logo.png"
              alt="AtlasKV"
              width={96}
              height={96}
              className="h-full w-full object-contain"
              priority
            />
          </div>
          {/* Subtle glow behind logo */}
          <div className="absolute inset-0 rounded-[1.75rem] bg-emerald-500/10 blur-2xl -z-10 scale-150" />
        </motion.div>

        {/* Title + Subtitle */}
        <div className="space-y-2">
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-[var(--foreground)]">
            AtlasKV
          </h1>
          <p className="text-base sm:text-lg font-medium text-neutral-500 dark:text-neutral-400">
            Distributed Key-Value Database
          </p>
        </div>

        {/* Badges */}
        <div className="flex items-center gap-3">
          <span className="inline-flex items-center px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/25 text-emerald-700 dark:text-emerald-400 text-xs font-bold font-mono tracking-wide">
            v3.3
          </span>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-neutral-100 dark:bg-white/5 border border-neutral-200/70 dark:border-white/8 text-neutral-600 dark:text-neutral-400 text-xs font-semibold">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
            Stable Release
          </span>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-3 pt-2">
          <a
            href="https://github.com/Immortal4728/AtlasKV"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-neutral-900 dark:bg-white text-white dark:text-neutral-900 text-sm font-semibold shadow-lg shadow-neutral-900/10 dark:shadow-white/5 hover:scale-[1.02] active:scale-[0.98] transition-transform"
          >
            <GithubIcon className="h-4 w-4" />
            GitHub
          </a>
          <a
            href="https://github.com/Immortal4728/AtlasKV#readme"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-neutral-100 dark:bg-white/6 border border-neutral-200/80 dark:border-white/10 text-neutral-700 dark:text-neutral-300 text-sm font-semibold hover:bg-neutral-200/70 dark:hover:bg-white/10 hover:scale-[1.02] active:scale-[0.98] transition-all"
          >
            <BookOpen className="h-4 w-4" />
            Documentation
          </a>
        </div>
      </motion.section>

      {/* ─── DEVELOPERS ─── */}
      <motion.section
        initial="initial"
        animate="animate"
        variants={stagger}
        className="mb-20"
      >
        <motion.h2
          variants={fadeUp}
          transition={{ duration: 0.4 }}
          className="text-center text-xs font-bold uppercase tracking-[0.2em] text-neutral-400 dark:text-neutral-500 mb-10 font-mono"
        >
          Built by
        </motion.h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {developers.map((dev, i) => (
            <DeveloperCard key={dev.name} dev={dev} index={i} />
          ))}
        </div>
      </motion.section>

      {/* ─── HIGHLIGHTS ─── */}
      <motion.section
        initial="initial"
        animate="animate"
        variants={stagger}
      >
        <motion.h2
          variants={fadeUp}
          transition={{ duration: 0.4 }}
          className="text-center text-xs font-bold uppercase tracking-[0.2em] text-neutral-400 dark:text-neutral-500 mb-8 font-mono"
        >
          Project Highlights
        </motion.h2>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
          {highlights.map((item, i) => (
            <motion.div
              key={item.label}
              variants={fadeUp}
              transition={{ duration: 0.35, delay: i * 0.04 }}
              whileHover={{ y: -3, transition: { duration: 0.2 } }}
              className="group flex flex-col items-center gap-2.5 p-5 rounded-2xl bg-white dark:bg-white/[0.03] border border-neutral-200/60 dark:border-white/[0.06] shadow-sm hover:shadow-md hover:border-emerald-500/30 dark:hover:border-emerald-500/20 transition-all cursor-default"
            >
              <item.icon className="h-5 w-5 text-neutral-400 dark:text-neutral-500 group-hover:text-emerald-600 dark:group-hover:text-emerald-400 transition-colors" strokeWidth={1.6} />
              <div className="text-center">
                <div className="text-sm font-bold text-[var(--foreground)] font-mono">
                  {item.value}
                </div>
                <div className="text-[11px] text-neutral-500 dark:text-neutral-400 font-medium mt-0.5">
                  {item.label}
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </motion.section>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════════════════
   DEVELOPER CARD
   ═══════════════════════════════════════════════════════════════════════════ */

function DeveloperCard({
  dev,
  index,
}: {
  dev: (typeof developers)[number];
  index: number;
}) {
  const borderColor =
    dev.color === 'emerald'
      ? 'border-emerald-500/25 hover:border-emerald-500/40'
      : 'border-cyan-500/25 hover:border-cyan-500/40';

  const ringColor =
    dev.color === 'emerald'
      ? 'ring-emerald-500/20'
      : 'ring-cyan-500/20';

  const roleColor =
    dev.color === 'emerald'
      ? 'text-emerald-700 dark:text-emerald-400 bg-emerald-500/10 border-emerald-500/20'
      : 'text-cyan-700 dark:text-cyan-400 bg-cyan-500/10 border-cyan-500/20';

  return (
    <motion.div
      variants={fadeUp}
      transition={{ duration: 0.5, delay: index * 0.12 }}
      whileHover={{ y: -4, transition: { duration: 0.25 } }}
      className={`relative flex flex-col items-center text-center p-8 sm:p-10 rounded-3xl bg-white dark:bg-white/[0.02] border ${borderColor} shadow-sm hover:shadow-xl dark:hover:shadow-2xl dark:hover:shadow-black/20 transition-all duration-300`}
    >
      {/* Profile Photo */}
      <motion.div
        whileHover={{ scale: 1.05, transition: { duration: 0.3 } }}
        className={`relative h-32 w-32 sm:h-36 sm:w-36 rounded-full overflow-hidden shadow-xl ring-4 ${ringColor} mb-6`}
      >
        <Image
          src={dev.photo}
          alt={dev.name}
          fill
          className="object-cover"
          priority
        />
      </motion.div>

      {/* Name */}
      <h3 className="text-xl font-bold tracking-tight text-[var(--foreground)] mb-1">
        {dev.name}
      </h3>

      {/* Role Badge */}
      <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold font-mono border ${roleColor} mb-4`}>
        {dev.role}
      </span>

      {/* Bio */}
      <p className="text-sm text-neutral-500 dark:text-neutral-400 leading-relaxed max-w-xs mb-6">
        {dev.bio}
      </p>

      {/* Responsibility Badges */}
      <motion.div
        initial="initial"
        animate="animate"
        variants={stagger}
        className="flex flex-wrap justify-center gap-1.5 mb-8"
      >
        {dev.responsibilities.map((resp, i) => (
          <motion.span
            key={resp}
            variants={fadeUp}
            transition={{ duration: 0.25, delay: i * 0.03 }}
            className="px-2.5 py-1 rounded-lg bg-neutral-100 dark:bg-white/[0.04] border border-neutral-200/60 dark:border-white/[0.06] text-neutral-700 dark:text-neutral-300 text-[11px] font-medium hover:bg-neutral-200/60 dark:hover:bg-white/[0.08] hover:border-neutral-300 dark:hover:border-white/10 transition-colors cursor-default"
          >
            {resp}
          </motion.span>
        ))}
      </motion.div>

      {/* Social Links */}
      <div className="flex items-center gap-3">
        <a
          href={dev.github}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-neutral-100 dark:bg-white/5 border border-neutral-200/60 dark:border-white/8 text-neutral-600 dark:text-neutral-400 text-xs font-semibold hover:bg-neutral-200/70 dark:hover:bg-white/10 hover:text-neutral-900 dark:hover:text-white transition-all"
        >
          <GithubIcon className="h-3.5 w-3.5" />
          GitHub
        </a>
        <a
          href={dev.linkedin}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-neutral-100 dark:bg-white/5 border border-neutral-200/60 dark:border-white/8 text-neutral-600 dark:text-neutral-400 text-xs font-semibold hover:bg-neutral-200/70 dark:hover:bg-white/10 hover:text-neutral-900 dark:hover:text-white transition-all"
        >
          <LinkedInIcon className="h-3.5 w-3.5" />
          LinkedIn
        </a>
      </div>
    </motion.div>
  );
}

/* ═══════════════════════════════════════════════════════════════════════════
   SVG ICONS
   ═══════════════════════════════════════════════════════════════════════════ */

function GithubIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg {...props} fill="currentColor" viewBox="0 0 24 24">
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
      />
    </svg>
  );
}

function LinkedInIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg {...props} fill="currentColor" viewBox="0 0 24 24">
      <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433a2.062 2.062 0 01-2.063-2.065 2.064 2.064 0 112.063 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z" />
    </svg>
  );
}
