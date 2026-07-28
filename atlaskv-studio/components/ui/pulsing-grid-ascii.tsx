'use client';

import React, { useEffect, useRef, useState } from 'react';

export interface PulsingGridConfig {
  renderMode?: string;
  bgMode?: string;
  bgBlur?: number;
  bgOpacity?: number;
  cellSize?: number;
  coverage?: number;
  invert?: boolean;
  styleBlend?: string;
  charSet?: string;
  customChars?: string;
  brightness?: number;
  contrast?: number;
  edgeEmphasis?: number;
  density?: number;
  toneCurve?: Array<{ x: number; y: number }>;
  tint?: string;
  tintOpacity?: number;
  overlayBlend?: GlobalCompositeOperation;
  saturation?: number;
  grayscale?: number;
  blurType?: string;
  blurAmount?: number;
  blurAngle?: number;
  pfx?: {
    vignette?: { enabled: boolean; intensity: number };
    scanLines?: { enabled: boolean; intensity: number };
    chromatic?: { enabled: boolean; intensity: number };
    bloom?: { enabled: boolean; intensity: number };
    filmGrain?: { enabled: boolean; intensity: number };
    glitch?: { enabled: boolean; intensity: number };
    pixelate?: { enabled: boolean; intensity: number };
    halftone?: { enabled: boolean; intensity: number };
    filmDust?: { enabled: boolean; intensity: number };
  };
  animated?: boolean;
  animStyle?: string;
  animSpeed?: { enabled: boolean; intensity: number };
  animIntensity?: { enabled: boolean; intensity: number };
  lights?: {
    enabled: boolean;
    points: Array<{ x: number; y: number; radius: number; intensity: number }>;
  };
  mask?: {
    enabled: boolean;
    dataUrl?: string | null;
    invert?: boolean;
  };
  imageSrc?: string;
}

export const DEFAULT_PULSING_GRID_CONFIG: PulsingGridConfig = {
  renderMode: 'lego',
  bgMode: 'solid',
  bgBlur: 12,
  bgOpacity: 90,
  cellSize: 18,
  coverage: 100,
  invert: false,
  styleBlend: 'source-over',
  charSet: 'standard',
  customChars: '',
  brightness: 0,
  contrast: 125,
  edgeEmphasis: 0,
  density: 0,
  toneCurve: [
    { x: 0, y: 0 },
    { x: 1, y: 1 },
  ],
  tint: '#3ca6ff',
  tintOpacity: 0,
  overlayBlend: 'multiply',
  saturation: 100,
  grayscale: 0,
  blurType: 'off',
  blurAmount: 35,
  blurAngle: 0,
  pfx: {
    vignette: { enabled: true, intensity: 30 },
    scanLines: { enabled: false, intensity: 40 },
    chromatic: { enabled: false, intensity: 15 },
    bloom: { enabled: true, intensity: 25 },
    filmGrain: { enabled: false, intensity: 30 },
    glitch: { enabled: false, intensity: 20 },
    pixelate: { enabled: false, intensity: 15 },
    halftone: { enabled: false, intensity: 20 },
    filmDust: { enabled: false, intensity: 20 },
  },
  animated: true,
  animStyle: 'pulse',
  animSpeed: { enabled: true, intensity: 100 },
  animIntensity: { enabled: true, intensity: 60 },
  lights: { enabled: false, points: [] },
  mask: { enabled: false, invert: false, dataUrl: null },
  imageSrc: '/ascii-editor/demos/generated/ref-003.webp',
};

const CHAR_SETS: Record<string, string> = {
  standard: ' .:-=+*#%@',
  dense: ' .`^\",:;Il!i~+_-?][}{1)(|\\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$',
  binary: '01',
  hex: '0123456789ABCDEF',
  blocks: ' ░▒▓█',
  minimal: ' .oO@',
};

export function PulsingGridAscii({
  config = {},
  className = '',
}: {
  config?: Partial<PulsingGridConfig>;
  className?: string;
}) {
  const cfg = { ...DEFAULT_PULSING_GRID_CONFIG, ...config };
  const pfx = { ...DEFAULT_PULSING_GRID_CONFIG.pfx, ...config.pfx };

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animFrameRef = useRef<number>(0);
  const [sourceImg, setSourceImg] = useState<HTMLImageElement | null>(null);

  // Load ref-003.webp or generate exact monochrome landscape dunes artwork
  useEffect(() => {
    if (cfg.imageSrc) {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.src = cfg.imageSrc;
      img.onload = () => setSourceImg(img);
      img.onerror = () => {
        const offCanvas = document.createElement('canvas');
        offCanvas.width = 1200;
        offCanvas.height = 700;
        const ctx = offCanvas.getContext('2d');
        if (ctx) {
          drawOceanWavesRef003(ctx, 1200, 700);
          const fallbackImg = new Image();
          fallbackImg.src = offCanvas.toDataURL();
          fallbackImg.onload = () => setSourceImg(fallbackImg);
        }
      };
    }
  }, [cfg.imageSrc]);

  // Canvas Render Loop
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    if (!ctx) return;

    const startTime = performance.now();
    const bufferCanvas = document.createElement('canvas');
    const bufferCtx = bufferCanvas.getContext('2d', { willReadFrequently: true });

    const render = () => {
      const w = canvas.parentElement?.clientWidth || 800;
      const h = canvas.parentElement?.clientHeight || 500;

      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }

      const time = (performance.now() - startTime) * 0.001;
      const speedMult = cfg.animSpeed?.enabled ? cfg.animSpeed.intensity / 100 : 1.0;
      const animTime = time * speedMult;
      const intensityVal = cfg.animIntensity?.enabled ? cfg.animIntensity.intensity / 100 : 0.6;

      // Draw photo / dunes onto buffer canvas
      bufferCanvas.width = w;
      bufferCanvas.height = h;

      if (bufferCtx) {
        bufferCtx.clearRect(0, 0, w, h);
        if (sourceImg) {
          const imgAspect = sourceImg.width / sourceImg.height;
          const canvasAspect = w / h;
          let dw = w;
          let dh = h;
          let dx = 0;
          let dy = 0;
          if (canvasAspect > imgAspect) {
            dh = w / imgAspect;
            dy = (h - dh) / 2;
          } else {
            dw = h * imgAspect;
            dx = (w - dw) / 2;
          }
          bufferCtx.drawImage(sourceImg, dx, dy, dw, dh);
        } else {
          drawOceanWavesRef003(bufferCtx, w, h);
        }
      }

      // Step 1: Background Fill
      ctx.clearRect(0, 0, w, h);
      if (cfg.bgMode === 'solid') {
        ctx.fillStyle = '#0f172a';
        ctx.fillRect(0, 0, w, h);
      } else if (cfg.bgMode === 'photo' && bufferCanvas) {
        ctx.drawImage(bufferCanvas, 0, 0);
      }

      // Step 2: Sample Grid Data
      const cellSize = Math.max(8, cfg.cellSize || 18);
      const cols = Math.ceil(w / cellSize);
      const rows = Math.ceil(h / cellSize);

      let imgData: ImageData | null = null;
      if (bufferCtx) {
        try {
          imgData = bufferCtx.getImageData(0, 0, w, h);
        } catch {
          imgData = null;
        }
      }

      const contrastVal = cfg.contrast ?? 125;
      const contrastFactor = (259 * (contrastVal + 255)) / (255 * (259 - contrastVal));
      const brightnessAdd = cfg.brightness ?? 0;
      const invert = cfg.invert ?? false;
      const renderMode = cfg.renderMode || 'lego';

      // Step 3: Render Lego Grid Cells (Pixel-perfect match for ref-003 screenshot)
      for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
          if (cfg.coverage && cfg.coverage < 100) {
            const seed = (c * 374761393 + r * 668265263) % 100;
            if (seed > cfg.coverage) continue;
          }

          const cx = c * cellSize;
          const cy = r * cellSize;

          let sumR = 0;
          let sumG = 0;
          let sumB = 0;
          let samples = 0;

          if (imgData) {
            const step = Math.max(1, Math.floor(cellSize / 3));
            for (let sy = 0; sy < cellSize; sy += step) {
              for (let sx = 0; sx < cellSize; sx += step) {
                const px = Math.min(w - 1, cx + sx);
                const py = Math.min(h - 1, cy + sy);
                const idx = (py * w + px) * 4;
                sumR += imgData.data[idx];
                sumG += imgData.data[idx + 1];
                sumB += imgData.data[idx + 2];
                samples++;
              }
            }
          }

          let avgR = samples > 0 ? sumR / samples : 200;
          let avgG = samples > 0 ? sumG / samples : 205;
          let avgB = samples > 0 ? sumB / samples : 215;

          // Apply Contrast & Brightness
          avgR = Math.min(255, Math.max(0, contrastFactor * (avgR - 128) + 128 + brightnessAdd));
          avgG = Math.min(255, Math.max(0, contrastFactor * (avgG - 128) + 128 + brightnessAdd));
          avgB = Math.min(255, Math.max(0, contrastFactor * (avgB - 128) + 128 + brightnessAdd));

          let lum = 0.299 * avgR + 0.587 * avgG + 0.114 * avgB;
          let normLum = Math.min(1, Math.max(0, lum / 255));
          if (invert) normLum = 1 - normLum;

          // Spatial pulse wave animation
          let animPulse = 0;
          if (cfg.animated) {
            const distFromCenter = Math.hypot(cx + cellSize / 2 - w / 2, cy + cellSize / 2 - h / 2);
            if (cfg.animStyle === 'pulse') {
              animPulse = Math.sin(distFromCenter * 0.012 - animTime * 3) * intensityVal * 0.2;
            } else if (cfg.animStyle === 'wave') {
              animPulse = Math.sin((cx + cy) * 0.015 + animTime * 4) * intensityVal * 0.2;
            } else if (cfg.animStyle === 'shimmer') {
              animPulse = Math.sin(c * 0.5 + r * 0.3 + animTime * 5) * intensityVal * 0.18;
            } else {
              animPulse = Math.cos(distFromCenter * 0.02 - animTime * 4) * intensityVal * 0.2;
            }
          }

          const effectiveLum = Math.min(1, Math.max(0, normLum + animPulse));

          // Draw Shape per renderMode
          if (renderMode === 'lego') {
            const tileR = Math.round(avgR);
            const tileG = Math.round(avgG);
            const tileB = Math.round(avgB);

            // Square Cell Base
            ctx.fillStyle = `rgb(${tileR}, ${tileG}, ${tileB})`;
            ctx.fillRect(cx, cy, cellSize, cellSize);

            // Thin Dark Cell Border / Grid Line
            ctx.strokeStyle = 'rgba(0, 0, 0, 0.4)';
            ctx.lineWidth = 1;
            ctx.strokeRect(cx + 0.5, cy + 0.5, cellSize - 1, cellSize - 1);

            // Center Lego Stud Dot
            const studX = cx + cellSize / 2;
            const studY = cy + cellSize / 2;
            const studRadius = Math.max(1.5, cellSize * 0.22 * (0.8 + effectiveLum * 0.35));

            // Stud shading & 3D gradient
            const studGrad = ctx.createRadialGradient(
              studX - studRadius * 0.3,
              studY - studRadius * 0.3,
              0.5,
              studX,
              studY,
              studRadius
            );

            if (normLum > 0.45) {
              // Light cell (sky/snow) -> Bright stud with subtle border shadow
              studGrad.addColorStop(0, '#ffffff');
              studGrad.addColorStop(0.7, `rgb(${Math.round(tileR * 0.9)}, ${Math.round(tileG * 0.9)}, ${Math.round(tileB * 0.9)})`);
              studGrad.addColorStop(1, `rgb(${Math.round(tileR * 0.65)}, ${Math.round(tileG * 0.65)}, ${Math.round(tileB * 0.65)})`);
            } else {
              // Dark cell (dunes/mountains) -> Highlighted central dot
              const hlR = Math.min(255, Math.round(tileR * 1.5 + 35));
              const hlG = Math.min(255, Math.round(tileG * 1.5 + 35));
              const hlB = Math.min(255, Math.round(tileB * 1.5 + 35));
              studGrad.addColorStop(0, `rgb(${hlR}, ${hlG}, ${hlB})`);
              studGrad.addColorStop(0.8, `rgb(${tileR}, ${tileG}, ${tileB})`);
              studGrad.addColorStop(1, `rgb(${Math.max(0, tileR - 20)}, ${Math.max(0, tileG - 20)}, ${Math.max(0, tileB - 20)})`);
            }

            ctx.beginPath();
            ctx.arc(studX, studY, studRadius, 0, Math.PI * 2);
            ctx.fillStyle = studGrad;
            ctx.fill();

          } else if (renderMode === 'characters') {
            const set = CHAR_SETS[cfg.charSet || 'standard'] || CHAR_SETS.standard;
            const charIdx = Math.floor(effectiveLum * (set.length - 1));
            const char = set[charIdx] || set[0];

            ctx.fillStyle = `rgb(${Math.round(avgR)}, ${Math.round(avgG)}, ${Math.round(avgB)})`;
            ctx.font = `bold ${Math.floor(cellSize * 0.85)}px monospace`;
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(char, cx + cellSize / 2, cy + cellSize / 2);

          } else if (renderMode === 'matrix') {
            const matrixChars = '0123456789ABCDEF@#$%';
            const charIdx = Math.floor((c * 7 + r * 13 + animTime * 8) % matrixChars.length);
            const char = matrixChars[charIdx];

            ctx.fillStyle = effectiveLum > 0.8 ? '#ffffff' : `rgb(34, 211, 238)`;
            ctx.font = `bold ${Math.floor(cellSize * 0.85)}px monospace`;
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(char, cx + cellSize / 2, cy + cellSize / 2);

          } else {
            // Dots / Voxels
            const r = (cellSize / 2) * effectiveLum;
            ctx.beginPath();
            ctx.arc(cx + cellSize / 2, cy + cellSize / 2, Math.max(1, r), 0, Math.PI * 2);
            ctx.fillStyle = `rgb(${Math.round(avgR)}, ${Math.round(avgG)}, ${Math.round(avgB)})`;
            ctx.fill();
          }
        }
      }

      // Step 4: Post-Processing Effects
      // Vignette
      if (pfx.vignette?.enabled) {
        const intensity = (pfx.vignette.intensity || 30) / 100;
        const outerRad = Math.hypot(w / 2, h / 2);
        const vigGrad = ctx.createRadialGradient(w / 2, h / 2, outerRad * 0.4, w / 2, h / 2, outerRad);
        vigGrad.addColorStop(0, 'rgba(0, 0, 0, 0)');
        vigGrad.addColorStop(1, `rgba(0, 0, 0, ${intensity * 0.95})`);
        ctx.fillStyle = vigGrad;
        ctx.fillRect(0, 0, w, h);
      }

      // Bloom Soft Glow
      if (pfx.bloom?.enabled) {
        const intensity = (pfx.bloom.intensity || 25) / 100;
        ctx.save();
        ctx.globalCompositeOperation = 'screen';
        ctx.globalAlpha = intensity * 0.3;
        ctx.filter = 'blur(10px)';
        ctx.drawImage(canvas, 0, 0);
        ctx.restore();
      }

      animFrameRef.current = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animFrameRef.current);
    };
  }, [cfg, pfx, sourceImg]);

  return (
    <div className={`relative w-full h-full min-h-[500px] overflow-hidden ${className}`}>
      <canvas ref={canvasRef} className="absolute inset-0 w-full h-full object-cover block" />
    </div>
  );
}

// Draw ref-003 ocean waves artwork matching the reference photo silver monochrome ocean swells composition
function drawOceanWavesRef003(ctx: CanvasRenderingContext2D, width: number, height: number) {
  // 1. Sky & Horizon Water (top 38%)
  const skyGrad = ctx.createLinearGradient(0, 0, 0, height * 0.4);
  skyGrad.addColorStop(0, '#ffffff');
  skyGrad.addColorStop(0.5, '#e2e8f0');
  skyGrad.addColorStop(1, '#cbd5e1');
  ctx.fillStyle = skyGrad;
  ctx.fillRect(0, 0, width, height);

  // Distant Ocean Horizon Water Ripples
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.6)';
  ctx.lineWidth = 1.5;
  for (let i = 0; i < 5; i++) {
    const y = height * 0.25 + i * 8;
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.quadraticCurveTo(width * 0.5, y - 4, width, y + 2);
    ctx.stroke();
  }

  // 2. Distant Wave Ridge
  ctx.fillStyle = '#94a3b8';
  ctx.beginPath();
  ctx.moveTo(0, height * 0.38);
  ctx.bezierCurveTo(width * 0.3, height * 0.3, width * 0.7, height * 0.46, width, height * 0.34);
  ctx.lineTo(width, height);
  ctx.lineTo(0, height);
  ctx.closePath();
  ctx.fill();

  // 3. Main Rolling Ocean Swell (Cresting Wave 1)
  const waveGrad1 = ctx.createLinearGradient(0, height * 0.3, width, height * 0.8);
  waveGrad1.addColorStop(0, '#475569');
  waveGrad1.addColorStop(0.5, '#334155');
  waveGrad1.addColorStop(1, '#1e293b');
  ctx.fillStyle = waveGrad1;
  ctx.beginPath();
  ctx.moveTo(0, height * 0.46);
  ctx.bezierCurveTo(width * 0.25, height * 0.36, width * 0.48, height * 0.56, width * 0.82, height * 0.4);
  ctx.bezierCurveTo(width * 0.92, height * 0.38, width * 0.97, height * 0.44, width, height * 0.46);
  ctx.lineTo(width, height);
  ctx.lineTo(0, height);
  ctx.closePath();
  ctx.fill();

  // White Foam / Crest Highlight along Wave 1
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.9)';
  ctx.lineWidth = 5;
  ctx.shadowColor = 'rgba(255, 255, 255, 0.8)';
  ctx.shadowBlur = 6;
  ctx.beginPath();
  ctx.moveTo(0, height * 0.46);
  ctx.bezierCurveTo(width * 0.25, height * 0.36, width * 0.48, height * 0.56, width * 0.82, height * 0.4);
  ctx.stroke();
  ctx.shadowBlur = 0; // Reset shadow

  // 4. Foreground Sweeping Ocean Wave Swell 2 (Deep Water Trough)
  const waveGrad2 = ctx.createLinearGradient(0, height * 0.5, width, height);
  waveGrad2.addColorStop(0, '#1e293b');
  waveGrad2.addColorStop(0.5, '#0f172a');
  waveGrad2.addColorStop(1, '#020617');
  ctx.fillStyle = waveGrad2;
  ctx.beginPath();
  ctx.moveTo(0, height * 0.68);
  ctx.bezierCurveTo(width * 0.3, height * 0.52, width * 0.65, height * 0.8, width, height * 0.62);
  ctx.lineTo(width, height);
  ctx.lineTo(0, height);
  ctx.closePath();
  ctx.fill();

  // Silver Foam Crest Highlight along Foreground Wave 2
  ctx.strokeStyle = 'rgba(226, 232, 240, 0.75)';
  ctx.lineWidth = 4;
  ctx.beginPath();
  ctx.moveTo(0, height * 0.68);
  ctx.bezierCurveTo(width * 0.3, height * 0.52, width * 0.65, height * 0.8, width, height * 0.62);
  ctx.stroke();

  // 5. Liquid Water Ripple Highlights across wave surface
  ctx.strokeStyle = 'rgba(241, 245, 249, 0.25)';
  ctx.lineWidth = 2;
  for (let k = 0; k < 6; k++) {
    const rx = width * (0.15 + k * 0.12);
    const ry = height * (0.5 + k * 0.04);
    ctx.beginPath();
    ctx.moveTo(rx - 60, ry + 20);
    ctx.quadraticCurveTo(rx, ry - 10, rx + 60, ry + 15);
    ctx.stroke();
  }
}
