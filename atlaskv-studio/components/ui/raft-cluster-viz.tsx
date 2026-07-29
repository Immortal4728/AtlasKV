'use client';

import { useEffect, useRef, useState, useCallback } from 'react';

interface NodeState {
  id: string;
  label: string;
  role: 'leader' | 'follower' | 'candidate';
  x: number;
  y: number;
  term: number;
  commitIndex: number;
  logLength: number;
  lastHeartbeat: number;
}

interface Pulse {
  id: string;
  fromX: number;
  fromY: number;
  toX: number;
  toY: number;
  progress: number;
  type: 'heartbeat' | 'append' | 'vote' | 'commit';
  startTime: number;
}

export function RaftClusterViz({ className = '' }: { className?: string }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animRef = useRef<number>(0);
  const nodesRef = useRef<NodeState[]>([]);
  const pulsesRef = useRef<Pulse[]>([]);
  const phaseRef = useRef<'steady' | 'election' | 'replication'>('steady');
  const phaseTimerRef = useRef(0);
  const tickRef = useRef(0);

  const initNodes = useCallback(() => {
    const cx = 240;
    const cy = 170;
    const r = 110;
    nodesRef.current = [
      { id: 'n1', label: 'Node 1', role: 'leader', x: cx, y: cy - r, term: 3, commitIndex: 42, logLength: 45, lastHeartbeat: 0 },
      { id: 'n2', label: 'Node 2', role: 'follower', x: cx + r * Math.cos(Math.PI / 6), y: cy + r * Math.sin(Math.PI / 6), term: 3, commitIndex: 42, logLength: 44, lastHeartbeat: 0 },
      { id: 'n3', label: 'Node 3', role: 'follower', x: cx - r * Math.cos(Math.PI / 6), y: cy + r * Math.sin(Math.PI / 6), term: 3, commitIndex: 41, logLength: 43, lastHeartbeat: 0 },
    ];
  }, []);

  useEffect(() => {
    initNodes();
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    let lastTime = performance.now();

    const resize = () => {
      const rect = canvas.parentElement?.getBoundingClientRect();
      if (!rect) return;
      canvas.width = rect.width * dpr;
      canvas.height = rect.height * dpr;
      canvas.style.width = `${rect.width}px`;
      canvas.style.height = `${rect.height}px`;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };

    resize();
    window.addEventListener('resize', resize);

    const addPulse = (from: NodeState, to: NodeState, type: Pulse['type']) => {
      pulsesRef.current.push({
        id: `${from.id}-${to.id}-${Date.now()}-${Math.random()}`,
        fromX: from.x, fromY: from.y,
        toX: to.x, toY: to.y,
        progress: 0,
        type,
        startTime: performance.now(),
      });
    };

    const render = (now: number) => {
      const dt = (now - lastTime) / 1000;
      lastTime = now;
      tickRef.current += dt;

      const w = canvas.width / dpr;
      const h = canvas.height / dpr;
      const nodes = nodesRef.current;

      ctx.clearRect(0, 0, w, h);

      // Phase management
      phaseTimerRef.current += dt;
      const phase = phaseRef.current;

      if (phase === 'steady' && phaseTimerRef.current > 4) {
        // Send heartbeats periodically
        const leader = nodes.find(n => n.role === 'leader');
        if (leader) {
          nodes.forEach(n => {
            if (n.role === 'follower') {
              addPulse(leader, n, 'heartbeat');
              n.lastHeartbeat = tickRef.current;
            }
          });
        }
        phaseTimerRef.current = 0;

        // Occasionally trigger election cycle
        if (Math.random() < 0.15) {
          phaseRef.current = 'election';
          phaseTimerRef.current = 0;
        }
        // Occasionally trigger replication
        if (Math.random() < 0.3 && phaseRef.current === 'steady') {
          phaseRef.current = 'replication';
          phaseTimerRef.current = 0;
        }
      }

      if (phase === 'election') {
        if (phaseTimerRef.current < 0.5) {
          // Candidate phase
          const candidate = nodes[1]; // Node 2 becomes candidate
          candidate.role = 'candidate';
          candidate.term += 1;
        } else if (phaseTimerRef.current >= 0.5 && phaseTimerRef.current < 1.0) {
          // Send vote requests
          if (phaseTimerRef.current - dt < 0.5) {
            const candidate = nodes[1];
            nodes.forEach(n => {
              if (n.id !== candidate.id) addPulse(candidate, n, 'vote');
            });
          }
        } else if (phaseTimerRef.current >= 1.5 && phaseTimerRef.current < 2.0) {
          // Vote responses
          if (phaseTimerRef.current - dt < 1.5) {
            const candidate = nodes[1];
            nodes.forEach(n => {
              if (n.id !== candidate.id) addPulse(n, candidate, 'vote');
            });
          }
        } else if (phaseTimerRef.current >= 2.5) {
          // New leader
          nodes[0].role = 'follower';
          nodes[1].role = 'leader';
          nodes[1].term = nodes[1].term;
          nodes.forEach(n => { n.term = nodes[1].term; });
          phaseRef.current = 'steady';
          phaseTimerRef.current = 0;
        }
      }

      if (phase === 'replication') {
        const leader = nodes.find(n => n.role === 'leader');
        if (leader) {
          if (phaseTimerRef.current >= 0.3 && phaseTimerRef.current - dt < 0.3) {
            leader.logLength += 1;
            nodes.forEach(n => {
              if (n.role === 'follower') addPulse(leader, n, 'append');
            });
          }
          if (phaseTimerRef.current >= 1.5 && phaseTimerRef.current - dt < 1.5) {
            nodes.forEach(n => {
              if (n.role === 'follower') {
                n.logLength = leader.logLength;
                addPulse(n, leader, 'commit');
              }
            });
          }
          if (phaseTimerRef.current >= 2.5 && phaseTimerRef.current - dt < 2.5) {
            nodes.forEach(n => { n.commitIndex = leader.logLength; });
          }
          if (phaseTimerRef.current >= 3.0) {
            phaseRef.current = 'steady';
            phaseTimerRef.current = 0;
          }
        } else {
          phaseRef.current = 'steady';
          phaseTimerRef.current = 0;
        }
      }

      // Update pulses
      pulsesRef.current = pulsesRef.current.filter(p => {
        p.progress = Math.min(1, (now - p.startTime) / 800);
        return p.progress < 1;
      });

      const isDark = document.documentElement.classList.contains('dark');

      // Draw connections (thin lines between nodes)
      ctx.strokeStyle = isDark ? 'rgba(255, 255, 255, 0.10)' : 'rgba(0, 0, 0, 0.15)';
      ctx.lineWidth = 1.5;
      for (let i = 0; i < nodes.length; i++) {
        for (let j = i + 1; j < nodes.length; j++) {
          ctx.beginPath();
          ctx.moveTo(nodes[i].x, nodes[i].y);
          ctx.lineTo(nodes[j].x, nodes[j].y);
          ctx.stroke();
        }
      }

      // Draw pulses
      pulsesRef.current.forEach(p => {
        const px = p.fromX + (p.toX - p.fromX) * p.progress;
        const py = p.fromY + (p.toY - p.fromY) * p.progress;
        const alpha = 1 - p.progress * 0.7;

        let color = '20, 224, 197'; // teal
        let radius = 3;
        if (p.type === 'heartbeat') { color = '20, 224, 197'; radius = 2.5; }
        if (p.type === 'append') { color = '99, 179, 237'; radius = 3.5; }
        if (p.type === 'vote') { color = '183, 148, 244'; radius = 3; }
        if (p.type === 'commit') { color = '72, 187, 120'; radius = 3; }

        // Trail
        ctx.strokeStyle = `rgba(${color}, ${alpha * 0.3})`;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(p.fromX, p.fromY);
        ctx.lineTo(px, py);
        ctx.stroke();

        // Dot
        ctx.beginPath();
        ctx.arc(px, py, radius, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${color}, ${alpha})`;
        ctx.fill();

        // Glow
        ctx.beginPath();
        ctx.arc(px, py, radius * 2.5, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${color}, ${alpha * 0.15})`;
        ctx.fill();
      });

      // Draw nodes
      nodes.forEach(node => {
        const isLeader = node.role === 'leader';
        const isCandidate = node.role === 'candidate';
        const nodeRadius = isLeader ? 30 : 26;

        // Outer glow for leader
        if (isLeader) {
          const glowPulse = 0.12 + Math.sin(tickRef.current * 2) * 0.06;
          ctx.beginPath();
          ctx.arc(node.x, node.y, nodeRadius + 12, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(20, 224, 197, ${glowPulse})`;
          ctx.fill();
        }
        if (isCandidate) {
          const glowPulse = 0.15 + Math.sin(tickRef.current * 4) * 0.1;
          ctx.beginPath();
          ctx.arc(node.x, node.y, nodeRadius + 10, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(183, 148, 244, ${glowPulse})`;
          ctx.fill();
        }

        // Node circle
        ctx.beginPath();
        ctx.arc(node.x, node.y, nodeRadius, 0, Math.PI * 2);
        if (isLeader) {
          ctx.fillStyle = isDark ? 'rgba(20, 224, 197, 0.15)' : 'rgba(20, 224, 197, 0.20)';
          ctx.strokeStyle = isDark ? 'rgba(20, 224, 197, 0.6)' : 'rgba(16, 185, 129, 0.8)';
        } else if (isCandidate) {
          ctx.fillStyle = isDark ? 'rgba(183, 148, 244, 0.15)' : 'rgba(183, 148, 244, 0.20)';
          ctx.strokeStyle = isDark ? 'rgba(183, 148, 244, 0.6)' : 'rgba(147, 51, 234, 0.8)';
        } else {
          ctx.fillStyle = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.05)';
          ctx.strokeStyle = isDark ? 'rgba(255, 255, 255, 0.18)' : 'rgba(0, 0, 0, 0.25)';
        }
        ctx.lineWidth = 1.5;
        ctx.fill();
        ctx.stroke();

        // Role label
        ctx.fillStyle = isLeader
          ? (isDark ? '#2dd4bf' : '#047857')
          : isCandidate
          ? (isDark ? '#c084fc' : '#7e22ce')
          : (isDark ? '#94a3b8' : '#475569');
        ctx.font = '600 10px "Space Grotesk", system-ui, -apple-system, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(node.role.toUpperCase(), node.x, node.y - 6);

        // Node ID
        ctx.fillStyle = isDark ? '#f8fafc' : '#0f172a';
        ctx.font = '700 12px "Space Grotesk", system-ui, -apple-system, sans-serif';
        ctx.fillText(node.label, node.x, node.y + 8);

        // Stats below node
        ctx.fillStyle = isDark ? '#94a3b8' : '#475569';
        ctx.font = '600 10px "SF Mono", "Geist Mono", monospace';
        ctx.fillText(`T:${node.term} CI:${node.commitIndex} L:${node.logLength}`, node.x, node.y + nodeRadius + 15);
      });

      // Phase indicator
      const phaseLabel = phase === 'election' ? 'LEADER ELECTION' : phase === 'replication' ? 'LOG REPLICATION' : 'HEARTBEAT';
      const phaseColor = phase === 'election'
        ? (isDark ? '#c084fc' : '#7e22ce')
        : phase === 'replication'
        ? (isDark ? '#60a5fa' : '#2563eb')
        : (isDark ? '#2dd4bf' : '#047857');

      // Status dot
      ctx.beginPath();
      ctx.arc(16, 18, 4, 0, Math.PI * 2);
      ctx.fillStyle = phaseColor;
      ctx.fill();

      ctx.fillStyle = phaseColor;
      ctx.font = '600 11px "Space Grotesk", system-ui, -apple-system, sans-serif';
      ctx.textAlign = 'left';
      ctx.textBaseline = 'top';
      ctx.fillText(phaseLabel, 26, 13);

      animRef.current = requestAnimationFrame(render);
    };

    animRef.current = requestAnimationFrame(render);

    return () => {
      cancelAnimationFrame(animRef.current);
      window.removeEventListener('resize', resize);
    };
  }, [initNodes]);

  return (
    <div className={`relative w-full h-full ${className}`}>
      <canvas ref={canvasRef} className="absolute inset-0 w-full h-full" />
    </div>
  );
}
