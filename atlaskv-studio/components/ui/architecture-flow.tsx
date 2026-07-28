'use client';

import { useEffect, useRef } from 'react';

const STEPS = [
  { label: 'Client', sub: 'PUT /keys/user:1', color: '255, 255, 255' },
  { label: 'Leader', sub: 'Append to Log', color: '20, 224, 197' },
  { label: 'Followers', sub: 'Replicate Entry', color: '99, 179, 237' },
  { label: 'Quorum', sub: 'Majority ACK', color: '72, 187, 120' },
  { label: 'Commit', sub: 'Advance commitIndex', color: '183, 148, 244' },
  { label: 'State Machine', sub: 'Apply Command', color: '236, 201, 75' },
  { label: 'Storage', sub: 'ConcurrentHashMap', color: '237, 137, 54' },
  { label: 'Response', sub: '200 OK', color: '72, 187, 120' },
];

export function ArchitectureFlow({ className = '' }: { className?: string }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animRef = useRef<number>(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;

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

    const render = (now: number) => {
      const t = now * 0.001;
      const w = canvas.width / dpr;
      const h = canvas.height / dpr;

      ctx.clearRect(0, 0, w, h);

      const stepHeight = h / (STEPS.length + 1);
      const centerX = w / 2;

      // Draw each step
      STEPS.forEach((step, i) => {
        const y = stepHeight * (i + 1);
        const cycleT = ((t * 0.4 + i * 0.12) % 1);
        const isActive = cycleT > 0.2 && cycleT < 0.8;
        const alpha = isActive ? 0.9 : 0.35;

        // Connector line to next
        if (i < STEPS.length - 1) {
          const nextY = stepHeight * (i + 2);
          ctx.strokeStyle = `rgba(${step.color}, 0.08)`;
          ctx.lineWidth = 1;
          ctx.setLineDash([3, 4]);
          ctx.beginPath();
          ctx.moveTo(centerX, y + 14);
          ctx.lineTo(centerX, nextY - 14);
          ctx.stroke();
          ctx.setLineDash([]);

          // Traveling dot on the connector
          if (isActive) {
            const dotProgress = (cycleT - 0.2) / 0.6;
            const dotY = y + 14 + (nextY - y - 28) * dotProgress;
            ctx.beginPath();
            ctx.arc(centerX, dotY, 2, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(${step.color}, ${0.8 - dotProgress * 0.5})`;
            ctx.fill();
          }
        }

        // Step label
        ctx.fillStyle = `rgba(${step.color}, ${alpha})`;
        ctx.font = `600 12px "Space Grotesk", system-ui, -apple-system, sans-serif`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(step.label, centerX, y - 4);

        // Sub label
        ctx.fillStyle = `rgba(255, 255, 255, ${alpha * 0.35})`;
        ctx.font = `400 9px "SF Mono", "Geist Mono", monospace`;
        ctx.fillText(step.sub, centerX, y + 10);

        // Active indicator dot
        if (isActive) {
          ctx.beginPath();
          ctx.arc(centerX - 50, y, 2.5, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(${step.color}, 0.7)`;
          ctx.fill();
        }
      });

      animRef.current = requestAnimationFrame(render);
    };

    animRef.current = requestAnimationFrame(render);

    return () => {
      cancelAnimationFrame(animRef.current);
      window.removeEventListener('resize', resize);
    };
  }, []);

  return (
    <div className={`relative w-full h-full ${className}`}>
      <canvas ref={canvasRef} className="absolute inset-0 w-full h-full" />
    </div>
  );
}
