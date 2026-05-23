import React, { useState, useMemo } from 'react';
import { motion } from 'motion/react';
import { Search as SearchIcon, X, Flame, ShieldCheck, Play, ArrowRight, Disc } from 'lucide-react';
import { useAudio } from '../context/AudioContext';

export const SearchScreen: React.FC = () => {
  const { songs, play } = useAudio();
  const [query, setQuery] = useState('');

  const filteredSongs = useMemo(() => {
    if (!query.trim()) return [];
    const lower = query.toLowerCase();
    return songs.filter(s => 
      s.title.toLowerCase().includes(lower) || 
      (s.artist && s.artist.toLowerCase().includes(lower)) ||
      (s.album && s.album.toLowerCase().includes(lower)) ||
      (s.genre && s.genre.toLowerCase().includes(lower))
    );
  }, [songs, query]);

  // Extract unique genres and artists dynamically from active music library
  const dynamicGenres = useMemo(() => {
    const list = songs.map(s => s.genre).filter((g): g is string => !!g);
    return Array.from(new Set(list)).slice(0, 4);
  }, [songs]);

  const dynamicArtists = useMemo(() => {
    const list = songs.map(s => s.artist).filter(a => !!a && a !== "Unknown Artist" && a !== "Offline Local Track");
    return Array.from(new Set(list)).slice(0, 6);
  }, [songs]);

  return (
    <div className="min-h-screen pb-44 pt-10 px-6 flex flex-col space-y-7 overflow-hidden relative font-sans">
      <div className="space-y-1">
        <p className="text-[10px] font-bold uppercase tracking-[0.4em] text-white/30">Pulse Scanner</p>
        <h1 className="text-3xl font-light tracking-tight text-white font-display">Library Search</h1>
      </div>

      {songs.length > 0 ? (
        <>
          <div className="relative groupAndSearch">
            <div className="absolute inset-y-0 left-5 flex items-center text-white/20 transition-all">
              <SearchIcon size={18} />
            </div>
            <input 
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Songs, Artists, Albums..."
              className="w-full bg-[#0d0d0d] border border-white/5 focus:border-white/15 rounded-2xl py-4.5 pl-14 pr-12 text-white placeholder-white/20 text-xs focus:outline-none focus:ring-1 focus:ring-white/10 transition-all font-medium tracking-wide uppercase"
            />
            {query && (
              <button 
                onClick={() => setQuery('')}
                className="absolute inset-y-0 right-4 p-2 text-white/40 hover:text-white cursor-pointer"
              >
                <X size={16} />
              </button>
            )}
          </div>

          <div className="flex-1 overflow-y-auto no-scrollbar space-y-6 pt-3">
            {query ? (
              <div className="space-y-4">
                <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-white/20 p-1">Search Results for "{query}"</h3>
                {filteredSongs.length > 0 ? (
                  filteredSongs.map((song, i) => (
                    <motion.div
                      key={song.id}
                      initial={{ opacity: 0, x: -10 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.05 }}
                      onClick={() => play(song)}
                      className="p-3 bg-white/[0.02] border border-white/5 rounded-2xl flex items-center justify-between cursor-pointer hover:bg-white/5 transition-all group"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg overflow-hidden bg-white/5 flex-shrink-0">
                          {song.coverArt && <img src={song.coverArt} alt="" className="w-full h-full object-cover" />}
                        </div>
                        <div>
                          <h4 className="text-xs font-semibold text-white group-hover:text-glow">{song.title}</h4>
                          <p className="text-[9px] text-white/40 uppercase tracking-[0.2em] mt-0.5">{song.artist}</p>
                        </div>
                      </div>
                      <button className="p-2 text-white/30 group-hover:text-white group-hover:scale-110 transition-all cursor-pointer">
                        <Play size={14} fill="currentColor" />
                      </button>
                    </motion.div>
                  ))
                ) : (
                  <div className="py-16 text-center text-white/20 text-xs uppercase font-bold tracking-[0.2em] glass rounded-2xl border border-white/5">
                    No matching tracks found
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-8">
                {/* Dynamically extracted Genres list */}
                {dynamicGenres.length > 0 && (
                  <section className="space-y-4">
                    <h3 className="text-[10px] font-bold uppercase tracking-[0.25em] text-white/30 flex items-center gap-2">
                      <Flame size={12} className="text-orange-400" />
                      Explore Genres
                    </h3>
                    
                    <div className="grid grid-cols-2 gap-3.5">
                      {dynamicGenres.map((genre) => (
                        <button
                          key={genre}
                          onClick={() => setQuery(genre)}
                          className="p-5 rounded-2xl bg-white/[0.02] hover:bg-white/5 border border-white/5 text-left transition-all cursor-pointer group relative overflow-hidden"
                        >
                          <div className="absolute inset-0 bg-gradient-to-br from-white/[0.01] to-transparent" />
                          <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-white/80 group-hover:text-glow">
                            {genre}
                          </span>
                          <ArrowRight size={12} className="text-white/20 group-hover:text-white absolute bottom-4 right-4 group-hover:translate-x-1 transition-all" />
                        </button>
                      ))}
                    </div>
                  </section>
                )}

                {/* Dynamically extracted Suggested Artists list */}
                {dynamicArtists.length > 0 && (
                  <section className="space-y-4">
                    <h3 className="text-[10px] font-bold uppercase tracking-[0.25em] text-white/30">Suggested Artists</h3>
                    <div className="flex flex-wrap gap-2.5">
                      {dynamicArtists.map((keyword) => (
                        <button
                          key={keyword}
                          onClick={() => setQuery(keyword)}
                          className="px-4 py-2 rounded-full bg-white/5 border border-white/5 hover:bg-white/10 hover:border-white/10 text-[9px] font-extrabold uppercase tracking-widest text-white/60 hover:text-white cursor-pointer transition-all"
                        >
                          {keyword}
                        </button>
                      ))}
                    </div>
                  </section>
                )}

                {dynamicGenres.length === 0 && dynamicArtists.length === 0 && (
                  <div className="py-20 text-center text-white/20 text-xs uppercase font-extrabold tracking-[0.2em] glass rounded-3xl border border-white/5">
                    Type a query above to scan tracks
                  </div>
                )}
              </div>
            )}
          </div>
        </>
      ) : (
        <div className="py-20 text-center flex flex-col items-center justify-center gap-4 glass rounded-3xl p-8 border border-white/5 my-auto">
          <SearchIcon size={44} className="text-white/15 stroke-[1.5px] animate-pulse" />
          <h4 className="text-xs font-bold uppercase tracking-[0.2em] text-white/70">Search Index Empty</h4>
          <p className="text-[10px] max-w-xs leading-relaxed text-white/35 mx-auto">
            Your audio search query catalog depends strictly on tracked metadata. Go to your Library tab to scan and load high-res tracks first.
          </p>
        </div>
      )}
    </div>
  );
};
