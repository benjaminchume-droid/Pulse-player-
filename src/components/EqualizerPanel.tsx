import React from 'react';
import { useAudio, FREQUENCY_BANDS } from '../context/AudioContext';
import { cn } from '../lib/utils';
import { SlidersHorizontal, Settings2 } from 'lucide-react';

export const EqualizerPanel: React.FC = () => {
  const { 
    eqGains, setEqGain, bassBoost, setBassBoost, spatialAudio, setSpatialAudio 
  } = useAudio();

  const presets = [
    { name: 'Balanced', gains: { 31: 0, 62: 0, 125: 0, 250: 0, 500: 0, 1000: 0, 2000: 0, 4000: 0, 8000: 0, 16000: 0 } },
    { name: 'Sub-bass Extractor', gains: { 31: 8, 62: 6, 125: 4, 250: 0, 500: 0, 1000: 0, 2000: 2, 4000: 0, 8000: 0, 16000: 0 } },
    { name: 'Electro Stadium', gains: { 31: 6, 62: 5, 125: -2, 250: 0, 500: 1, 1000: 3, 2000: 4, 4000: 2, 8000: 5, 16000: 3 } },
    { name: 'Vocal Presence', gains: { 31: -3, 62: -2, 125: 0, 250: 2, 500: 4, 1000: 6, 2000: 5, 4000: 3, 8000: 1, 16000: 0 } }
  ];

  const applyPreset = (gains: Record<number, number>) => {
    FREQUENCY_BANDS.forEach(freq => {
      setEqGain(freq, gains[freq] || 0);
    });
  };

  return (
    <div className="glass rounded-3xl p-6 border border-white/5 space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <SlidersHorizontal size={18} className="text-white/60" />
          <h3 className="text-sm font-bold uppercase tracking-[0.2em] text-white">10-Band Equalizer</h3>
        </div>
        <span className="text-[9px] font-bold text-white/30 uppercase tracking-[0.2em]">DSP Active</span>
      </div>

      {/* EQ Preset Chips */}
      <div className="flex gap-2 overflow-x-auto no-scrollbar py-1">
        {presets.map(p => (
          <button
            key={p.name}
            onClick={() => applyPreset(p.gains)}
            className="px-3 py-1.5 rounded-full bg-white/5 hover:bg-white/10 text-[9px] font-bold uppercase tracking-widest text-white/60 hover:text-white border border-white/5 whitespace-nowrap cursor-pointer transition-all"
          >
            {p.name}
          </button>
        ))}
      </div>

      {/* 10 vertical sliders represented cleanly */}
      <div className="grid grid-cols-10 h-32 items-center gap-1.5 pt-4">
        {FREQUENCY_BANDS.map(freq => {
          const gain = eqGains[freq] || 0;
          return (
            <div key={freq} className="flex flex-col items-center h-full justify-between">
              {/* dB Label */}
              <span className="text-[7px] font-mono text-white/30">{gain > 0 ? `+${gain}` : gain}</span>
              
              {/* slider track vertical representation */}
              <div className="relative h-20 w-1 bg-white/10 rounded-full flex items-center justify-center">
                <input
                  type="range"
                  min={-12}
                  max={12}
                  step={1}
                  value={gain}
                  onChange={(e) => setEqGain(freq, Number(e.target.value))}
                  className="absolute inset-0 w-full h-full opacity-0 cursor-pointer [-webkit-appearance:slider-vertical]"
                  style={{ transform: 'rotate(0deg)' }}
                />
                <div 
                  className="absolute bottom-0 w-full bg-white rounded-full" 
                  style={{ height: `${((gain + 12) / 24) * 100}%` }}
                />
              </div>

              {/* Freq Label */}
              <span className="text-[7px] font-mono text-white/40 mt-1">
                {freq >= 1000 ? `${freq / 1000}k` : freq}
              </span>
            </div>
          );
        })}
      </div>

      {/* Spatializer & Hyper-bass switches */}
      <div className="grid grid-cols-2 gap-3 pt-2">
        <button
          onClick={() => setBassBoost(!bassBoost)}
          className={cn(
            "p-3 rounded-2xl flex flex-col gap-1 text-left transition-all cursor-pointer border",
            bassBoost ? "bg-white text-black border-white" : "bg-white/5 border-white/5"
          )}
        >
          <span className="text-[9px] font-bold uppercase tracking-wider opacity-60">Overdrive Bass</span>
          <span className="text-xs font-semibold">{bassBoost ? 'Active +12dB' : 'Disabled'}</span>
        </button>

        <button
          onClick={() => setSpatialAudio(!spatialAudio)}
          className={cn(
            "p-3 rounded-2xl flex flex-col gap-1 text-left transition-all cursor-pointer border",
            spatialAudio ? "bg-white text-black border-white" : "bg-white/5 border-white/5"
          )}
        >
          <span className="text-[9px] font-bold uppercase tracking-wider opacity-60">Spatial stage</span>
          <span className="text-xs font-semibold">{spatialAudio ? 'Atmosphere Pro' : 'Stereo Normal'}</span>
        </button>
      </div>
    </div>
  );
};
