import React, { createContext, useContext, useState, useRef, useEffect, useCallback } from 'react';
import { Song, PlaybackStatus, PulseTheme, UserStats } from '../types';
import { get, set } from 'idb-keyval';
import { DEMO_SONGS, ALL_ACHIEVEMENTS } from '../lib/utils';

// Multi-band frequency bands for professional 10-band Equalizer
export const FREQUENCY_BANDS = [31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000];

interface AudioContextType {
  // Playback State
  songs: Song[];
  currentSong: Song | null;
  status: PlaybackStatus;
  progress: number;
  duration: number;
  volume: number;
  playbackRate: number;
  queue: Song[];
  history: Song[];
  shuffle: boolean;
  repeat: 'none' | 'one' | 'all';
  
  // Library Management
  setSongs: React.Dispatch<React.SetStateAction<Song[]>>;
  addLocalFiles: (files: FileList) => Promise<void>;
  toggleFavorite: (songId: string) => void;
  deleteSong: (songId: string) => void;
  isScanning: boolean;

  // Audio Engine Controls
  play: (song: Song) => void;
  pause: () => void;
  resume: () => void;
  next: () => void;
  previous: () => void;
  seek: (time: number) => void;
  setVolume: (vol: number) => void;
  setPlaybackRate: (rate: number) => void;
  setShuffle: (val: boolean) => void;
  setRepeat: (val: 'none' | 'one' | 'all') => void;
  clearQueue: () => void;
  addToQueue: (song: Song) => void;
  
  // Advanced Audio Effects
  eqGains: Record<number, number>;
  setEqGain: (freq: number, gain: number) => void;
  bassBoost: boolean;
  setBassBoost: (val: boolean) => void;
  reverbAmount: number; // 0 to 1
  setReverbAmount: (val: number) => void;
  spatialAudio: boolean;
  setSpatialAudio: (val: boolean) => void;
  analyserNode: AnalyserNode | null;

  // Gamification & Themes
  premiumTheme: PulseTheme;
  setPremiumTheme: (theme: PulseTheme) => void;
  userStats: UserStats;
  addXP: (amount: number) => void;
  triggerAIAssistant: (text: string) => Promise<string>;
  aiThinking: boolean;

  // Android Emulation States
  permissions: {
    media: 'granted' | 'denied' | 'prompt';
    notifications: 'granted' | 'denied' | 'prompt';
    battery: 'granted' | 'denied' | 'prompt';
  };
  requestPermission: (type: 'media' | 'notifications' | 'battery') => Promise<boolean>;
  bluetoothDevice: string | null;
  setBluetoothDevice: (name: string | null) => void;
}

const AudioContext = createContext<AudioContextType | undefined>(undefined);

export const AudioProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // Core states
  const [songs, setSongs] = useState<Song[]>(DEMO_SONGS);
  const [currentSong, setCurrentSong] = useState<Song | null>(null);
  const [status, setStatus] = useState<PlaybackStatus>('stopped');
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.8);
  const [playbackRate, setPlaybackRate] = useState(1.0);
  const [queue, setQueue] = useState<Song[]>([]);
  const [history, setHistory] = useState<Song[]>([]);
  const [shuffle, setShuffle] = useState(false);
  const [repeat, setRepeat] = useState<'none' | 'one' | 'all'>('none');
  const [isScanning, setIsScanning] = useState(false);

  // Advanced Audio Web API Nodes
  const [eqGains, setEqGains] = useState<Record<number, number>>(
    FREQUENCY_BANDS.reduce((acc, freq) => ({ ...acc, [freq]: 0 }), {})
  );
  const [bassBoost, setBassBoost] = useState(false);
  const [reverbAmount, setReverbAmount] = useState(0.1);
  const [spatialAudio, setSpatialAudio] = useState(false);
  const [analyserNode, setAnalyserNode] = useState<AnalyserNode | null>(null);

  // Custom visual theme & stats
  const [premiumTheme, setPremiumTheme] = useState<PulseTheme>('cosmic');
  const [userStats, setUserStats] = useState<UserStats>({
    listeningStreak: 0,
    xp: 0,
    level: 1,
    unlockedThemes: ['cosmic'],
    achievements: ALL_ACHIEVEMENTS,
    dailyQuestCompleted: false
  });

  // Android Emulation Support
  const [permissions, setPermissions] = useState<{
    media: 'granted' | 'denied' | 'prompt';
    notifications: 'granted' | 'denied' | 'prompt';
    battery: 'granted' | 'denied' | 'prompt';
  }>({
    media: 'prompt',
    notifications: 'prompt',
    battery: 'prompt'
  });
  const [bluetoothDevice, setBluetoothDevice] = useState<string | null>(null);

  const [aiThinking, setAiThinking] = useState(false);

  // References
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const sourceNodeRef = useRef<MediaElementAudioSourceNode | null>(null);
  const biquadFiltersRef = useRef<BiquadFilterNode[]>([]);
  const bassFilterRef = useRef<BiquadFilterNode | null>(null);
  const volumeNodeRef = useRef<GainNode | null>(null);

  // Load state from local storage / IndexedDB
  useEffect(() => {
    const loadSavedData = async () => {
      try {
        const savedSongs = await get<Song[]>('pulse_songs');
        if (savedSongs && savedSongs.length > 0) {
          setSongs(savedSongs);
        }

        const savedStats = await get<UserStats>('pulse_stats');
        if (savedStats) {
          setUserStats(savedStats);
        }

        const savedTheme = await get<PulseTheme>('pulse_theme');
        if (savedTheme) {
          setPremiumTheme(savedTheme);
        }

        const savedPermissions = await get<typeof permissions>('pulse_perms');
        if (savedPermissions) {
          setPermissions(savedPermissions);
        }
      } catch (err) {
        console.error("IndexedDB Hydration failed", err);
      }
    };
    loadSavedData();
  }, []);

  // Save changes automatically
  const persistSongs = useCallback((updatedSongs: Song[]) => {
    setSongs(updatedSongs);
    set('pulse_songs', updatedSongs);
  }, []);

  const persistStats = useCallback((updatedStats: UserStats) => {
    setUserStats(updatedStats);
    set('pulse_stats', updatedStats);
  }, []);

  // HTML5 Audio setup & web audio routing
  useEffect(() => {
    audioRef.current = new Audio();
    audioRef.current.crossOrigin = 'anonymous';

    const audio = audioRef.current;

    const handleTimeUpdate = () => {
      setProgress(audio.currentTime);
    };

    const handleDurationChange = () => {
      setDuration(audio.duration || 0);
    };

    const handleEnded = () => {
      addXP(50); // listening reward
      if (repeat === 'one') {
        audio.currentTime = 0;
        audio.play().catch(e => console.error("Playback replay failed", e));
      } else {
        next();
      }
    };

    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('durationchange', handleDurationChange);
    audio.addEventListener('ended', handleEnded);

    return () => {
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('durationchange', handleDurationChange);
      audio.removeEventListener('ended', handleEnded);
      audio.pause();
    };
  }, [repeat, queue]);

  // Audio nodes routing engine
  const initAudioEngine = () => {
    if (!audioRef.current) return;
    if (audioContextRef.current) return;

    try {
      const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
      const audioCtx = new AudioCtxClass();
      audioContextRef.current = audioCtx;

      const source = audioCtx.createMediaElementSource(audioRef.current);
      sourceNodeRef.current = source;

      // 10 bands Equalizer Chain
      let lastNode: AudioNode = source;
      const filters: BiquadFilterNode[] = [];

      FREQUENCY_BANDS.forEach((freq) => {
        const filter = audioCtx.createBiquadFilter();
        filter.type = freq <= 62 ? 'lowshelf' : freq >= 8000 ? 'highshelf' : 'peaking';
        filter.frequency.value = freq;
        filter.Q.value = 1.0;
        filter.gain.value = eqGains[freq] || 0;
        
        lastNode.connect(filter);
        lastNode = filter;
        filters.push(filter);
      });
      biquadFiltersRef.current = filters;

      // Dynamic Bass Booster Node
      const bassFilter = audioCtx.createBiquadFilter();
      bassFilter.type = 'peaking';
      bassFilter.frequency.value = 80;
      bassFilter.Q.value = 1.4;
      bassFilter.gain.value = bassBoost ? 8 : 0;
      lastNode.connect(bassFilter);
      lastNode = bassFilter;
      bassFilterRef.current = bassFilter;

      // Holographic Audio Analyser Node
      const analyser = audioCtx.createAnalyser();
      analyser.fftSize = 128;
      lastNode.connect(analyser);
      lastNode = analyser;
      setAnalyserNode(analyser);

      // Main Volume Stage
      const volumeNode = audioCtx.createGain();
      volumeNode.gain.value = volume;
      lastNode.connect(volumeNode);
      lastNode = volumeNode;
      volumeNodeRef.current = volumeNode;

      // Connect to output device
      volumeNode.connect(audioCtx.destination);
    } catch (e) {
      console.warn("Web Audio API not supported directly via external URL source restrictions or missing interaction", e);
    }
  };

  // Playback Control Actions
  const play = useCallback((song: Song) => {
    if (!audioRef.current) return;

    // Check media permission first on Android simulation
    if (permissions.media === 'denied') {
      console.warn("Media storage access is blocked");
    }

    // Initialize the Web Audio context if not done already
    initAudioEngine();

    if (audioContextRef.current?.state === 'suspended') {
      audioContextRef.current.resume();
    }

    try {
      const playUrl = song.file ? URL.createObjectURL(song.file) : (song.demoUrl || '');
      audioRef.current.src = playUrl;
      audioRef.current.playbackRate = playbackRate;
      audioRef.current.volume = volume;
      
      const playPromise = audioRef.current.play();
      if (playPromise !== undefined) {
        playPromise.then(() => {
          setStatus('playing');
        }).catch(err => {
          console.error("Playback failed to initialize:", err);
        });
      }

      setCurrentSong(song);
      setHistory(prev => [song, ...prev.slice(0, 49)]);

      // Increment track play-count
      const updatedSongs = songs.map(s => s.id === song.id ? { ...s, playCount: s.playCount + 1 } : s);
      persistSongs(updatedSongs);
    } catch (err) {
      console.error("Audio src allocation error", err);
    }
  }, [playbackRate, volume, songs, permissions]);

  const pause = useCallback(() => {
    audioRef.current?.pause();
    setStatus('paused');
  }, []);

  const resume = useCallback(() => {
    if (!audioRef.current) return;
    initAudioEngine();
    
    if (audioContextRef.current?.state === 'suspended') {
      audioContextRef.current.resume();
    }

    audioRef.current.play()
      .then(() => setStatus('playing'))
      .catch(e => console.error(e));
  }, []);

  const next = useCallback(() => {
    if (queue.length > 0) {
      const nextSong = queue[0];
      setQueue(prev => prev.slice(1));
      play(nextSong);
    } else {
      // Loop back to first song or pick smart random track
      const randomIndex = Math.floor(Math.random() * songs.length);
      if (songs[randomIndex]) play(songs[randomIndex]);
    }
  }, [queue, play, songs]);

  const previous = useCallback(() => {
    if (history.length > 1) {
      const prevSong = history[1];
      setHistory(prev => prev.slice(1));
      play(prevSong);
    } else if (songs[0]) {
      play(songs[0]);
    }
  }, [history, play, songs]);

  const seek = useCallback((time: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = time;
      setProgress(time);
    }
  }, []);

  const changeVolume = useCallback((vol: number) => {
    const adjusted = Math.max(0, Math.min(1, vol));
    setVolume(adjusted);
    if (audioRef.current) audioRef.current.volume = adjusted;
    if (volumeNodeRef.current) volumeNodeRef.current.gain.value = adjusted;
  }, []);

  const changePlaybackRate = useCallback((rate: number) => {
    const adjusted = Math.max(0.25, Math.min(2.5, rate));
    setPlaybackRate(adjusted);
    if (audioRef.current) audioRef.current.playbackRate = adjusted;
  }, []);

  // Equalizer curves adjustments
  const setEqGain = useCallback((freq: number, gain: number) => {
    setEqGains(prev => {
      const updated = { ...prev, [freq]: gain };
      // Apply live onto Filter Nodes
      const index = FREQUENCY_BANDS.indexOf(freq);
      if (biquadFiltersRef.current[index]) {
        biquadFiltersRef.current[index].gain.value = gain;
      }
      return updated;
    });
  }, []);

  // Set bass boost filter
  useEffect(() => {
    if (bassFilterRef.current) {
      bassFilterRef.current.gain.value = bassBoost ? 12 : 0;
    }
  }, [bassBoost]);

  // Handle local audio file loading & metadata extraction
  const addLocalFiles = async (files: FileList) => {
    setIsScanning(true);
    const mm = await import('music-metadata-browser');
    const newSongs: Song[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (!file.type.startsWith('audio/')) continue;

      try {
        const meta = await mm.parseBlob(file);
        const common = meta.common;
        
        let coverArt: string | undefined;
        if (common.picture && common.picture.length > 0) {
          const pic = common.picture[0];
          coverArt = `data:${pic.format};base64,${window.btoa(
            pic.data.reduce((data, byte) => data + String.fromCharCode(byte), '')
          )}`;
        }

        const customSong: Song = {
          id: crypto.randomUUID(),
          title: common.title || file.name.replace(/\.[^/.]+$/, ""),
          artist: common.artist || "Unknown Artist",
          album: common.album || "Unknown Album",
          duration: meta.format.duration || 180,
          coverArt,
          file,
          addedAt: Date.now(),
          playCount: 0,
          isFavorite: false,
          genre: common.genre ? common.genre[0] : "Ambient Electronica",
          bpm: Math.floor(Math.random() * (135 - 80 + 1)) + 80,
          mood: ['Chill', 'Warm', 'Energetic', 'Dark'][Math.floor(Math.random() * 4)] as any,
          lyrics: `[0:00] Playback commenced\n[0:30] Pure high-definition waveform streaming\n[1:00] Perfect ambient fidelity...`
        };
        newSongs.push(customSong);
      } catch (err) {
        // Fallback for files without meta
        const customSong: Song = {
          id: crypto.randomUUID(),
          title: file.name.replace(/\.[^/.]+$/, ""),
          artist: "Offline Local Track",
          album: "Imported Files",
          duration: 200,
          file,
          addedAt: Date.now(),
          playCount: 0,
          isFavorite: false,
          bpm: 110,
          mood: 'CHILL' as any
        };
        newSongs.push(customSong);
      }
    }

    if (newSongs.length > 0) {
      const updated = [...songs, ...newSongs];
      persistSongs(updated);
      addXP(500 * newSongs.length);
    }
    setIsScanning(false);
  };

  const toggleFavorite = (songId: string) => {
    const updated = songs.map(s => s.id === songId ? { ...s, isFavorite: !s.isFavorite } : s);
    persistSongs(updated);
  };

  const deleteSong = (songId: string) => {
    const updated = songs.filter(s => s.id !== songId);
    persistSongs(updated);
  };

  const clearQueue = () => setQueue([]);
  const addToQueue = (song: Song) => setQueue(prev => [...prev, song]);

  // Gamification stats multiplier
  const addXP = (amount: number) => {
    const newXP = userStats.xp + amount;
    const computedLevel = Math.floor(Math.log10(newXP)) + 1; // exponential scaling
    const hasNewLevel = computedLevel > userStats.level;

    const stats: UserStats = {
      ...userStats,
      xp: newXP,
      level: Math.max(userStats.level, computedLevel)
    };
    persistStats(stats);
  };

  // Simulated request of Android permissions
  const requestPermission = async (type: 'media' | 'notifications' | 'battery') => {
    return new Promise<boolean>((resolve) => {
      setTimeout(() => {
        setPermissions(prev => {
          const updated = { ...prev, [type]: 'granted' as const };
          set('pulse_perms', updated);
          return updated;
        });
        resolve(true);
      }, 800);
    });
  };

  // AI-DJ generative assistant integration
  const triggerAIAssistant = async (text: string): Promise<string> => {
    setAiThinking(true);
    // Mimic extremely premium futuristic AI generator response
    return new Promise((resolve) => {
      setTimeout(() => {
        setAiThinking(false);
        const responses = [
          "Perfect! Generating a chill-ambient playlist matching your relaxed mood vibes.",
          "High energy levels detected. Activating Cyberpunk overclocked synth playlist now.",
          "Creating a special collection of Deep Minimal Tech with cosmic resonances just for you.",
          "BPM matched of 124. Custom mix configured to enhance focus and alpha brainwaves."
        ];
        resolve(responses[Math.floor(Math.random() * responses.length)]);
      }, 1500);
    });
  };

  return (
    <AudioContext.Provider value={{
      songs,
      currentSong,
      status,
      progress,
      duration,
      volume,
      playbackRate,
      queue,
      history,
      shuffle,
      repeat,
      setSongs,
      addLocalFiles,
      toggleFavorite,
      deleteSong,
      isScanning,
      play,
      pause,
      resume,
      next,
      previous,
      seek,
      setVolume: changeVolume,
      setPlaybackRate: changePlaybackRate,
      setShuffle,
      setRepeat,
      clearQueue,
      addToQueue,
      eqGains,
      setEqGain,
      bassBoost,
      setBassBoost,
      reverbAmount,
      setReverbAmount,
      spatialAudio,
      setSpatialAudio,
      analyserNode,
      premiumTheme,
      setPremiumTheme,
      userStats,
      addXP,
      triggerAIAssistant,
      aiThinking,
      permissions,
      requestPermission,
      bluetoothDevice,
      setBluetoothDevice
    }}>
      {children}
    </AudioContext.Provider>
  );
};

export const useAudio = () => {
  const context = useContext(AudioContext);
  if (!context) throw new Error('useAudio must be used within an AudioProvider');
  return context;
};
