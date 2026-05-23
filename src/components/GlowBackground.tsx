import React from 'react';
import { useAudio } from '../context/AudioContext';
import { cn } from '../lib/utils';

export const GlowBackground: React.FC = () => {
  const { premiumTheme } = useAudio();

  const getThemeBlobs = () => {
    switch (premiumTheme) {
      case 'cyberpunk':
        return (
          <>
            <div className="absolute -top-[10%] -left-[10%] w-[550px] h-[550px] rounded-full bg-cyan-500/10 blur-[130px] animate-blob-slow-1" />
            <div className="absolute -bottom-[10%] -right-[10%] w-[600px] h-[600px] rounded-full bg-pink-500/10 blur-[150px] animate-blob-slow-2" />
            <div className="absolute top-[40%] left-[30%] w-[350px] h-[350px] rounded-full bg-yellow-500/5 blur-[100px] animate-blob-slow-3" />
          </>
        );
      case 'celestial':
        return (
          <>
            <div className="absolute -top-[5%] -left-[5%] w-[600px] h-[600px] rounded-full bg-slate-300/5 blur-[140px] animate-blob-slow-1" />
            <div className="absolute -bottom-[5%] -right-[5%] w-[550px] h-[550px] rounded-full bg-zinc-400/5 blur-[130px] animate-blob-slow-2" />
            <div className="absolute top-[30%] left-[40%] w-[400px] h-[400px] rounded-full bg-sky-200/5 blur-[110px] animate-blob-slow-3" />
          </>
        );
      case 'neon-city':
        return (
          <>
            <div className="absolute -top-[10%] -left-[10%] w-[500px] h-[500px] rounded-full bg-emerald-500/10 blur-[120px] animate-blob-slow-1" />
            <div className="absolute -bottom-[10%] -right-[10%] w-[500px] h-[500px] rounded-full bg-cyan-400/10 blur-[120px] animate-blob-slow-2" />
            <div className="absolute top-[50%] left-[20%] w-[400px] h-[400px] rounded-full bg-lime-400/5 blur-[100px] animate-blob-slow-3" />
          </>
        );
      case 'void-energy':
        return (
          <>
            <div className="absolute -top-[15%] -left-[15%] w-[650px] h-[650px] rounded-full bg-violet-950/20 blur-[160px] animate-blob-slow-1" />
            <div className="absolute -bottom-[15%] -right-[15%] w-[650px] h-[650px] rounded-full bg-zinc-900/30 blur-[160px] animate-blob-slow-2" />
            <div className="absolute top-[35%] left-[25%] w-[450px] h-[450px] rounded-full bg-fuchsia-950/10 blur-[130px] animate-blob-slow-3" />
          </>
        );
      case 'aurora':
        return (
          <>
            <div className="absolute -top-[10%] -left-[5%] w-[700px] h-[500px] rounded-full bg-teal-500/10 blur-[140px] animate-blob-slow-1" />
            <div className="absolute -bottom-[10%] -right-[5%] w-[600px] h-[600px] rounded-full bg-indigo-500/10 blur-[140px] animate-blob-slow-2" />
            <div className="absolute top-[20%] left-[10%] w-[500px] h-[500px] rounded-full bg-emerald-500/5 blur-[130px] animate-blob-slow-3" />
          </>
        );
      case 'cosmic':
      default:
        return (
          <>
            <div className="absolute -top-[15%] -left-[15%] w-[600px] h-[600px] rounded-full bg-purple-600/10 blur-[140px] animate-blob-slow-1" />
            <div className="absolute -bottom-[15%] -right-[15%] w-[600px] h-[600px] rounded-full bg-indigo-600/10 blur-[140px] animate-blob-slow-2" />
            <div className="absolute top-[40%] left-[25%] w-[400px] h-[400px] rounded-full bg-fuchsia-600/5 blur-[120px] animate-blob-slow-3" />
          </>
        );
    }
  };

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none z-0 bg-[#030303]">
      {/* Slow translating blur layer spheres */}
      {getThemeBlobs()}

      {/* Atmospheric noise layer overlay for premium film grain looks */}
      <div className="absolute inset-0 bg-noise pointer-events-none opacity-[0.02] mix-blend-overlay" />
    </div>
  );
};
