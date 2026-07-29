'use client';

import { useState } from 'react';
import { Check, Copy } from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

interface CodeBlockProps {
  code: string;
  language?: string;
  title?: string;
  className?: string;
}

export function CodeBlock({ code, language = 'bash', title, className }: CodeBlockProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(code.trim());
    setCopied(true);
    toast.success('Copied code to clipboard');
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div
      className={cn(
        'rounded-xl border border-[#2A2A2A] bg-[#0D0D0D] overflow-hidden shadow-lg font-mono text-xs my-3',
        className
      )}
    >
      {/* Code Header Bar */}
      <div className="flex items-center justify-between px-4 py-2 bg-[#18181B] border-b border-[#2A2A2A] text-neutral-400 select-none">
        <div className="flex items-center gap-2">
          <span className="h-2.5 w-2.5 rounded-full bg-[#FF5F57] inline-block" />
          <span className="h-2.5 w-2.5 rounded-full bg-[#FEBC2E] inline-block" />
          <span className="h-2.5 w-2.5 rounded-full bg-[#28C840] inline-block" />
          <span className="text-[11px] font-mono text-neutral-400 font-semibold ml-2">
            {title || language}
          </span>
        </div>

        <button
          onClick={handleCopy}
          className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-[#27272A] hover:bg-[#3F3F46] text-neutral-300 hover:text-white text-[11px] transition-colors cursor-pointer border border-[#3F3F46]"
          title="Copy code snippet"
        >
          {copied ? (
            <>
              <Check className="h-3.5 w-3.5 text-emerald-400" />
              <span className="text-emerald-400 font-bold">Copied</span>
            </>
          ) : (
            <>
              <Copy className="h-3 w-3 text-neutral-400" />
              <span>Copy</span>
            </>
          )}
        </button>
      </div>

      {/* Code Body */}
      <pre className="p-4 overflow-x-auto text-neutral-200 bg-[#0D0D0D] font-mono text-xs leading-relaxed scrollbar-thin scrollbar-thumb-neutral-800">
        <code>{code.trim()}</code>
      </pre>
    </div>
  );
}
