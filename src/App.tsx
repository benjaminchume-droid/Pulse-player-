import React, { useState, useEffect } from 'react';
import { AudioProvider, useAudio } from './context/AudioContext';
import { Navigation } from './components/Navigation';
import { MiniPlayer } from './components/MiniPlayer';
import { NowPlaying } from './components/NowPlaying';
import { GlowBackground } from './components/GlowBackground';
import { PermissionDialog } from './components/PermissionDialog';
import { Home } from './screens/Home';
import { LibraryScreen } from './screens/Library';
import { SearchScreen } from './screens/Search';
import { PlaylistsScreen } from './screens/Playlists';
import { SettingsScreen } from './screens/Settings';
import { AnimatePresence, motion } from 'motion/react';

type Tab = 'home' | 'library' | 'search' | 'playlists' | 'settings';

const AppContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<Tab>('home');
  const [isPlayerExpanded, setIsPlayerExpanded] = useState(false);
  const [navigationHistory, setNavigationHistory] = useState<Tab[]>(['home']);
  const [exitToast, setExitToast] = useState(false);
  const [lastBackPressedTime, setLastBackPressedTime] = useState(0);

  // Hook tab changes to trace histories
  const handleTabChange = (tab: Tab) => {
    setActiveTab(tab);
    setNavigationHistory(prev => {
      // Avoid duplicate sibling tabs in stack history
      if (prev[prev.length - 1] === tab) return prev;
      return [...prev, tab];
    });
  };

  // Emulate Android Device back-button handling
  useEffect(() => {
    const handleAndroidBackGesture = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        
        // If Player is expanded, minimize/close it first
        if (isPlayerExpanded) {
          setIsPlayerExpanded(false);
          return;
        }

        // If deep navigation stack exists, pop and return
        if (navigationHistory.length > 1) {
          const updatedHistory = [...navigationHistory];
          updatedHistory.pop(); // Remove current
          const previousTab = updatedHistory[updatedHistory.length - 1];
          setNavigationHistory(updatedHistory);
          setActiveTab(previousTab);
        } else {
          // Double back to exit warning logic at landing Home view
          const now = Date.now();
          if (now - lastBackPressedTime < 2000) {
            alert("Exiting Android Emulated Studio Player...");
            window.close();
          } else {
            setLastBackPressedTime(now);
            setExitToast(true);
            setTimeout(() => setExitToast(false), 2000);
          }
        }
      }
    };

    window.addEventListener('keydown', handleAndroidBackGesture);
    return () => {
      window.removeEventListener('keydown', handleAndroidBackGesture);
    };
  }, [isPlayerExpanded, navigationHistory, lastBackPressedTime]);

  const renderScreen = () => {
    switch (activeTab) {
      case 'home': return <Home />;
      case 'library': return <LibraryScreen />;
      case 'search': return <SearchScreen />;
      case 'playlists': return <PlaylistsScreen />;
      case 'settings': return <SettingsScreen />;
      default: return <Home />;
    }
  };

  return (
    <div className="relative w-full h-screen text-[#F5F5F5] overflow-hidden selection:bg-white/20 select-none">
      {/* Cinematic animated glowing backdrop */}
      <GlowBackground />

      {/* Main Screen Router Outlet */}
      <AnimatePresence mode="wait">
        <motion.main
          key={activeTab}
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -15 }}
          transition={{ duration: 0.35, ease: [0.25, 1, 0.5, 1] }}
          className="relative h-full z-10 overflow-hidden"
        >
          {renderScreen()}
        </motion.main>
      </AnimatePresence>

      {/* Persistence Players */}
      <MiniPlayer onExpand={() => setIsPlayerExpanded(true)} />
      
      {/* Expandable Holo Player Overlay */}
      <NowPlaying 
        isOpen={isPlayerExpanded} 
        onClose={() => setIsPlayerExpanded(false)} 
      />

      {/* Floating Bottom Nav controllers */}
      <Navigation 
        activeTab={activeTab} 
        onTabChange={handleTabChange} 
      />

      {/* Android System Permission Alert Dialouge */}
      <PermissionDialog />

      {/* Simulated Android Back Exit Toast */}
      <AnimatePresence>
        {exitToast && (
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 30 }}
            className="fixed bottom-28 left-1/2 -translate-x-1/2 px-5 py-3 rounded-2xl bg-black/90 text-white text-[10px] font-bold uppercase tracking-wider border border-white/5 shadow-2xl z-[150] whitespace-nowrap"
          >
            Press Back (ESC key) again to leave Pulse Player
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default function App() {
  return (
    <AudioProvider>
      <AppContent />
    </AudioProvider>
  );
}
