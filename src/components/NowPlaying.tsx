import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  ChevronDown, Play, Pause, SkipForward, SkipBack, Shuffle, Repeat, 
  Volume2, Settings2, Sparkles, HelpCircle, Bluetooth, Heart, Share2, Music2
} from 'lucide-react';
import { useAudio } from '../context/AudioContext';
import { cn, formatDuration } from '../lib/utils';
import { EqualizerPanel } from './EqualizerPanel';

interface NowPlayingProps {
  isOpen: boolean;
  onClose: () => void;
}

export const NowPlaying: React.FC<NowPlayingProps> = ({ isOpen, onClose }) => {
  const { 
    currentSong, status, progress, duration, resume, pause, next, previous, seek, 
    volume, setVolume, toggleFavorite, premiumTheme, triggerAIAssistant, aiThinking,
    bluetoothDevice
  } = useAudio();

  const [activeTab, setActiveTab] = useState<'record' | 'lyrics' | 'eq'>('record');
  const [prompt, setPrompt] = useState('');
  const [aiResponse, setAiResponse] = useState<string | null>(null);
  const lyricsContainerRef = useRef<HTMLDivElement | null>(null);

  // Auto-scroll lyrics
  useEffect(() => {
    if (activeTab === 'lyrics' && lyricsContainerRef.current) {
      const activeElement = lyricsContainerRef.current.querySelector('.lyric-active');
      if (activeElement) {
        activeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }, [progress, activeTab]);

  if (!currentSong) return null;

  const currentSyncedLyric = currentSong.syncedLyrics?.find((l, i, arr) => {
    const nextLine = arr[i + 1];
    return progress >= l.time && (!nextLine || progress < nextLine.time);
  });

  const handleAskAIDJ = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim()) return;
    const response = await triggerAIAssistant(prompt);
    setAiResponse(response);
    setPrompt('');
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ y: '100%' }}
          animate={{ y: 0 }}
          exit={{ y: '100%' }}
          transition={{ type: 'spring', damping: 24, stiffness: 220 }}
          className="fixed inset-0 z-[60] bg-[#020202] text-[#F5F5F5] flex flex-col justify-between overflow-hidden"
        >
          {/* Real-time Dynamic Blur Mesh Background */}
          <div className="absolute inset-0 z-0 overflow-hidden pointer-events-none">
            {currentSong.coverArt ? (
              <img 
                src={currentSong.coverArt} 
                alt="" 
                className="w-full h-full object-cover opacity-25 blur-[120px] scale-150 transition-all duration-1000"
              />
            ) : (
              <div className="w-full h-full bg-gradient-to-tr from-purple-950/20 via-black to-cyan-950/20 blur-[120px]" />
            )}
            {/* Pulsing neon layers matching premium theme */}
            <div className={cn(
              "absolute inset-0 opacity-10 blur-[80px] transition-all duration-1000",
              premiumTheme === 'cyberpunk' && "bg-gradient-to-br from-cyan-500 to-pink-500",
              premiumTheme === 'celestial' && "bg-gradient-to-br from-white via-zinc-400 to-slate-200",
              premiumTheme === 'neon-city' && "bg-gradient-to-br from-emerald-500 to-lime-500",
              premiumTheme === 'cosmic' && "bg-gradient-to-br from-purple-500 to-indigo-500"
            )} />
          </div>

          {/* Header Action Bar */}
          <header className="relative z-10 flex items-center justify-between px-6 pt-10 pb-4">
            <button 
              onClick={onClose} 
              className="p-2.5 rounded-full bg-white/5 hover:bg-white/10 active:scale-90 transition-all cursor-pointer"
            >
              <ChevronDown size={22} />
            </button>

            <div className="flex gap-2 p-1 bg-white/5 rounded-full backdrop-blur-md">
              {(['record', 'lyrics', 'eq'] as const).map(tab => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={cn(
                    "px-4 py-1.5 rounded-full text-[10px] font-bold uppercase tracking-[0.2em] transition-all cursor-pointer",
                    activeTab === tab ? "bg-white text-black" : "text-white/40 hover:text-white/70"
                  )}
                >
                  {tab === 'record' ? 'Player' : tab === 'lyrics' ? 'Lyrics' : 'Equalizer'}
                </button>
              ))}
            </div>

            <button className="p-2.5 rounded-full bg-white/5 hover:bg-white/10 transition-all cursor-pointer relative">
              <span className="w-1.5 h-1.5 bg-cyan-400 absolute top-2 right-2 rounded-full animate-ping" />
              <Bluetooth size={20} className="text-white/80" />
            </button>
          </header>

          {/* Centerpiece Screens Switcher */}
          <div className="relative z-10 flex-1 flex flex-col justify-center items-center px-6">
            <AnimatePresence mode="wait">
              {activeTab === 'record' && (
                <motion.div 
                  key="recordTab"
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 1.05 }}
                  className="w-full max-w-sm flex flex-col items-center flex-1 justify-center gap-10"
                >
                  {/* Rotating Vinyl/Album Cover */}
                  <div className="relative w-72 h-72">
                    {/* Breathing Dynamic Ambient Aura */}
                    <div className={cn(
                      "absolute inset-[5%] rounded-full blur-[40px] opacity-40 transition-all duration-1000",
                      status === 'playing' ? "scale-105 animate-pulse" : "scale-100",
                      premiumTheme === 'cyberpunk' && "shadow-[0_0_100px_#f72585]",
                      premiumTheme === 'celestial' && "shadow-[0_0_100px_rgba(255,255,255,0.4)]",
                      premiumTheme === 'neon-city' && "shadow-[0_0_100px_#00b4d8]"
                    )} />

                    {/* Gloss Shield Card Disc */}
                    <div className="w-full h-full rounded-full border border-white/10 p-[6px] glass relative overflow-hidden flex items-center justify-center">
                      <div className="absolute inset-0 mesh-bg opacity-30" />
                      
                      {/* Inner Disk Rotating Body */}
                      <div 
                        className={cn(
                          "w-full h-full rounded-full overflow-hidden border border-white/8 transition-transform",
                          status === 'playing' ? "animate-spin" : ""
                        )}
                        style={{ animationDuration: '20s', animationTimingFunction: 'linear' }}
                      >
                        {currentSong.coverArt ? (
                          <img src={currentSong.coverArt} alt="" className="w-full h-full object-cover" />
                        ) : (
                          <div className="w-full h-full bg-zinc-900 flex flex-col justify-center items-center text-center">
                            <span className="text-6xl font-thin text-white/5 font-display tracking-[0.2em]">PULSE</span>
                          </div>
                        )}
                      </div>

                      {/* Vinyl Hole Shield */}
                      <div className="absolute w-12 h-12 rounded-full bg-[#020202] border border-white/20 flex items-center justify-center z-10">
                        <div className="w-3.5 h-3.5 rounded-full bg-[#111] border border-white/40 shadow-inner" />
                      </div>
                    </div>

                    <div className="absolute -top-3 -right-3 py-1 px-3 bg-white text-black text-[9px] font-extrabold uppercase tracking-[0.25em] rounded-full shadow-2xl z-20">
                      LOSSLESS HD
                    </div>
                  </div>

                  {/* Audio Device Connected Status Bar */}
                  {bluetoothDevice && (
                    <div className="px-4 py-1.5 rounded-full bg-white/5 border border-white/5 text-[9px] text-white/40 font-bold uppercase tracking-[0.25em] flex items-center gap-2">
                      <Bluetooth size={10} className="text-cyan-400" />
                      Streaming on: <span className="text-white">{bluetoothDevice}</span>
                    </div>
                  )}

                  {/* Song Metadata Titles */}
                  <div className="text-center space-y-2">
                    <h2 className="text-3xl font-light tracking-tight font-display text-glow truncate max-w-sm">
                      {currentSong.title}
                    </h2>
                    <p className="text-xs font-bold text-white/30 uppercase tracking-[0.3em]">
                      {currentSong.artist}
                    </p>
                  </div>
                </motion.div>
              )}

              {activeTab === 'lyrics' && (
                <motion.div 
                  key="lyricsTab"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="w-full max-w-md h-[420px] overflow-y-auto no-scrollbar py-6 flex flex-col gap-6 relative"
                  ref={lyricsContainerRef}
                >
                  {currentSong.syncedLyrics ? (
                    currentSong.syncedLyrics.map((lyric, index) => {
                      const isActive = currentSyncedLyric?.time === lyric.time;
                      return (
                        <p
                          key={index}
                          onClick={() => seek(lyric.time)}
                          className={cn(
                            "lyric-line text-lg md:text-xl font-medium tracking-tight py-1 transition-all duration-300 cursor-pointer",
                            isActive 
                              ? "text-white text-glow font-bold scale-[1.02] lyric-active" 
                              : "text-white/20 hover:text-white/50"
                          )}
                        >
                          {lyric.text}
                        </p>
                      );
                    })
                  ) : (
                    <div className="h-full flex items-center justify-center text-center text-white/40 font-light">
                      <p className="text-sm font-bold uppercase tracking-widest">No embedded lyrics available</p>
                    </div>
                  )}
                </motion.div>
              )}

              {activeTab === 'eq' && (
                <motion.div 
                  key="eqTab"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  className="w-full max-w-sm"
                >
                  <EqualizerPanel />
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* AI DJ Assistant Interactive Drawer Trigger Panel */}
          <div className="relative z-10 px-6 max-w-sm mx-auto w-full mb-4">
            <div className="glass rounded-2xl p-3 border border-white/5 space-y-2">
              <div className="flex items-center gap-2">
                <Sparkles size={14} className="text-purple-400" />
                <span className="text-[10px] font-bold uppercase tracking-widest text-white/40">AI DJ Prompt</span>
              </div>
              
              <form onSubmit={handleAskAIDJ} className="flex gap-2">
                <input 
                  type="text"
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  placeholder="Request mix (e.g. 'high energy synthwave')"
                  className="flex-1 bg-white/5 rounded-xl px-3 py-1.5 text-xs text-white placeholder:text-white/20 border border-white/5 focus:outline-none focus:border-white/20"
                />
                <button 
                  type="submit"
                  disabled={aiThinking}
                  className="px-3 rounded-xl bg-white text-black text-[9px] font-bold uppercase tracking-widest hover:scale-105 active:scale-95 transition-all"
                >
                  {aiThinking ? 'DJ...' : 'Ask'}
                </button>
              </form>

              {aiResponse && (
                <p className="text-[10px] text-purple-200/80 italic leading-relaxed pt-1 select-text">
                  ✨ {aiResponse}
                </p>
              )}
            </div>
          </div>

          {/* Lower Playback Controls Interface */}
          <footer className="relative z-10 p-6 bg-gradient-to-t from-black/80 to-transparent backdrop-blur-3xl border-t border-white/5 space-y-6">
            {/* Interactive Progress Bar */}
            <div className="space-y-2">
              <div className="relative h-[4px] w-full bg-white/10 rounded-full overflow-hidden cursor-pointer group">
                <div 
                  className="absolute inset-y-0 left-0 bg-white transition-all duration-100"
                  style={{ width: `${duration ? (progress / duration) * 100 : 0}%` }}
                />
                <input 
                  type="range"
                  min={0}
                  max={duration || 100}
                  step={1}
                  value={progress}
                  onChange={(e) => seek(Number(e.target.value))}
                  className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                />
              </div>
              <div className="flex justify-between text-[10px] font-mono text-white/30 tracking-widest">
                <span>{formatDuration(progress)}</span>
                <span>{formatDuration(duration)}</span>
              </div>
            </div>

            {/* Core Controls */}
            <div className="flex items-center justify-between px-2">
              <button 
                onClick={() => toggleFavorite(currentSong!.id)}
                className="p-2.5 rounded-full bg-white/5 hover:bg-white/10 cursor-pointer"
              >
                <Heart size={18} className={cn("transition-colors", currentSong.isFavorite ? "fill-white text-white" : "text-white/40")} />
              </button>

              <div className="flex items-center gap-7">
                <button 
                  onClick={previous} 
                  className="p-3.5 text-white/60 hover:text-white active:scale-95 transition-all cursor-pointer"
                >
                  <SkipBack size={26} fill="currentColor" />
                </button>

                <button 
                  onClick={() => status === 'playing' ? pause() : resume()}
                  className="w-18 h-18 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 active:scale-95 transition-all cursor-pointer shadow-glow"
                >
                  {status === 'playing' ? (
                    <Pause size={24} fill="currentColor" />
                  ) : (
                    <Play size={24} fill="currentColor" className="ml-1" />
                  )}
                </button>

                <button 
                  onClick={next} 
                  className="p-3.5 text-white/60 hover:text-white active:scale-95 transition-all cursor-pointer"
                >
                  <SkipForward size={26} fill="currentColor" />
                </button>
              </div>

              <button className="p-2.5 rounded-full bg-white/5 hover:bg-white/10 cursor-pointer text-white/40 hover:text-white">
                <Share2 size={18} />
              </button>
            </div>
            
            {/* Dynamic micro visual representation (waveform representation) */}
            <div className="flex justify-center items-end gap-1.5 h-6 opacity-30">
              {Array.from({ length: 24 }).map((_, i) => {
                const heightPercent = Math.sin(progress + i * 0.3) * 30 + 50;
                return (
                  <div 
                    key={i} 
                    className="w-1 bg-white rounded-md transition-all duration-300"
                    style={{ height: `${status === 'playing' ? heightPercent : 15}%` }}
                  />
                );
              })}
            </div>
          </footer>
        </motion.div>
      )}
    </AnimatePresence>
  );
};
