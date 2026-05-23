export interface Song {
  id: string;
  title: string;
  artist: string;
  album?: string;
  duration: number; // in seconds
  coverArt?: string; // data URL or path
  file?: File; // optional, exists if locally uploaded or scanned
  demoUrl?: string; // fallback playable audio
  addedAt: number;
  playCount: number;
  isFavorite: boolean;
  genre?: string;
  bpm?: number;
  mood?: 'Energetic' | 'Chill' | 'Dark' | 'Warm' | 'Melancholic';
  lyrics?: string; // Synthesized / synced lyrics metadata
  syncedLyrics?: Array<{ time: number; text: string }>;
}

export type PlaybackStatus = 'playing' | 'paused' | 'stopped';

export type PulseTheme = 'cosmic' | 'cyberpunk' | 'celestial' | 'neon-city' | 'void-energy' | 'aurora';

export interface UserStats {
  listeningStreak: number;
  xp: number;
  level: number;
  unlockedThemes: string[];
  achievements: Achievement[];
  dailyQuestCompleted: boolean;
}

export interface Achievement {
  id: string;
  title: string;
  description: string;
  unlockedAt?: number;
  icon: string;
  xpReward: number;
}

export interface Playlist {
  id: string;
  name: string;
  description: string;
  coverArt?: string;
  songs: string[]; // Song IDs
  createdAt: number;
  isAiGenerated?: boolean;
}

export interface EqualizerSetting {
  frequency: number; // in Hz
  gain: number; // in dB
}
