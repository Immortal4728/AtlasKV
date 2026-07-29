'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Zap,
  Vote,
  Activity,
  RotateCcw,
  Crown,
  Server,
  Heart,
  Radio,
  CheckCircle2,
  Cpu,
  Layers,
  Sparkles,
  Wifi,
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface NodeState {
  id: string;
  label: string;
  host: string;
  port: number;
  role: 'LEADER' | 'FOLLOWER' | 'CANDIDATE';
  x: number;
  y: number;
  term: number;
  commitIndex: number;
  appliedIndex: number;
  latencyMs: number;
  healthy: boolean;
}

interface Particle {
  id: string;
  fromX: number;
  fromY: number;
  toX: number;
  toY: number;
  progress: number;
  speed: number;
  type: 'heartbeat' | 'append' | 'vote' | 'commit';
  color: string;
}

export function InteractiveClusterViz() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animRef = useRef<number>(0);
  const tickRef = useRef<number>(0);

  const [nodes, setNodes] = useState<NodeState[]>([
    {
      id: 'node3',
      label: 'Node 3',
      host: 'localhost',
      port: 8083,
      role: 'LEADER',
      x: 320,
      y: 75,
      term: 47,
      commitIndex: 34601,
      appliedIndex: 34601,
      latencyMs: 0.38,
      healthy: true,
    },
    {
      id: 'node1',
      label: 'Node 1',
      host: 'localhost',
      port: 8081,
      role: 'FOLLOWER',
      x: 120,
      y: 250,
      term: 47,
      commitIndex: 34601,
      appliedIndex: 34601,
      latencyMs: 0.45,
      healthy: true,
    },
    {
      id: 'node2',
      label: 'Node 2',
      host: 'localhost',
      port: 8082,
      role: 'FOLLOWER',
      x: 520,
      y: 250,
      term: 47,
      commitIndex: 34601,
      appliedIndex: 34601,
      latencyMs: 0.52,
      healthy: true,
    },
  ]);

  const [hoveredNode, setHoveredNode] = useState<NodeState | null>(null);
  const [activeSimulation, setActiveSimulation] = useState<'idle' | 'traffic' | 'election' | 'sync'>('idle');
  const [rpcCount, setRpcCount] = useState(1482);
  const particlesRef = useRef<Particle[]>([]);

  // Spawn dynamic particle
  const spawnParticle = useCallback((from: NodeState, to: NodeState, type: Particle['type']) => {
    let color = '#10b981'; // green for heartbeat
    if (type === 'append') color = '#3b82f6'; // blue
    if (type === 'vote') color = '#a855f7'; // purple
    if (type === 'commit') color = '#f59e0b'; // amber

    particlesRef.current.push({
      id: Math.random().toString(36).substring(7),
      fromX: from.x,
      fromY: from.y,
      toX: to.x,
      toY: to.y,
      progress: 0,
      speed: 0.015 + Math.random() * 0.01,
      type,
      color,
    });
  }, []);

  // Simulation Trigger: Traffic Burst
  const triggerTraffic = () => {
    setActiveSimulation('traffic');
    setRpcCount((prev) => prev + 120);

    const leader = nodes.find((n) => n.role === 'LEADER');
    const followers = nodes.filter((n) => n.role !== 'LEADER');

    if (leader && followers.length >= 2) {
      for (let i = 0; i < 8; i++) {
        setTimeout(() => {
          spawnParticle(leader, followers[0], 'append');
          spawnParticle(leader, followers[1], 'append');
        }, i * 180);
      }
    }

    setTimeout(() => setActiveSimulation('idle'), 2000);
  };

  // Simulation Trigger: Leader Election
  const triggerElection = () => {
    setActiveSimulation('election');

    setNodes((prev) =>
      prev.map((n) => {
        if (n.id === 'node1') return { ...n, role: 'LEADER', term: n.term + 1 };
        if (n.id === 'node3') return { ...n, role: 'FOLLOWER', term: n.term + 1 };
        return { ...n, term: n.term + 1 };
      })
    );

    setTimeout(() => {
      const candidate = nodes.find((n) => n.id === 'node1');
      const peers = nodes.filter((n) => n.id !== 'node1');
      if (candidate) {
        peers.forEach((p) => spawnParticle(candidate, p, 'vote'));
      }
    }, 200);

    setTimeout(() => setActiveSimulation('idle'), 2500);
  };

  // Simulation Trigger: Heartbeat Ping
  const triggerHeartbeat = () => {
    setActiveSimulation('sync');
    setRpcCount((prev) => prev + 15);

    const leader = nodes.find((n) => n.role === 'LEADER');
    const followers = nodes.filter((n) => n.role !== 'LEADER');

    if (leader) {
      followers.forEach((f) => spawnParticle(leader, f, 'heartbeat'));
    }

    setTimeout(() => setActiveSimulation('idle'), 1500);
  };

  // Main Canvas Render Loop
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    let animationFrame: number;

    const resize = () => {
      const parent = canvas.parentElement;
      if (!parent) return;
      canvas.width = parent.clientWidth * dpr;
      canvas.height = parent.clientHeight * dpr;
      canvas.style.width = `${parent.clientWidth}px`;
      canvas.style.height = `${parent.clientHeight}px`;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };

    resize();
    window.addEventListener('resize', resize);

    // Periodic Heartbeats
    const heartbeatInterval = setInterval(() => {
      const leader = nodes.find((n) => n.role === 'LEADER');
      const followers = nodes.filter((n) => n.role !== 'LEADER');
      if (leader) {
        followers.forEach((f) => spawnParticle(leader, f, 'heartbeat'));
      }
    }, 2200);

    const render = () => {
      tickRef.current += 0.02;
      const w = canvas.clientWidth;
      const h = canvas.clientHeight;
      const isDark = document.documentElement.classList.contains('dark');

      ctx.clearRect(0, 0, w, h);

      // Node coordinates scaled to canvas size
      const scaleX = w / 640;
      const scaleY = h / 320;

      const scaledNodes = nodes.map((n) => ({
        ...n,
        cx: n.x * scaleX,
        cy: n.y * scaleY,
      }));

      // 1. Draw Mesh Connection Lines
      ctx.lineWidth = isDark ? 1.5 : 2;
      for (let i = 0; i < scaledNodes.length; i++) {
        for (let j = i + 1; j < scaledNodes.length; j++) {
          const n1 = scaledNodes[i];
          const n2 = scaledNodes[j];

          const isLeaderConn = n1.role === 'LEADER' || n2.role === 'LEADER';

          ctx.beginPath();
          ctx.moveTo(n1.cx, n1.cy);
          ctx.lineTo(n2.cx, n2.cy);

          if (isLeaderConn) {
            ctx.strokeStyle = isDark ? 'rgba(16, 185, 129, 0.25)' : 'rgba(16, 185, 129, 0.35)';
            ctx.setLineDash([]);
          } else {
            ctx.strokeStyle = isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.12)';
            ctx.setLineDash([4, 4]);
          }
          ctx.stroke();
          ctx.setLineDash([]);
        }
      }

      // 2. Render Animated Particles
      particlesRef.current = particlesRef.current.filter((p) => {
        p.progress += p.speed;
        return p.progress <= 1;
      });

      particlesRef.current.forEach((p) => {
        const fromNode = scaledNodes.find((n) => n.x === p.fromX && n.y === p.fromY);
        const toNode = scaledNodes.find((n) => n.x === p.toX && n.y === p.toY);

        if (!fromNode || !toNode) return;

        const currX = fromNode.cx + (toNode.cx - fromNode.cx) * p.progress;
        const currY = fromNode.cy + (toNode.cy - fromNode.cy) * p.progress;

        // Particle Glow Trail
        ctx.beginPath();
        ctx.arc(currX, currY, 6, 0, Math.PI * 2);
        ctx.fillStyle = p.color + '40';
        ctx.fill();

        // Core Particle
        ctx.beginPath();
        ctx.arc(currX, currY, 3.5, 0, Math.PI * 2);
        ctx.fillStyle = p.color;
        ctx.fill();
      });

      // 3. Render Nodes
      scaledNodes.forEach((node) => {
        const isLeader = node.role === 'LEADER';
        const isCandidate = node.role === 'CANDIDATE';
        const radius = isLeader ? 26 : 22;

        // Leader Glowing Aura Ring
        if (isLeader) {
          const auraRadius = radius + 12 + Math.sin(tickRef.current * 3) * 4;
          ctx.beginPath();
          ctx.arc(node.cx, node.cy, auraRadius, 0, Math.PI * 2);
          ctx.fillStyle = isDark ? 'rgba(16, 185, 129, 0.12)' : 'rgba(16, 185, 129, 0.18)';
          ctx.fill();

          ctx.beginPath();
          ctx.arc(node.cx, node.cy, radius + 6, 0, Math.PI * 2);
          ctx.strokeStyle = isDark ? 'rgba(16, 185, 129, 0.4)' : 'rgba(16, 185, 129, 0.6)';
          ctx.lineWidth = 1.5;
          ctx.stroke();
        }

        // Candidate Aura Ring
        if (isCandidate) {
          const auraRadius = radius + 10 + Math.sin(tickRef.current * 5) * 5;
          ctx.beginPath();
          ctx.arc(node.cx, node.cy, auraRadius, 0, Math.PI * 2);
          ctx.fillStyle = isDark ? 'rgba(168, 85, 247, 0.15)' : 'rgba(168, 85, 247, 0.22)';
          ctx.fill();
        }

        // Node Main Body Circle
        ctx.beginPath();
        ctx.arc(node.cx, node.cy, radius, 0, Math.PI * 2);

        if (isLeader) {
          ctx.fillStyle = isDark ? 'rgba(16, 185, 129, 0.22)' : 'rgba(16, 185, 129, 0.25)';
          ctx.strokeStyle = isDark ? '#10b981' : '#059669';
          ctx.lineWidth = 2.5;
        } else if (isCandidate) {
          ctx.fillStyle = isDark ? 'rgba(168, 85, 247, 0.22)' : 'rgba(168, 85, 247, 0.25)';
          ctx.strokeStyle = isDark ? '#a855f7' : '#7e22ce';
          ctx.lineWidth = 2.5;
        } else {
          ctx.fillStyle = isDark ? 'rgba(30, 41, 59, 0.7)' : 'rgba(241, 245, 249, 0.9)';
          ctx.strokeStyle = isDark ? 'rgba(148, 163, 184, 0.3)' : 'rgba(100, 116, 139, 0.4)';
          ctx.lineWidth = 2;
        }

        ctx.fill();
        ctx.stroke();

        // Role Icon indicator inside node
        ctx.fillStyle = isLeader
          ? isDark ? '#34d399' : '#047857'
          : isCandidate
          ? isDark ? '#c084fc' : '#7e22ce'
          : isDark ? '#94a3b8' : '#334155';

        ctx.font = '700 10px "Space Grotesk", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(node.role, node.cx, node.cy - 5);

        // Node ID Label
        ctx.fillStyle = isDark ? '#f8fafc' : '#0f172a';
        ctx.font = '700 12px "Space Grotesk", sans-serif';
        ctx.fillText(node.label, node.cx, node.cy + 7);

        // Telemetry Subtext
        ctx.fillStyle = isDark ? '#94a3b8' : '#475569';
        ctx.font = '600 10px "SF Mono", monospace';
        ctx.fillText(`Term ${node.term} · ${node.latencyMs}ms`, node.cx, node.cy + radius + 15);
      });

      animationFrame = requestAnimationFrame(render);
    };

    animationFrame = requestAnimationFrame(render);

    return () => {
      cancelAnimationFrame(animationFrame);
      clearInterval(heartbeatInterval);
      window.removeEventListener('resize', resize);
    };
  }, [nodes, spawnParticle]);

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.45 }}
      className="glass-card rounded-2xl p-6 relative overflow-hidden border border-border dark:border-[oklch(1_0_0/8%)]"
    >
      {/* Ambient background glow */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[380px] h-[220px] bg-emerald-500/10 dark:bg-emerald-500/8 blur-[90px] pointer-events-none" />

      {/* Header & Controls Bar */}
      <div className="relative flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4 pb-4 border-b border-border dark:border-[oklch(1_0_0/6%)]">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30">
            <Radio className="h-4.5 w-4.5 animate-pulse" />
          </div>
          <div>
            <h3 className="text-sm font-bold tracking-tight text-[var(--foreground)] flex items-center gap-2 font-mono">
              Cluster Topology Mesh
            </h3>
            <p className="text-xs text-neutral-600 dark:text-neutral-400 font-medium">
              Node status and RPC packet flow.
            </p>
          </div>
        </div>

        {/* Live Simulation Controls */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={triggerTraffic}
            disabled={activeSimulation !== 'idle'}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold bg-emerald-500/12 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/20 active:scale-95 transition-all cursor-pointer shadow-sm disabled:opacity-50"
          >
            <Zap className="h-3.5 w-3.5" />
            Simulate Traffic
          </button>

          <button
            onClick={triggerElection}
            disabled={activeSimulation !== 'idle'}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold bg-purple-500/12 text-purple-600 dark:text-purple-400 border border-purple-500/30 hover:bg-purple-500/20 active:scale-95 transition-all cursor-pointer shadow-sm disabled:opacity-50"
          >
            <Vote className="h-3.5 w-3.5" />
            Trigger Election
          </button>

          <button
            onClick={triggerHeartbeat}
            disabled={activeSimulation !== 'idle'}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold bg-cyan-500/12 text-cyan-600 dark:text-cyan-400 border border-cyan-500/30 hover:bg-cyan-500/20 active:scale-95 transition-all cursor-pointer shadow-sm disabled:opacity-50"
          >
            <Activity className="h-3.5 w-3.5" />
            Ping Heartbeats
          </button>
        </div>
      </div>

      {/* Interactive Animation Canvas Area */}
      <div className="relative w-full h-[300px] flex items-center justify-center rounded-xl bg-neutral-100/50 dark:bg-[var(--surface-0)] border border-border dark:border-[oklch(1_0_0/5%)] overflow-hidden shadow-inner">
        <canvas ref={canvasRef} className="w-full h-full block" />

        {/* Hover telemetry card */}
        <AnimatePresence>
          {hoveredNode && (
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="absolute bottom-4 left-4 p-3.5 rounded-xl glass-card border border-emerald-500/30 text-xs font-mono space-y-1.5 shadow-xl max-w-xs"
            >
              <div className="flex items-center justify-between font-bold text-[var(--foreground)] border-b border-border pb-1">
                <span className="flex items-center gap-1.5">
                  <Crown className="h-3.5 w-3.5 text-emerald-500" />
                  {hoveredNode.label}
                </span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                  {hoveredNode.role}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-x-3 gap-y-1 text-neutral-600 dark:text-neutral-400">
                <span>Address:</span>
                <span className="text-[var(--foreground)] font-semibold">{hoveredNode.host}:{hoveredNode.port}</span>
                <span>Term:</span>
                <span className="text-[var(--foreground)] font-semibold">{hoveredNode.term}</span>
                <span>Commit Index:</span>
                <span className="text-emerald-600 dark:text-emerald-400 font-semibold">{hoveredNode.commitIndex}</span>
                <span>Latency:</span>
                <span className="text-cyan-600 dark:text-cyan-400 font-semibold">{hoveredNode.latencyMs}ms</span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Live Cluster Metrics Footer */}
      <div className="mt-4 pt-3 border-t border-border dark:border-[oklch(1_0_0/6%)] flex flex-wrap items-center justify-between gap-3 text-xs font-mono">
        <div className="flex items-center gap-4 text-neutral-600 dark:text-neutral-400 font-medium">
          <span className="flex items-center gap-1.5 text-emerald-600 dark:text-emerald-400 font-semibold">
            <CheckCircle2 className="h-3.5 w-3.5" />
            3/3 Quorum Active
          </span>
          <span>RPCs Handled: <strong className="text-[var(--foreground)] font-bold">{rpcCount.toLocaleString()}</strong></span>
          <span>Avg Latency: <strong className="text-emerald-600 dark:text-emerald-400 font-bold">0.42ms</strong></span>
        </div>

        <div className="flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-emerald-500 animate-ping" />
          <span className="text-neutral-600 dark:text-neutral-400 font-semibold">Joint Consensus Active</span>
        </div>
      </div>
    </motion.div>
  );
}
