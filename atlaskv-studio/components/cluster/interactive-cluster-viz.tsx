'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Zap,
  Vote,
  Activity,
  Crown,
  Server,
  Radio,
  CheckCircle2,
} from 'lucide-react';
import type { NodeDetail } from '@/types/api';

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

interface InteractiveClusterVizProps {
  liveNodes?: NodeDetail[];
  leaderId?: string;
  term?: number;
  commitIndex?: number;
}

export function InteractiveClusterViz({
  liveNodes,
  leaderId,
  term = 1,
  commitIndex = 0,
}: InteractiveClusterVizProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animRef = useRef<number>(0);
  const tickRef = useRef<number>(0);

  const defaultNodes: NodeState[] = [
    {
      id: 'node1',
      label: 'Node 1',
      host: '127.0.0.1',
      port: 8081,
      role: 'LEADER',
      x: 320,
      y: 75,
      term: term || 1,
      commitIndex: commitIndex || 1,
      appliedIndex: commitIndex || 1,
      latencyMs: 0.0,
      healthy: true,
    },
    {
      id: 'node2',
      label: 'Node 2',
      host: '127.0.0.1',
      port: 8082,
      role: 'FOLLOWER',
      x: 140,
      y: 250,
      term: term || 1,
      commitIndex: commitIndex || 1,
      appliedIndex: commitIndex || 1,
      latencyMs: 0.45,
      healthy: true,
    },
    {
      id: 'node3',
      label: 'Node 3',
      host: '127.0.0.1',
      port: 8083,
      role: 'FOLLOWER',
      x: 500,
      y: 250,
      term: term || 1,
      commitIndex: commitIndex || 1,
      appliedIndex: commitIndex || 1,
      latencyMs: 0.48,
      healthy: true,
    },
  ];

  const [nodes, setNodes] = useState<NodeState[]>(defaultNodes);
  const [hoveredNode, setHoveredNode] = useState<NodeState | null>(null);
  const [activeSimulation, setActiveSimulation] = useState<'idle' | 'traffic' | 'election' | 'sync'>('idle');
  const [rpcCount, setRpcCount] = useState(1482);
  const particlesRef = useRef<Particle[]>([]);

  // Update nodes when liveNodes changes
  useEffect(() => {
    if (liveNodes && liveNodes.length > 0) {
      const positions = [
        { x: 320, y: 75 },
        { x: 140, y: 250 },
        { x: 500, y: 250 },
        { x: 230, y: 160 },
        { x: 410, y: 160 },
      ];

      // Place leader at top if possible
      const sorted = [...liveNodes].sort((a, b) => (b.isLeader ? 1 : 0) - (a.isLeader ? 1 : 0));

      setNodes(
        sorted.map((n, idx) => {
          const pos = positions[idx % positions.length];
          const role = (n.role || (n.isLeader ? 'LEADER' : 'FOLLOWER')) as 'LEADER' | 'FOLLOWER' | 'CANDIDATE';
          return {
            id: n.id,
            label: n.id.toUpperCase(),
            host: n.host,
            port: n.port,
            role,
            x: pos.x,
            y: pos.y,
            term: n.term,
            commitIndex: n.commitIndex,
            appliedIndex: n.appliedIndex,
            latencyMs: n.latencyMs,
            healthy: n.healthy,
          };
        })
      );
    }
  }, [liveNodes]);

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
      speed: 0.018 + Math.random() * 0.01,
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

    if (leader && followers.length > 0) {
      for (let i = 0; i < 8; i++) {
        setTimeout(() => {
          followers.forEach((f) => spawnParticle(leader, f, 'append'));
        }, i * 180);
      }
    }

    setTimeout(() => setActiveSimulation('idle'), 2000);
  };

  // Simulation Trigger: Leader Election
  const triggerElection = () => {
    setActiveSimulation('election');

    setNodes((prev) =>
      prev.map((n, idx) => {
        if (idx === 1) return { ...n, role: 'LEADER', term: n.term + 1 };
        if (idx === 0) return { ...n, role: 'FOLLOWER', term: n.term + 1 };
        return { ...n, term: n.term + 1 };
      })
    );

    setTimeout(() => {
      const candidate = nodes[1] || nodes[0];
      const peers = nodes.filter((n) => n.id !== candidate.id);
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
        }
      }

      // 2. Animate and Render Data Transmission Particles
      particlesRef.current = particlesRef.current.filter((p) => p.progress < 1);

      particlesRef.current.forEach((p) => {
        p.progress += p.speed;

        const currentX = (p.fromX + (p.toX - p.fromX) * p.progress) * scaleX;
        const currentY = (p.fromY + (p.toY - p.fromY) * p.progress) * scaleY;

        ctx.beginPath();
        ctx.arc(currentX, currentY, 4, 0, Math.PI * 2);
        ctx.fillStyle = p.color;
        ctx.shadowColor = p.color;
        ctx.shadowBlur = 8;
        ctx.fill();
        ctx.shadowBlur = 0;
      });

      // 3. Render Raft Nodes
      scaledNodes.forEach((node) => {
        const isLeader = node.role === 'LEADER';
        const isCandidate = node.role === 'CANDIDATE';
        const radius = isLeader ? 32 : 26;

        // Outer Aura Ring
        ctx.beginPath();
        ctx.arc(node.cx, node.cy, radius + 8, 0, Math.PI * 2);
        if (isLeader) {
          ctx.fillStyle = isDark ? 'rgba(16, 185, 129, 0.12)' : 'rgba(16, 185, 129, 0.15)';
        } else if (isCandidate) {
          ctx.fillStyle = isDark ? 'rgba(168, 85, 247, 0.12)' : 'rgba(168, 85, 247, 0.15)';
        } else {
          ctx.fillStyle = isDark ? 'rgba(59, 130, 246, 0.06)' : 'rgba(59, 130, 246, 0.1)';
        }
        ctx.fill();

        // Base Node Circle
        ctx.beginPath();
        ctx.arc(node.cx, node.cy, radius, 0, Math.PI * 2);
        ctx.fillStyle = isDark ? '#0f172a' : '#ffffff';
        ctx.strokeStyle = isLeader
          ? '#10b981'
          : isCandidate
          ? '#a855f7'
          : isDark
          ? '#334155'
          : '#cbd5e1';
        ctx.lineWidth = isLeader ? 3 : 2;
        ctx.shadowColor = isLeader ? 'rgba(16, 185, 129, 0.4)' : 'transparent';
        ctx.shadowBlur = isLeader ? 16 : 0;
        ctx.fill();
        ctx.stroke();
        ctx.shadowBlur = 0;

        // Node Title Text
        ctx.fillStyle = isDark ? '#f8fafc' : '#0f172a';
        ctx.font = 'bold 12px monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(node.label, node.cx, node.cy - 4);

        // Role Subtext
        ctx.fillStyle = isLeader ? '#10b981' : isCandidate ? '#a855f7' : '#64748b';
        ctx.font = '10px monospace';
        ctx.fillText(node.role, node.cx, node.cy + 10);
      });

      animRef.current = requestAnimationFrame(render);
    };

    animRef.current = requestAnimationFrame(render);

    return () => {
      clearInterval(heartbeatInterval);
      cancelAnimationFrame(animRef.current);
      window.removeEventListener('resize', resize);
    };
  }, [nodes]);

  // Canvas Mouse Move Interaction
  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const x = (e.clientX - rect.left) * (640 / canvas.clientWidth);
    const y = (e.clientY - rect.top) * (320 / canvas.clientHeight);

    const hit = nodes.find((n) => {
      const dx = n.x - x;
      const dy = n.y - y;
      return Math.sqrt(dx * dx + dy * dy) < 35;
    });

    setHoveredNode(hit || null);
  };

  const healthyCount = nodes.filter((n) => n.healthy).length;
  const totalCount = nodes.length;

  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      className="glass-card rounded-2xl p-5 border border-border dark:border-[oklch(1_0_0/8%)] bg-[var(--surface-1)] shadow-sm"
    >
      {/* Control Header & Simulation Controls */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-4 pb-3 border-b border-border dark:border-[oklch(1_0_0/6%)]">
        <div className="flex items-center gap-2">
          <Radio className="h-4 w-4 text-emerald-500 animate-pulse" />
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] font-mono">
            Interactive Consensus Visualizer
          </h3>
        </div>

        {/* Simulation Action Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            id="viz-simulate-traffic-btn"
            onClick={triggerTraffic}
            disabled={activeSimulation !== 'idle'}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold bg-emerald-500/12 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/20 active:scale-95 transition-all cursor-pointer shadow-sm disabled:opacity-50"
          >
            <Zap className="h-3.5 w-3.5" />
            Simulate Traffic
          </button>

          <button
            id="viz-trigger-election-btn"
            onClick={triggerElection}
            disabled={activeSimulation !== 'idle'}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold bg-purple-500/12 text-purple-600 dark:text-purple-400 border border-purple-500/30 hover:bg-purple-500/20 active:scale-95 transition-all cursor-pointer shadow-sm disabled:opacity-50"
          >
            <Vote className="h-3.5 w-3.5" />
            Trigger Election
          </button>

          <button
            id="viz-ping-heartbeats-btn"
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
        <canvas
          ref={canvasRef}
          onMouseMove={handleMouseMove}
          onMouseLeave={() => setHoveredNode(null)}
          className="w-full h-full block cursor-crosshair"
        />

        {/* Hover telemetry card */}
        <AnimatePresence>
          {hoveredNode && (
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="absolute bottom-4 left-4 p-3.5 rounded-xl glass-card border border-emerald-500/30 text-xs font-mono space-y-1.5 shadow-xl max-w-xs pointer-events-none"
            >
              <div className="flex items-center justify-between font-bold text-[var(--foreground)] border-b border-border pb-1">
                <span className="flex items-center gap-1.5">
                  {hoveredNode.role === 'LEADER' ? (
                    <Crown className="h-3.5 w-3.5 text-emerald-500" />
                  ) : (
                    <Server className="h-3.5 w-3.5 text-cyan-500" />
                  )}
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
                <span className="text-cyan-600 dark:text-cyan-400 font-semibold">{hoveredNode.latencyMs.toFixed(2)}ms</span>
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
            {healthyCount}/{totalCount} Quorum Active
          </span>
          <span>RPCs Handled: <strong className="text-[var(--foreground)] font-bold">{rpcCount.toLocaleString()}</strong></span>
          <span>Commit Index: <strong className="text-emerald-600 dark:text-emerald-400 font-bold">#{commitIndex}</strong></span>
        </div>

        <div className="flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-emerald-500 animate-ping" />
          <span className="text-neutral-600 dark:text-neutral-400 font-semibold">Joint Consensus Active</span>
        </div>
      </div>
    </motion.div>
  );
}
