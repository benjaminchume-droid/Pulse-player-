import React from 'react';
import { motion } from 'motion/react';
import { Play, Pause, SkipForward, Disc } from 'lucide-react';
import { useAudio } from '../context/AudioContext';
import { cn } from '../lib/utils';

interface MiniPlayerProps {
  onExpand: () => void;
}

export const MiniPlayer: React.FC<MiniPlayerProps> = ({ onExpand }) => {
  const { currentSong, status, progress, duration, resume, pause, next } = useAudio();

  if (!currentSong) return null;

  const progressPercent = duration ? (progress / duration) * 100 : 0;

  return (
    <div className="fixed bottom-[88px] left-0 right-0 z-40 px-6 pointer-events-none select-none">
      <motion.div 
        initial={{ y: 50, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: "spring", stiffness: 180, damping: 22 }}
        onClick={onExpand}
        className="glass pointer-events-auto rounded-2xl p-3 flex items-center justify-between gap-4 cursor-pointer hover:bg-white/10 active:scale-[0.98] transition-all duration-300 border border-white/8 shadow-2xl relative overflow-hidden"
      >
        {/* Glow behind cover */}
        <div className="absolute inset-0 bg-gradient-to-r from-purple-500/5 to-cyan-500/5 pointer-events-none" />

        <div className="flex items-center gap-3.5 min-w-0 flex-1">
          {/* Cover / Vinyl Disc */}
          <div className="relative w-11 h-11 flex-shrink-0 rounded-lg overflow-hidden glass-card border border-white/10 flex items-center justify-center">
            {currentSong.coverArt ? (
              <img src={currentSong.coverArt} alt="" className="w-full h-full object-cover" />
            ) : (
              <Disc size={20} className="text-white/40 animate-spin" style={{ animationDuration: '6s' }} />
            )}
          </div>

          <div className="min-w-0 flex-1">
            <h4 className="text-sm font-semibold text-white tracking-tight truncate text-glow">
              {currentSong.title}
            </h4>
            <p className="text-[10px] text-white/40 font-medium uppercase tracking-[0.2em] truncate mt-0.5">
              {currentSong.artist}
            </p>
          </div>
        </div>

        {/* Media Controls */}
        <div className="flex items-center gap-3" onClick={e => e.stopPropagation()}>
          <button 
            onClick={() => status === 'playing' ? pause() : resume()}
            className="w-9 h-9 rounded-full bg-white text-black flex items-center justify-center hover:scale-110 active:scale-95 transition-transform shadow-lg cursor-pointer"
          >
            {status === 'playing' ? (
              <Pause size={15} fill="currentColor" />
            ) : (
              <Play size={15} fill="currentColor" className="ml-[1.5px]" />
            )}
          </button>
          
          <button 
            onClick={next} 
            className="p-1.5 text-white/60 hover:text-white transition-all cursor-pointer"
          >
            <SkipForward size={18} fill="currentColor" />
          </button>
        </div>

        {/* Sleek Line Progress Bar at exact bottom */}
        <div className="absolute bottom-0 left-0 right-0 h-[3px] bg-white/10">
          <div 
            className="h-full bg-gradient-to-r from-purple-500 via-white to-cyan-400 shadow-[0_0_8px_rgba(255,255,255,0.7)] transition-all duration-300"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
      </motion.div>
    </div>
  );
};
