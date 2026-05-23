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

// Generate transparent core for local play
export const DEMO_SONGS: Song[] = [];

export const ALL_ACHIEVEMENTS: Achievement[] = [
  {
    id: 'streak-3',
    title: 'Rhythm Devotee',
    description: 'Listen to Pulse Player for 3 days in a row.',
    icon: '🔥',
    xpReward: 1500
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
