import React, { useMemo } from 'react';
import { motion } from 'motion/react';
import { Play, Sparkles, Flame, Headphones, Star, Zap, Library, Calendar, ShieldCheck } from 'lucide-react';
import { useAudio } from '../context/AudioContext';
import { formatDuration } from '../lib/utils';
import { Song } from '../types';

export const Home: React.FC = () => {
  const { songs, play, userStats, premiumTheme } = useAudio();

  const getGreeting = () => {
    const hours = new Date().getHours();
    if (hours < 12) return 'Good Morning';
    if (hours < 18) return 'Good Afternoon';
    return 'Good Evening';
  };

  const getThemeAesthetic = () => {
    switch (premiumTheme) {
      case 'cyberpunk':
        return { text: 'text-pink-400', glow: 'shadow-glow-pink', label: 'Overclocked Cyberpunk Edition' };
      case 'celestial':
        return { text: 'text-slate-200', glow: 'shadow-glow', label: 'Celestial Hi-Fi Sphere' };
      case 'neon-city':
        return { text: 'text-emerald-400', glow: 'shadow-glow-cyan', label: 'Neon Emerald Grid' };
      default:
        return { text: 'text-purple-400', glow: 'shadow-glow-purple', label: 'Cosmic Atmos Player' };
    }
  };

  const aesthetic = getThemeAesthetic();
  const sortedByRecent = useMemo(() => {
    return [...songs].sort((a, b) => b.addedAt - a.addedAt);
  }, [songs]);

  const favorites = useMemo(() => {
    return songs.filter(s => s.isFavorite);
  }, [songs]);

  const featuredSong = useMemo(() => {
    if (favorites.length > 0) return favorites[0];
    if (songs.length > 0) return songs[0];
    return null;
  }, [songs, favorites]);

  // Derive daily quest status dynamically from real user playback history
  const totalPlays = useMemo(() => {
    return songs.reduce((acc, current) => acc + current.playCount, 0);
  }, [songs]);

  const questProgressCount = Math.min(totalPlays, 3);
  const questProgressPercent = (questProgressCount / 3) * 100;

  return (
    <div className="min-h-screen pb-44 pt-10 px-6 space-y-9 overflow-y-auto no-scrollbar relative font-sans">
      {/* Dynamic Header */}
      <motion.div 
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.6 }}
        className="flex justify-between items-start"
      >
        <div className="space-y-1">
          <p className="text-[10px] font-bold uppercase tracking-[0.4em] text-white/30">Pulse Vision</p>
          <h1 className="text-3xl font-light tracking-tight text-white font-display">
            {getGreeting()} <span className="font-semibold text-glow">Listener</span>
          </h1>
        </div>
        
        {/* Streak XP Badge Widget */}
        <div className="flex items-center gap-2.5 px-3 py-1.5 rounded-full bg-white/5 border border-white/5 shadow-lg">
          <Flame size={14} className="text-orange-400 animate-pulse" />
          <span className="text-[9px] font-bold uppercase tracking-widest text-white/80">
            {userStats.listeningStreak} Days Streak
          </span>
        </div>
      </motion.div>

      {/* Hero Animated Glass Card */}
      {featuredSong ? (
        <motion.div
          initial={{ opacity: 0, scale: 0.98, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={{ delay: 0.1, duration: 0.7 }}
          className="relative h-60 rounded-[32px] overflow-hidden glass p-8 flex flex-col justify-end border border-white/8 shadow-2xl group cursor-pointer"
          onClick={() => play(featuredSong)}
        >
          {/* Core glow */}
          <div className="absolute top-[20%] right-[-10%] w-72 h-72 rounded-full bg-white/5 blur-[90px] group-hover:bg-white/10 transition-all duration-700" />
          <div className="absolute inset-0 bg-gradient-to-t from-black via-black/30 to-transparent pointer-events-none" />

          {featuredSong.coverArt && (
            <img 
              src={featuredSong.coverArt} 
              alt="" 
              className="absolute inset-0 w-full h-full object-cover opacity-10 blur-md pointer-events-none scale-105" 
            />
          )}

          <div className="relative z-10 space-y-4">
            <div className="flex items-center gap-2 text-white/40">
              <Sparkles size={14} className={aesthetic.text} />
              <span className="text-[9px] font-bold uppercase tracking-[0.3em]">Quick Ambient Stage • Playback Ready</span>
            </div>
            
            <div className="space-y-1.5">
              <h2 className="text-2xl font-light text-white leading-tight font-display tracking-tight group-hover:text-glow transition-all">
                {featuredSong.title}
              </h2>
              <p className="text-xs text-white/40 uppercase tracking-[0.2em]">{featuredSong.artist || "Unknown Artist"}</p>
            </div>

            <button className="px-6 py-2.5 rounded-full bg-white text-black text-[9px] font-bold uppercase tracking-[0.25em] flex items-center gap-2 hover:scale-105 active:scale-95 transition-all shadow-glow cursor-pointer">
              <Play size={10} fill="currentColor" /> Stream Featured
            </button>
          </div>
        </motion.div>
      ) : (
        <motion.div
          initial={{ opacity: 0, scale: 0.98, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={{ delay: 0.1, duration: 0.7 }}
          className="relative h-60 rounded-[32px] overflow-hidden glass p-8 flex flex-col justify-end border border-white/5 shadow-xl group"
        >
          <div className="absolute top-[20%] right-[-10%] w-60 h-60 rounded-full bg-purple-500/5 blur-[80px]" />
          <div className="absolute inset-0 bg-gradient-to-t from-black via-black/30 to-transparent pointer-events-none" />

          <div className="relative z-10 space-y-4">
            <div className="flex items-center gap-2 text-white/30">
              <Headphones size={14} />
              <span className="text-[9px] font-bold uppercase tracking-[0.3em]">{aesthetic.label}</span>
            </div>
            
            <div className="space-y-1.5">
              <h2 className="text-2xl font-light text-white/90 leading-tight font-display tracking-tight">
                No Track Synchronized
              </h2>
              <p className="text-xs text-white/35">Upload digital FLAC, MP3, or WAV sound staging in Library.</p>
            </div>
          </div>
        </motion.div>
      )}

      {/* Listen XP Streak Progress Slider */}
      <section className="space-y-4">
        <div className="flex items-center justify-between mx-1">
          <div className="flex items-center gap-2 text-white/50">
            <Zap size={14} className="text-yellow-400 animate-bounce" />
            <span className="text-[10px] font-extrabold uppercase tracking-[0.2em]">Daily Listening Quest</span>
          </div>
          <span className="text-[9px] font-mono text-white/30">Level {userStats.level} ({userStats.xp} XP)</span>
        </div>
        <div className="p-4 rounded-2xl glass-card space-y-3">
          <div className="flex justify-between items-center">
            <p className="text-xs text-white/80 font-medium">Quest: Play 3 audio streams today</p>
            <span className="text-[10px] font-mono font-bold text-white">{questProgressCount}/3 Tracks</span>
          </div>
          <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
            <div 
              style={{ width: `${questProgressPercent}%` }} 
              className="h-full bg-white shadow-[0_0_10px_rgba(255,255,255,0.7)] transition-all duration-500" 
            />
          </div>
        </div>
      </section>

      {/* Recently Added Section Carousel */}
      <section className="space-y-5">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold uppercase tracking-[0.25em] text-white flex items-center gap-2">
            <Calendar size={14} className="text-white/40" />
            Recently Added
          </h3>
          <span className="text-[9px] text-white/30 uppercase tracking-[0.2em]">{songs.length} Tracks</span>
        </div>

        {sortedByRecent.length > 0 ? (
          <div className="flex gap-4 overflow-x-auto no-scrollbar pb-2 -mx-6 px-6">
            {sortedByRecent.map((song, idx) => (
              <motion.div
                key={song.id}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: idx * 0.08 }}
                onClick={() => play(song)}
                className="w-36 flex-shrink-0 group cursor-pointer space-y-3 animate-fade-in"
              >
                <div className="relative aspect-square rounded-[22px] overflow-hidden glass-card border border-white/6 group-hover:border-white/20 transition-all duration-500 shadow-md">
                  {song.coverArt ? (
                    <img src={song.coverArt} alt="" className="w-full h-full object-cover grayscale-[0.1] group-hover:scale-105 transition-all duration-700" />
                  ) : (
                    <div className="w-full h-full bg-white/5 flex items-center justify-center">
                      <Headphones size={22} className="text-white/20" />
                    </div>
                  )}
                  
                  {/* Custom Hover Glass Overlay Play Indicator */}
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-[1px]">
                    <div className="w-11 h-11 bg-white text-black rounded-full flex items-center justify-center shadow-lg transform translate-y-3 group-hover:translate-y-0 transition-transform duration-300">
                      <Play size={16} fill="currentColor" className="ml-0.5" />
                    </div>
                  </div>
                </div>

                <div className="px-1 text-center">
                  <h4 className="text-xs font-semibold text-white truncate group-hover:text-glow">
                    {song.title}
                  </h4>
                  <p className="text-[9px] text-white/30 uppercase tracking-[0.2em] truncate mt-1">
                    {song.artist || "Unknown Artist"}
                  </p>
                </div>
              </motion.div>
            ))}
          </div>
        ) : (
          <div className="py-12 px-4 rounded-3xl glass border border-white/5 text-center flex flex-col items-center justify-center gap-2">
            <Library size={24} className="text-white/20 stroke-[1.5px]" />
            <h4 className="text-[10px] font-bold uppercase tracking-[0.25em] text-white/45">Library is Empty</h4>
            <p className="text-[9px] text-white/30 max-w-[200px] leading-relaxed mx-auto">
              Your audio file catalog is currently empty. Upload tracks in the Library tab.
            </p>
          </div>
        )}
      </section>

      {/* Favorites Album Selection */}
      <section className="space-y-5">
        <div className="flex justify-between items-center">
          <h3 className="text-sm font-bold uppercase tracking-[0.25em] text-white flex items-center gap-2">
            <Star size={14} className="text-white/40" />
            Your Favorites Range
          </h3>
        </div>

        {favorites.length > 0 ? (
          <div className="grid grid-cols-2 gap-4">
            {favorites.slice(0, 4).map((song) => (
              <div 
                key={song.id}
                onClick={() => play(song)}
                className="p-3.5 rounded-2xl glass-card border border-white/5 flex items-center gap-3 cursor-pointer hover:bg-white/10 transition-all"
              >
                <div className="w-11 h-11 rounded-xl overflow-hidden bg-white/5 flex-shrink-0">
                  {song.coverArt ? (
                    <img src={song.coverArt} alt="" className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full bg-white/5 flex items-center justify-center">
                      <Headphones size={14} className="text-white/30" />
                    </div>
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <h4 className="text-xs font-semibold text-white truncate">{song.title}</h4>
                  <p className="text-[9px] text-white/30 truncate uppercase tracking-widest mt-0.5">{song.artist || "Unknown Artist"}</p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="py-10 text-center rounded-2xl border border-dashed border-white/10 text-white/20 text-[10px] font-bold uppercase tracking-[0.2em] glass">
            No Favorites Tracked Yet
          </div>
        )}
      </section>
    </div>
  );
};
