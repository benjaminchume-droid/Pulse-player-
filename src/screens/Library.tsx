import React, { useState } from 'react';
import { motion } from 'motion/react';
import { 
  Play, Plus, Music2, Folder, Library, Disc, User, Heart, Trash2, ShieldAlert
} from 'lucide-react';
import { useAudio } from '../context/AudioContext';
import { cn, formatDuration } from '../lib/utils';
import { Song } from '../types';

export const LibraryScreen: React.FC = () => {
  const { 
    songs, addLocalFiles, play, currentSong, status, toggleFavorite, deleteSong, isScanning, permissions, requestPermission
  } = useAudio();
  const [activeGrouping, setActiveGrouping] = useState<'all' | 'albums' | 'artists'>('all');

  const getFilteredSongs = () => {
    switch (activeGrouping) {
      default:
        return songs;
    }
  };

  const handleFileDrop = (e: React.DragEvent) => {
    e.preventDefault();
    if (e.dataTransfer.files) {
      addLocalFiles(e.dataTransfer.files);
    }
  };

  const handleStorageAuthorize = async () => {
    await requestPermission('media');
  };

  const currentSongsList = getFilteredSongs();

  return (
    <div className="min-h-screen pb-44 pt-10 px-6 flex flex-col space-y-7 overflow-hidden relative">
      <div className="flex justify-between items-center">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.4em] text-white/30">Audiophile Core</p>
          <h1 className="text-3xl font-light tracking-tight text-white font-display">Your Library</h1>
        </div>

        {/* Local File Picker trigger */}
        <label className="w-11 h-11 rounded-full glass hover:bg-white/10 flex items-center justify-center cursor-pointer active:scale-95 transition-all">
          <Plus size={22} />
          <input 
            type="file" 
            multiple 
            accept="audio/*"
            className="hidden" 
            onChange={(e) => e.target.files && addLocalFiles(e.target.files)}
          />
        </label>
      </div>

      {permissions.media === 'denied' && (
        <div className="p-4 rounded-2xl bg-red-950/20 border border-red-500/20 flex flex-col gap-3">
          <div className="flex items-center gap-2 text-red-200">
            <ShieldAlert size={16} />
            <span className="text-xs font-bold uppercase tracking-widest">Storage Blocked</span>
          </div>
          <p className="text-[10px] text-white/40 leading-snug">
            Emulated Android storage permission is disabled. Grant permission to safely index songs.
          </p>
          <button 
            onClick={handleStorageAuthorize}
            className="px-4 py-2 rounded-xl bg-white text-black text-[9px] font-bold uppercase tracking-widest self-start"
          >
            Authorize Storage
          </button>
        </div>
      )}

      {/* Upload Dropzone Container */}
      <div 
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleFileDrop}
        className="glass rounded-3xl p-6 border border-dashed border-white/10 text-center space-y-1.5 hover:border-white/30 transition-all duration-300 relative cursor-pointer group"
      >
        <div className="absolute inset-0 bg-white/[0.01] pointer-events-none" />
        <p className="text-xs font-bold uppercase tracking-widest text-white/60">Drag local music files here</p>
        <p className="text-[9px] text-white/30 leading-snug">Accepts MP3, high-res FLAC, OPUS, AAC, WAV formats</p>
      </div>

      {isScanning && (
        <motion.div 
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="p-4 rounded-2xl glass-solid flex items-center justify-center gap-3 border border-white/10"
        >
          <div className="w-4 h-4 border-2 border-white border-t-transparent animate-spin rounded-full" />
          <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-white">Extracting Audio Metadata...</p>
        </motion.div>
      )}

      {/* List Filter Tabs */}
      <div className="flex gap-2">
        {(['all', 'albums', 'artists'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveGrouping(tab)}
            className={cn(
              "px-4 py-2 rounded-full text-[9px] font-bold uppercase tracking-[0.25em] transition-all cursor-pointer",
              activeGrouping === tab ? "bg-white text-black font-extrabold" : "bg-white/5 text-white/40 hover:text-white/70"
            )}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Song list layout */}
      <div className="flex-1 overflow-y-auto no-scrollbar space-y-2">
        {currentSongsList.length > 0 ? (
          currentSongsList.map((song, i) => {
            const isCurrent = currentSong?.id === song.id;
            const isPlaying = isCurrent && status === 'playing';

            return (
              <motion.div
                key={song.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.04 }}
                className={cn(
                  "p-3 rounded-2xl border flex items-center justify-between gap-4 cursor-pointer transition-all duration-300 group",
                  isCurrent ? "bg-white/10 border-white/15" : "bg-white/3 border-transparent hover:bg-white/6"
                )}
                onClick={() => play(song)}
              >
                <div className="flex items-center gap-3.5 min-w-0">
                  <div className="relative w-11 h-11 rounded-xl overflow-hidden bg-white/5 flex-shrink-0 flex items-center justify-center">
                    {song.coverArt ? (
                      <img src={song.coverArt} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <Music2 size={16} className="text-white/20" />
                    )}

                    {isPlaying && (
                      <div className="absolute inset-0 bg-black/50 flex items-end justify-center pb-2.5 gap-[2px]">
                        {[0.5, 0.8, 0.4].map((h, idx) => (
                          <div 
                            key={idx}
                            style={{ animationDelay: `${idx * 0.15}s` }}
                            className="w-[2px] bg-white anim-vbar h-5 rounded-full"
                          />
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="min-w-0">
                    <h4 className={cn(
                      "text-xs font-semibold truncate transition-colors",
                      isCurrent ? "text-white text-glow" : "text-white/80 group-hover:text-white"
                    )}>
                      {song.title}
                    </h4>
                    <p className="text-[10px] text-white/30 truncate uppercase tracking-widest mt-1">
                      {song.artist}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3.5 flex-shrink-0" onClick={e => e.stopPropagation()}>
                  <span className="text-[9px] font-mono text-white/20">{formatDuration(song.duration)}</span>
                  <button 
                    onClick={() => toggleFavorite(song.id)}
                    className="p-1.5 text-white/35 hover:text-white transition-colors cursor-pointer"
                  >
                    <Heart size={14} className={song.isFavorite ? "fill-white text-white" : ""} />
                  </button>
                  <button 
                    onClick={() => deleteSong(song.id)}
                    className="p-1.5 text-white/10 hover:text-red-400 transition-colors opacity-0 group-hover:opacity-100 cursor-pointer"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </motion.div>
            );
          })
        ) : (
          <div className="py-20 text-center opacity-25 flex flex-col items-center justify-center gap-3">
            <Disc size={44} className="stroke-[1px] animate-spin" style={{ animationDuration: '8s' }} />
            <p className="text-xs font-bold uppercase tracking-[0.2em]">Your library is empty</p>
            <p className="text-[10px] max-w-xs leading-normal">Drag and drop audio tracks here to initialize local playback list.</p>
          </div>
        )}
      </div>
    </div>
  );
};
