'use client';

import { motion } from 'framer-motion';
import { Settings } from 'lucide-react';

export default function SettingsPage() {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="flex flex-col items-center justify-center py-32"
    >
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white/[0.06] mb-4">
        <Settings className="h-7 w-7 text-white/40" strokeWidth={1.5} />
      </div>
      <h2 className="text-lg font-semibold text-white/80 mb-1">
        Settings
      </h2>
      <p className="text-sm text-white/30 max-w-md text-center">
        Configure server URL, polling interval, and theme preferences. Coming in the next iteration.
      </p>
    </motion.div>
  );
}
