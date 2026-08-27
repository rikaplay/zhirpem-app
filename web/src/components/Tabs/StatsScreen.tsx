"use client";

import React from 'react';
import { BarChart3, TrendingUp, Heart, Eye } from 'lucide-react';

export const StatsScreen = () => {
  return (
    <div className="min-h-screen px-4 pt-4 pb-32 space-y-6">
      <h2 className="text-2xl font-black uppercase tracking-tighter text-zinc-400 mb-6 uppercase tracking-tighter">Статистика 📈</h2>

      <div className="glass p-6 rounded-[32px] shadow-sm border-white/10">
        <h3 className="font-black text-lg mb-6 uppercase tracking-tight text-primary">Обзор активности</h3>
        <div className="grid grid-cols-2 gap-4">
            <div className="bg-zinc-100 dark:bg-zinc-800/40 p-5 rounded-[24px]">
                <Eye className="text-blue-500 mb-2" size={20} />
                <p className="text-2xl font-black">0</p>
                <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest">Просмотры</p>
            </div>
            <div className="bg-zinc-100 dark:bg-zinc-800/40 p-5 rounded-[24px]">
                <Heart className="text-pink-500 mb-2" size={20} />
                <p className="text-2xl font-black">0</p>
                <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest">Лайки</p>
            </div>
        </div>
      </div>

      <div className="glass p-6 rounded-[32px] shadow-sm border-white/10 text-center py-20 opacity-20">
        <TrendingUp size={48} className="mx-auto mb-4" />
        <p className="font-black uppercase tracking-widest text-sm">Недостаточно данных</p>
      </div>
    </div>
  );
};
