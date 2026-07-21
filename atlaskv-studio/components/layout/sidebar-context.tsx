'use client';

import { createContext, useContext, useState } from 'react';

const SidebarContext = createContext<{
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  toggle: () => void;
}>({
  isOpen: false,
  setIsOpen: () => {},
  toggle: () => {},
});

export function SidebarProvider({ children }: { children: React.ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const toggle = () => setIsOpen((prev) => !prev);
  return (
    <SidebarContext.Provider value={{ isOpen, setIsOpen, toggle }}>
      {children}
    </SidebarContext.Provider>
  );
}

export const useSidebar = () => useContext(SidebarContext);
