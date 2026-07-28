'use client';

import { useEffect, useRef, useState } from 'react';

interface AnimatedCounterProps {
  value: number | string;
  duration?: number;
  className?: string;
  prefix?: string;
  suffix?: string;
}

export function AnimatedCounter({
  value,
  duration = 800,
  className = '',
  prefix = '',
  suffix = '',
}: AnimatedCounterProps) {
  const [displayValue, setDisplayValue] = useState<string>('0');
  const prevValueRef = useRef<number>(0);
  const animationRef = useRef<number>(0);

  useEffect(() => {
    const numericValue = typeof value === 'string' ? parseFloat(value.replace(/,/g, '')) : value;
    
    if (isNaN(numericValue)) {
      setDisplayValue(String(value));
      return;
    }

    const startValue = prevValueRef.current;
    const endValue = numericValue;
    const startTime = performance.now();

    const isDecimal = String(value).includes('.') || endValue % 1 !== 0;
    const decimalPlaces = isDecimal ? (String(value).split('.')[1]?.length || 2) : 0;

    const animate = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
      // Ease-out cubic
      const eased = 1 - Math.pow(1 - progress, 3);
      
      const current = startValue + (endValue - startValue) * eased;
      
      if (isDecimal) {
        setDisplayValue(current.toFixed(decimalPlaces));
      } else {
        setDisplayValue(Math.round(current).toLocaleString());
      }

      if (progress < 1) {
        animationRef.current = requestAnimationFrame(animate);
      } else {
        prevValueRef.current = endValue;
      }
    };

    animationRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [value, duration]);

  return (
    <span className={className}>
      {prefix}{displayValue}{suffix}
    </span>
  );
}
