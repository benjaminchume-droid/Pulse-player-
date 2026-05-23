import React from 'react';
import { motion } from 'motion/react';
import { Home, Library, Search, ListMusic, Settings } from 'lucide-react';
import { cn } from '../lib/utils';

type Tab = 'home' | 'library' | 'search' | 'playlists' | 'settings';

interface NavigationProps {
  activeTab: Tab;
  onTabChange: (tab: Tab) => void;
}

export const Navigation: React.FC<NavigationProps> = ({ activeTab, onTabChange }) => {
  const tabs = [
    { id: 'home', icon: Home, label: 'Home' },
    { id: 'library', icon: Library, label: 'Library' },
    { id: 'search', icon: Search, label: 'Search' },
    { id: 'playlists', icon: ListMusic, label: 'Mixes' },
    { id: 'settings', icon: Settings, label: 'Engine' },
  ] as const;

  return (
    <div className="fixed bottom-6 left-0 right-0 z-50 flex justify-center px-6 pointer-events-none select-none">
      <motion.nav 
        initial={{ y: 80, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: "spring", stiffness: 151, damping: 20 }}
        className="glass pointer-events-auto rounded-3xl px-6 py-3.5 flex items-center justify-around gap-2 w-full max-w-md border border-white/8 shadow-2xl relative"
      >
        {tabs.map(({ id, icon: Icon, label }) => {
          const isActive = activeTab === id;
          return (
            <button
              key={id}
              onClick={() => onTabChange(id)}
              className="relative py-2 flex-grow flex flex-col items-center gap-1 cursor-pointer transition-all duration-300"
            >
              <div className="relative">
                <Icon size={21} className={cn(
                  "transition-all duration-300",
                  isActive ? "text-white scale-110" : "text-white/40 hover:text-white/75"
                )} />
                {isActive && (
                  <span className="absolute -inset-2 bg-white/10 rounded-full blur-[6px] -z-10" />
                )}
              </div>
              
              <span className={cn(
                "text-[9px] font-bold uppercase tracking-[0.2em] transition-all duration-300",
                isActive ? "text-white opacity-100 transform translate-y-0" : "text-white/0 opacity-0 transform translate-y-1"
              )}>
                {label}
              </span>

              {isActive && (
                <motion.div 
                  layoutId="activeTabIndicatorGlow"
                  className="absolute bottom-[-10px] w-6 h-[2px] bg-white rounded-full shadow-[0_0_12px_rgba(255,255,255,0.8)]"
                  transition={{ type: 'spring', bounce: 0.15, duration: 0.5 }}
                />
              )}
            </button>
          );
        })}
      </motion.nav>
    </div>
  );
};
