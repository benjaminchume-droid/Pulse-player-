import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { Song, Achievement } from '../types';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDuration(seconds: number): string {
  if (isNaN(seconds) || seconds < 0) return '0:00';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

// Generate premium demo songs to give immediate beautiful fidelity
export const DEMO_SONGS: Song[] = [
  {
    id: 'demo-1',
    title: 'Eternal Recurrence',
    artist: 'The Midnight Architect',
    album: 'Cosmic Arch',
    duration: 215,
    coverArt: 'https://images.unsplash.com/photo-1614680376593-902f74fa0d41?q=80&w=300&auto=format&fit=crop',
    demoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
    addedAt: Date.now() - 86400000 * 3,
    playCount: 42,
    isFavorite: true,
    genre: 'Liquid Ambient',
    bpm: 112,
    mood: 'Chill',
    lyrics: `[0:00] Floating inside the cosmic structure\n[0:15] Lost in reflections of time\n[0:30] We find our path inside the dark matter\n[0:45] Pure signal in cosmic light\n[1:10] The sound is returning, endless loops\n[1:40] Restoring ancient fragments of the star\n[2:10] Eternal recurrence`,
    syncedLyrics: [
      { time: 0, text: "Floating inside the cosmic structure" },
      { time: 15, text: "Lost in reflections of time" },
      { time: 30, text: "We find our path inside the dark matter" },
      { time: 45, text: "Pure signal in cosmic light" },
      { time: 70, text: "The sound is returning, endless loops" },
      { time: 100, text: "Restoring ancient fragments of the star" },
      { time: 130, text: "Eternal recurrence" }
    ]
  },
  {
    id: 'demo-2',
    title: 'Ghost in the Shell',
    artist: 'Kenji Kawai / Pulse Remix',
    album: 'Future Shells',
    duration: 184,
    coverArt: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=300&auto=format&fit=crop',
    demoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
    addedAt: Date.now() - 86400000 * 5,
    playCount: 89,
    isFavorite: false,
    genre: 'Cyberpunk Industrial',
    bpm: 135,
    mood: 'Energetic',
    lyrics: `[0:00] Digital consciousness awakened\n[0:12] High frequencies pulsing inside our blood\n[0:30] Synthesized ghosts seeking liberty\n[1:00] Cybernetic heartbeat\n[1:30] Pure digital transcendence`,
    syncedLyrics: [
      { time: 0, text: "Digital consciousness awakened" },
      { time: 12, text: "High frequencies pulsing inside our blood" },
      { time: 30, text: "Synthesized ghosts seeking liberty" },
      { time: 60, text: "Cybernetic heartbeat" },
      { time: 90, text: "Pure digital transcendence" }
    ]
  },
  {
    id: 'demo-3',
    title: 'Digital Ocean Vibrations',
    artist: 'Aeon Beats',
    album: 'Neptune Code',
    duration: 254,
    coverArt: 'https://images.unsplash.com/photo-1541701494587-cb58502866ab?q=80&w=300&auto=format&fit=crop',
    demoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',
    addedAt: Date.now() - 86400000 * 10,
    playCount: 154,
    isFavorite: true,
    genre: 'Minimal Synthwave',
    bpm: 120,
    mood: 'Warm',
    lyrics: `[0:00] Dive deep into deep cyan depths\n[0:20] Water glows under the synthetic sun\n[0:50] Bioluminescent signals calling us\n[1:20] Echoes of the oceanic sub-bass\n[1:50] Waves of future memories`,
    syncedLyrics: [
      { time: 0, text: "Dive deep into deep cyan depths" },
      { time: 20, text: "Water glows under the synthetic sun" },
      { time: 50, text: "Bioluminescent signals calling us" },
      { time: 80, text: "Echoes of the oceanic sub-bass" },
      { time: 110, text: "Waves of future memories" }
    ]
  },
  {
    id: 'demo-4',
    title: 'Neon Void Echoes',
    artist: 'Vektor Space',
    album: 'Black Mirror',
    duration: 212,
    coverArt: 'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?q=80&w=300&auto=format&fit=crop',
    demoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',
    addedAt: Date.now() - 86400000 * 15,
    playCount: 22,
    isFavorite: false,
    genre: 'Darkwave Synth',
    bpm: 98,
    mood: 'Dark',
    lyrics: `[0:00] Darkness calling, wrapped in neon wires\n[0:15] Moving silent past the tower lights\n[0:40] Cold light scanning our electronic grid\n[1:10] In the sector of the sleeping bots\n[1:40] Resonance inside the digital void`,
    syncedLyrics: [
      { time: 0, text: "Darkness calling, wrapped in neon wires" },
      { time: 15, text: "Moving silent past the tower lights" },
      { time: 40, text: "Cold light scanning our electronic grid" },
      { time: 70, text: "In the sector of the sleeping bots" },
      { time: 100, text: "Resonance inside the digital void" }
    ]
  }
];

export const ALL_ACHIEVEMENTS: Achievement[] = [
  {
    id: 'streak-3',
    title: 'Rhythm Devotee',
    description: 'Listen to Pulse Player for 3 days in a row.',
    icon: '🔥',
    xpReward: 1500,
    unlockedAt: Date.now()
  },
  {
    id: 'audiophile',
    title: 'Lossless Master',
    description: 'Import custom FLAC or high-res audio tracks into Library.',
    icon: '🎧',
    xpReward: 2500
  },
  {
    id: 'eq-sculptor',
    title: 'Sound Sculptor',
    description: 'Design a custom Equalizer curve adjustment.',
    icon: '🎚️',
    xpReward: 1000
  },
  {
    id: 'cosmic-unlock',
    title: 'Space Explorer',
    description: 'Unlock the Celestial theme through continuous XP gain.',
    icon: '🌌',
    xpReward: 3000
  }
];
