'use client';

import { useState } from 'react';
import { Settings, Server, Sliders, Moon, Shield, Save, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export default function SettingsPage() {
  const [endpoint, setEndpoint] = useState('');
  const [timeoutMs, setTimeoutMs] = useState('5000');
  const [refreshIntervalSec, setRefreshIntervalSec] = useState('2');
  const [themeMode, setThemeMode] = useState('dark-modern');
  const [saved, setSaved] = useState(false);

  const handleSave = () => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('atlaskv-server-url', endpoint);
    }
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
          <Settings className="h-5 w-5 text-emerald-400" />
          Studio Settings & Connection
        </h1>
        <p className="text-xs text-zinc-400 mt-1">
          Configure backend API endpoints, Raft cluster connections, theme aesthetics, and polling frequencies
        </p>
      </div>

      {/* Connection Settings Card */}
      <div className="p-6 rounded-xl border border-white/[0.08] bg-zinc-900/50 backdrop-blur-md space-y-4">
        <div className="flex items-center gap-2 border-b border-white/[0.08] pb-3">
          <Server className="h-4 w-4 text-emerald-400" />
          <h2 className="text-sm font-semibold text-white">Cluster REST Endpoint</h2>
        </div>

        <div className="space-y-4 text-xs">
          <div className="space-y-1.5">
            <label className="font-semibold text-zinc-300 font-mono">AtlasKV Server Base URL</label>
            <Input
              value={endpoint}
              onChange={(e) => setEndpoint(e.target.value)}
              placeholder="http://localhost:8081"
              className="bg-zinc-950 border-white/10 text-xs font-mono text-zinc-200"
            />
            <p className="text-[11px] text-zinc-500">
              Enter any cluster node endpoint. The SDK/Studio will automatically handle 503 Leader Redirection.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="font-semibold text-zinc-300 font-mono">HTTP Request Timeout (ms)</label>
              <Input
                value={timeoutMs}
                onChange={(e) => setTimeoutMs(e.target.value)}
                className="bg-zinc-950 border-white/10 text-xs font-mono text-zinc-200"
              />
            </div>

            <div className="space-y-1.5">
              <label className="font-semibold text-zinc-300 font-mono">Metrics Auto-Refresh (seconds)</label>
              <Input
                value={refreshIntervalSec}
                onChange={(e) => setRefreshIntervalSec(e.target.value)}
                className="bg-zinc-950 border-white/10 text-xs font-mono text-zinc-200"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Theme & Aesthetics Card */}
      <div className="p-6 rounded-xl border border-white/[0.08] bg-zinc-900/50 backdrop-blur-md space-y-4">
        <div className="flex items-center gap-2 border-b border-white/[0.08] pb-3">
          <Moon className="h-4 w-4 text-purple-400" />
          <h2 className="text-sm font-semibold text-white">Theme & UI Preferences</h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {[
            { id: 'dark-modern', title: 'Grafana Dark', desc: '#09090b deep dark mode with emerald accents' },
            { id: 'dark-oled', title: 'OLED Black', desc: '#000000 true black contrast for AMOLED screens' },
            { id: 'slate', title: 'Slate Tech', desc: '#0f172a slate blue theme with cyan accents' },
          ].map((theme) => (
            <button
              key={theme.id}
              onClick={() => setThemeMode(theme.id)}
              className={`p-4 rounded-xl border text-left transition-all ${
                themeMode === theme.id
                  ? 'bg-emerald-500/10 border-emerald-500/40 text-white shadow-lg shadow-emerald-500/5'
                  : 'bg-zinc-950/60 border-white/10 text-zinc-400 hover:text-zinc-200 hover:border-white/20'
              }`}
            >
              <div className="font-semibold text-xs mb-1 flex items-center justify-between">
                {theme.title}
                {themeMode === theme.id && <Check className="h-3.5 w-3.5 text-emerald-400" />}
              </div>
              <p className="text-[11px] text-zinc-500">{theme.desc}</p>
            </button>
          ))}
        </div>
      </div>

      {/* Save Button Bar */}
      <div className="flex items-center justify-end gap-3 pt-2">
        <Button
          onClick={handleSave}
          className="bg-emerald-500 hover:bg-emerald-600 text-zinc-950 font-semibold text-xs px-5 py-2.5 shadow-lg shadow-emerald-500/20 gap-2"
        >
          {saved ? <Check className="h-4 w-4" /> : <Save className="h-4 w-4" />}
          {saved ? 'Settings Saved!' : 'Save Preferences'}
        </Button>
      </div>
    </div>
  );
}
