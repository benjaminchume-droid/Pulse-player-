import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ShieldAlert, Image, BellRing, Eye, Settings } from 'lucide-react';
import { useAudio } from '../context/AudioContext';

export const PermissionDialog: React.FC = () => {
  const { permissions, requestPermission } = useAudio();
  const [show, setShow] = useState(() => {
    return permissions.media === 'prompt' || permissions.notifications === 'prompt';
  });

  const handleGrant = async (type: 'media' | 'notifications' | 'battery') => {
    await requestPermission(type);
    if (permissions.media === 'granted' && permissions.notifications === 'granted') {
      setShow(false);
    }
  };

  if (!show) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 bg-black/60 backdrop-blur-xl z-[100] flex items-center justify-center p-6 select-none">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 30 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="glass rounded-3xl p-6 max-w-sm w-full border border-white/10 space-y-6 text-center shadow-2xl relative"
        >
          {/* Neon Pulse Accent */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 w-16 h-16 bg-white/10 rounded-full flex items-center justify-center border border-white/10 shadow-lg backdrop-blur-md">
            <ShieldAlert size={26} className="text-white" />
          </div>

          <div className="space-y-2 pt-6">
            <h2 className="text-xl font-light tracking-tight font-display text-white">Android System Permissions</h2>
            <p className="text-xs text-white/40 leading-relaxed">
              Pulse Player needs permissions to index local Hi-Res FLAC tracks and deliver background media overlay controls.
            </p>
          </div>

          <div className="space-y-3">
            {/* Storage Permission */}
            {permissions.media === 'prompt' && (
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 text-left flex items-start gap-3.5">
                <div className="p-2.5 rounded-xl bg-white/5 border border-white/5 text-white/60">
                  <Image size={18} />
                </div>
                <div className="flex-1 space-y-1 min-w-0">
                  <h4 className="text-xs font-bold text-white uppercase tracking-wider">Local Audio Storage</h4>
                  <p className="text-[10px] text-white/40 leading-snug">Read media audio records from folders.</p>
                  <button 
                    onClick={() => handleGrant('media')}
                    className="mt-2 text-[10px] font-bold text-white hover:text-white/80 uppercase tracking-widest cursor-pointer"
                  >
                    Grant Access &rarr;
                  </button>
                </div>
              </div>
            )}

            {/* Notifications Permission */}
            {permissions.notifications === 'prompt' && (
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 text-left flex items-start gap-3.5">
                <div className="p-2.5 rounded-xl bg-white/5 border border-white/5 text-white/60">
                  <BellRing size={18} />
                </div>
                <div className="flex-1 space-y-1 min-w-0">
                  <h4 className="text-xs font-bold text-white uppercase tracking-wider">Hi-Fi Background Playback</h4>
                  <p className="text-[10px] text-white/40 leading-snug">Display active player notification sliders.</p>
                  <button 
                    onClick={() => handleGrant('notifications')}
                    className="mt-2 text-[10px] font-bold text-white hover:text-white/80 uppercase tracking-widest cursor-pointer"
                  >
                    Grant Access &rarr;
                  </button>
                </div>
              </div>
            )}
          </div>

          <div className="flex gap-3 pt-2">
            <button 
              onClick={() => setShow(false)}
              className="flex-1 py-3.5 rounded-2xl bg-white/5 hover:bg-white/10 text-[10px] font-bold uppercase tracking-widest hover:scale-103 active:scale-95 transition-all text-white/40 hover:text-white"
            >
              Configure Later
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
