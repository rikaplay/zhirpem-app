"use client";

import React from 'react';
import { X, Moon, Sun, Monitor, User, Shield, Zap } from 'lucide-react';

interface SettingsScreenProps {
  onClose: () => void;
  user: any;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ onClose, user }) => {
  return (
    <div className="fixed inset-0 z-[70] bg-background-light dark:bg-background-dark flex flex-col animate-in slide-in-from-right duration-300">
      {/* Header */}
      <div className="p-6 flex items-center justify-between border-b border-zinc-500/10">
        <div className="flex items-center gap-4">
            <button onClick={onClose} className="p-2 hover:bg-zinc-500/10 rounded-full btn-bounce">
                <X size={24} />
            </button>
            <h1 className="text-xl font-black uppercase tracking-tight">Настройки</h1>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-6 space-y-8">
        {/* Profile Section */}
        <section>
          <h2 className="text-zinc-400 text-xs font-black uppercase tracking-widest mb-4">Аккаунт</h2>
          <div className="glass p-4 rounded-[24px] flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-primary/10 flex items-center justify-center text-xl font-black text-primary">
                {user.name.charAt(0).toUpperCase()}
            </div>
            <div>
                <p className="font-bold text-lg">{user.name}</p>
                <p className="text-zinc-500 text-sm">@{user.id}</p>
            </div>
          </div>
        </section>

        {/* Appearance */}
        <section>
          <h2 className="text-zinc-400 text-xs font-black uppercase tracking-widest mb-4">Внешний вид</h2>
          <div className="glass rounded-[24px] overflow-hidden divide-y divide-white/5">
            <div className="p-4 flex items-center justify-between hover:bg-white/5 cursor-pointer transition-colors">
                <div className="flex items-center gap-3">
                    <Sun size={20} className="text-orange-500" />
                    <span className="font-bold">Тема оформления</span>
                </div>
                <span className="text-sm text-zinc-500 font-bold">Светлая</span>
            </div>
            <div className="p-4 flex items-center justify-between hover:bg-white/5 cursor-pointer transition-colors">
                <div className="flex items-center gap-3">
                    <Zap size={20} className="text-yellow-500" />
                    <span className="font-bold">Энергосбережение</span>
                </div>
                <span className="text-sm text-zinc-500 font-bold">Выкл</span>
            </div>
          </div>
        </section>

        {/* Privacy */}
        <section>
          <h2 className="text-zinc-400 text-xs font-black uppercase tracking-widest mb-4">Безопасность</h2>
          <div className="glass rounded-[24px] overflow-hidden">
            <div className="p-4 flex items-center justify-between hover:bg-white/5 cursor-pointer transition-colors">
                <div className="flex items-center gap-3">
                    <Shield size={20} className="text-primary" />
                    <span className="font-bold">Двухфакторная аутентификация</span>
                </div>
            </div>
          </div>
        </section>
      </div>

      <div className="p-8 text-center opacity-20">
        <p className="font-black text-sm uppercase tracking-tighter italic">Zhirpem Web Port v1.0.0</p>
      </div>
    </div>
  );
};
