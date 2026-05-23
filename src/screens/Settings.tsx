import React from 'react';
import { motion } from 'motion/react';
import { 
  Palette, SlidersHorizontal, Shield, Smartphone, HelpCircle, 
  Bluetooth, User, Zap, Star, Sparkles
} from 'lucide-react';
import { useAudio } from '../context/AudioContext';
import { cn } from '../lib/utils';
import { PulseTheme } from '../types';

export const SettingsScreen: React.FC = () => {
  const { 
    premiumTheme, setPremiumTheme, userStats, bluetoothDevice, setBluetoothDevice,
    spatialAudio, setSpatialAudio
  } = useAudio();

  const themes: Array<{ id: PulseTheme; label: string; desc: string }> = [
    { id: 'cosmic', label: 'Cosmic Atmos', desc: 'Purple-blue majestic stellar glows' },
    { id: 'cyberpunk', label: 'Overclocked Cyber', desc: 'High-contrast neon cyan and pink tags' },
    { id: 'celestial', label: 'Lunar Spheres', desc: 'Minimal clean monochromic silver auras' },
    { id: 'neon-city', label: 'Emerald Neon', desc: 'Glow city green and deep cyan stripes' },
    { id: 'void-energy', label: 'Dark Void', desc: 'Deep ultraviolet void absolute black' },
    { id: 'aurora', label: 'Aurora Emerald', desc: 'Northern lights fluid green waves' }
  ];

  const handleDeviceToggle = () => {
    if (bluetoothDevice) {
      setBluetoothDevice(null);
    } else {
      setBluetoothDevice("Pulse Ear Pro");
    }
  };

  return (
    <div className="min-h-screen pb-44 pt-10 px-6 space-y-9 overflow-y-auto no-scrollbar relative">
      <div className="space-y-1">
        <p className="text-[10px] font-bold uppercase tracking-[0.4em] text-white/30">System Configuration</p>
        <h1 className="text-3xl font-light tracking-tight text-white font-display">Audio Engine</h1>
      </div>

      {/* Visual Themes Selector grid */}
      <section className="space-y-4">
        <h3 className="text-[10px] font-extrabold uppercase tracking-[0.25em] text-white/40 flex items-center gap-2">
          <Palette size={12} />
          Pulse Theme Realms
        </h3>

        <div className="grid grid-cols-2 gap-3">
          {themes.map((t) => {
            const isActive = premiumTheme === t.id;
            return (
              <button
                key={t.id}
                onClick={() => setPremiumTheme(t.id)}
                className={cn(
                  "p-4 rounded-2xl text-left border cursor-pointer relative overflow-hidden transition-all duration-300",
                  isActive 
                    ? "bg-white text-black border-white shadow-lg font-bold" 
                    : "bg-white/3 border-white/5 hover:bg-white/6 text-white"
                )}
              >
                <h4 className="text-xs font-semibold uppercase tracking-wider">{t.label}</h4>
                <p className={cn("text-[9px] leading-snug mt-1", isActive ? "text-black/60" : "text-white/40")}>
                  {t.desc}
                </p>
              </button>
            );
          })}
        </div>
      </section>

      {/* Sound Stream Parameters list */}
      <section className="space-y-4">
        <h3 className="text-[10px] font-extrabold uppercase tracking-[0.25em] text-white/40 flex items-center gap-2">
          <SlidersHorizontal size={12} />
          DSP Streaming parameters
        </h3>

        <div className="glass rounded-3xl overflow-hidden divide-y divide-white/5">
          <div className="p-4 flex items-center justify-between">
            <div className="space-y-0.5">
              <h4 className="text-xs font-semibold text-white">Spatial Soundstage Stage II</h4>
              <p className="text-[9px] text-white/40 leading-snug">Pre-renders customized 3D binaural ambiance simulation.</p>
            </div>
            <button 
              onClick={() => setSpatialAudio(!spatialAudio)}
              className={cn(
                "w-10 h-5.5 rounded-full p-0.5 transition-colors cursor-pointer",
                spatialAudio ? "bg-white" : "bg-white/10"
              )}
            >
              <div className={cn("w-4.5 h-4.5 rounded-full transition-transform", spatialAudio ? "transform translate-x-[18px] bg-black" : "bg-white/60")} />
            </button>
          </div>

          <div className="p-4 flex items-center justify-between">
            <div className="space-y-0.5">
              <h4 className="text-xs font-semibold text-white">Losslessupsampling Core</h4>
              <p className="text-[9px] text-white/40 leading-snug">Upsamples local audio files to high-definition 24bit/96kHz.</p>
            </div>
            <span className="text-[9px] font-bold text-cyan-400 uppercase tracking-widest bg-cyan-400/10 px-2 py-1 rounded-md">
              lossless flac
            </span>
          </div>

          <div className="p-4 flex items-center justify-between">
            <div className="space-y-0.5">
              <h4 className="text-xs font-semibold text-white">Wireless Headsets Link</h4>
              <p className="text-[9px] text-white/40 leading-snug">{bluetoothDevice ? `Linked to ${bluetoothDevice}` : 'No wireless headset paired'}</p>
            </div>
            <button
              onClick={handleDeviceToggle}
              className="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[9px] font-bold uppercase tracking-widest cursor-pointer text-white"
            >
              {bluetoothDevice ? 'Disconnect' : 'Connect'}
            </button>
          </div>
        </div>
      </section>

      {/* Unlocked Achievements Stats display list */}
      <section className="space-y-4">
        <h3 className="text-[10px] font-extrabold uppercase tracking-[0.25em] text-white/40 flex items-center gap-2">
          <Star size={12} />
          Digital Achievements Credentials
        </h3>

        <div className="space-y-3">
          {userStats.achievements.map((a) => (
            <div 
              key={a.id}
              className="p-4 rounded-2xl glass-card border border-white/5 flex items-start gap-4"
            >
              <span className="text-2xl pt-0.5">{a.icon}</span>
              <div className="space-y-1 flex-1 min-w-0">
                <div className="flex justify-between items-center">
                  <h4 className="text-xs font-semibold text-white">{a.title}</h4>
                  <span className="text-[9px] text-yellow-500 font-bold uppercase tracking-widest">+{a.xpReward} xp</span>
                </div>
                <p className="text-[9px] text-white/40 leading-relaxed">{a.description}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Technical Credits footer */}
      <footer className="text-center pt-4 pb-8 space-y-2">
        <p className="text-[8px] font-bold uppercase tracking-[0.6em] text-white/10">Pulse Audio Architecture v1.0.8</p>
        <p className="text-[9px] text-white/20 select-text">Designed under Vision OS glass reflections specification.</p>
      </footer>
    </div>
  );
};
