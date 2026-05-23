import React, { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { Sparkles, Play, ListMusic, Plus, Music2, Share2, Disc } from 'lucide-react';
import { useAudio } from '../context/AudioContext';
import { cn } from '../lib/utils';
import { Playlist } from '../types';

export const PlaylistsScreen: React.FC = () => {
  const { songs, play, triggerAIAssistant, aiThinking } = useAudio();
  const [playlists, setPlaylists] = useState<Playlist[]>(() => {
    try {
      const saved = localStorage.getItem('pulse_playlists');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [prompt, setPrompt] = useState('');
  const [newPlaylistName, setNewPlaylistName] = useState('');
  const [createdFeedback, setCreatedFeedback] = useState<string | null>(null);

  // Persist playlists
  useEffect(() => {
    localStorage.setItem('pulse_playlists', JSON.stringify(playlists));
  }, [playlists]);

  const handleCreatePlaylist = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPlaylistName.trim()) return;

    const customPL: Playlist = {
      id: crypto.randomUUID(),
      name: newPlaylistName,
      description: 'User Custom Playlist Archive',
      songs: [],
      createdAt: Date.now()
    };

    setPlaylists(prev => [customPL, ...prev]);
    setNewPlaylistName('');
  };

  const handleGenerateAIMix = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim()) return;

    const responseText = await triggerAIAssistant(prompt);
    
    // Choose 3 random songs from real library if available
    const randomSongs = [...songs].sort(() => 0.5 - Math.random()).slice(0, 3);
    const aiPL: Playlist = {
      id: crypto.randomUUID(),
      name: `AI: ${prompt}`,
      description: responseText,
      songs: randomSongs.map(s => s.id),
      createdAt: Date.now(),
      isAiGenerated: true
    };

    setPlaylists(prev => [aiPL, ...prev]);
    setCreatedFeedback(`Formulated special AI Mixture: "${aiPL.name}"`);
    setPrompt('');
    setTimeout(() => setCreatedFeedback(null), 4000);
  };

  const handlePlayPlaylist = (playlist: Playlist) => {
    const plSongs = songs.filter(s => playlist.songs.includes(s.id));
    if (plSongs[0]) play(plSongs[0]);
  };

  return (
    <div className="min-h-screen pb-44 pt-10 px-6 space-y-7 overflow-y-auto no-scrollbar relative">
      <div className="space-y-1">
        <p className="text-[10px] font-bold uppercase tracking-[0.4em] text-white/30">Atmospheric Compilations</p>
        <h1 className="text-3xl font-light tracking-tight text-white font-display">Playlists</h1>
      </div>

      {/* AI Generative Formulation Box */}
      <section className="glass rounded-[28px] p-5 border border-white/5 space-y-4">
        <div className="flex items-center gap-2">
          <Sparkles className="text-purple-400" size={16} />
          <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-white">AI DJ Mix Compiler</h3>
        </div>
        <p className="text-[10px] text-white/40 leading-relaxed">
          Supply a prompt characterizing your mood (e.g., 'ambient rain', 'overclocked hardware run') to auto-compile a synced mixture.
        </p>
        
        <form onSubmit={handleGenerateAIMix} className="space-y-3">
          <input 
            type="text"
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="Describe atmosphere..."
            className="w-full bg-[#0d0d0d] border border-white/5 focus:border-white/15 px-4.5 py-3.5 text-xs rounded-xl focus:outline-none focus:ring-1 focus:ring-white/5"
          />
          <button
            type="submit"
            disabled={aiThinking}
            className="w-full py-3.5 rounded-xl bg-white text-black font-extrabold uppercase tracking-[0.25em] text-[9px] hover:scale-[1.01] active:scale-95 transition-all shadow-glow cursor-pointer"
          >
            {aiThinking ? 'AI Synthesis in progress...' : 'Formulate Mixture'}
          </button>
        </form>

        {createdFeedback && (
          <p className="text-[10px] text-purple-300 font-bold uppercase tracking-wider bg-white/5 p-3 rounded-xl">
            {createdFeedback}
          </p>
        )}
      </section>

      {/* List playlists */}
      <section className="space-y-4">
        <div className="flex justify-between items-center">
          <h3 className="text-[10px] font-bold uppercase tracking-[0.25em] text-white/40">Mixtures Archives</h3>
        </div>

        {playlists.length > 0 ? (
          <div className="space-y-4">
            {playlists.map((playlist) => (
              <div 
                key={playlist.id}
                className="p-4 rounded-2xl glass-card border border-white/5 flex items-center justify-between group"
              >
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-white/50 relative overflow-hidden flex-shrink-0">
                    {playlist.isAiGenerated ? (
                      <Sparkles size={18} className="text-purple-400 absolute animate-pulse" />
                    ) : (
                      <ListMusic size={18} />
                    )}
                  </div>
                  <div>
                    <h4 className="text-sm font-semibold text-white group-hover:text-glow">{playlist.name}</h4>
                    <p className="text-[10px] text-white/40 leading-snug truncate mt-0.5">{playlist.songs.length} Tracks • {playlist.isAiGenerated ? 'AI Synced' : 'Offline Custom'}</p>
                  </div>
                </div>
                <button 
                  onClick={() => handlePlayPlaylist(playlist)}
                  className="w-9 h-9 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 active:scale-95 transition-all cursor-pointer shadow-lg"
                >
                  <Play size={14} fill="currentColor" className="ml-0.5" />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="py-12 px-4 rounded-3xl glass border border-white/5 text-center flex flex-col items-center justify-center gap-3">
            <ListMusic size={36} className="text-white/20 stroke-[1.5px] animate-pulse" />
            <h4 className="text-xs font-bold uppercase tracking-[0.2em] text-white/55">No Playlists Found</h4>
            <p className="text-[10px] text-white/30 max-w-[200px] leading-relaxed mx-auto">
              Create your first offline mixture or request the AI DJ helper to formulate a personalized stream.
            </p>
          </div>
        )}
      </section>

      {/* Manual Creation Box Form */}
      <section className="p-4 rounded-2xl bg-[#090909] border border-white/5 space-y-3">
        <h4 className="text-[10px] font-bold uppercase tracking-[0.2em] text-white/40">Create Offline Playlist</h4>
        <form onSubmit={handleCreatePlaylist} className="flex gap-2">
          <input 
            type="text"
            value={newPlaylistName}
            onChange={(e) => setNewPlaylistName(e.target.value)}
            placeholder="Playlist name..."
            className="flex-1 bg-white/5 rounded-xl px-4 py-2.5 text-xs text-white placeholder-white/10"
          />
          <button 
            type="submit"
            className="px-4 rounded-xl bg-white/5 hover:bg-white/10 border border-white/5 text-white text-[10px] font-bold uppercase tracking-wider cursor-pointer"
          >
            Create
          </button>
        </form>
      </section>
    </div>
  );
};
